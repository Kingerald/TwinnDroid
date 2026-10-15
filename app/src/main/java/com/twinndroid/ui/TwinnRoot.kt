package com.twinndroid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TwinnRoot(vm: MainViewModel) {
    BackHandler(enabled = vm.screen != Screen.Home) {
        when (vm.screen) {
            Screen.Editor -> vm.closeEditor()
            else -> vm.screen = Screen.Home
        }
    }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AnimatedContent(
            targetState = vm.screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Home -> HomeScreen(vm)
                Screen.PickApp -> PickAppScreen(vm)
                Screen.Editor -> EditorScreen(vm)
                Screen.Settings -> SettingsScreen(vm)
                Screen.Appearance -> AppearanceScreen(vm)
            }
        }

        if (vm.busy) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }

    vm.build?.let { BuildDialog(vm, it) }

    vm.message?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.message = null },
            confirmButton = { TextButton(onClick = { vm.message = null }) { Text("OK") } },
            title = { Text("Something went wrong") },
            text = { Text(msg) },
        )
    }
}

@Composable
private fun BuildDialog(vm: MainViewModel, state: BuildState) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    LaunchedEffect(state.logs.size) { scroll.animateScrollTo(scroll.maxValue) }
    AlertDialog(
        onDismissRequest = { if (!state.running) vm.dismissBuild() },
        title = {
            Text(
                when {
                    state.running -> "Cloning…"
                    state.result?.success == true -> "Clone ready"
                    else -> "Clone failed"
                },
            )
        },
        text = {
            Column {
                if (state.running) LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 12.dp))
                SelectionContainer {
                    Column(
                        Modifier
                            .heightIn(max = 280.dp)
                            .verticalScroll(scroll)
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest, MaterialTheme.shapes.small)
                            .padding(10.dp),
                    ) {
                        state.logs.forEach {
                            Text(it, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!state.running) {
                val launch = if (state.result?.success == true) vm.launchIntent(state.clonePackage) else null
                if (launch != null) {
                    TextButton(onClick = { vm.dismissBuild(); context.startActivity(launch) }) { Text("Open clone") }
                }
            }
        },
        dismissButton = {
            if (!state.running) TextButton(onClick = vm::dismissBuild) { Text("Close") }
        },
    )
}
