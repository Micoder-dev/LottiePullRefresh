package io.github.micoder.lottiepullrefresh.sample

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.micoder.lottiepullrefresh.LottiePullRefresh
import kotlinx.coroutines.delay
import io.github.micoder.lottiepullrefresh.sample.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * The different pull-to-refresh "types" this sample demonstrates. Each maps to a bundled Lottie
 * animation plus a different presentation (size, circular background, overlay vs push-down).
 */
private enum class RefreshType(
    val label: String,
    val file: String,
    val indicatorSize: Int,
    val background: Boolean,
    val overlay: Boolean,
) {
    Butterfly("Butterfly", "files/loader.json", indicatorSize = 48, background = true, overlay = true),
    Monitor("Monitor", "files/monitor_progress.json", indicatorSize = 64, background = true, overlay = true),
    Spinner("Spinner (no bg)", "files/spinner.json", indicatorSize = 72, background = false, overlay = true),
    PushDown("Butterfly · push-down", "files/loader.json", indicatorSize = 48, background = true, overlay = false),
}

@OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        var selectedType by remember { mutableStateOf(RefreshType.Butterfly) }

        // Cache the JSON for every animation so switching types is instant.
        val jsonCache = remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        LaunchedEffect(Unit) {
            val loaded = RefreshType.entries
                .map { it.file }
                .distinct()
                .associateWith { Res.readBytes(it).decodeToString() }
            jsonCache.value = loaded
        }

        var isRefreshing by remember { mutableStateOf(false) }
        var refreshCount by remember { mutableStateOf(0) }
        var items by remember { mutableStateOf((1..20).map { "Item #$it" }) }

        LaunchedEffect(isRefreshing) {
            if (isRefreshing) {
                delay(2000)
                refreshCount++
                items = (1..20).map { "Item #$it · refresh $refreshCount" }
                isRefreshing = false
            }
        }

        Scaffold(
            topBar = { TopAppBar(title = { Text("LottiePullRefresh") }) },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                // Type selector.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RefreshType.entries.forEach { type ->
                        FilterChip(
                            selected = type == selectedType,
                            onClick = { selectedType = type },
                            label = { Text(type.label) },
                        )
                    }
                }

                Text(
                    text = "Pull down to refresh — the animation scrubs with your finger.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )

                val json = jsonCache.value[selectedType.file]
                if (json == null) {
                    Box(Modifier.fillMaxSize())
                } else {
                    // keyed so switching type rebuilds cleanly with a fresh state
                    LottiePullRefresh(
                        isRefreshing = isRefreshing,
                        onRefresh = { isRefreshing = true },
                        lottieJson = json,
                        modifier = Modifier.fillMaxSize(),
                        indicatorSize = selectedType.indicatorSize.dp,
                        indicatorBackground = selectedType.background,
                        indicatorOverlay = selectedType.overlay,
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(items) { item ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = item,
                                        modifier = Modifier.padding(20.dp),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
