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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieComposition
import io.github.alexzhirkevich.compottie.animateLottieCompositionAsState
import io.github.alexzhirkevich.compottie.rememberLottiePainter

/**
 * The default indicator for [LottiePullRefresh]. It renders a Lottie [composition] whose progress
 * follows the pull gesture in real time, and loops continuously while
 * [LottiePullRefreshState.isRefreshing] is `true`.
 *
 * The indicator translates directly with [LottiePullRefreshState.indicatorOffset], so it tracks the
 * user's finger while dragging and animates smoothly to its resting position on release (the
 * settle animation is driven by [LottiePullRefresh]).
 *
 * @param state the [LottiePullRefreshState] which is driving this indicator.
 * @param refreshTriggerDistance the distance the user needs to pull for a refresh to be triggered.
 * Used to map the pull distance to the Lottie progress `[0f, 1f]`.
 * @param composition the [LottieComposition] to render, typically from `rememberLottieComposition`.
 * @param modifier the modifier to apply to this layout.
 * @param indicatorSize the size of the Lottie animation.
 * @param background whether to draw a circular surface behind the animation.
 * @param backgroundColor the color of the circular surface (only used when [background] is `true`).
 * @param elevation the elevation of the circular surface (only used when [background] is `true`).
 */
@Composable
fun LottieRefreshIndicator(
    state: LottiePullRefreshState,
    refreshTriggerDistance: Dp,
    composition: LottieComposition?,
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 48.dp,
    background: Boolean = true,
    backgroundColor: Color = Color.White,
    elevation: Dp = 6.dp,
) {
    val density = LocalDensity.current
    val trigger = with(density) { refreshTriggerDistance.toPx() }
    val indicatorHeightPx = with(density) { indicatorSize.toPx() }

    // A looping animation that only advances while we are refreshing.
    val loopState = animateLottieCompositionAsState(
        composition = composition,
        isPlaying = state.isRefreshing,
        iterations = Compottie.IterateForever,
        restartOnPlay = false,
    )

    val painter: Painter = rememberLottiePainter(
        composition = composition,
        // Read the offset (and loop value) *inside* the progress lambda so the painter re-renders
        // on every frame of the drag — the Lottie scrubs forward as the user pulls down and reverses
        // as they pull back up. When refreshing, it plays the looping animation instead.
        progress = {
            if (state.isRefreshing) {
                loopState.value
            } else {
                (state.indicatorOffset / trigger).coerceIn(0f, 1f)
            }
        },
    )

    Box(
        modifier = modifier
            .size(indicatorSize)
            // Everything is driven by the pull:
            //  - translationY slides the indicator down under the finger,
            //  - alpha fades it in from fully hidden (so the shadow can't peek at rest),
            //  - scale grows it toward full size as the pull approaches the trigger.
            // Reading state inside the graphicsLayer block updates every frame without recomposition.
            .graphicsLayer {
                val offset = state.indicatorOffset
                val revealed = if (state.isRefreshing) 1f else (offset / trigger).coerceIn(0f, 1f)
                translationY = offset - indicatorHeightPx
                alpha = revealed
                val scale = 0.6f + 0.4f * revealed
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (background) {
                    Modifier
                        .shadow(elevation, CircleShape)
                        .background(backgroundColor, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painter,
            contentDescription = null,
            modifier = Modifier
                .size(indicatorSize)
                .padding(if (background) 8.dp else 0.dp),
            contentScale = ContentScale.Fit,
        )
    }
}
