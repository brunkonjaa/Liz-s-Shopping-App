package ie.suric.lizshopping

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LizBottomSheet(
    title: String,
    subtitle: String? = null,
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        dragHandle = { androidx.compose.material3.BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
    ) {
        Column(
            Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

@Composable
fun EditorialBanner(title: String, subtitle: String, symbol: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary, imagePainter: Painter? = null) {
    val headingStyle = if (title.length > 7) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.displaySmall
    val heroPainter = imagePainter ?: painterResource(when (title) {
        "ALL ITEMS" -> R.drawable.hero_helmet_flavours
        "FAVOURITES" -> R.drawable.hero_favourites
        "MY LIST" -> R.drawable.hero_shopping_list
        "SETTINGS" -> R.drawable.hero_settings
        "SHOP MANAGEMENT" -> R.drawable.hero_shop_management
        "PREVIOUS ITEMS" -> R.drawable.hero_previous
        else -> R.drawable.hero_catalogue
    })
    Box(modifier.fillMaxWidth().heightIn(min = 196.dp).clip(MaterialTheme.shapes.large)) {
        Image(painter = heroPainter, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        Box(Modifier.matchParentSize().background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha = .96f), MaterialTheme.colorScheme.surface.copy(alpha = .72f), Color.Transparent))))
        Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 20.dp, vertical = 18.dp).fillMaxWidth(.86f)) {
            Text(title, style = headingStyle, color = accent, softWrap = true)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth(.76f), softWrap = true)
        }
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f)); if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) } }
}

@Composable
fun FeatureCard(title: String, subtitle: String, symbol: String, onClick: () -> Unit, accent: Color = MaterialTheme.colorScheme.primary, imagePainter: Painter? = null) {
    val heroPainter = imagePainter ?: painterResource(R.drawable.grocery_hero_small)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Box(Modifier.fillMaxWidth().height(118.dp)) {
            Image(painter = heroPainter, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
            Box(Modifier.fillMaxWidth().height(118.dp).background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha = .94f), Color.Transparent))))
            Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 18.dp, vertical = 14.dp).fillMaxWidth(.72f)) { Text(title, style = MaterialTheme.typography.headlineMedium, color = accent); Text(subtitle, style = MaterialTheme.typography.bodyMedium) }
            Text(symbol, fontSize = 32.sp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 22.dp))
        }
    }
}

