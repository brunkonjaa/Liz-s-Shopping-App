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
import java.util.UUID

class ShoppingViewModel(app: Application) : AndroidViewModel(app) {
    private val store = LocalStore(app)
    private val mutex = Mutex()
    private val mutable = MutableStateFlow<ShoppingState?>(null)
    val state = mutable.asStateFlow()
    val message = MutableStateFlow<String?>(null)
    private var pendingRestore: ShoppingState? = null
    val restoreReady = MutableStateFlow(false)
    val loadFailed = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            try { mutable.value = withContext(Dispatchers.IO) { store.load() } }
            catch (_: Exception) { loadFailed.value = true; message.value = "Could not load saved data. Restore a backup or restart the app." }
        }
    }

    private fun update(note: String? = null, change: (ShoppingState) -> ShoppingState) {
        viewModelScope.launch { mutex.withLock {
            var currentForRollback: ShoppingState? = null
            try {
                val current = mutable.value ?: return@withLock
                currentForRollback = current
                val next = change(current)
                // Publish the small state change before the snapshot write so
                // quantity and completion controls feel immediate. The mutex
                // still serializes writes, and a failed write rolls back the
                // in-memory value to the last known durable state.
                mutable.value = next
                if (note != null) message.value = note
                withContext(Dispatchers.IO) { store.save(next) }
            } catch (e: Exception) {
                // The change is serialized by mutex, so this is the state
                // immediately preceding the failed durable write.
                currentForRollback?.let { mutable.value = it }
                message.value = e.message ?: "Change could not be saved."
            }
        } }
    }

    private fun canonical(state: ShoppingState, item: Item): Item {
        val product = item.productId?.let { id -> state.allProducts().firstOrNull { it.id == id } }
            ?: state.allProducts().firstOrNull { it.name.equals(item.name.trim(), true) }
        return item.copy(productId = product?.id)
    }

    fun select(shop: String) = update { state -> state.shop(shop); state.copy(selected = state.shop(shop).name) }

    private fun shop(shop: String, note: String? = null, change: (ShopData) -> ShopData) = update(note) { state ->
        val definition = state.shop(shop)
        val existing = state.stores[definition.id] ?: ShopData()
        state.copy(stores = state.stores + (definition.id to change(existing)))
    }

    fun add(shop: String, item: Item) = shop(shop, "${item.name} added to $shop") { data ->
        val current = mutable.value ?: ShoppingState()
        val clean = canonical(current, item)
        data.copy(items = Rules.add(data.items, clean), associatedProductIds = data.associatedProductIds + listOfNotNull(clean.productId))
    }

    fun addProduct(shop: String, product: Product) = update("${product.name} added to $shop") { state ->
        val definition = state.shop(shop)
        val data = state.stores[definition.id] ?: ShopData()
        val item = requireNotNull(Rules.defaultItem(product, data)) { "Choose the product details before adding it." }
        state.copy(
            stores = state.stores + (definition.id to data.copy(
                items = Rules.add(data.items, item),
                associatedProductIds = data.associatedProductIds + product.id
            ))
        )
    }

    fun edit(shop: String, item: Item) = shop(shop, "Item updated") { data ->
        val current = mutable.value ?: ShoppingState()
        data.copy(items = Rules.edit(data.items, canonical(current, item)))
    }

    fun quantity(shop: String, id: String, delta: Int) = shop(shop) { data -> data.copy(items = data.items.map { i -> if (i.id == id) Rules.changeQuantity(i, delta) else i }) }
    fun complete(shop: String, id: String) = shop(shop) { data -> data.copy(items = data.items.map { i -> if (i.id == id) i.copy(done = !i.done) else i }) }
    fun remove(shop: String, id: String) = shop(shop, "Item removed") { data -> data.copy(items = data.items.filterNot { it.id == id }) }

    fun favourite(shop: String, item: Item) = shop(shop, "Favourite saved for $shop") { data ->
        val current = mutable.value ?: ShoppingState()
        val clean = canonical(current, item)
        if (clean.productId == null) Rules.favourite(data, clean) else Rules.withAssociation(Rules.favourite(data, clean), clean.productId)
    }

    fun removeFavourite(shop: String, item: Item) = shop(shop, "Favourite removed") { data -> data.copy(favourites = data.favourites.filterNot { it.key() == item.key() }) }

    fun editFavourite(shop: String, old: Item, new: Item) = shop(shop, "Favourite updated") { data ->
        val current = mutable.value ?: ShoppingState()
        Rules.favourite(data.copy(favourites = data.favourites.filterNot { it.id == old.id }), canonical(current, new))
    }

    fun custom(item: Item, shop: String, category: String, favourite: Boolean, image: ImportedImage? = null) = update("${item.name} saved") { state ->
        val existing = state.allProducts().firstOrNull { it.name.equals(item.name.trim(), true) }
        val cleanCategory = Rules.category(state.categories(), category)
        val product = existing ?: Product(item.name.trim(), cleanCategory, "🛍", listOf(item.variety), listOf(item.size), id = "custom:${UUID.randomUUID()}", imageRef = image?.reference, imageDataBase64 = image?.base64)
        val clean = Rules.checked(item.copy(productId = product.id))
        val definition = state.shop(shop)
        val data = state.stores[definition.id] ?: ShopData()
        val added = data.copy(items = Rules.add(data.items, clean), associatedProductIds = data.associatedProductIds + product.id)
        state.copy(
            custom = if (existing != null || Catalogue.find(clean.name) != null) state.custom.map { if (it.id == existing?.id && image != null) it.copy(imageRef = image.reference, imageDataBase64 = image.base64) else it } else state.custom + product,
            stores = state.stores + (definition.id to if (favourite) Rules.favourite(added, clean) else added)
        )
    }

    fun clearCompleted(shop: String) = shop(shop, "Completed items cleared") { data ->
        val cleared = data.items.filter { it.done }.map { it.copy(done = false) }
        data.copy(items = data.items.filterNot { it.done }, previous = cleared.fold(data.previous) { all, item -> if (all.any { it.key() == item.key() }) all.map { if (it.key() == item.key()) item else it } else all + item })
    }

    fun fresh(shop: String) = shop(shop, "Fresh list ready. Previous items are saved.", Rules::fresh)
    fun reuse(shop: String) = shop(shop, "Previous items added", Rules::reuse)
    fun addShop(name: String) = update("Shop added") { Rules.addShop(it, name) }
    fun renameShop(shop: String, name: String) = update("Shop renamed") { state -> Rules.renameShop(state, state.shop(shop).id, name) }
    fun removeShop(shop: String) = update("Shop removed") { state -> Rules.removeShop(state, state.shop(shop).id) }

    fun importProductImage(productId: String, image: ImportedImage) = update("Product image saved") { state ->
        state.copy(custom = state.custom.map { if (it.id == productId) it.copy(imageRef = image.reference, imageDataBase64 = image.base64) else it })
    }

    fun export(uri: Uri) { viewModelScope.launch { mutex.withLock {
        try {
            val state = mutable.value ?: return@withLock
            withContext(Dispatchers.IO) { getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(Backup.encode(state).toByteArray(Charsets.UTF_8)) } }
            message.value = "Backup saved"
        } catch (_: Exception) { message.value = "Backup could not be saved." }
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
            pendingRestore = decoded
            restoreReady.value = true
        } catch (_: Exception) { message.value = "Invalid or unreadable backup. Your shopping data has not changed." }
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
