package ie.suric.lizshopping

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

private data class Editing(val item: Item, val product: Product?, val mode: String)

private sealed class ActiveSheet {
    object ChangeShop : ActiveSheet()
    object AddShop : ActiveSheet()
    object Options : ActiveSheet()
    data class RenameShop(val shop: Shop) : ActiveSheet()
    data class RemoveShop(val shop: Shop) : ActiveSheet()
    data class Confirm(val title: String, val message: String, val action: () -> Unit) : ActiveSheet()
}

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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ShoppingApp(themeMode: String = "system", onThemeChange: (String) -> Unit = {}, vm: ShoppingViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val restoreReady by vm.restoreReady.collectAsStateWithLifecycle()
    val loadFailed by vm.loadFailed.collectAsStateWithLifecycle()
    // A fresh activity starts in the persisted shop list when one exists.
    // First-time users still land on Home because no shop is selected yet.
    var choosing by remember { mutableStateOf(false) }
    var settings by rememberSaveable { mutableStateOf(false) }
    // The current shopping list is the launch task for returning users. Tab
    // changes remain instant within the activity but do not override that
    // first screen after a process restart.
    var tab by remember { mutableIntStateOf(2) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Editing?>(null) }
    var sheet by remember { mutableStateOf<ActiveSheet?>(null) }
    var selectedImage by remember { mutableStateOf<ImportedImage?>(null) }
    var selectedShopImage by remember { mutableStateOf<ImportedImage?>(null) }
    var removeShopLogo by remember { mutableStateOf(false) }
    // Drawer visibility is transient UI state; do not restore an open drawer
    // over the user's shopping list after a process restart.
    val drawerState = remember { DrawerState(DrawerValue.Closed) }
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
    val shopImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { selectedUri -> scope.launch {
            try {
                selectedShopImage = withContext(Dispatchers.IO) { ImageStore.copyFromUri(context, selectedUri) }
                removeShopLogo = false
                vm.message.value = "Shop image ready to save"
            } catch (_: Exception) { vm.message.value = "Image could not be used." }
        } }
    }
    LaunchedEffect(message) { message?.let { snack.showSnackbar(it); vm.message.value = null } }
    val shop = state?.selected
    fun closeSheet() { sheet = null; selectedImage = null; selectedShopImage = null; removeShopLogo = false }
    fun goHome() { choosing = true; settings = false; editing = null; closeSheet(); query = ""; category = null }
    fun closeDrawer(action: () -> Unit) { scope.launch { drawerState.close(); action() } }
    BackHandler(sheet != null || !choosing || settings || editing != null) {
        when { sheet != null -> closeSheet(); editing != null -> editing = null; settings -> settings = false; else -> goHome() }
    }
    val s = state
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = s != null,
        drawerContent = {
            if (s != null) NavigationDrawerContentV2(s.shopDefinitions, shop, choosing, tab, themeMode, { closeDrawer(::goHome) }, { selected -> closeDrawer { vm.select(selected); choosing = false; settings = false; tab = 2; query = "" } }, { destination -> closeDrawer { vm.select(s.selected ?: s.shopNames().first()); choosing = false; settings = false; tab = destination; query = "" } }, { closeDrawer { choosing = false; settings = false; editing = Editing(Item(name = ""), null, "custom") } }, { closeDrawer { choosing = false; settings = true } }, onThemeChange)
        }
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Image(painterResource(R.drawable.liz_portrait_background), contentDescription = null, modifier = Modifier.align(Alignment.BottomEnd).size(340.dp).alpha(.16f), contentScale = ContentScale.Fit)
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { if (editing == null && sheet == null) SnackbarHost(snack) },
            topBar = {
                Column {
                    AppTopBarV2(
                        title = when { settings -> "Settings"; choosing -> "Liz's Shopping List"; editing != null -> editing?.item?.name.orEmpty(); else -> shop.orEmpty() },
                        shopMark = if (!choosing && !settings && editing == null) s?.shopDefinitions?.firstOrNull { it.name == shop } else null,
                        onMenu = { scope.launch { drawerState.open() } },
                        onBack = if (settings || editing != null) ({ if (editing != null) editing = null else settings = false }) else null,
                        onOptions = if (!choosing && !settings && editing == null && sheet == null) ({ sheet = ActiveSheet.Options }) else null,
                        onSettings = if (choosing) ({ settings = true; choosing = false }) else null
                    )
                    if (!choosing && !settings && editing == null && sheet == null && shop != null) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.OutlinedButton(onClick = { sheet = ActiveSheet.ChangeShop }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Change shop") }
                        }
                    }
                }
            },
            bottomBar = {
                if (!choosing && !settings && editing == null && sheet == null && shop != null) NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    listOf("All Items", "Favourites", "Shopping List").forEachIndexed { index, label ->
                        NavigationBarItem(selected = tab == index, onClick = { tab = index; query = "" }, icon = { Text(listOf("\u25A6", "\u2661", "\u2611")[index], fontSize = 24.sp) }, label = { Text(label) })
                    }
                }
            }
        ) { padding ->
            val route = when {
                s == null -> "loading"
                settings -> "settings"
                choosing || shop == null -> "home"
                editing != null -> "editor"
                else -> "shop:${shop}:$tab"
            }
            if (route == "loading" || s == null) {
                Column(Modifier.padding(padding).padding(24.dp)) { if (loadFailed) Text("Saved data could not be loaded. Open options to restore a backup, or restart the app.") else { CircularProgressIndicator(); Text("Loading saved shopping lists\u2026") } }
            } else if (route == "settings") {
                SettingsPageV2(Modifier.padding(padding), s, themeMode, onThemeChange, { sheet = ActiveSheet.AddShop; selectedShopImage = null }, { target -> selectedShopImage = null; removeShopLogo = false; sheet = ActiveSheet.RenameShop(target) }, { name -> sheet = ActiveSheet.RemoveShop(s.shop(name)) })
            } else if (route == "home") {
                val homeShop = s.selected ?: s.shopNames().first()
                HomePageV2(Modifier.padding(padding), s, { selected -> vm.select(selected); choosing = false; tab = 0; query = ""; category = null }, { vm.select(homeShop); choosing = false; tab = 1 }, { vm.select(homeShop); choosing = false; tab = 2 }, { vm.select(homeShop); choosing = false; tab = 0 })
            } else {
                val activeShop = requireNotNull(shop)
                val data = s.dataFor(activeShop)
                val edit = editing
        if (route == "editor" && edit != null) {
                    LizBottomSheet(if (edit.mode == "custom") "Add new product" else "Edit ${edit.item.name}", "Configure this product without leaving your shopping list.", { selectedImage = null; editing = null }) {
                    ProductEditor(Modifier.fillMaxWidth(), edit, s.allProducts(), data.favourites.any { it.key() == edit.item.key() }, selectedImage, { imagePicker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) }, { selectedImage = null; editing = null }) { item, productCategory, heart, image ->
                        when (edit.mode) { "list" -> vm.edit(activeShop, item); "favourite" -> vm.editFavourite(activeShop, edit.item, item); "custom" -> vm.custom(item, activeShop, productCategory, heart, image); else -> vm.add(activeShop, item) }
                        if (image != null && edit.mode != "custom") vm.importProductImage(edit.item.productId ?: edit.product?.id.orEmpty(), image)
                        if (edit.mode != "custom" && edit.mode != "favourite") { val wasFavourite = data.favourites.any { it.key() == edit.item.key() }; if (heart && !wasFavourite) vm.favourite(activeShop, item) else if (!heart && wasFavourite) vm.removeFavourite(activeShop, edit.item) }
                        selectedImage = null
                        editing = null
                    }
                    }
                } else {
                    when (tab) {
                         0 -> AllItemsPage(Modifier.padding(padding), activeShop, s, query, category, { query = it.take(100) }, { category = it }, { product -> editing = Editing(Item(name = product.name, variety = product.varieties.firstOrNull().orEmpty(), size = product.sizes.firstOrNull().orEmpty(), productId = product.id), product, "add") }, { product -> vm.addProduct(activeShop, product) }, { editing = Editing(Item(name = ""), null, "custom") }, { product -> toggleFavourite(vm, activeShop, data, product) })
                         1 -> FavouritesPage(Modifier.padding(padding), activeShop, data, query, { query = it.take(100) }, { item -> editing = Editing(item, s.productFor(item) ?: s.allProducts().firstOrNull { p -> p.name.equals(item.name, true) }, "favourite") }, { item -> vm.removeFavourite(activeShop, item) }, { item -> vm.add(activeShop, item.copy(id = UUID.randomUUID().toString(), productId = item.productId)) })
                         else -> ShoppingListPage(Modifier.padding(padding), activeShop, data, { item -> vm.complete(activeShop, item.id) }, { item -> vm.quantity(activeShop, item.id, -1) }, { item -> vm.quantity(activeShop, item.id, 1) }, { item -> editing = Editing(item, s.productFor(item) ?: s.allProducts().firstOrNull { p -> p.name.equals(item.name, true) }, "list") }, { item -> vm.remove(activeShop, item.id) }, { tab = 0; query = "" })
                    }
                }
            }
        }
    }
    }
    if (restoreReady) ConfirmSheet(
        "Restore this backup?",
        "This replaces all shopping lists, favourites and custom products. Save a backup first if you want to keep current data.",
        { vm.restore(); choosing = true; closeSheet() },
        vm::cancelRestore
    )
    when (val active = sheet) {
        ActiveSheet.ChangeShop -> if (s != null) ChangeShopSheet(s, shop, { selected -> vm.select(selected); choosing = false; closeSheet() }, ::closeSheet)
        ActiveSheet.AddShop -> if (s != null) AddShopSheet(s.shopNames(), selectedShopImage, { shopImagePicker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) }, { selectedShopImage = null }, { name, logo ->
            try {
                val clean = Rules.validateShopName(name)
                require(s.shopNames().none { it.equals(clean, true) }) { "That shop already exists." }
                vm.addShop(clean, logo); choosing = true; closeSheet(); null
            } catch (e: IllegalArgumentException) { e.message ?: "Shop name is not valid." }
        }, { dirty -> if (dirty) sheet = ActiveSheet.Confirm("Discard shop name?", "The entered name will be lost.", ::closeSheet) else closeSheet() })
        ActiveSheet.Options -> if (s != null && shop != null) ShoppingOptionsSheet(
            hasPrevious = s.dataFor(shop).previous.isNotEmpty(),
            onClearCompleted = { sheet = ActiveSheet.Confirm("Clear completed items?", "Completed items will be removed from this shop's active list. Favourites stay saved.", { vm.clearCompleted(shop); closeSheet() }) },
            onFresh = { sheet = ActiveSheet.Confirm("Start a fresh list?", "Current items move to previous items so they can be reused later.", { vm.fresh(shop); closeSheet() }) },
            onReuse = { vm.reuse(shop); closeSheet() },
            onBackup = { closeSheet(); export.launch("Liz-Shopping-Backup.json") },
            onRestore = { closeSheet(); import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            onDismiss = ::closeSheet
        )
        is ActiveSheet.RenameShop -> RenameShopSheet(active.shop, selectedShopImage, removeShopLogo, { shopImagePicker.launch(arrayOf("image/png", "image/jpeg", "image/webp")) }, { selectedShopImage = null; removeShopLogo = active.shop.logoImageRef != null }, { selectedShopImage = null; removeShopLogo = true }, { name, logo, removeLogo ->
            try {
                val clean = Rules.validateShopName(name)
                require(s!!.shopDefinitions.none { it.id != active.shop.id && it.name.equals(clean, true) }) { "That shop already exists." }
                vm.renameShop(active.shop.name, clean)
                if (removeLogo || logo != null) vm.updateShopLogo(active.shop.id, if (removeLogo) null else logo)
                closeSheet()
            } catch (e: IllegalArgumentException) { vm.message.value = e.message ?: "Shop name is not valid." }
        }, { dirty -> if (dirty) sheet = ActiveSheet.Confirm("Discard shop name?", "The entered name will be lost.", ::closeSheet) else closeSheet() })
        is ActiveSheet.RemoveShop -> RemoveShopSheet(active.shop, { vm.removeShop(active.shop.name); closeSheet() }, ::closeSheet)
        is ActiveSheet.Confirm -> ConfirmSheet(active.title, active.message, { active.action(); if (sheet === active) closeSheet() }, ::closeSheet)
        null -> Unit
    }
}

