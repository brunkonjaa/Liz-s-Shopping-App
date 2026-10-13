package ie.suric.lizshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.UUID

private val Green = Color(0xFF237747)
private val Ink = Color(0xFF17362A)
private val Background = Color(0xFFF8F9F5)
private val Colours = lightColorScheme(primary = Green, onPrimary = Color.White, background = Background, surface = Color.White, onSurface = Ink, onBackground = Ink, secondaryContainer = Color(0xFFE4F2E7), onSecondaryContainer = Ink)
private data class Editing(val item: Item, val product: Product?, val mode: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme(colorScheme = Colours) { ShoppingApp() } }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingApp(vm: ShoppingViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val restoreReady by vm.restoreReady.collectAsStateWithLifecycle()
    val loadFailed by vm.loadFailed.collectAsStateWithLifecycle()
    var choosing by rememberSaveable { mutableStateOf(true) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    var more by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    val snack = remember { SnackbarHostState() }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::export) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::readBackup) }
    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); vm.message.value = null } }
    val shop = state?.selected
    BackHandler(!choosing || editing != null) { if (editing != null) editing = null else { choosing = true; query = ""; category = null } }
    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snack) },
        topBar = {
            Column(Modifier.statusBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!choosing) TextButton(onClick = { if (editing != null) editing = null else choosing = true }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) { Text("‹", fontSize = 32.sp, modifier = Modifier.semantics { contentDescription = "Back" }) }
                    Column(Modifier.weight(1f)) {
                        Text(if (choosing) "Liz’s Shopping List" else editing?.item?.name ?: shop.orEmpty(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (!choosing && editing == null) TextButton(onClick = { choosing = true }, contentPadding = PaddingValues(0.dp)) { Text("Change shop ▾") }
                    }
                    TextButton(onClick = { more = true }) { Text("•••", modifier = Modifier.semantics { contentDescription = "List and backup options" }) }
                }
            }
        },
        bottomBar = {
            if (!choosing && editing == null && shop != null) NavigationBar(containerColor = Color.White) {
                listOf("All Items", "Favourites", "Shopping List").forEachIndexed { n, label ->
                    NavigationBarItem(selected = tab == n, onClick = { tab = n; query = "" }, icon = { Text(listOf("▦", "♡", "☑")[n], fontSize = 24.sp) }, label = { Text(label) })
                }
            }
        }
    ) { padding ->
        val s = state
        if (s == null) {
            Column(Modifier.padding(padding).padding(24.dp)) { if (loadFailed) Text("Saved data could not be loaded. Open ••• to restore a backup, or restart the app.") else { CircularProgressIndicator(); Text("Loading saved shopping lists…") } }
        } else if (choosing || shop == null) {
            LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Text("Where are we shopping today?", style = MaterialTheme.typography.titleLarge); Spacer(Modifier.height(8.dp)); Text("Your lists, ready when you need them.", color = Ink) }
                items(shops) { name ->
                    val tint = when(name) { "Lidl" -> Color(0xFFE6F0FA); "Aldi" -> Color(0xFFEBEDF9); "Dunnes Stores" -> Color(0xFFE7F1E9); else -> Color(0xFFF8EAEA) }
                    Card(onClick = { vm.select(name); choosing = false; tab = 0; query = ""; category = null }, colors = CardDefaults.cardColors(containerColor = tint), shape = RoundedCornerShape(22.dp)) {
                        Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) { Text(name, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("${s.stores.getValue(name).items.count { !it.done }} items to buy" + if (s.selected == name) " · Last used" else "", fontSize = 14.sp) }
                            Text("›", fontSize = 30.sp)
                        }
                    }
                }
                item { Text("Simple lists. Happier shopping.", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp)) }
            }
        } else {
            val data = s.stores.getValue(shop)
            val edit = editing
            if (edit != null) {
                key(edit) { ProductEditor(Modifier.padding(padding), edit, data.favourites.any { it.key() == edit.item.key() }, onCancel = { editing = null }) { item, heart ->
                    when (edit.mode) {
                        "list" -> vm.edit(shop, item)
                        "favourite" -> vm.editFavourite(shop, edit.item, item)
                        "custom" -> vm.custom(item, shop, heart)
                        else -> vm.add(shop, item)
                    }
                    if (edit.mode != "custom" && edit.mode != "favourite") {
                        val wasFavourite = data.favourites.any { it.key() == edit.item.key() }
                        if (heart && !wasFavourite) vm.favourite(shop, item) else if (!heart && wasFavourite) vm.removeFavourite(shop, edit.item)
                    }
                    editing = null
                } }
            } else {
                Column(Modifier.padding(padding).fillMaxSize()) {
                    OutlinedTextField(query, { query = it.take(100) }, label = { Text("Search for a product") }, singleLine = true, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                    when(tab) {
                        0 -> {
                            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item { Button(onClick = { editing = Editing(Item(name = ""), null, "custom") }, modifier = Modifier.fillMaxWidth()) { Text("+ Add custom item") } }
                                item {
                                    Text("Categories", fontWeight = FontWeight.Bold)
                                    CategoryPicker(category) { category = it }
                                }
                                val products = (Catalogue.products + s.custom).distinctBy { it.name.lowercase(java.util.Locale.ROOT) }.filter { (category == null || it.category == category) && it.name.contains(query, true) }
                                items(products, key = { it.name.lowercase(java.util.Locale.ROOT) }) { p ->
                                    ProductCard(p.symbol, p.name, p.category, { editing = Editing(Item(name = p.name, variety = p.varieties.first(), size = p.sizes.first()), p, "add") })
                                }
                                if (products.isEmpty()) item { Text("No matches. Add a custom item above.") }
                            }
                        }
                        1 -> {
                            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item { Text("Your usual picks for $shop", style = MaterialTheme.typography.titleMedium) }
                                val favourites = data.favourites.filter { it.name.contains(query, true) }
                                if (favourites.isEmpty()) item { Text("Save an item with the heart when adding it. Your favourites belong to this shop.") }
                                items(favourites, key = { it.id }) { i ->
                                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
                                        Column(Modifier.padding(16.dp)) {
                                            Text("${Catalogue.symbol(i.name)}  ${i.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                            Text("${i.detail()} · ×${i.quantity}")
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                TextButton(onClick = { editing = Editing(i, Catalogue.find(i.name), "favourite") }) { Text("Edit") }
                                                TextButton(onClick = { vm.removeFavourite(shop, i) }) { Text("♥", fontSize = 24.sp, modifier = Modifier.semantics { contentDescription = "Remove ${i.name} from $shop favourites" }) }
                                                Button(onClick = { vm.add(shop, i.copy(id = UUID.randomUUID().toString())) }) { Text("+ Add") }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {
                            LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                item { Text("${data.items.count { it.done }} of ${data.items.size} completed", style = MaterialTheme.typography.titleMedium) }
                                if (data.items.isEmpty()) item { Text("Your shopping list is empty. Add everyday essentials or choose from favourites.") }
                                items(data.items.filter { it.name.contains(query, true) }.sortedBy { it.done }, key = { it.id }) { i ->
                                    Card(colors = CardDefaults.cardColors(containerColor = if (i.done) Color(0xFFEDF0EB) else Color.White), shape = RoundedCornerShape(20.dp)) {
                                        Column(Modifier.padding(12.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Checkbox(checked = i.done, onCheckedChange = { vm.complete(shop, i.id) }, modifier = Modifier.semantics { contentDescription = if(i.done) "Mark ${i.name} still to buy" else "Mark ${i.name} bought" })
                                                Column(Modifier.weight(1f)) { Text(i.name, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(i.detail()); if(i.done) Text("Bought ✓", fontWeight = FontWeight.Bold) }
                                                Text(Catalogue.symbol(i.name), fontSize = 28.sp, modifier = Modifier.padding(8.dp))
                                            }
                                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Quantity(i.quantity, { vm.quantity(shop, i.id, -1) }, { vm.quantity(shop, i.id, 1) }, i.name)
                                                TextButton(onClick = { editing = Editing(i, Catalogue.find(i.name), "list") }) { Text("Edit") }
                                                TextButton(onClick = { vm.remove(shop, i.id) }) { Text("Remove") }
                                            }
                                        }
                                    }
                                }
                                item { Button(onClick = { tab = 0; query = "" }, modifier = Modifier.fillMaxWidth()) { Text("+ Add another item") } }
                            }
                        }
                    }
                }
            }
        }
    }
    if (more) AlertDialog(onDismissRequest = { more = false }, title = { Text("Shopping options") }, text = {
        Column {
            if (shop != null && !choosing) {
                TextButton(onClick = { more = false; confirm = "Clear completed items" }) { Text("Clear completed items") }
                TextButton(onClick = { more = false; confirm = "Start a fresh list" }) { Text("Start a fresh list") }
                TextButton(onClick = { more = false; vm.reuse(shop) }, enabled = state?.stores?.get(shop)?.previous?.isNotEmpty() == true) { Text("Reuse previous items") }
            }
            TextButton(onClick = { more = false; export.launch("Liz-Shopping-Backup.json") }, enabled = state != null) { Text("Save backup") }
            TextButton(onClick = { more = false; import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Restore backup") }
            Text("Product sizes are suggestions, not shop stock records.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { more = false }) { Text("Close") } })
    if (confirm != null && shop != null) AlertDialog(onDismissRequest = { confirm = null }, title = { Text(confirm!!) }, text = { Text("This changes only your $shop shopping list. Favourites are kept. Removed items can be reused from Shopping options.") }, confirmButton = { TextButton(onClick = { if (confirm == "Clear completed items") vm.clearCompleted(shop) else vm.fresh(shop); confirm = null }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } })
    if (restoreReady) AlertDialog(onDismissRequest = vm::cancelRestore, title = { Text("Restore this backup?") }, text = { Text("This replaces all four shopping lists, favourites and custom products. Save a backup first if you want to keep your current data.") }, confirmButton = { TextButton(onClick = { vm.restore(); choosing = true; editing = null }) { Text("Replace data") } }, dismissButton = { TextButton(onClick = vm::cancelRestore) { Text("Cancel") } })
}

@Composable
private fun CategoryPicker(selected: String?, onSelect: (String?) -> Unit) {
    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item { FilterChip(selected == null, onClick = { onSelect(null) }, label = { Text("All") }, modifier = Modifier.heightIn(min = 48.dp)) }
        items(Catalogue.categories) { c -> FilterChip(selected == c, onClick = { onSelect(if (selected == c) null else c) }, label = { Text(c) }, modifier = Modifier.heightIn(min = 48.dp)) }
    }
}

@Composable
private fun ProductCard(symbol: String, name: String, detail: String, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(symbol, fontSize = 32.sp, modifier = Modifier.padding(end = 16.dp))
            Column(Modifier.weight(1f)) { Text(name, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text(detail, style = MaterialTheme.typography.bodySmall) }
            Text("›", fontSize = 28.sp)
        }
    }
}

@Composable
private fun Quantity(value: Int, minus: () -> Unit, plus: () -> Unit, name: String = "item") {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = minus, enabled = value > 1, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp).semantics { contentDescription = "Decrease $name quantity" }, shape = RoundedCornerShape(14.dp)) { Text("−", fontSize = 24.sp) }
        Text("×$value", modifier = Modifier.widthIn(min = 44.dp).padding(horizontal = 6.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        OutlinedButton(onClick = plus, enabled = value < 999, contentPadding = PaddingValues(0.dp), modifier = Modifier.size(48.dp).semantics { contentDescription = "Increase $name quantity" }, shape = RoundedCornerShape(14.dp)) { Text("+", fontSize = 24.sp) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Options(title: String, options: List<String>, value: String, onChange: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.filter { it.isNotBlank() }.forEach { option -> FilterChip(value == option, onClick = { onChange(option) }, label = { Text(option) }, modifier = Modifier.heightIn(min = 48.dp)) }
    }
    OutlinedTextField(value, { onChange(it.take(100)) }, label = { Text("$title or your own") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp))
}

@Composable
private fun ProductEditor(modifier: Modifier, edit: Editing, initialHeart: Boolean, onCancel: () -> Unit, onSave: (Item, Boolean) -> Unit) {
    var name by rememberSaveable { mutableStateOf(edit.item.name) }
    var variety by rememberSaveable { mutableStateOf(edit.item.variety) }
    var size by rememberSaveable { mutableStateOf(edit.item.size) }
    var qty by rememberSaveable { mutableIntStateOf(edit.item.quantity) }
    var heart by rememberSaveable { mutableStateOf(initialHeart) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(edit.product?.symbol ?: "🛍", fontSize = 64.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        if (edit.mode == "custom" || Catalogue.find(edit.item.name) == null) OutlinedTextField(name, { name = it.take(100) }, label = { Text("Product name (required)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Options("Variety", edit.product?.varieties ?: emptyList(), variety) { variety = it }
        Options("Package size / unit", edit.product?.sizes ?: emptyList(), size) { size = it }
        Text("Number of packages", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Quantity(qty, { qty = (qty - 1).coerceAtLeast(1) }, { qty = (qty + 1).coerceAtMost(999) }, name)
        Text(if(size.startsWith("Loose")) "For loose items, enter an amount such as 500 g above. Quantity multiplies that amount." else "Size describes one package. Quantity is how many packages to buy.", style = MaterialTheme.typography.bodySmall)
        if (edit.mode != "favourite") Row(Modifier.fillMaxWidth().clickable { heart = !heart }.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(heart, { heart = it }); Text(if (initialHeart) "♥  Favourite saved (usual quantity kept)" else "♥  Save as a favourite for this shop")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            try { onSave(Rules.checked(edit.item.copy(name = name, variety = variety, size = size, quantity = qty)), heart) }
            catch(e: IllegalArgumentException) { error = e.message }
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(16.dp), enabled = name.isNotBlank()) { Text(if(edit.mode == "list" || edit.mode == "favourite") "Save changes" else "+ Add to shopping list", fontSize = 18.sp) }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}
