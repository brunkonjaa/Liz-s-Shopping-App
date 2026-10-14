package ie.suric.lizshopping

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ShoppingViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val mutex = Mutex()
    private val mutable = MutableStateFlow<ShoppingState?>(null)
    val state = mutable.asStateFlow()
    val message = MutableStateFlow<String?>(null)
    private var pendingRestore: ShoppingState? = null
    val restoreReady = MutableStateFlow(false)
    val loadFailed = MutableStateFlow(false)
    init { viewModelScope.launch { try { mutable.value = withContext(Dispatchers.IO) { store.load() } } catch (e: Exception) { loadFailed.value = true; message.value = "Could not load saved data. Restore a backup or restart the app." } } }
    private fun update(note: String? = null, change: (ShoppingState) -> ShoppingState) {
        viewModelScope.launch { mutex.withLock {
            try {
                val current = mutable.value ?: return@withLock
                val next = change(current)
                withContext(Dispatchers.IO) { store.save(next) }
                mutable.value = next
                if (note != null) message.value = note
            } catch (e: Exception) { message.value = e.message ?: "Change could not be saved." }
        } }
    }
    fun select(shop: String) = update { state ->
        val target = state.shop(shop) ?: error("Shop not found.")
        require(!target.archived) { "Archived shops cannot be selected." }
        state.copy(selectedShopId = target.id)
    }
    private fun shop(shopRef: String, note: String? = null, change: (ShopData) -> ShopData) = update(note) { state ->
        val target = state.shop(shopRef) ?: error("Shop not found.")
        require(!target.archived) { "This shop is archived." }
        val current = state.stores[target.id] ?: ShopData()
        val changed = change(current)
        val scoped = changed.copy(
            items = changed.items.map { it.copy(shopId = target.id) },
            favourites = changed.favourites.map { it.copy(shopId = target.id) },
            previous = changed.previous.map { it.copy(shopId = target.id) }
        )
        state.copy(stores = state.stores + (target.id to scoped))
    }
    private fun checkedForShop(state: ShoppingState, shopRef: String, item: Item): Item {
        val target = state.shop(shopRef) ?: error("Shop not found.")
        return Rules.checked(item.copy(shopId = target.id), target.id, state.allProducts)
    }
    fun add(shop: String, item: Item) = update("${item.name} added to $shop") { state ->
        val target = state.shop(shop) ?: error("Shop not found.")
        require(!target.archived)
        val clean = checkedForShop(state, shop, item)
        val data = state.stores.getValue(target.id)
        state.copy(stores = state.stores + (target.id to Rules.add(data.items, clean).let { updated ->
            data.copy(items = updated, associatedProductIds = data.associatedProductIds + clean.productId)
        }))
    }
    fun edit(shop: String, item: Item) = update("Item updated") { state ->
        val target = state.shop(shop) ?: error("Shop not found.")
        val old = state.stores.getValue(target.id).items.firstOrNull { it.id == item.id } ?: error("Shopping item not found.")
        val clean = checkedForShop(state, shop, item.copy(shopId = target.id, productId = old.productId))
        val data = state.stores.getValue(target.id)
        state.copy(stores = state.stores + (target.id to data.copy(items = Rules.edit(data.items, clean))))
    }
    fun quantity(shop: String, id: String, delta: Int) = this.shop(shop) { it.copy(items = it.items.map { i -> if (i.id == id) Rules.changeQuantity(i, delta) else i }) }
    fun complete(shop: String, id: String) = this.shop(shop) { it.copy(items = it.items.map { i -> if (i.id == id) i.copy(done = !i.done) else i }) }
    fun remove(shop: String, id: String) = this.shop(shop, "Item removed") { it.copy(items = it.items.filterNot { i -> i.id == id }) }
    fun favourite(shop: String, item: Item) = this.shop(shop, "Favourite saved for $shop") { data -> Rules.favourite(data, item.copy(shopId = data.items.firstOrNull { it.productId == item.productId }?.shopId ?: item.shopId)) }
    fun removeFavourite(shop: String, item: Item) = this.shop(shop, "Favourite removed") { it.copy(favourites = it.favourites.filterNot { i -> i.key() == item.key() }) }
    fun editFavourite(shop: String, old: Item, new: Item) = this.shop(shop, "Favourite updated") { Rules.favourite(it.copy(favourites = it.favourites.filterNot { i -> i.id == old.id }), new.copy(shopId = old.shopId)) }
    fun custom(item: Item, shop: String, favourite: Boolean) = update("${item.name} saved") { s ->
        val clean = Rules.checked(item)
        val target = s.shop(shop) ?: error("Shop not found.")
        require(!target.archived)
        val existing = s.allProducts.firstOrNull { it.name.equals(clean.name, true) }
        val product = existing ?: Rules.checkedProduct(Product(clean.name, "Other", "🛍", listOf(clean.variety).filter(String::isNotBlank), listOf(clean.size).filter(String::isNotBlank), id = ProductIds.custom(), categoryId = CategoryIds.fromName("Other")))
        val configured = if (existing?.builtIn == true) product else product.copy(
            varieties = (product.varieties + clean.variety).filter(String::isNotBlank).distinctBy { it.lowercase() },
            sizes = (product.sizes + clean.size).filter(String::isNotBlank).distinctBy { it.lowercase() }
        )
        val entry = clean.copy(name = configured.name, productId = configured.id, shopId = target.id)
        Rules.checked(entry, target.id, s.allProducts + if (existing == null) listOf(configured) else emptyList())
        val data = s.stores.getValue(target.id)
        val withAssociation = data.copy(associatedProductIds = data.associatedProductIds + configured.id)
        val added = withAssociation.copy(items = Rules.add(withAssociation.items, entry))
        val products = when {
            existing == null -> s.custom + configured
            existing.builtIn -> s.custom
            else -> s.custom.map { if (it.id == existing.id) configured else it }
        }
        s.copy(custom = products, stores = s.stores + (target.id to if (favourite) Rules.favourite(added, entry) else added))
    }
    fun saveProduct(product: Product) = update("Product saved to your catalogue") { state ->
        val clean = Rules.checkedProduct(product)
        require(state.allCategories.any { it.id == clean.categoryId }) { "Choose a saved category." }
        require(state.allProducts.none { it.id != clean.id && it.name.equals(clean.name, true) }) { "A similar product already exists. Choose it from search to reuse it." }
        val old = state.allProducts.firstOrNull { it.id == clean.id }
        require(old == null || !old.builtIn) { "Bundled product definitions cannot be overwritten." }
        state.copy(custom = if (old == null) state.custom + clean else state.custom.map { if (it.id == old.id) clean else it })
    }
    fun clearCompleted(shop: String) = this.shop(shop, "Completed items cleared") { d ->
        val cleared = d.items.filter { it.done }.map { it.copy(done = false) }
        d.copy(items = d.items.filterNot { it.done }, previous = cleared.fold(d.previous) { all, i -> if (all.any { it.key() == i.key() }) all.map { if (it.key() == i.key()) i else it } else all + i })
    }
    fun fresh(shop: String) = this.shop(shop, "Fresh list ready. Previous items are saved.", Rules::fresh)
    fun reuse(shop: String) = this.shop(shop, "Previous items added", Rules::reuse)
    fun createShop(name: String) = update("Shop added") { Rules.createShop(it, name) }
    fun renameShop(shopId: String, name: String) = update("Shop renamed") { Rules.renameShop(it, shopId, name) }
    fun archiveShop(shopId: String) = update("Shop archived. Its lists and favourites remain saved.") { Rules.archiveShop(it, shopId) }
    fun associateProduct(shopId: String, productId: String) = update { Rules.associate(it, shopId, productId) }
    fun disassociateProduct(shopId: String, productId: String) = update { Rules.disassociate(it, shopId, productId) }
    fun createCategory(name: String) = update("Category added") { Rules.createCategory(it, name) }
    fun setProductImageReference(productId: String, reference: ProductImageReference?) = update("Product image updated") { state ->
        val product = state.custom.firstOrNull { it.id == productId } ?: error("Only user-created products can be edited.")
        val safeReference = reference?.checked()
        state.copy(custom = state.custom.map { if (it.id == productId) it.copy(image = safeReference) else it })
    }
    fun export(uri: Uri) { viewModelScope.launch { mutex.withLock {
        try { val s = mutable.value ?: return@withLock; withContext(Dispatchers.IO) { getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(Backup.encode(s).toByteArray(Charsets.UTF_8)) } }; message.value = "Backup saved" }
        catch (e: Exception) { message.value = "Backup could not be saved." }
    } } }
    fun readBackup(uri: Uri) { viewModelScope.launch {
        try {
            val decoded = withContext(Dispatchers.IO) { getApplication<Application>().contentResolver.openInputStream(uri)!!.use { input ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count == -1) break
                    require(output.size() + count <= Backup.MAX_BYTES)
                    output.write(buffer, 0, count)
                }
                Backup.decode(output.toByteArray().toString(Charsets.UTF_8))
            } }
            pendingRestore = decoded; restoreReady.value = true
        } catch (e: Exception) { message.value = "Invalid or unreadable backup. Your shopping data has not changed." }
    } }
    fun cancelRestore() { pendingRestore = null; restoreReady.value = false }
    fun restore() {
        val restored = pendingRestore ?: return
        cancelRestore()
        viewModelScope.launch { mutex.withLock {
            try { withContext(Dispatchers.IO) { store.save(restored) }; mutable.value = restored; loadFailed.value = false; message.value = "Backup restored" }
            catch (_: Exception) { message.value = "Restore could not be saved. Existing data was preserved." }
        } }
    }
    override fun onCleared() { store.close() }
}