@Composable
private fun AppTopBar(title: String, onMenu: () -> Unit, onBack: (() -> Unit)?, onOptions: (() -> Unit)?, onSettings: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back" }) { Text("\u2039", fontSize = 36.sp) } else IconButton(onClick = onMenu, modifier = Modifier.semantics { contentDescription = "Open navigation menu" }) { Text("\u2630", fontSize = 28.sp) }
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        if (onOptions != null) IconButton(onClick = onOptions, modifier = Modifier.semantics { contentDescription = "List and backup options" }) { Text("\u22EE", fontSize = 28.sp) } else Spacer(Modifier.size(48.dp))
    }
}

@Composable
private fun AppTopBarV2(title: String, shopMark: Shop?, onMenu: () -> Unit, onBack: (() -> Unit)?, onOptions: (() -> Unit)?, onSettings: (() -> Unit)?) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back" }) { Text("\u2039", fontSize = 36.sp) } else IconButton(onClick = onMenu, modifier = Modifier.semantics { contentDescription = "Open navigation menu" }) { Text("\u2630", fontSize = 28.sp) }
        if (shopMark != null) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                ShopMark(shopMark, Modifier.size(42.dp, 34.dp))
                Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp), textAlign = TextAlign.Center, maxLines = 2, softWrap = true)
            }
        } else Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, maxLines = 2, softWrap = true)
        when {
            onSettings != null -> IconButton(onClick = onSettings, modifier = Modifier.semantics { contentDescription = "Open settings" }) { Text("\u2699", fontSize = 28.sp) }
            onOptions != null -> IconButton(onClick = onOptions, modifier = Modifier.semantics { contentDescription = "List and backup options" }) { Text("\u22ee", fontSize = 28.sp) }
            else -> Spacer(Modifier.size(48.dp))
        }
    }
}

