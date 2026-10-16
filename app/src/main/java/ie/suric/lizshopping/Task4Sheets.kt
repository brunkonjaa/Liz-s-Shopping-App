@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ie.suric.lizshopping

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ChangeShopSheet(state: ShoppingState, selected: String?, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    LizBottomSheet("Change shop", "Each shop keeps its own list, favourites and completion state.", onDismissRequest = onDismiss) {
        state.shopDefinitions.forEach { shop ->
            Row(Modifier.fillMaxWidth().clickable { onSelect(shop.name) }.heightIn(min = 64.dp).padding(horizontal = 8.dp).semantics { contentDescription = "Select ${shop.name} shop" }, verticalAlignment = Alignment.CenterVertically) {
                ShopMark(shop, Modifier.size(48.dp, 38.dp))
                RadioButton(selected == shop.name, { onSelect(shop.name) })
                Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(shop.name, style = MaterialTheme.typography.titleMedium)
                    Text("${state.dataFor(shop.name).items.count { !it.done }} items", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (selected == shop.name) Text("Selected", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
fun AddShopSheet(
    existingNames: List<String>,
    selectedLogo: ImportedImage?,
    onPickLogo: () -> Unit,
    onClearLogo: () -> Unit,
    onCreate: (String, ImportedImage?) -> String?,
    onDismiss: (Boolean) -> Unit
) {
    var custom by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    LizBottomSheet("Add a shop", "Choose a suggested local shop or enter a custom name.", onDismissRequest = { onDismiss(custom.isNotBlank()) }) {
        Text("Suggested shops", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ShopDefaults.suggestedNames.forEach { name ->
            TextButton(onClick = { error = onCreate(name, selectedLogo) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("+ $name", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
        }
        androidx.compose.material3.OutlinedTextField(custom, { custom = it.take(100); error = null }, label = { Text("Custom shop name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShopMark(selectedLogo?.let { Shop("new-shop-image", "Shop image", logoImageRef = it.reference, logoImageDataBase64 = it.base64) } ?: Shop("new-shop", "New shop"), Modifier.size(56.dp, 44.dp))
            TextButton(onClick = onPickLogo, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(if (selectedLogo == null) "Choose an optional shop image" else "Change shop image") }
            if (selectedLogo != null) TextButton(onClick = onClearLogo, modifier = Modifier.heightIn(min = 48.dp)) { Text("Remove") }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { error = onCreate(custom, selectedLogo) }, enabled = custom.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Create shop") }
        Text("Existing shops: ${existingNames.size}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = { onDismiss(custom.isNotBlank()) }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
fun RenameShopSheet(
    shop: Shop,
    selectedLogo: ImportedImage?,
    removeLogo: Boolean,
    onPickLogo: () -> Unit,
    onClearSelectedLogo: () -> Unit,
    onRemoveLogo: () -> Unit,
    onSave: (String, ImportedImage?, Boolean) -> Unit,
    onDismiss: (Boolean) -> Unit
) {
    var name by rememberSaveable { mutableStateOf(shop.name) }
    LizBottomSheet("Rename ${shop.name}", "Only this custom shop name changes; its saved data stays attached.", onDismissRequest = { onDismiss(name != shop.name) }) {
        androidx.compose.material3.OutlinedTextField(name, { name = it.take(100) }, label = { Text("New shop name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            ShopMark(selectedLogo?.let { shop.copy(logoImageRef = it.reference, logoImageDataBase64 = it.base64) } ?: shop, Modifier.size(56.dp, 44.dp))
            TextButton(onClick = onPickLogo, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text(if (selectedLogo == null && shop.logoImageRef == null) "Choose a shop image" else "Change shop image") }
            if (selectedLogo != null || shop.logoImageRef != null) TextButton(onClick = if (selectedLogo != null) onClearSelectedLogo else onRemoveLogo, modifier = Modifier.heightIn(min = 48.dp)) { Text("Remove") }
        }
        Button(onClick = { onSave(name, selectedLogo, removeLogo) }, enabled = name.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Save changes") }
        TextButton(onClick = { onDismiss(name != shop.name) }, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
fun RemoveShopSheet(shop: Shop, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    LizBottomSheet("Remove ${shop.name}?", "This is destructive. The shop can be removed only after its lists and favourites are empty.", onDismissRequest = onDismiss) {
        Text("Default shops are protected. Removing a custom shop does not silently delete shopping data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Remove shop") }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
fun ConfirmSheet(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    LizBottomSheet(title, message, onDismissRequest = onDismiss) {
        Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Confirm") }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
fun ShoppingOptionsSheet(hasPrevious: Boolean, onClearCompleted: () -> Unit, onFresh: () -> Unit, onReuse: () -> Unit, onBackup: () -> Unit, onRestore: () -> Unit, onDismiss: () -> Unit) {
    LizBottomSheet("Shopping options", "Manage this shop's list or your offline backup.", onDismissRequest = onDismiss) {
        if (hasPrevious) EditorialBanner("PREVIOUS ITEMS", "Bring back products from an earlier list", "", accent = MaterialTheme.colorScheme.primary)
        TextButton(onClick = onClearCompleted, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Clear completed items", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
        TextButton(onClick = onFresh, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Start a fresh list", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
        TextButton(onClick = onReuse, enabled = hasPrevious, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Reuse previous items", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
        TextButton(onClick = onBackup, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Save backup", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
        TextButton(onClick = onRestore, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Restore backup", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) }
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
    }
}

@Composable
fun CategoryChips(categories: List<String>, selected: String, onSelect: (String) -> Unit) {
    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { value -> FilterChip(selected.equals(value, true), { onSelect(value) }, label = { Text(value) }, modifier = Modifier.heightIn(min = 48.dp)) }
    }
}
