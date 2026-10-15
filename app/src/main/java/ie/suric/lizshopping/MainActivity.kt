package ie.suric.lizshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private data class Editing(val item: Item, val product: Product?, val mode: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preferences = getSharedPreferences("appearance", MODE_PRIVATE)
        setContent {
            var themeMode by rememberSaveable { mutableStateOf(preferences.getString("theme", "system") ?: "system") }
            LaunchedEffect(themeMode) { preferences.edit().putString("theme", themeMode).apply() }
            LizTheme(themeMode) { ShoppingApp(themeMode, { themeMode = it }) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShoppingApp(themeMode: String = "system", onThemeChange: (String) -> Unit = {}, vm: ShoppingViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val restoreReady by vm.restoreReady.collectAsStateWithLifecycle()
    val loadFailed by vm.loadFailed.collectAsStateWithLifecycle()
    var choosing by rememberSaveable { mutableStateOf(true) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    var more by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<String?>(null) }
    var selectedImage by remember { mutableStateOf<ImportedImage?>(null) }
    var addingShop by remember { mutableStateOf(false) }
    var newShopName by rememberSaveable { mutableStateOf("") }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snack = remember { SnackbarHostState() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(editing) {
        if (editing != null) { focusManager.clearFocus(); keyboard?.hide(); snack.currentSnackbarData?.dismiss() }
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(vm::export) }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::readBackup) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selectedUri -> scope.launch {
            try { selectedImage = withContext(Dispatchers.IO) { ImageStore.copyFromUri(context, selectedUri) }; vm.message.value = "Image ready to save" }
            catch (_: Exception) { vm.message.value = "Image could not be used." }
        } }
    }
    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); vm.message.value = null } }
    val shop = state?.selected
    fun goHome() { choosing = true; settings = false; editing = null; query = ""; category = null }
    fun closeDrawer(action: () -> Unit) { scope.launch { drawerState.close(); action() } }
    BackHandler(!choosing || settings || editing != null) {
        when { editing != null -> editing = null; settings -> settings = false; else -> goHome() }
    }
    val s = state
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = s != null,
        drawerContent = {
            if (s != null) NavigationDrawerContentV2(s.shopNames(), shop, choosing, tab, themeMode, { closeDrawer(::goHome) }, { selected -> closeDrawer { vm.select(selected); choosing = false; settings = false; tab = 2; query = "" } }, { destination -> closeDrawer { vm.select(s.selected ?: s.shopNames().first()); choosing = false; settings = false; tab = destination; query = "" } }, { closeDrawer { choosing = false; settings = true } }, onThemeChange)
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { if (editing == null) SnackbarHost(snack) },
            topBar = {
                AppTopBarV2(
                    title = when { settings -> "Settings"; choosing -> "Liz's Shopping List"; editing != null -> editing?.item?.name.orEmpty(); else -> shop.orEmpty() },
                    onMenu = { scope.launch { drawerState.open() } },
                    onBack = if (settings || editing != null) ({ if (editing != null) editing = null else settings = false }) else null,
                    onOptions = if (!choosing && !settings && editing == null) ({ more = true }) else null,
                    onSettings = if (choosing) ({ settings = true; choosing = false }) else null
                )
                if (!choosing && !settings && editing == null && shop != null) TextButton(onClick = ::goHome, modifier = Modifier.padding(start = 56.dp)) { Text("Change shop ▾") }
            },
            bottomBar = {
                if (!choosing && !settings && editing == null && shop != null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf("All Items", "Favourites", "Shopping List").forEachIndexed { index, label ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index; query = "" }, icon = { Text(listOf("▦", "♡", "☑")[index], fontSize = 24.sp) }, label = { Text(label) })
                    }
                }
            }
        ) { padding ->
            if (s == null) {
                Column(Modifier.padding(padding).padding(24.dp)) { if (loadFailed) Text("Saved data could not be loaded. Open options to restore a backup, or restart the app.") else { CircularProgressIndicator(); Text("Loading saved shopping lists…") } }
            } else if (settings) {
                SettingsPage(Modifier.padding(padding), s, themeMode, onThemeChange, { addingShop = true }, vm::renameShop, vm::removeShop)
            } else if (choosing || shop == null) {
                val homeShop = s.selected ?: s.shopNames().first()
                HomePageV2(Modifier.padding(padding), s, { selected -> vm.select(selected); choosing = false; tab = 0; query = ""; category = null }, { vm.select(homeShop); choosing = false; tab = 1 }, { vm.select(homeShop); choosing = false; tab = 2 }, { vm.select(homeShop); choosing = false; tab = 0 })
            } else {
                val data = s.dataFor(shop)
                val edit = editing
        if (edit != null) {
                    ProductEditor(Modifier.padding(padding), edit, data.favourites.any { it.key() == edit.item.key() }, selectedImage, { imagePicker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) }, { selectedImage = null; editing = null }) { item, heart, image ->
                        when (edit.mode) { "list" -> vm.edit(shop, item); "favourite" -> vm.editFavourite(shop, edit.item, item); "custom" -> vm.custom(item, shop, heart, image); else -> vm.add(shop, item) }
                        if (image != null && edit.mode != "custom") vm.importProductImage(edit.item.productId ?: edit.product?.id.orEmpty(), image)
                        if (edit.mode != "custom" && edit.mode != "favourite") { val wasFavourite = data.favourites.any { it.key() == edit.item.key() }; if (heart && !wasFavourite) vm.favourite(shop, item) else if (!heart && wasFavourite) vm.removeFavourite(shop, edit.item) }
                        selectedImage = null
                        editing = null
                    }
                } else {
                    when (tab) {
                        0 -> AllItemsPage(Modifier.padding(padding), shop, s, query, category, { query = it.take(100) }, { category = it }, { product -> editing = Editing(Item(name = product.name, variety = product.varieties.firstOrNull().orEmpty(), size = product.sizes.firstOrNull().orEmpty(), productId = product.id), product, "add") }, { editing = Editing(Item(name = ""), null, "custom") })
                        1 -> FavouritesPage(Modifier.padding(padding), shop, data, query, { query = it.take(100) }, { item -> editing = Editing(item, s.productFor(item) ?: s.allProducts().firstOrNull { p -> p.name.equals(item.name, true) }, "favourite") }, { item -> vm.removeFavourite(shop, item) }, { item -> vm.add(shop, item.copy(id = UUID.randomUUID().toString(), productId = item.productId)) })
                        else -> ShoppingListPage(Modifier.padding(padding), shop, data, { item -> vm.complete(shop, item.id) }, { item -> vm.quantity(shop, item.id, -1) }, { item -> vm.quantity(shop, item.id, 1) }, { item -> editing = Editing(item, s.productFor(item) ?: s.allProducts().firstOrNull { p -> p.name.equals(item.name, true) }, "list") }, { item -> vm.remove(shop, item.id) }, { tab = 0; query = "" })
                    }
                }
            }
        }
    }
    if (more) AlertDialog(onDismissRequest = { more = false }, title = { Text("Shopping options") }, text = { Column { if (shop != null && !choosing) { TextButton(onClick = { more = false; confirm = "Clear completed items" }) { Text("Clear completed items") }; TextButton(onClick = { more = false; confirm = "Start a fresh list" }) { Text("Start a fresh list") }; TextButton(onClick = { more = false; vm.reuse(shop) }, enabled = state?.dataFor(shop)?.previous?.isNotEmpty() == true) { Text("Reuse previous items") } }; TextButton(onClick = { more = false; export.launch("Liz-Shopping-Backup.json") }, enabled = state != null) { Text("Save backup") }; TextButton(onClick = { more = false; import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) { Text("Restore backup") }; Text("Product sizes are suggestions, not shop stock records.", style = MaterialTheme.typography.bodySmall) } }, confirmButton = { TextButton(onClick = { more = false }) { Text("Close") } })
    if (confirm != null && shop != null) AlertDialog(onDismissRequest = { confirm = null }, title = { Text(confirm!!) }, text = { Text("This changes only your $shop shopping list. Favourites are kept. Removed items can be reused from Shopping options.") }, confirmButton = { TextButton(onClick = { if (confirm == "Clear completed items") vm.clearCompleted(shop) else vm.fresh(shop); confirm = null }) { Text("Confirm") } }, dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } })
    if (restoreReady) AlertDialog(onDismissRequest = vm::cancelRestore, title = { Text("Restore this backup?") }, text = { Text("This replaces all four shopping lists, favourites and custom products. Save a backup first if you want to keep your current data.") }, confirmButton = { TextButton(onClick = { vm.restore(); choosing = true; editing = null }) { Text("Replace data") } }, dismissButton = { TextButton(onClick = vm::cancelRestore) { Text("Cancel") } })
    if (addingShop) AlertDialog(
        onDismissRequest = { addingShop = false },
        title = { Text("Add a shop") },
        text = { OutlinedTextField(newShopName, { newShopName = it.take(100) }, label = { Text("Shop name") }, singleLine = true) },
        confirmButton = { TextButton(onClick = { vm.addShop(newShopName); newShopName = ""; addingShop = false; choosing = true }) { Text("Add") } },
        dismissButton = { TextButton(onClick = { addingShop = false }) { Text("Cancel") } }
    )
}