private fun toggleFavourite(vm: ShoppingViewModel, shop: String, data: ShopData, product: Product) {
    val existing = data.favourites.firstOrNull { it.productId == product.id || it.name.equals(product.name, true) }
    if (existing == null) vm.favourite(shop, Item(name = product.name, variety = product.varieties.firstOrNull().orEmpty(), size = product.sizes.firstOrNull().orEmpty(), productId = product.id))
    else vm.removeFavourite(shop, existing)
}

@Composable
private fun HomePortraitBanner() {
    Box(
        Modifier.fillMaxWidth().height(196.dp).clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Image(
            painter = painterResource(R.drawable.hero_home_liz),
            contentDescription = "Liz",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
private fun HomePageV2(modifier: Modifier, state: ShoppingState, onShop: (String) -> Unit, onFavourites: () -> Unit, onList: () -> Unit, onAllItems: () -> Unit) {
    val current = state.selected ?: state.shopNames().first()
    val data = state.dataFor(current)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { HomePortraitBanner() }
        items(state.shopDefinitions.chunked(2)) { pair -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { pair.forEach { item -> ShopCard(item, state.dataFor(item.name), state.selected == item.name, { onShop(item.name) }, Modifier.weight(1f)) }; if (pair.size == 1) Spacer(Modifier.weight(1f)) } }
        item { FeatureCard("FAVOURITES", if (data.favourites.isEmpty()) "Build your usual picks" else "${data.favourites.size} usual products", "\u2661", onFavourites, MaterialTheme.colorScheme.secondary, painterResource(R.drawable.hero_favourites)) }
        item { FeatureCard("MY LIST", "View and manage your shopping list", "\u2611", onList, MaterialTheme.colorScheme.primary, painterResource(R.drawable.hero_shopping_list)) }
        item { FeatureCard("ALL ITEMS", "Browse products by category", "\u25A6", onAllItems, MaterialTheme.colorScheme.tertiary, painterResource(R.drawable.hero_catalogue)) }
    }
}

@Composable
private fun HomePage(modifier: Modifier, state: ShoppingState, onShop: (String) -> Unit, onFavourites: () -> Unit, onList: () -> Unit, onAllItems: () -> Unit) {
    val current = state.selected ?: state.shopNames().first()
    val data = state.dataFor(current)
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { HomePortraitBanner() }
        items(state.shopDefinitions.chunked(2)) { pair -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { pair.forEach { item -> ShopCard(item, state.dataFor(item.name), state.selected == item.name, { onShop(item.name) }, Modifier.weight(1f)) }; if (pair.size == 1) Spacer(Modifier.weight(1f)) } }
        item { SectionHeader("FAVOURITES", "See all", onFavourites); Spacer(Modifier.height(8.dp)); Card(onClick = onFavourites, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("\u2661", fontSize = 42.sp, color = MaterialTheme.colorScheme.secondary); Column(Modifier.padding(start = 14.dp)) { Text(if (data.favourites.isEmpty()) "Build your usual picks" else "${data.favourites.size} usual products", style = MaterialTheme.typography.titleLarge); Text("Quickly add your favourites for $current") } } } }
        item { SectionHeader("MY LIST", "Open list", onList); Spacer(Modifier.height(8.dp)); Card(onClick = onList, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("Shopping at $current", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f)); Text("\u2611", fontSize = 28.sp) }; ShoppingProgress(data.items.count { it.done }, data.items.size) } } }
        item { SectionHeader("ALL ITEMS", "Browse catalogue", onAllItems); Spacer(Modifier.height(8.dp)); Card(onClick = onAllItems, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text("\u25A6", fontSize = 42.sp, color = MaterialTheme.colorScheme.tertiary); Column(Modifier.padding(start = 14.dp)) { Text("Everyday essentials", style = MaterialTheme.typography.titleLarge); Text("Search ${Catalogue.products.size} reusable products by category") } } } }
    }
}

