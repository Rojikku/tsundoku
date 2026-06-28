package eu.kanade.presentation.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persistent reading status bar showing time, battery, chapter, and progress as a pill overlay.
 *
 * [chapterText] is the pre-formatted chapter string from the call site (respects the
 * novelChapterTitleDisplay preference). Pass null when no chapter is loaded yet.
 */
@Composable
fun NovelStatusBar(
    chapterText: String?,
    progressPercent: Int,
    showTime: Boolean,
    showBattery: Boolean,
    showChapter: Boolean,
    showProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    // Clock — updated on the minute boundary to stay in sync with the system clock
    var timeText by remember { mutableStateOf(currentTimeHHmm()) }
    LaunchedEffect(Unit) {
        val msUntilNextMinute = 60_000L - (System.currentTimeMillis() % 60_000L)
        delay(msUntilNextMinute)
        timeText = currentTimeHHmm()
        while (true) {
            delay(60_000L)
            timeText = currentTimeHHmm()
        }
    }

    // Battery — ACTION_BATTERY_CHANGED is sticky so registerReceiver returns current state
    // immediately, avoiding an "unknown" flash on the first frame
    var batteryPercent by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent ?: return
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level >= 0 && scale > 0) batteryPercent = level * 100 / scale
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level >= 0 && scale > 0) batteryPercent = level * 100 / scale
        }
        onDispose { context.unregisterReceiver(receiver) }
    }

    val segments = buildList {
        if (showTime) add(timeText)
        if (showBattery && batteryPercent >= 0) add("$batteryPercent%")
        if (showChapter && chapterText != null) add(chapterText)
        if (showProgress) add("$progressPercent%")
    }

    if (segments.isEmpty()) return

    // Pill style mirrors NovelTtsControlsOverlay for visual consistency
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        segments.forEachIndexed { index, text ->
            if (index > 0) {
                Text(
                    text = " • ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            )
        }
    }
}

private fun currentTimeHHmm(): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