@Composable
private fun AppTopBar(title: String, onMenu: () -> Unit, onBack: (() -> Unit)?, onOptions: (() -> Unit)?, onSettings: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back" }) { Text("‹", fontSize = 36.sp) } else IconButton(onClick = onMenu, modifier = Modifier.semantics { contentDescription = "Open navigation menu" }) { Text("☰", fontSize = 28.sp) }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        if (onOptions != null) IconButton(onClick = onOptions, modifier = Modifier.semantics { contentDescription = "List and backup options" }) { Text("⋮", fontSize = 28.sp) } else Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun AppTopBarV2(title: String, onMenu: () -> Unit, onBack: (() -> Unit)?, onOptions: (() -> Unit)?, onSettings: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back" }) { Text("\u2039", fontSize = 36.sp) } else IconButton(onClick = onMenu, modifier = Modifier.semantics { contentDescription = "Open navigation menu" }) { Text("\u2630", fontSize = 28.sp) }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        when {
            onSettings != null -> IconButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = "Open settings" }) { Text("\u2699", fontSize = 28.sp) }
            onOptions != null -> IconButton(onClick = onOptions, modifier = Modifier.semantics { contentDescription = "List and backup options" }) { Text("\u22ee", fontSize = 28.sp) }
            else -> Spacer(Modifier.size(48.dp))
        }
    }
}

