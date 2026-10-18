package com.twinndroid.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import com.twinndroid.ui.theme.AnimatedBackground
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private val TABS = listOf(
    TabItem("Clones", Icons.Rounded.Layers),
    TabItem("New clone", Icons.Rounded.AddCircle),
    TabItem("Extract", Icons.Rounded.Unarchive),
)

@Composable
fun TwinnRoot(vm: MainViewModel) {
    BackHandler(enabled = vm.screen != Screen.Home && vm.build == null) {
        when (vm.screen) {
            Screen.Editor -> vm.closeEditor()
            else -> vm.screen = Screen.Home
        }
    }
    BackHandler(enabled = vm.build?.running == false) { vm.dismissBuild() }

    // Pages aren't inside a Scaffold/Surface, so set the content colour explicitly.
    androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
    ) { RootContent(vm) }

    vm.message?.let { msg ->
        AlertDialog(
            onDismissRequest = { vm.message = null },
            confirmButton = { TextButton(onClick = { vm.message = null }) { Text("OK") } },
            text = { Text(msg, style = androidx.compose.material3.MaterialTheme.typography.bodyLarge) },
        )
    }
}

@Composable
private fun RootContent(vm: MainViewModel) {
    Box(Modifier.fillMaxSize()) {
        AnimatedBackground()

        AnimatedContent(
            targetState = vm.screen,
            transitionSpec = {
                val forward = targetState != Screen.Home
                val enter = slideInHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { w -> if (forward) w / 4 else -w / 4 } +
                    fadeIn(tween(220))
                val exit = slideOutHorizontally(spring(stiffness = Spring.StiffnessMediumLow)) { w -> if (forward) -w / 6 else w / 6 } +
                    fadeOut(tween(160))
                enter togetherWith exit
            },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Home -> MainTabs(vm)
                Screen.Editor -> EditorScreen(vm)
                Screen.Settings -> SettingsScreen(vm)
                Screen.Appearance -> AppearanceScreen(vm)
            }
        }

        AnimatedVisibility(vm.busy, enter = fadeIn(), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        AnimatedVisibility(
            vm.build != null,
            enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.96f),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 0.96f),
        ) {
            vm.build?.let { BuildOverlay(vm, it) }
        }
    }
}

@Composable
private fun MainTabs(vm: MainViewModel) {
    val pager = rememberPagerState(initialPage = vm.tab) { TABS.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pager) { snapshotFlow { pager.settledPage }.collect { vm.tab = it } }
    LaunchedEffect(vm.tab) { if (vm.tab != pager.currentPage && !pager.isScrollInProgress) pager.animateScrollToPage(vm.tab) }

    Column(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.weight(1f),
            beyondViewportPageCount = 1,
            key = { it },
        ) { page ->
            Box(
                Modifier.fillMaxSize().graphicsLayer {
                    val offset = pager.getOffsetDistanceInPages(page).absoluteValue.coerceIn(0f, 1f)
                    alpha = lerp(1f, 0.35f, offset)
                    val scale = lerp(1f, 0.94f, offset)
                    scaleX = scale
                    scaleY = scale
                },
            ) {
                when (page) {
                    0 -> ClonesPage(vm, onNewClone = { scope.launch { pager.animateScrollToPage(1) } })
                    1 -> NewClonePage(vm)
                    else -> ExtractPage(vm)
                }
            }
        }
        PillTabBar(
            tabs = TABS,
            selected = pager.targetPage,
            onSelect = { i -> scope.launch { pager.animateScrollToPage(i, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) } },
        )
    }
}