@Composable
private fun AllItemsPage(modifier: Modifier, shop: String, state: ShoppingState, query: String, category: String?, onQuery: (String) -> Unit, onCategory: (String?) -> Unit, onConfigure: (Product) -> Unit, onAdd: (Product) -> Unit, onCustom: () -> Unit, onFavourite: (Product) -> Unit) {
    val products = state.allProducts().filter {
        (it.researchShops.isEmpty() || shop in it.researchShops) &&
            (category == null || it.category == category) && it.name.contains(query.trim(), true)
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { EditorialBanner("ALL ITEMS", "Browse products at $shop", "\uD83E\uDD57", accent = MaterialTheme.colorScheme.primary) }
        item { OutlinedTextField(query, onQuery, label = { Text("Search for a product") }, singleLine = true, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) }
        item { Text("Categories", style = MaterialTheme.typography.titleMedium); CategoryPicker(category, onCategory) }
        item { Button(onClick = onCustom, modifier = Modifier.fillMaxWidth()) { Text("+ Add custom item") } }
        items(products, key = { it.id }) { product ->
            val favourite = state.dataFor(shop).favourites.any { it.productId == product.id || it.name.equals(product.name, true) }
            ProductCardV2(
                product = product,
                onConfigure = { onConfigure(product) },
                onAdd = { onAdd(product) },
                canAdd = Rules.defaultItem(product, state.dataFor(shop)) != null,
                favourite = favourite,
                onFavourite = { onFavourite(product) }
            )
        }
        if (products.isEmpty()) item { EmptyState("No matching products", "Try another search or create a custom item.", "+ Add custom item", onCustom) }
    }
}

