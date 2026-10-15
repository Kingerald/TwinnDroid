package com.twinndroid.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.twinndroid.CloneOperations
import com.twinndroid.data.CloneConfig
import com.twinndroid.data.CloneRepository
import com.twinndroid.data.IconMode
import com.twinndroid.data.Settings
import com.twinndroid.data.SourceKind
import com.twinndroid.engine.ApkSources
import com.twinndroid.engine.IconFactory
import com.twinndroid.engine.SourceApks
import com.twinndroid.install.InstallResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

enum class Screen { Home, PickApp, Editor, Settings, Appearance }

data class AppEntry(val packageName: String, val label: String, val icon: ImageBitmap?, val isSystem: Boolean)

data class CloneItem(
    val config: CloneConfig,
    val icon: ImageBitmap?,
    val installedVersion: Long?,
    val updateAvailable: Boolean,
)

data class EditorState(
    val config: CloneConfig,
    val isNew: Boolean,
    val sourceIcon: Drawable?,
    val sourceVersion: String?,
    val permissions: List<String>,
    val preview: ImageBitmap?,
    val packageError: String? = null,
)

data class BuildState(
    val clonePackage: String,
    val running: Boolean,
    val logs: List<String>,
    val result: InstallResult? = null,
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx get() = getApplication<Application>()
    private val repo = CloneRepository.get(app)
    val settings = Settings(app)

    var screen by mutableStateOf(Screen.Home)
    var clones by mutableStateOf<List<CloneItem>>(emptyList())
        private set
    var apps by mutableStateOf<List<AppEntry>?>(null)
        private set
    var editor by mutableStateOf<EditorState?>(null)
        private set
    var build by mutableStateOf<BuildState?>(null)
        private set
    var message by mutableStateOf<String?>(null)
    var busy by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch { repo.clones.collect { refreshClones() } }
    }

    fun refreshClones() = viewModelScope.launch {
        clones = withContext(Dispatchers.IO) {
            val outdated = CloneOperations.outdated(ctx).map { it.id }.toSet()
            repo.clones.value.sortedByDescending { it.createdAt }.map { c ->
                val installed = CloneOperations.installedVersion(ctx, c.clonePackage)
                val icon = runCatching {
                    if (installed != null) ctx.packageManager.getApplicationIcon(c.clonePackage)
                    else ctx.packageManager.getApplicationIcon(c.sourcePackage)
                }.getOrNull()
                CloneItem(c, icon?.toBitmap(128, 128)?.asImageBitmap(), installed, c.id in outdated)
            }
        }
    }

    fun loadApps(force: Boolean = false) {
        if (apps != null && !force) return
        viewModelScope.launch {
            apps = withContext(Dispatchers.IO) {
                val pm = ctx.packageManager
                pm.getInstalledApplications(0)
                    .asSequence()
                    .filter { it.packageName != ctx.packageName }
                    .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                    .map { ai ->
                        AppEntry(
                            packageName = ai.packageName,
                            label = ai.loadLabel(pm).toString(),
                            icon = runCatching { ai.loadIcon(pm).toBitmap(96, 96).asImageBitmap() }.getOrNull(),
                            isSystem = ai.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                        )
                    }
                    .sortedBy { it.label.lowercase() }
                    .toList()
            }
        }
    }

    // ---- editor -----------------------------------------------------------------------------

    fun newFromInstalled(packageName: String) = launchBusy {
        val source = withContext(Dispatchers.IO) { ApkSources.installed(ctx, packageName) }
        openEditor(newConfig(UUID.randomUUID().toString(), SourceKind.INSTALLED, source), source, isNew = true)
    }

    fun newFromFile(uri: Uri) = launchBusy {
        val id = UUID.randomUUID().toString()
        val source = withContext(Dispatchers.IO) {
            val dir = ApkSources.sourceDir(ctx, id)
            try {
                ApkSources.import(ctx, uri, dir)
                ApkSources.stored(ctx, dir)
            } catch (t: Throwable) {
                dir.deleteRecursively()
                throw t
            }
        }
        openEditor(newConfig(id, SourceKind.FILE, source), source, isNew = true)
    }

    fun edit(config: CloneConfig) = launchBusy {
        val source = withContext(Dispatchers.IO) { CloneOperations.resolveSource(ctx, config) }
        openEditor(config, source, isNew = false)
    }

    private fun newConfig(id: String, kind: SourceKind, source: SourceApks): CloneConfig {
        val existing = repo.clones.value.filter { it.sourcePackage == source.packageName }
        var n = existing.size + 2
        val usedPackages = repo.clones.value.map { it.clonePackage }.toSet()
        while ("${source.packageName}.twinn$n" in usedPackages ||
            CloneOperations.installedVersion(ctx, "${source.packageName}.twinn$n") != null
        ) n++
        return CloneConfig(
            id = id,
            sourceKind = kind,
            sourcePackage = source.packageName,
            sourceLabel = source.label,
            clonePackage = "${source.packageName}.twinn$n",
            label = "${source.label} $n",
            badgeText = n.toString(),
        )
    }

    private fun openEditor(config: CloneConfig, source: SourceApks, isNew: Boolean) {
        editor = EditorState(
            config = config,
            isNew = isNew,
            sourceIcon = source.icon,
            sourceVersion = source.versionName,
            permissions = source.requestedPermissions.distinct().sorted(),
            preview = preview(config, source.icon),
        )
        screen = Screen.Editor
    }

    private fun preview(config: CloneConfig, icon: Drawable?): ImageBitmap? =
        (IconFactory.render(config, icon) ?: icon?.toBitmap(IconFactory.SIZE, IconFactory.SIZE))?.asImageBitmap()

    fun updateConfig(transform: (CloneConfig) -> CloneConfig) {
        val e = editor ?: return
        val c = transform(e.config)
        val iconChanged = c.iconMode != e.config.iconMode || c.badgeColor != e.config.badgeColor ||
            c.badgeText != e.config.badgeText || c.customIconPath != e.config.customIconPath
        editor = e.copy(
            config = c,
            preview = if (iconChanged) preview(c, e.sourceIcon) else e.preview,
            packageError = null,
        )
    }

    fun setCustomIcon(uri: Uri) = launchBusy {
        val e = editor ?: return@launchBusy
        val file = withContext(Dispatchers.IO) {
            File(ctx.filesDir, "icons").mkdirs()
            File(ctx.filesDir, "icons/${e.config.id}-${System.currentTimeMillis()}.img").also { f ->
                ctx.contentResolver.openInputStream(uri)!!.use { i -> f.outputStream().use { i.copyTo(it) } }
            }
        }
        updateConfig { it.copy(iconMode = IconMode.CUSTOM, customIconPath = file.path) }
    }

    fun closeEditor() {
        val e = editor
        if (e != null && e.isNew && e.config.sourceKind == SourceKind.FILE) {
            ApkSources.sourceDir(ctx, e.config.id).deleteRecursively()
        }
        editor = null
        screen = Screen.Home
    }

    fun saveAndBuild() {
        val e = editor ?: return
        val c = e.config.copy(label = e.config.label.trim(), clonePackage = e.config.clonePackage.trim())
        val err = when {
            !PACKAGE_REGEX.matches(c.clonePackage) -> "Not a valid package name"
            c.clonePackage == c.sourcePackage -> "Must differ from the original package"
            repo.clones.value.any { it.id != c.id && it.clonePackage == c.clonePackage } -> "Another clone already uses this package"
            c.label.isEmpty() -> "Name cannot be empty"
            else -> null
        }
        if (err != null) {
            editor = e.copy(packageError = err)
            return
        }
        repo.upsert(c)
        editor = null
        screen = Screen.Home
        runBuild(c)
    }

    fun rebuild(config: CloneConfig) = runBuild(config)

    private fun runBuild(config: CloneConfig) {
        if (build?.running == true) return
        build = BuildState(config.clonePackage, running = true, logs = listOf("Building ${config.label}…"))
        viewModelScope.launch {
            val result = try {
                CloneOperations.buildAndInstall(ctx, config) { line ->
                    viewModelScope.launch(Dispatchers.Main) {
                        build?.let { build = it.copy(logs = it.logs + line) }
                    }
                }
            } catch (t: Throwable) {
                InstallResult(false, t.message ?: t.javaClass.simpleName).also {
                    build?.let { b -> build = b.copy(logs = b.logs + "✗ ${it.message}") }
                }
            }
            build?.let { build = it.copy(running = false, result = result) }
            refreshClones()
        }
    }

    fun dismissBuild() {
        build = null
    }

    fun delete(config: CloneConfig) {
        repo.remove(config.id)
        ApkSources.sourceDir(ctx, config.id).deleteRecursively()
        config.customIconPath?.let { File(it).delete() }
    }

    fun launchIntent(packageName: String): Intent? = ctx.packageManager.getLaunchIntentForPackage(packageName)

    private fun launchBusy(block: suspend () -> Unit) = viewModelScope.launch {
        busy = true
        try {
            block()
        } catch (t: Throwable) {
            message = t.message ?: t.javaClass.simpleName
        } finally {
            busy = false
        }
    }

    companion object {
        val PACKAGE_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    }
}