@Composable
private fun HomePageV2(modifier: Modifier, state: ShoppingState, onShop: (String) -> Unit, onFavourites: () -> Unit, onList: () -> Unit, onAllItems: () -> Unit) {
    val current = state.selected ?: state.shopNames().first()
    val data = state.dataFor(current)
    val heroPainter = painterResource(R.drawable.grocery_hero_small)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { EditorialBanner("SHOP", "Choose where you're shopping today", "", accent = MaterialTheme.colorScheme.primary, imagePainter = heroPainter) }
        items(state.shopNames().chunked(2)) { pair -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { pair.forEach { name -> ShopCard(name, state.dataFor(name), state.selected == name, { onShop(name) }, Modifier.weight(1f)) }; if (pair.size == 1) Spacer(Modifier.weight(1f)) } }
        item { FeatureCard("FAVOURITES", if (data.favourites.isEmpty()) "Build your usual picks" else "${data.favourites.size} usual products", "\u2661", onFavourites, MaterialTheme.colorScheme.secondary, heroPainter) }
        item { FeatureCard("MY LIST", "View and manage your shopping list", "\u2611", onList, MaterialTheme.colorScheme.primary, heroPainter) }
        item { FeatureCard("ALL ITEMS", "Browse products by category", "\u25A6", onAllItems, MaterialTheme.colorScheme.tertiary, heroPainter) }
    }
}

@Composable
private fun HomePage(modifier: Modifier, state: ShoppingState, onShop: (String) -> Unit, onFavourites: () -> Unit, onList: () -> Unit, onAllItems: () -> Unit) {
    val current = state.selected ?: state.shopNames().first()
    val data = state.dataFor(current)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { EditorialBanner("SHOP", "Choose where you're shopping today", "🛍", accent = MaterialTheme.colorScheme.primary) }
        items(state.shopNames().chunked(2)) { pair -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { pair.forEach { name -> ShopCard(name, state.dataFor(name), state.selected == name, { onShop(name) }, Modifier.weight(1f)) }; if (pair.size == 1) Spacer(Modifier.weight(1f)) } }
        item { SectionHeader("FAVOURITES", "See all", onFavourites); Spacer(Modifier.height(8.dp)); Card(onClick = onFavourites, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("♡", fontSize = 42.sp, color = MaterialTheme.colorScheme.secondary); Column(Modifier.padding(start = 14.dp)) { Text(if (data.favourites.isEmpty()) "Build your usual picks" else "${data.favourites.size} usual products", style = MaterialTheme.typography.titleLarge); Text("Quickly add your favourites for $current") } } } }
        item { SectionHeader("MY LIST", "Open list", onList); Spacer(Modifier.height(8.dp)); Card(onClick = onList, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Shopping at $current", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); Text("☑", fontSize = 28.sp) }; ShoppingProgress(data.items.count { it.done }, data.items.size) } } }
        item { SectionHeader("ALL ITEMS", "Browse catalogue", onAllItems); Spacer(Modifier.height(8.dp)); Card(onClick = onAllItems, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("▦", fontSize = 42.sp, color = MaterialTheme.colorScheme.tertiary); Column(Modifier.padding(start = 14.dp)) { Text("Everyday essentials", style = MaterialTheme.typography.titleLarge); Text("Search ${Catalogue.products.size} reusable products by category") } } } }
    }
}

