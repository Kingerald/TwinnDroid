package com.twinndroid.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.automirrored.rounded.RotateRight
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Colorize
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.twinndroid.data.IconAdjust
import com.twinndroid.data.IconMode
import com.twinndroid.data.SourceKind

private val BADGE_COLORS = listOf(
    0xFF7C4DFF, 0xFF2EE6B6, 0xFFFF4D8D, 0xFFFF9F1C, 0xFF3D8BFF, 0xFFE53935, 0xFF43A047, 0xFF212121,
)

/** Permissions people most often strip from alt-account clones. */
private val SENSITIVE = setOf(
    "android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION",
    "android.permission.ACCESS_BACKGROUND_LOCATION", "android.permission.READ_CONTACTS",
    "android.permission.CAMERA", "android.permission.RECORD_AUDIO", "android.permission.READ_PHONE_STATE",
    "android.permission.READ_CALL_LOG", "android.permission.READ_SMS", "android.permission.GET_ACCOUNTS",
    "android.permission.BLUETOOTH_SCAN", "android.permission.NEARBY_WIFI_DEVICES",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(vm: MainViewModel) {
    val state = vm.editor ?: return
    val c = state.config
    var permsExpanded by rememberSaveable { mutableStateOf(false) }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) vm.setCustomIcon(uri)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
                title = { Text(if (state.isNew) "New clone" else "Customize clone") },
                navigationIcon = { IconButton(onClick = vm::closeEditor) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)) {
                Button(
                    onClick = vm::saveAndBuild,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
                ) {
                    Icon(Icons.Rounded.Check, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.isNew) "Clone & install" else "Save, rebuild & install")
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Preview
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Box(
                    Modifier.size(88.dp).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    state.preview?.let { Image(it, null, Modifier.size(72.dp)) }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(c.label.ifBlank { "Unnamed" }, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Clone of ${c.sourceLabel}${state.sourceVersion?.let { " $it" } ?: ""}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (c.sourceKind == SourceKind.INSTALLED) "From installed app · auto-update capable" else "From APK file",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            Section("Identity") {
                OutlinedTextField(
                    value = c.label,
                    onValueChange = { v -> vm.updateConfig { it.copy(label = v) } },
                    label = { Text("App name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = c.clonePackage,
                    onValueChange = { v -> vm.updateConfig { it.copy(clonePackage = v.replace(" ", "")) } },
                    label = { Text("Package name") },
                    singleLine = true,
                    isError = state.packageError != null,
                    supportingText = {
                        Text(
                            state.packageError
                                ?: if (!state.isNew && c.builtVersionCode > 0) "Changing this creates a new app (fresh data) instead of updating the existing clone."
                                else "Each clone needs a unique package. This is what keeps its login separate.",
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Section("Icon") {
                val modes = listOf(IconMode.ORIGINAL to "Original", IconMode.BADGE to "Badge", IconMode.CUSTOM to "Custom")
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    modes.forEachIndexed { i, (mode, label) ->
                        SegmentedButton(
                            selected = c.iconMode == mode,
                            onClick = {
                                if (mode == IconMode.CUSTOM && c.customIconPath == null) {
                                    pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                } else {
                                    vm.updateConfig { it.copy(iconMode = mode) }
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(i, modes.size),
                        ) { Text(label) }
                    }
                }
                when (c.iconMode) {
                    IconMode.BADGE -> {
                        Spacer(Modifier.height(12.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            BADGE_COLORS.forEach { color ->
                                val selected = c.badgeColor == color
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(color))
                                        .border(
                                            if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else BorderStroke(0.dp, Color.Transparent),
                                            CircleShape,
                                        )
                                        .clickable { vm.updateConfig { it.copy(badgeColor = color) } },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = c.badgeText,
                            onValueChange = { v -> vm.updateConfig { it.copy(badgeText = v.take(2)) } },
                            label = { Text("Badge text (up to 2 characters)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    IconMode.CUSTOM -> {
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = {
                            pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }) {
                            Icon(Icons.Rounded.Image, null); Spacer(Modifier.width(8.dp)); Text("Choose another image")
                        }
                    }
                    IconMode.ORIGINAL -> Unit
                }
                Spacer(Modifier.height(8.dp))
                IconAdjustPanel(c.iconAdjust) { a -> vm.updateConfig { it.copy(iconAdjust = a) } }
            }

            Section("Permissions") {
                val removed = c.removedPermissions.toSet()
                Row(
                    Modifier.fillMaxWidth().clickable { permsExpanded = !permsExpanded }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("${state.permissions.size - removed.size} of ${state.permissions.size} kept")
                        Text(
                            "Untick a permission to remove it from the clone. Removing ones the app needs can make it crash.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(if (permsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                }
                if (permsExpanded) {
                    if (state.permissions.any { it in SENSITIVE }) {
                        TextButton(onClick = {
                            vm.updateConfig { cfg -> cfg.copy(removedPermissions = (cfg.removedPermissions + state.permissions.filter { it in SENSITIVE }).distinct()) }
                        }) { Text("Remove privacy-sensitive permissions") }
                    }
                    HorizontalDivider()
                    state.permissions.forEach { perm ->
                        val kept = perm !in removed
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                vm.updateConfig { cfg ->
                                    cfg.copy(removedPermissions = if (kept) cfg.removedPermissions + perm else cfg.removedPermissions - perm)
                                }
                            },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(checked = kept, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    perm.substringAfterLast('.'),
                                    fontWeight = if (perm in SENSITIVE) FontWeight.SemiBold else FontWeight.Normal,
                                )
                                Text(perm, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Section("Advanced") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Debuggable")
                        Text(
                            "Lets you inspect the clone with adb / Android Studio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = c.debuggable, onCheckedChange = { v -> vm.updateConfig { it.copy(debuggable = v) } })
                }
            }

            TwinnCard(container = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.padding(16.dp)) {
                    Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Clones are re-signed with TwinnDroid's key. Apps that check their own signature or use Play Integrity " +
                            "(most banking apps, some games, Google Sign-In tied to the original) may refuse to log in.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    TwinnCard {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

private val TINTS = listOf(0xFF7C4DFF, 0xFF2962FF, 0xFF00BFA5, 0xFF43A047, 0xFFFFC107, 0xFFFF6F00, 0xFFE53935, 0xFFEC407A, 0xFF000000, 0xFFFFFFFF)

/** App Cloner-style icon colour controls. Changes re-render the preview live. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconAdjustPanel(a: IconAdjust, onChange: (IconAdjust) -> Unit) {
    var open by rememberSaveable { mutableStateOf(!a.isIdentity) }
    var customTint by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { open = !open }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Tune, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("Colour & tint")
            Text(
                if (a.isIdentity) "Hue, saturation, brightness, tint, flip" else "Adjusted",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
    }
    AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Column {
            AdjustSlider("Hue", "${a.hue.toInt()}°", a.hue, -180f..180f) { onChange(a.copy(hue = it)) }
            AdjustSlider("Saturation", "${(a.saturation * 100).toInt()}%", a.saturation, 0f..2f) { onChange(a.copy(saturation = it)) }
            AdjustSlider("Brightness", "${(a.brightness * 100).toInt()}", a.brightness, -0.6f..0.6f) { onChange(a.copy(brightness = it)) }
            AdjustSlider("Contrast", "${(a.contrast * 100).toInt()}", a.contrast, -0.5f..0.5f) { onChange(a.copy(contrast = it)) }

            Text("Tint", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier.size(32.dp).clip(CircleShape)
                        .border(if (a.tint == null) 3.dp else 1.dp, if (a.tint == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { onChange(a.copy(tint = null)) },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Block, "No tint", Modifier.size(18.dp)) }
                TINTS.forEach { t ->
                    Box(
                        Modifier.size(32.dp).clip(CircleShape).background(Color(t))
                            .border(
                                if (a.tint == t) 3.dp else 1.dp,
                                if (a.tint == t) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                CircleShape,
                            )
                            .clickable { onChange(a.copy(tint = t)) },
                    )
                }
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        .clickable { customTint = true },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Colorize, "Custom colour", Modifier.size(18.dp)) }
            }
            AnimatedVisibility(a.tint != null) {
                AdjustSlider("Tint strength", "${(a.tintStrength * 100).toInt()}%", a.tintStrength, 0.1f..1f) { onChange(a.copy(tintStrength = it)) }
            }

            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = a.invert, onClick = { onChange(a.copy(invert = !a.invert)) }, label = { Text("Invert") })
                FilterChip(selected = a.flip, onClick = { onChange(a.copy(flip = !a.flip)) }, label = { Text("Flip") })
                FilterChip(
                    selected = a.rotation != 0,
                    onClick = { onChange(a.copy(rotation = (a.rotation + 90) % 360)) },
                    label = { Text(if (a.rotation == 0) "Rotate" else "Rotate ${a.rotation}°") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Rounded.RotateRight, null, Modifier.size(18.dp)) },
                )
            }
            if (!a.isIdentity) {
                TextButton(onClick = { onChange(IconAdjust()) }) { Text("Reset adjustments") }
            }
        }
    }
    if (customTint) {
        ColorPickerDialog(
            title = "Tint colour",
            initial = a.tint ?: 0xFF7C4DFF,
            onDismiss = { customTint = false },
            onApply = { v -> onChange(a.copy(tint = v)); customTint = false },
        )
    }
}

@Composable
private fun AdjustSlider(label: String, value: String, v: Float, range: ClosedFloatingPointRange<Float>, onChange: (Float) -> Unit) {
    Column(Modifier.padding(top = 4.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
            Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(value = v, onValueChange = onChange, valueRange = range)
    }
}