@Composable
fun ProductThumbnail(symbol: String, modifier: Modifier = Modifier) {
    Box(modifier.size(74.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) { Text(symbol, fontSize = 38.sp) }
}

@Composable
fun ShopMark(name: String, modifier: Modifier = Modifier) {
    val shop = ShopDefaults.definitions.firstOrNull { it.name == name } ?: Shop("custom:$name", name)
    ShopMark(shop, modifier)
}

object ShopBranding {
    fun logoResource(shopId: String): Int? = when (shopId) {
        "Lidl" -> R.drawable.brand_lidl
        "Aldi" -> R.drawable.brand_aldi
        "Dunnes Stores" -> R.drawable.brand_dunnes_stores
        "SuperValu" -> R.drawable.brand_supervalu
        else -> null
    }
}

@Composable
fun ShopMark(shop: Shop, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val customLogo by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, shop.id, shop.logoImageRef, shop.logoImageDataBase64) {
        value = withContext(Dispatchers.IO) {
            val fileBytes = shop.logoImageRef?.let { reference ->
                runCatching { ImageStore.file(context, reference).takeIf { it.isFile && it.length() <= ImageStore.MAX_BYTES }?.readBytes() }.getOrNull()
            }
            val bytes = fileBytes ?: shop.logoImageDataBase64?.let { encoded ->
                runCatching { Base64.decode(encoded, Base64.DEFAULT).takeIf { it.size <= ImageStore.MAX_BYTES } }.getOrNull()
            }
            bytes?.let { raw ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
                if (bounds.outWidth !in 1..4096 || bounds.outHeight !in 1..4096) null else {
                    val sample = generateSequence(1) { it * 2 }.takeWhile { maxOf(bounds.outWidth, bounds.outHeight) / it > 192 }.last()
                    BitmapFactory.decodeByteArray(raw, 0, raw.size, BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
                }
            }
        }
    }
    val brandResource = ShopBranding.logoResource(shop.id)
    Box(modifier.clip(MaterialTheme.shapes.small).background(if (customLogo != null || brandResource != null) Color.White else MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        when {
            customLogo != null -> Image(customLogo!!, contentDescription = null, modifier = Modifier.fillMaxSize().padding(3.dp), contentScale = ContentScale.Fit)
            brandResource != null -> Image(painterResource(brandResource), contentDescription = null, modifier = Modifier.fillMaxSize().padding(3.dp), contentScale = ContentScale.Fit)
            else -> Text("🛒", fontSize = 23.sp)
        }
    }
}

@Composable
fun ShopCard(shop: Shop, data: ShopData, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val name = shop.name
    val itemCount = data.items.count { !it.done }
    val brandSurface = when (shop.id) { "Lidl" -> Color(0xFFE8F0F7); "Aldi" -> Color(0xFFF9F0E4); "Dunnes Stores" -> Color(0xFFEAF3E9); "SuperValu" -> Color(0xFFFBE9E9); else -> MaterialTheme.colorScheme.surface }
    Card(onClick = onClick, modifier = modifier.testTag("shop-card-${shop.id}").semantics { contentDescription = "Select $name shop, $itemCount items" }, shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else brandSurface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 112.dp).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ShopMark(shop, Modifier.size(42.dp))
            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("$itemCount items", color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false)
            }
            Text("\u203A", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ProductCardV2(
    product: Product,
    onConfigure: () -> Unit,
    onAdd: () -> Unit,
    canAdd: Boolean = true,
    favourite: Boolean = false,
    onFavourite: (() -> Unit)? = null
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProductThumbnail(product.symbol)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium)
                Text(product.category, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(product.sizes.firstOrNull { it.isNotBlank() }.orEmpty(), color = MaterialTheme.colorScheme.secondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = onAdd, enabled = canAdd, modifier = Modifier.semantics { contentDescription = "Add ${product.name} to shopping list" }) { Text("+ Add") }
                    TextButton(onClick = onConfigure, modifier = Modifier.heightIn(min = 48.dp)) { Text("Configure") }
                }
            }
            onFavourite?.let { onHeart ->
                IconButton(onClick = onHeart, modifier = Modifier.semantics { contentDescription = if (favourite) "Remove ${product.name} from favourites" else "Save ${product.name} as favourite" }) {
                    Text(if (favourite) "♥" else "♡", fontSize = 26.sp, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit, favourite: Boolean = false, onFavourite: (() -> Unit)? = null) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { ProductThumbnail(product.symbol); Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text(product.name, style = MaterialTheme.typography.titleMedium); Text(product.category, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(product.sizes.firstOrNull().orEmpty(), color = MaterialTheme.colorScheme.secondary) }; onFavourite?.let { onHeart -> IconButton(onClick = onHeart, modifier = Modifier.semantics { contentDescription = if (favourite) "Remove ${product.name} from favourites" else "Save ${product.name} as favourite" }) { Text(if (favourite) "♥" else "♡", fontSize = 26.sp, color = MaterialTheme.colorScheme.secondary) } }; Text("+", fontSize = 30.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.semantics { contentDescription = "Add ${product.name}" }) }
    }
}

@Composable
fun FavouriteCard(item: Item, onAdd: () -> Unit, onEdit: () -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { ProductThumbnail(Catalogue.symbol(item.name), Modifier.size(62.dp)); Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(item.name, style = MaterialTheme.typography.titleMedium); Text("${item.detail()} · ×${item.quantity}", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Column(horizontalAlignment = Alignment.End) { TextButton(onClick = onAdd, modifier = Modifier.semantics { contentDescription = "Add ${item.name} to shopping list" }) { Text("+ Add") }; Row { TextButton(onClick = onEdit) { Text("Edit") }; IconButton(onClick = onRemove, modifier = Modifier.semantics { contentDescription = "Remove ${item.name} from favourites" }) { Text("♡", fontSize = 24.sp) } } }
        }
    }
}

@Composable
fun ShoppingProgress(completed: Int, total: Int) {
    val progress = if (total == 0) 0f else completed.toFloat() / total.toFloat()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("$completed of $total completed", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f)); Text(if (total == 0) "Ready to shop" else "${(progress * 100).toInt()}%", color = MaterialTheme.colorScheme.secondary) }; LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.secondary, trackColor = MaterialTheme.colorScheme.secondaryContainer) }
}

@Composable
fun ShoppingListRow(item: Item, onToggle: () -> Unit, onMinus: () -> Unit, onPlus: () -> Unit, onEdit: () -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = if (item.done) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = item.done, onCheckedChange = { onToggle() }, modifier = Modifier.semantics { contentDescription = if (item.done) "Mark ${item.name} still to buy" else "Mark ${item.name} bought" }); ProductThumbnail(Catalogue.symbol(item.name), Modifier.size(64.dp)); Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(item.detail().ifBlank { "Everyday essential" }, color = MaterialTheme.colorScheme.onSurfaceVariant); if (item.done) Text("Bought ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) }; Column(horizontalAlignment = Alignment.End) { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = onMinus, enabled = item.quantity > 1, modifier = Modifier.size(40.dp).semantics { contentDescription = "Decrease ${item.name} quantity" }) { Text("−", fontSize = 22.sp) }; Text("×${item.quantity}", fontWeight = FontWeight.Bold, modifier = Modifier.width(42.dp)); IconButton(onClick = onPlus, enabled = item.quantity < 999, modifier = Modifier.size(40.dp).semantics { contentDescription = "Increase ${item.name} quantity" }) { Text("+", fontSize = 22.sp) } }; Row { TextButton(onClick = onEdit) { Text("Edit") }; TextButton(onClick = onRemove) { Text("Remove") } } } }
    }
}

@Composable
fun EmptyState(title: String, message: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.fillMaxWidth().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleLarge); Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant); if (action != null && onAction != null) TextButton(onClick = onAction) { Text(action) } } }
}