@Composable
private fun AllItemsPage(modifier: Modifier, shop: String, state: ShoppingState, query: String, category: String?, onQuery: (String) -> Unit, onCategory: (String?) -> Unit, onProduct: (Product) -> Unit, onCustom: () -> Unit) {
    val products = state.allProducts().filter { (category == null || it.category == category) && it.name.contains(query.trim(), true) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { EditorialBanner("ALL ITEMS", "Browse products at $shop", "🥕", accent = MaterialTheme.colorScheme.primary) }
        item { OutlinedTextField(query, onQuery, label = { Text("Search for a product") }, singleLine = true, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) }
        item { Text("Categories", style = MaterialTheme.typography.titleMedium); CategoryPicker(category, onCategory) }
        item { Button(onClick = onCustom, modifier = Modifier.fillMaxWidth()) { Text("+ Add custom item") } }
        items(products, key = { it.id }) { product -> ProductCard(product) { onProduct(product) } }
        if (products.isEmpty()) item { EmptyState("No matching products", "Try another search or create a custom item.", "+ Add custom item", onCustom) }
    }
}

@Composable
private fun FavouritesPage(modifier: Modifier, shop: String, data: ShopData, query: String, onQuery: (String) -> Unit, onEdit: (Item) -> Unit, onRemove: (Item) -> Unit, onAdd: (Item) -> Unit) {
    val favourites = data.favourites.filter { it.name.contains(query.trim(), true) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { EditorialBanner("FAVOURITES", "Your everyday essentials at $shop", "🍅", accent = MaterialTheme.colorScheme.secondary) }
        item { OutlinedTextField(query, onQuery, label = { Text("Search favourites") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        if (favourites.isEmpty()) item { EmptyState("No favourites yet", "Save an item with the heart when adding it. Favourites belong to this shop.") }
        items(favourites, key = { it.id }) { item -> FavouriteCard(item, { onAdd(item) }, { onEdit(item) }, { onRemove(item) }) }
    }
}

@Composable
private fun ShoppingListPage(modifier: Modifier, shop: String, data: ShopData, onToggle: (Item) -> Unit, onMinus: (Item) -> Unit, onPlus: (Item) -> Unit, onEdit: (Item) -> Unit, onRemove: (Item) -> Unit, onAdd: () -> Unit) {
    val listItems = data.items.sortedBy { it.done }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { EditorialBanner("MY LIST", "Your shopping list for $shop", "🧺", accent = MaterialTheme.colorScheme.primary) }
        item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Shopping at $shop", style = MaterialTheme.typography.titleLarge); ShoppingProgress(data.items.count { it.done }, data.items.size) } } }
        item { SectionHeader("Your items") }
        if (listItems.isEmpty()) item { EmptyState("Your shopping list is empty", "Add everyday essentials or choose from your favourites.", "+ Add another item", onAdd) }
        items(listItems, key = { it.id }) { item -> ShoppingListRow(item, { onToggle(item) }, { onMinus(item) }, { onPlus(item) }, { onEdit(item) }, { onRemove(item) }) }
        item { Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("+ Add another item") } }
    }
}

@Composable
private fun CategoryPicker(selected: String?, onSelect: (String?) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { item { FilterChip(selected == null, { onSelect(null) }, label = { Text("All") }, modifier = Modifier.height(48.dp)) }; items(Catalogue.categories) { category -> FilterChip(selected == category, { onSelect(if (selected == category) null else category) }, label = { Text(category) }, modifier = Modifier.height(48.dp)) } }
}

@Composable
private fun SettingsPage(modifier: Modifier, state: ShoppingState, themeMode: String, onThemeChange: (String) -> Unit, onAddShop: () -> Unit, onRenameShop: (String, String) -> Unit, onRemoveShop: (String) -> Unit) {
    var editingShop by rememberSaveable { mutableStateOf<String?>(null) }
    var draftName by rememberSaveable { mutableStateOf("") }
    var removeTarget by remember { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { EditorialBanner("SETTINGS", "Make the app feel like yours", "⚙", accent = MaterialTheme.colorScheme.tertiary); Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Appearance", style = MaterialTheme.typography.titleLarge); Text("Choose how Liz's Shopping List should look.", color = MaterialTheme.colorScheme.onSurfaceVariant); ThemeControls(themeMode, onThemeChange) } }; Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Shops", style = MaterialTheme.typography.titleLarge); Text("Add a local shop without changing the four default shops.", color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = onAddShop, modifier = Modifier.fillMaxWidth()) { Text("+ Add a shop") }; state.shopDefinitions.filterNot { it.builtIn }.forEach { shop -> if (editingShop == shop.id) { OutlinedTextField(draftName, { draftName = it.take(100) }, label = { Text("New name") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { onRenameShop(shop.name, draftName); editingShop = null }) { Text("Save name") }; TextButton(onClick = { editingShop = null }) { Text("Cancel") } } } else { Row(verticalAlignment = Alignment.CenterVertically) { Text(shop.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); TextButton(onClick = { editingShop = shop.id; draftName = shop.name }) { Text("Rename") }; TextButton(onClick = { removeTarget = shop.name }) { Text("Remove") } } } } } }; Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Text("Shopping lists, favourites and product data stay on this device and work offline.", modifier = Modifier.padding(18.dp)) } }
    if (removeTarget != null) AlertDialog(onDismissRequest = { removeTarget = null }, title = { Text("Remove ${removeTarget!!}?") }, text = { Text("A shop can only be removed when its lists and favourites are empty. The four default shops are always kept.") }, confirmButton = { TextButton(onClick = { onRemoveShop(removeTarget!!); removeTarget = null }) { Text("Remove") } }, dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("Cancel") } })
}

@Composable
private fun ThemeControls(themeMode: String, onThemeChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (value, label) -> FilterChip(themeMode == value, { onThemeChange(value) }, label = { Text(label) }, modifier = Modifier.height(48.dp)) } }
}