@Composable
private fun FavouritesPage(modifier: Modifier, shop: String, data: ShopData, query: String, onQuery: (String) -> Unit, onEdit: (Item) -> Unit, onRemove: (Item) -> Unit, onAdd: (Item) -> Unit) {
    val favourites = data.favourites.filter { it.name.contains(query.trim(), true) }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { EditorialBanner("FAVOURITES", "Your everyday essentials at $shop", "\uD83C\uDF45", accent = MaterialTheme.colorScheme.secondary) }
        item { OutlinedTextField(query, onQuery, label = { Text("Search favourites") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        if (favourites.isEmpty()) item { EmptyState("No favourites yet", "Save an item with the heart when adding it. Favourites belong to this shop.") }
        items(favourites, key = { it.id }) { item -> FavouriteCard(item, { onAdd(item) }, { onEdit(item) }, { onRemove(item) }) }
    }
}

@Composable
private fun ShoppingListPage(modifier: Modifier, shop: String, data: ShopData, onToggle: (Item) -> Unit, onMinus: (Item) -> Unit, onPlus: (Item) -> Unit, onEdit: (Item) -> Unit, onRemove: (Item) -> Unit, onAdd: () -> Unit) {
    val listItems = data.items.sortedBy { it.done }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { EditorialBanner("MY LIST", "Your shopping list for $shop", "\uD83E\uDDFA", accent = MaterialTheme.colorScheme.primary) }
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
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) { EditorialBanner("SETTINGS", "Make the app feel like yours", "\u2699", accent = MaterialTheme.colorScheme.tertiary); Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Appearance", style = MaterialTheme.typography.titleLarge); Text("Choose how Liz's Shopping List should look.", color = MaterialTheme.colorScheme.onSurfaceVariant); ThemeControls(themeMode, onThemeChange) } }; Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Shops", style = MaterialTheme.typography.titleLarge); Text("Add a local shop without changing the four default shops.", color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = onAddShop, modifier = Modifier.fillMaxWidth()) { Text("+ Add a shop") }; state.shopDefinitions.filterNot { it.builtIn }.forEach { shop -> if (editingShop == shop.id) { OutlinedTextField(draftName, { draftName = it.take(100) }, label = { Text("New name") }, singleLine = true, modifier = Modifier.fillMaxWidth()); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TextButton(onClick = { onRenameShop(shop.name, draftName); editingShop = null }) { Text("Save name") }; TextButton(onClick = { editingShop = null }) { Text("Cancel") } } } else { Row(verticalAlignment = Alignment.CenterVertically) { Text(shop.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium); TextButton(onClick = { editingShop = shop.id; draftName = shop.name }) { Text("Rename") }; TextButton(onClick = { removeTarget = shop.name }) { Text("Remove") } } } } } }; Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Text("Shopping lists, favourites and product data stay on this device and work offline.", modifier = Modifier.padding(18.dp)) } }
    if (removeTarget != null) AlertDialog(onDismissRequest = { removeTarget = null }, title = { Text("Remove ${removeTarget!!}?") }, text = { Text("A shop can only be removed when its lists and favourites are empty. The four default shops are always kept.") }, confirmButton = { TextButton(onClick = { onRemoveShop(removeTarget!!); removeTarget = null }) { Text("Remove") } }, dismissButton = { TextButton(onClick = { removeTarget = null }) { Text("Cancel") } })
}

@Composable
private fun SettingsPageV2(modifier: Modifier, state: ShoppingState, themeMode: String, onThemeChange: (String) -> Unit, onAddShop: () -> Unit, onRenameShop: (Shop) -> Unit, onRemoveShop: (String) -> Unit) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EditorialBanner("SETTINGS", "Make the app feel like yours", "\u2699", accent = MaterialTheme.colorScheme.tertiary)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Appearance", style = MaterialTheme.typography.titleLarge); Text("Choose how Liz's Shopping List should look.", color = MaterialTheme.colorScheme.onSurfaceVariant); ThemeControls(themeMode, onThemeChange) } }
        EditorialBanner("SHOP MANAGEMENT", "Add a shop and give it a familiar look", "", accent = MaterialTheme.colorScheme.secondary)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Shops", style = MaterialTheme.typography.titleLarge); Text("Add a local shop without changing the four default shops.", color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = onAddShop, modifier = Modifier.fillMaxWidth()) { Text("+ Add a shop") }; state.shopDefinitions.filterNot { it.builtIn }.forEach { shop -> Row(verticalAlignment = Alignment.CenterVertically) { ShopMark(shop, Modifier.size(48.dp, 40.dp)); Text(shop.name, modifier = Modifier.weight(1f).padding(start = 10.dp), style = MaterialTheme.typography.titleMedium); TextButton(onClick = { onRenameShop(shop) }) { Text("Rename") }; TextButton(onClick = { onRemoveShop(shop.name) }) { Text("Remove") } } } } }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) { Text("Shopping lists, favourites and product data stay on this device and work offline.", modifier = Modifier.padding(18.dp)) }
    }
}

