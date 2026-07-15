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

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex

/**
 * Creates a [LottiePullRefreshState] that is remembered across compositions.
 *
 * Changes to [isRefreshing] will result in the [LottiePullRefreshState] being updated.
 *
 * @param isRefreshing the value for [LottiePullRefreshState.isRefreshing]
 */
@Composable
fun rememberLottiePullRefreshState(isRefreshing: Boolean): LottiePullRefreshState {
    return remember { LottiePullRefreshState(isRefreshing) }.apply {
        this.isRefreshing = isRefreshing
    }
}

/**
 * A state object that can be hoisted to control and observe changes for [LottiePullRefresh].
 *
 * In most cases, this will be created via [rememberLottiePullRefreshState].
 *
 * @param isRefreshing the initial value for [LottiePullRefreshState.isRefreshing]
 */
class LottiePullRefreshState(isRefreshing: Boolean) {
    private val _indicatorOffset = Animatable(0f)
    private val mutatorMutex = MutatorMutex()

    /**
     * Whether this [LottiePullRefreshState] is currently refreshing or not.
     */
    var isRefreshing: Boolean by mutableStateOf(isRefreshing)

    /**
     * Whether a swipe/drag is currently in progress.
     */
    var isSwipeInProgress: Boolean by mutableStateOf(false)
        internal set

    /**
     * The current offset for the indicator, in pixels.
     */
    val indicatorOffset: Float get() = _indicatorOffset.value

    internal suspend fun animateOffsetTo(offset: Float) {
        mutatorMutex.mutate {
            _indicatorOffset.animateTo(offset)
        }
    }

    /**
     * Dispatch scroll delta in pixels from touch events.
     */
    internal suspend fun dispatchScrollDelta(delta: Float) {
        mutatorMutex.mutate(MutatePriority.UserInput) {
            _indicatorOffset.snapTo(_indicatorOffset.value + delta)
        }
    }
}
