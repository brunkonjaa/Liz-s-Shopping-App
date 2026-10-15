package ie.suric.lizshopping

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EditorialBanner(title: String, subtitle: String, symbol: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary, imagePainter: Painter? = null) {
    val headingStyle = if (title.length > 8) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.displayLarge
    val heroPainter = imagePainter ?: painterResource(R.drawable.grocery_hero_small)
    Box(modifier.fillMaxWidth().height(196.dp).clip(MaterialTheme.shapes.large)) {
        Image(painter = heroPainter, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxWidth().height(196.dp).background(Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha = .94f), Color.Transparent))))
        Column(Modifier.align(Alignment.CenterStart).padding(horizontal = 20.dp, vertical = 18.dp).fillMaxWidth(.68f)) {
            Text(title, style = headingStyle, color = accent)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
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
    val tint = when (name) { "Lidl" -> Color(0xFF07519D); "Aldi" -> Color(0xFF253B93); "Dunnes Stores" -> Color(0xFF174D37); else -> Color(0xFFC51F3C) }
    Box(modifier.clip(MaterialTheme.shapes.small).background(tint), contentAlignment = Alignment.Center) {
        when (name) {
            "Lidl" -> Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFFFE500)), contentAlignment = Alignment.Center) { Text("LIDL", color = tint, fontWeight = FontWeight.Bold, fontSize = 8.sp) }
            "Aldi" -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("ALDI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp); Box(Modifier.width(30.dp).height(2.dp).background(Color(0xFF62C2EF))) }
            "Dunnes Stores" -> Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("DUNNES", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 9.sp); Text("STORES", color = Color.White, fontSize = 7.sp) }
            else -> Text("SuperValu", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
fun ShopCard(name: String, data: ShopData, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val brandSurface = when (name) { "Lidl" -> Color(0xFFE8F0F7); "Aldi" -> Color(0xFFF9F0E4); "Dunnes Stores" -> Color(0xFFEAF3E9); else -> Color(0xFFFBE9E9) }
    Card(onClick = onClick, modifier = modifier, shape = MaterialTheme.shapes.medium, colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else brandSurface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 112.dp).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ShopMark(name, Modifier.size(54.dp))
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(name, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp)); Text("${data.items.count { !it.done }} items", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("\u203A", fontSize = 32.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { ProductThumbnail(product.symbol); Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text(product.name, style = MaterialTheme.typography.titleMedium); Text(product.category, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(product.sizes.firstOrNull().orEmpty(), color = MaterialTheme.colorScheme.secondary) }; Text("+", fontSize = 30.sp, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.semantics { contentDescription = "Add ${product.name}" }) }
    }
}

@Composable
fun FavouriteCard(item: Item, onAdd: () -> Unit, onEdit: () -> Unit, onRemove: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) { ProductThumbnail(Catalogue.symbol(item.name), Modifier.size(62.dp)); Column(Modifier.weight(1f).padding(horizontal = 12.dp)) { Text(item.name, style = MaterialTheme.typography.titleMedium); Text("${item.detail()} · ×${item.quantity}", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Column(horizontalAlignment = Alignment.End) { TextButton(onClick = onAdd) { Text("+ Add") }; Row { TextButton(onClick = onEdit) { Text("Edit") }; IconButton(onClick = onRemove, modifier = Modifier.semantics { contentDescription = "Remove ${item.name} from favourites" }) { Text("♡", fontSize = 24.sp) } } }
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