@Composable
private fun ThemeControls(themeMode: String, onThemeChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("light" to "Light", "dark" to "Dark", "system" to "System").forEach { (value, label) -> FilterChip(themeMode == value, { onThemeChange(value) }, label = { Text(label) }, modifier = Modifier.height(48.dp)) } }
}

@Composable
private fun NavigationDrawerContentV2(availableShops: List<Shop>, selectedShop: String?, choosing: Boolean, tab: Int, themeMode: String, onHome: () -> Unit, onShop: (String) -> Unit, onTab: (Int) -> Unit, onAddProduct: () -> Unit, onSettings: () -> Unit, onThemeChange: (String) -> Unit) {
    ModalDrawerSheet(modifier = Modifier.widthIn(max = 320.dp), drawerContainerColor = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Liz's Shopping List", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp))
            Text("Choose a shop and keep your list close.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 8.dp))
            Spacer(Modifier.height(8.dp))
            DrawerItemV2("+", "Add New Product", "Search or create a reusable product", false, onAddProduct)
            DrawerItemV2("\u2302", "Home", "Choose a shop and start shopping", choosing, onHome)
            Text("YOUR SHOPS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(start = 8.dp, top = 12.dp))
            availableShops.forEach { shop -> DrawerShopItem(shop, !choosing && selectedShop == shop.name && tab == 2) { onShop(shop.name) } }
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
private fun DrawerShopItem(shop: Shop, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).padding(horizontal = 10.dp, vertical = 8.dp).semantics { contentDescription = "${shop.name}. Your ${shop.name} list" }, verticalAlignment = Alignment.CenterVertically) {
        ShopMark(shop, Modifier.size(44.dp, 36.dp))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) { Text(shop.name, style = MaterialTheme.typography.titleMedium); Text("Your ${shop.name} list", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Text("\u203A", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) { Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Liz's Shopping List", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary); Text("A calmer way to shop", color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(8.dp)); DrawerItem("\u2302", "Home", choosing, onHome, "Choose a shop and start shopping"); Text("YOUR SHOPS", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 12.dp)); shops.forEach { name -> DrawerItem("\u2022", name, !choosing && selectedShop == name && tab == 2, { onShop(name) }, "Your $name list") }; Spacer(Modifier.height(4.dp)); DrawerItem("\u2661", "Favourites", !choosing && tab == 1, { onTab(1) }, "Your usual products"); DrawerItem("\u2611", "Current Shopping List", !choosing && tab == 2, { onTab(2) }, "View, tick off and edit"); DrawerItem("\u25A6", "All Items", !choosing && tab == 0, { onTab(0) }, "Browse reusable products"); DrawerItem("\u2699", "Settings", false, onSettings, "Appearance and app options"); Text("APPEARANCE", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 12.dp)); ThemeControls(themeMode, onThemeChange) } }
}

@Composable
private fun DrawerItem(icon: String, title: String, selected: Boolean, onClick: () -> Unit, description: String) {
    Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable(onClick = onClick).background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface).padding(12.dp).semantics { contentDescription = "$title. $description" }, verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 24.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.widthIn(min = 36.dp)); Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductEditor(modifier: Modifier, edit: Editing, catalogue: List<Product>, initialHeart: Boolean, selectedImage: ImportedImage?, onPickImage: () -> Unit, onCancel: () -> Unit, onSave: (Item, String, Boolean, ImportedImage?) -> Unit) {
    var name by rememberSaveable { mutableStateOf(edit.item.name) }
    var variety by rememberSaveable { mutableStateOf(edit.item.variety) }
    var size by rememberSaveable { mutableStateOf(edit.item.size) }
    var productCategory by rememberSaveable { mutableStateOf(edit.product?.category ?: "Other") }
    var reusedProductId by rememberSaveable { mutableStateOf(edit.item.productId) }
    var qty by rememberSaveable { mutableIntStateOf(edit.item.quantity) }
    var heart by rememberSaveable { mutableStateOf(initialHeart) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EditorialBanner(if (edit.mode == "custom") "ADD ITEM" else "CONFIGURE", if (edit.mode == "custom") "Add a custom item to your shopping list" else "Choose the details that work for you", edit.product?.symbol ?: "\uD83D\uDED2", accent = MaterialTheme.colorScheme.primary)
        if (edit.mode == "custom" || Catalogue.find(edit.item.name) == null) {
            OutlinedTextField(name, { name = it.take(100) }, label = { Text("Product name (required)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (edit.mode == "custom" && name.trim().isNotEmpty()) {
                val matches = catalogue.filter { it.name.contains(name.trim(), true) }.take(5)
                if (matches.isNotEmpty()) {
                    Text("Reuse an existing catalogue product", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    matches.forEach { product -> TextButton(onClick = { reusedProductId = product.id; name = product.name; productCategory = product.category; variety = product.varieties.firstOrNull().orEmpty(); size = product.sizes.firstOrNull().orEmpty() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("${product.name} \u00B7 ${product.category}", textAlign = TextAlign.Start, modifier = Modifier.fillMaxWidth()) } }
                }
            }
        }
        if (edit.mode == "custom") {
            Text("Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { (Catalogue.categories + productCategory).distinctBy { stableKey(it) }.forEach { option -> FilterChip(productCategory.equals(option, true), { productCategory = option }, label = { Text(option) }, modifier = Modifier.height(48.dp)) } }
            OutlinedTextField(productCategory, { productCategory = it.take(100) }, label = { Text("Category or create your own") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
        Options("Variety", edit.product?.varieties ?: emptyList(), variety) { variety = it }
        Options("Package size / unit", edit.product?.sizes ?: emptyList(), size) { size = it }
        if (edit.mode == "custom" || edit.product != null) {
            TextButton(onClick = { onPickImage() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text(if (selectedImage == null) "Choose an offline product image" else "Image selected") }
        }
        Text("Number of packages", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Quantity(qty, { qty = (qty - 1).coerceAtLeast(1) }, { qty = (qty + 1).coerceAtMost(999) }, name)
        Text(if (size.startsWith("Loose")) "For loose items, enter an amount such as 500 g above. Quantity multiplies that amount." else "Size describes one package. Quantity is how many packages to buy.", style = MaterialTheme.typography.bodySmall)
        if (edit.mode != "favourite") Row(Modifier.fillMaxWidth().clickable { heart = !heart }.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) { androidx.compose.material3.Checkbox(heart, { heart = it }); Text(if (initialHeart) "\u2665  Favourite saved (usual quantity kept)" else "\u2665  Save as a favourite for this shop") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = { try { onSave(Rules.checked(edit.item.copy(name = name, variety = variety, size = size, quantity = qty, productId = reusedProductId)), productCategory, heart, selectedImage) } catch (e: IllegalArgumentException) { error = e.message } }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp), shape = RoundedCornerShape(18.dp), enabled = name.isNotBlank()) { Text(if (edit.mode == "list" || edit.mode == "favourite") "Save changes" else "+ Add to shopping list", fontSize = 18.sp) }
        TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    }
}

@Composable
private fun Quantity(value: Int, minus: () -> Unit, plus: () -> Unit, name: String = "item") {
    Row(verticalAlignment = Alignment.CenterVertically) { IconButton(onClick = minus, enabled = value > 1, modifier = Modifier.size(48.dp).semantics { contentDescription = "Decrease $name quantity" }) { Text("\u2212", fontSize = 24.sp) }; Text("\u00D7$value", modifier = Modifier.widthIn(min = 52.dp), fontWeight = FontWeight.Bold, fontSize = 18.sp); IconButton(onClick = plus, enabled = value < 999, modifier = Modifier.size(48.dp).semantics { contentDescription = "Increase $name quantity" }) { Text("+", fontSize = 24.sp) } }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Options(title: String, options: List<String>, value: String, onChange: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { options.filter { it.isNotBlank() }.forEach { option -> FilterChip(value == option, { onChange(option) }, label = { Text(option) }, modifier = Modifier.height(48.dp)) } }
    OutlinedTextField(value, { onChange(it.take(100)) }, label = { Text("$title or your own") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
}
