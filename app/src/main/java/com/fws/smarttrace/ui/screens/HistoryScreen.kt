package com.fws.smarttrace.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fws.smarttrace.R
import com.fws.smarttrace.core.Languages
import com.fws.smarttrace.data.ScanFiles
import com.fws.smarttrace.model.HistoryItem
import com.fws.smarttrace.ui.AppViewModel
import com.fws.smarttrace.ui.FindingText
import com.fws.smarttrace.ui.components.BannerAd
import com.fws.smarttrace.ui.components.IconAction
import com.fws.smarttrace.ui.components.Ico
import com.fws.smarttrace.ui.components.SmartCard
import com.fws.smarttrace.ui.components.TopBar
import com.fws.smarttrace.ui.components.VerdictChip
import com.fws.smarttrace.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun rememberThumb(id: String, sample: Int = 2): ImageBitmap? {
    val ctx = LocalContext.current
    return produceState<ImageBitmap?>(null, id) {
        value = withContext(Dispatchers.IO) { ScanFiles.load(ctx, id, sample)?.asImageBitmap() }
    }.value
}

@Composable
fun HistoryScreen(vm: AppViewModel, favoritesOnly: Boolean) {
    val all by vm.history.items.collectAsState()
    val items = if (favoritesOnly) all.filter { it.favorite } else all
    var confirmClear by remember { mutableStateOf(false) }
    var deleteId by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        TopBar(
            stringResource(if (favoritesOnly) R.string.fav_title else R.string.hist_title), onBack = null,
            actions = {
                if (!favoritesOnly && items.isNotEmpty()) IconAction(R.drawable.ic_trash) { confirmClear = true }
            },
        )
        if (items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Ico(if (favoritesOnly) R.drawable.ic_heart else R.drawable.ic_history, tint = AppTheme.colors.gold, size = 48.dp)
                    Text(
                        stringResource(if (favoritesOnly) R.string.fav_empty else R.string.hist_empty),
                        modifier = Modifier.padding(top = 14.dp),
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            val bannerAt = minOf(2, items.lastIndex)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(items, key = { _, it -> it.id }) { index, item ->
                    Column {
                        HistoryRow(
                            item,
                            onOpen = { vm.open(item.id) },
                            onFavorite = { vm.toggleFavorite(item.id, item.favorite) },
                            onDelete = { deleteId = item.id },
                        )
                        if (index == bannerAt) BannerAd()
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.hist_clear)) },
            text = { Text(stringResource(R.string.hist_clear_confirm)) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; vm.clearHistory() }) { Text(stringResource(R.string.delete_item)) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    deleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { deleteId = null },
            text = { Text(stringResource(R.string.delete_one_confirm)) },
            confirmButton = {
                TextButton(onClick = { deleteId = null; vm.delete(id) }) { Text(stringResource(R.string.delete_item)) }
            },
            dismissButton = { TextButton(onClick = { deleteId = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun HistoryRow(item: HistoryItem, onOpen: () -> Unit, onFavorite: () -> Unit, onDelete: () -> Unit) {
    val thumb = rememberThumb(item.id, 4)
    val date = remember(item.createdAt) {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Languages.effectiveLocale()).format(Date(item.createdAt))
    }
    SmartCard(onClick = onOpen, contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)).background(AppTheme.colors.chipBg),
                contentAlignment = Alignment.Center,
            ) {
                if (thumb != null) Image(thumb, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Ico(R.drawable.ic_image, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.title.ifBlank { item.brand }.ifBlank { stringResource(R.string.hist_untitled) },
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(FindingText.categoryRes(item.category)) + " · " + date,
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Box(Modifier.padding(top = 6.dp)) { VerdictChip(item.verdict, stringResource(FindingText.verdictRes(item.verdict)) + " · " + item.score) }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconAction(
                    if (item.favorite) R.drawable.ic_heart_fill else R.drawable.ic_heart,
                    tint = if (item.favorite) AppTheme.colors.gold else MaterialTheme.colorScheme.onSurfaceVariant,
                    onClick = onFavorite,
                )
                IconAction(R.drawable.ic_trash, tint = MaterialTheme.colorScheme.onSurfaceVariant, onClick = onDelete)
            }
        }
    }
}
