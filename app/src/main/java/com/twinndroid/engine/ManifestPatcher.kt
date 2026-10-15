package com.twinndroid.engine

import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.arsc.value.ValueType

/**
 * Rewrites a binary AndroidManifest so the app can be installed side by side with the original,
 * the same way App Cloner does it: new package name, unique provider authorities and custom
 * permissions, no shared user id. Attributes are matched by resource id so obfuscated
 * manifests (stripped attribute names) still work.
 */
object ManifestPatcher {

    private val COMPONENT_TAGS = setOf("application", "activity", "activity-alias", "service", "receiver", "provider")
    private val PERMISSION_DECL_TAGS = setOf("permission", "permission-group", "permission-tree")
    private val USES_PERMISSION_TAGS = setOf("uses-permission", "uses-permission-sdk-23", "uses-permission-sdk-m")

    /** Attributes holding class names that may be written relative to the package (".Foo"). */
    private val CLASS_NAME_ATTRS = intArrayOf(
        android.R.attr.name,
        android.R.attr.targetActivity,
        android.R.attr.manageSpaceActivity,
        android.R.attr.backupAgent,
        android.R.attr.parentActivityName,
        android.R.attr.appComponentFactory,
    )

    private fun ResXmlElement.attr(id: Int): ResXmlAttribute? = searchAttributeByResourceId(id)

    private fun ResXmlAttribute.str(): String? =
        if (valueType == ValueType.STRING) valueAsString else null

    private fun ResXmlAttribute.setReference(resId: Int) {
        valueType = ValueType.REFERENCE
        data = resId
    }

    private fun ResXmlElement.isInsideQueries(): Boolean =
        getParentElementsWithSelf().asSequence().any { it.name == "queries" }

    private fun AndroidManifestBlock.allElements(): List<ResXmlElement> {
        val root = manifestElement ?: return emptyList()
        return (sequenceOf(root) + root.recursiveElements().asSequence()).distinct().toList()
    }

    /**
     * Renames [oldPkg] to [newPkg]. Must run on every APK of a split set (base and splits),
     * since each split manifest carries the package name too.
     */
    fun renamePackage(manifest: AndroidManifestBlock, oldPkg: String, newPkg: String) {
        val root = manifest.manifestElement ?: error("APK has no <manifest> element")

        // 1. Make relative class names absolute while the old package is still known.
        for (el in manifest.allElements()) {
            if (el.name !in COMPONENT_TAGS || el.isInsideQueries()) continue
            for (id in CLASS_NAME_ATTRS) {
                val a = el.attr(id) ?: continue
                val v = a.str() ?: continue
                when {
                    v.startsWith(".") -> a.setValueAsString(oldPkg + v)
                    !v.contains('.') -> a.setValueAsString("$oldPkg.$v")
                }
            }
        }

        // 2. The package itself.
        manifest.packageName = newPkg

        // 3. A clone can never join the original's shared uid (different signature).
        root.removeAttributesWithId(android.R.attr.sharedUserId)
        root.removeAttributesWithId(android.R.attr.sharedUserLabel)
        root.removeAttributesWithName("sharedUserMaxSdkVersion")
        root.removeElementsIf { it.name == "instrumentation" }

        val elements = manifest.allElements().filterNot { it.isInsideQueries() }

        // 4. Declared permissions must be unique device-wide -> rename them, then every reference.
        val permMap = HashMap<String, String>()
        for (el in elements) {
            if (el.name !in PERMISSION_DECL_TAGS) continue
            val v = el.attr(android.R.attr.name)?.str() ?: continue
            permMap[v] = scoped(v, oldPkg, newPkg)
        }
        if (permMap.isNotEmpty()) {
            for (el in elements) {
                for (a in el.attributes.asSequence()) {
                    val v = a.str() ?: continue
                    permMap[v]?.let { a.setValueAsString(it) }
                }
            }
        }

        // 5. Provider authorities must be unique device-wide.
        for (el in elements) {
            if (el.name != "provider") continue
            val a = el.attr(android.R.attr.authorities) ?: continue
            val v = a.str() ?: continue
            a.setValueAsString(v.split(';').joinToString(";") { scoped(it.trim(), oldPkg, newPkg) })
        }

        // 6. Keep clone tasks separate from the original's tasks in Recents.
        for (el in elements) {
            val a = el.attr(android.R.attr.taskAffinity) ?: continue
            val v = a.str() ?: continue
            if (v.startsWith(oldPkg)) a.setValueAsString(newPkg + v.removePrefix(oldPkg))
        }
    }

    private fun scoped(value: String, oldPkg: String, newPkg: String): String =
        if (value.contains(oldPkg)) value.replace(oldPkg, newPkg) else "$newPkg.$value"

    /** Activities / aliases that show up in the launcher. */
    private fun launcherComponents(manifest: AndroidManifestBlock): List<ResXmlElement> =
        manifest.allElements().filter { el ->
            (el.name == "activity" || el.name == "activity-alias") &&
                el.recursiveElements().asSequence().any { child ->
                    child.name == "category" &&
                        child.attr(android.R.attr.name)?.str() == "android.intent.category.LAUNCHER"
                }
        }

    fun setLabel(manifest: AndroidManifestBlock, label: String) {
        manifest.setApplicationLabel(label)
        for (el in launcherComponents(manifest)) {
            el.getOrCreateAndroidAttribute("label", android.R.attr.label).setValueAsString(label)
        }
    }

    fun setIcon(manifest: AndroidManifestBlock, resId: Int) {
        manifest.iconResourceId = resId
        manifest.roundIconResourceId = resId
        for (el in launcherComponents(manifest)) {
            el.attr(android.R.attr.icon)?.setReference(resId)
            el.attr(android.R.attr.roundIcon)?.setReference(resId)
        }
    }

    fun removePermissions(manifest: AndroidManifestBlock, permissions: Collection<String>) {
        if (permissions.isEmpty()) return
        val set = permissions.toHashSet()
        manifest.manifestElement?.removeElementsIf { el ->
            el.name in USES_PERMISSION_TAGS && el.attr(android.R.attr.name)?.str() in set
        }
    }
}
