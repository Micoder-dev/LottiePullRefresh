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

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/**
 * The vertical distance consumed by a drag is scaled by this factor to add a "sticky"
 * resistance to the pull gesture, matching the feel of the original view-based library.
 */
internal const val DragMultiplier = 0.5f

internal class LottiePullRefreshNestedScrollConnection(
    private val state: LottiePullRefreshState,
    private val coroutineScope: CoroutineScope,
    private val onRefresh: () -> Unit,
) : NestedScrollConnection {
    var enabled: Boolean = false
    var refreshTrigger: Float = 0f

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset = when {
        // If swiping isn't enabled, return zero
        !enabled -> Offset.Zero
        // If we're refreshing, return zero
        state.isRefreshing -> Offset.Zero
        // If the user is swiping up, handle it
        source == NestedScrollSource.UserInput && available.y < 0 -> onScroll(available)
        else -> Offset.Zero
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset = when {
        // If swiping isn't enabled, return zero
        !enabled -> Offset.Zero
        // If we're refreshing, return zero
        state.isRefreshing -> Offset.Zero
        // If the user is swiping down and there's y remaining, handle it
        source == NestedScrollSource.UserInput && available.y > 0 -> onScroll(available)
        else -> Offset.Zero
    }

    private fun onScroll(available: Offset): Offset {
        val newOffset = (available.y * DragMultiplier + state.indicatorOffset).coerceAtLeast(0f)
        val dragConsumed = newOffset - state.indicatorOffset

        return if (dragConsumed.absoluteValue >= 0.5f) {
            // Mark the swipe as in progress for *either* direction so the indicator tracks the
            // finger live as soon as the user starts pulling (not only after release).
            state.isSwipeInProgress = true
            coroutineScope.launch {
                state.dispatchScrollDelta(dragConsumed)
            }
            // Return the consumed Y
            Offset(x = 0f, y = dragConsumed / DragMultiplier)
        } else {
            Offset.Zero
        }
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        // If we're dragging and scrolled past the trigger point, refresh
        if (!state.isRefreshing && state.indicatorOffset >= refreshTrigger) {
            onRefresh()
        }

        // Reset the drag in progress state
        state.isSwipeInProgress = false

        // Don't consume any velocity, to allow the scrolling layout to fling
        return Velocity.Zero
    }
}
