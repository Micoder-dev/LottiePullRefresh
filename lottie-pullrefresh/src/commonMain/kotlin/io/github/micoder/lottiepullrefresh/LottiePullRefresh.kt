/*
 * Copyright 2026 Micoder-dev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.micoder.lottiepullrefresh

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import io.github.alexzhirkevich.compottie.LottieComposition
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition

/**
 * A layout which adds pull-to-refresh with a Lottie animation as the refresh indicator, for
 * Compose Multiplatform (Android & iOS).
 *
 * This is the low-level overload that accepts a custom [indicator] slot. For the common case, use
 * the overload that accepts a [LottieComposition] or a raw Lottie JSON string.
 *
 * @param isRefreshing whether the layout is currently refreshing.
 * @param onRefresh called when a swipe gesture triggers a refresh.
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the layout's state.
 * @param swipeEnabled whether the layout should react to swipe gestures or not.
 * @param refreshThreshold the threshold distance the user must pull before a refresh is triggered.
 * @param indicatorAlignment the alignment of the indicator within the layout bounds.
 * @param indicatorOverlay when `true` (default) the indicator floats over the content; when `false`
 * the content is pushed down by the pull distance, so the indicator sits in the gap above it.
 * @param indicator the indicator that represents the current state. By default this shows a Lottie
 * animation via [LottieRefreshIndicator].
 * @param content the content to be shown inside this layout.
 */
@Composable
fun LottiePullRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    state: LottiePullRefreshState = rememberLottiePullRefreshState(isRefreshing),
    swipeEnabled: Boolean = true,
    refreshThreshold: Dp = 80.dp,
    indicatorAlignment: Alignment = Alignment.TopCenter,
    indicatorOverlay: Boolean = true,
    indicator: @Composable (state: LottiePullRefreshState, refreshTriggerDistance: Dp) -> Unit,
    content: @Composable () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val updatedOnRefresh = rememberUpdatedState(onRefresh)
    val refreshTriggerPx = with(LocalDensity.current) { refreshThreshold.toPx() }

    // Settle the indicator to its resting position whenever the drag ends *or* the refresh finishes.
    // Keying on isRefreshing too ensures we animate back to 0 when the caller clears isRefreshing.
    LaunchedEffect(state.isSwipeInProgress, state.isRefreshing) {
        if (!state.isSwipeInProgress) {
            state.animateOffsetTo(if (state.isRefreshing) refreshTriggerPx else 0f)
        }
    }

    val connection = remember(state, coroutineScope) {
        LottiePullRefreshNestedScrollConnection(state, coroutineScope) {
            updatedOnRefresh.value.invoke()
        }
    }.apply {
        this.enabled = swipeEnabled
        this.refreshTrigger = refreshTriggerPx
    }

    Box(modifier = modifier.clipToBounds().nestedScroll(connection)) {
        val contentModifier = if (indicatorOverlay) {
            Modifier
        } else {
            Modifier.offset { IntOffset(x = 0, y = state.indicatorOffset.roundToInt()) }
        }
        Box(modifier = contentModifier) {
            content()
        }
        Box(modifier = Modifier.align(indicatorAlignment)) {
            indicator(state, refreshThreshold)
        }
    }
}

/**
 * A layout which adds pull-to-refresh with a Lottie animation as the refresh indicator, for
 * Compose Multiplatform (Android & iOS).
 *
 * @param isRefreshing whether the layout is currently refreshing.
 * @param onRefresh called when a swipe gesture triggers a refresh.
 * @param composition the [LottieComposition] to render as the indicator, typically obtained via
 * `rememberLottieComposition`.
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the layout's state.
 * @param swipeEnabled whether the layout should react to swipe gestures or not.
 * @param refreshThreshold the threshold distance the user must pull before a refresh is triggered.
 * @param indicatorSize the size of the Lottie indicator.
 * @param indicatorOverlay whether the indicator floats over the content (`true`) or pushes it down.
 * @param indicatorBackground whether to draw a circular surface behind the animation.
 * @param content the content to be shown inside this layout.
 */
@Composable
fun LottiePullRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    composition: LottieComposition?,
    modifier: Modifier = Modifier,
    state: LottiePullRefreshState = rememberLottiePullRefreshState(isRefreshing),
    swipeEnabled: Boolean = true,
    refreshThreshold: Dp = 80.dp,
    indicatorSize: Dp = 48.dp,
    indicatorOverlay: Boolean = true,
    indicatorBackground: Boolean = true,
    content: @Composable () -> Unit,
) {
    LottiePullRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier,
        state = state,
        swipeEnabled = swipeEnabled,
        refreshThreshold = refreshThreshold,
        indicatorOverlay = indicatorOverlay,
        indicator = { indicatorState, triggerDistance ->
            LottieRefreshIndicator(
                state = indicatorState,
                refreshTriggerDistance = triggerDistance,
                composition = composition,
                indicatorSize = indicatorSize,
                background = indicatorBackground,
            )
        },
        content = content,
    )
}

/**
 * A layout which adds pull-to-refresh with a Lottie animation as the refresh indicator, for
 * Compose Multiplatform (Android & iOS).
 *
 * This convenience overload takes the raw Lottie JSON as a string and loads the composition
 * internally. On Android/iOS you can read it from resources; e.g. via `Res.readBytes(...)`.
 *
 * @param isRefreshing whether the layout is currently refreshing.
 * @param onRefresh called when a swipe gesture triggers a refresh.
 * @param lottieJson the raw Lottie animation JSON.
 * @param modifier the modifier to apply to this layout.
 * @param state the state object to be used to control or observe the layout's state.
 * @param swipeEnabled whether the layout should react to swipe gestures or not.
 * @param refreshThreshold the threshold distance the user must pull before a refresh is triggered.
 * @param indicatorSize the size of the Lottie indicator.
 * @param content the content to be shown inside this layout.
 */
@Composable
fun LottiePullRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    lottieJson: String,
    modifier: Modifier = Modifier,
    state: LottiePullRefreshState = rememberLottiePullRefreshState(isRefreshing),
    swipeEnabled: Boolean = true,
    refreshThreshold: Dp = 80.dp,
    indicatorSize: Dp = 48.dp,
    indicatorOverlay: Boolean = true,
    indicatorBackground: Boolean = true,
    content: @Composable () -> Unit,
) {
    val composition by rememberLottieComposition(lottieJson) {
        LottieCompositionSpec.JsonString(lottieJson)
    }
    LottiePullRefresh(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        composition = composition,
        modifier = modifier,
        state = state,
        swipeEnabled = swipeEnabled,
        refreshThreshold = refreshThreshold,
        indicatorSize = indicatorSize,
        indicatorOverlay = indicatorOverlay,
        indicatorBackground = indicatorBackground,
        content = content,
    )
}