@Composable
private fun NavigationDrawerContentV2(availableShops: List<String>, selectedShop: String?, choosing: Boolean, tab: Int, themeMode: String, onHome: () -> Unit, onShop: (String) -> Unit, onTab: (Int) -> Unit, onSettings: () -> Unit, onThemeChange: (String) -> Unit) {
    ModalDrawerSheet(modifier = Modifier.widthIn(max = 320.dp), drawerContainerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Liz's Shopping List", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            Text("Choose a shop and keep your list close.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(8.dp))
            DrawerItemV2("\u2302", "Home", "Choose a shop and start shopping", choosing, onHome)
            Text("YOUR SHOPS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 8.dp, top = 12.dp))
            availableShops.forEach { name -> DrawerItemV2("\u2022", name, "Your $name list", !choosing && selectedShop == name && tab == 2, { onShop(name) }) }
            Spacer(Modifier.height(8.dp))
            DrawerItemV2("\u2661", "Favourites", "Your usual products", !choosing && tab == 1, { onTab(1) })
            DrawerItemV2("\u2611", "Current Shopping List", "View, tick off and edit", !choosing && tab == 2, { onTab(2) })
            DrawerItemV2("\u25A6", "All Items", "Browse reusable products", !choosing && tab == 0, { onTab(0) })
            DrawerItemV2("\u2699", "Settings", "Appearance and app options", false, onSettings)
            Text("APPEARANCE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 8.dp, top = 12.dp))
            ThemeControls(themeMode, onThemeChange)
        }
    }
}

@Composable
private fun DrawerItemV2(icon: String, title: String, description: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).padding(horizontal = 10.dp, vertical = 10.dp).semantics { contentDescription = "$title. $description" }, verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.widthIn(min = 36.dp))
        Column(Modifier.weight(1f).padding(horizontal = 8.dp)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("\u203A", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NavigationDrawerContent(selectedShop: String?, choosing: Boolean, tab: Int, themeMode: String, onHome: () -> Unit, onShop: (String) -> Unit, onTab: (Int) -> Unit, onSettings: () -> Unit, onThemeChange: (String) -> Unit) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Liz's Shopping List", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary); Text("A calmer way to shop", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(8.dp)); DrawerItem("⌂", "Home", choosing, onHome, "Choose a shop and start shopping"); Text("YOUR SHOPS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 12.dp)); shops.forEach { name -> DrawerItem("●", name, !choosing && selectedShop == name && tab == 2, { onShop(name) }, "Your $name list") }; Spacer(Modifier.height(4.dp)); DrawerItem("♡", "Favourites", !choosing && tab == 1, { onTab(1) }, "Your usual products"); DrawerItem("☑", "Current Shopping List", !choosing && tab == 2, { onTab(2) }, "View, tick off and edit"); DrawerItem("▦", "All Items", !choosing && tab == 0, { onTab(0) }, "Browse reusable products"); DrawerItem("⚙", "Settings", false, onSettings, "Appearance and app options"); Text("APPEARANCE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 12.dp)); ThemeControls(themeMode, onThemeChange) } }
}

@Composable
private fun DrawerItem(icon: String, title: String, selected: Boolean, onClick: () -> Unit, description: String) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).padding(12.dp).semantics { contentDescription = "$title. $description" }, verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.widthIn(min = 36.dp)); Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductEditor(modifier: Modifier, edit: Editing, initialHeart: Boolean, selectedImage: ImportedImage?, onPickImage: () -> Unit, onCancel: () -> Unit, onSave: (Item, Boolean, ImportedImage?) -> Unit) {
    var name by rememberSaveable { mutableStateOf(edit.item.name) }
    var variety by rememberSaveable { mutableStateOf(edit.item.variety) }
    var size by rememberSaveable { mutableStateOf(edit.item.size) }
    var qty by rememberSaveable { mutableIntStateOf(edit.item.quantity) }
    var heart by rememberSaveable { mutableStateOf(initialHeart) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EditorialBanner(if (edit.mode == "custom") "ADD ITEM" else "CONFIGURE", if (edit.mode == "custom") "Add a custom item to your shopping list" else "Choose the details that work for you", edit.product?.symbol ?: "🛍", accent = MaterialTheme.colorScheme.primary)
        if (edit.mode == "custom" || Catalogue.find(edit.item.name) == null) OutlinedTextField(name, { name = it.take(100) }, label = { Text("Product name (required)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Options("Variety", edit.product?.varieties ?: emptyList(), variety) { variety = it }
        Options("Package size / unit", edit.product?.sizes ?: emptyList(), size) { size = it }
        if (edit.mode == "custom" || edit.product != null) {
            TextButton(onClick = { onPickImage() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (selectedImage == null) "Choose an offline product image" else "Image selected") }
        }
        Text("Number of packages", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Quantity(qty, { qty = (qty - 1).coerceAtLeast(1) }, { qty = (qty + 1).coerceAtMost(999) }, name)
        Text(if (size.startsWith("Loose")) "For loose items, enter an amount such as 500 g above. Quantity multiplies that amount." else "Size describes one package. Quantity is how many packages to buy.", style = MaterialTheme.typography.bodySmall)
        if (edit.mode != "favourite") Row(Modifier.fillMaxWidth().clickable { heart = !heart }.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { androidx.compose.material3.Checkbox(heart, { heart = it }); Text(if (initialHeart) "♥  Favourite saved (usual quantity kept)" else "♥  Save as a favourite for this shop") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { try { onSave(Rules.checked(edit.item.copy(name = name, variety = variety, size = size, quantity = qty)), heart, selectedImage) } catch (e: IllegalArgumentException) { error = e.message } }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp), enabled = name.isNotBlank()) { Text(if (edit.mode == "list" || edit.mode == "favourite") "Save changes" else "+ Add to shopping list", fontSize = 18.sp) }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun Quantity(value: Int, minus: () -> Unit, plus: () -> Unit, name: String = "item") {
    Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = minus, enabled = value > 1, modifier = Modifier.size(48.dp).semantics { contentDescription = "Decrease $name quantity" }) { Text("−", fontSize = 24.sp) }; Text("×$value", modifier = Modifier.widthIn(min = 52.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp); IconButton(onClick = plus, enabled = value < 999, modifier = Modifier.size(48.dp).semantics { contentDescription = "Increase $name quantity" }) { Text("+", fontSize = 24.sp) } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Options(title: String, options: List<String>, value: String, onChange: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.filter { it.isNotBlank() }.forEach { option -> FilterChip(value == option, { onChange(option) }, label = { Text(option) }, modifier = Modifier.height(48.dp)) } }
    OutlinedTextField(value, { onChange(it.take(100)) }, label = { Text("$title or your own") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
}
