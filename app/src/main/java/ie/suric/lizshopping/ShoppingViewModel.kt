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
    fun select(shop: String) { require(shop in shops); update { it.copy(selected = shop) } }
    private fun shop(shop: String, note: String? = null, change: (ShopData) -> ShopData) = update(note) { it.copy(stores = it.stores + (shop to change(it.stores.getValue(shop)))) }
    fun add(shop: String, item: Item) = shop(shop, "${item.name} added to $shop") { it.copy(items = Rules.add(it.items, item)) }
    fun edit(shop: String, item: Item) = shop(shop, "Item updated") { it.copy(items = Rules.edit(it.items, item)) }
    fun quantity(shop: String, id: String, delta: Int) = shop(shop) { it.copy(items = it.items.map { i -> if (i.id == id) Rules.changeQuantity(i, delta) else i }) }
    fun complete(shop: String, id: String) = shop(shop) { it.copy(items = it.items.map { i -> if (i.id == id) i.copy(done = !i.done) else i }) }
    fun remove(shop: String, id: String) = shop(shop, "Item removed") { it.copy(items = it.items.filterNot { i -> i.id == id }) }
    fun favourite(shop: String, item: Item) = shop(shop, "Favourite saved for $shop") { Rules.favourite(it, item) }
    fun removeFavourite(shop: String, item: Item) = shop(shop, "Favourite removed") { it.copy(favourites = it.favourites.filterNot { i -> i.key() == item.key() }) }
    fun editFavourite(shop: String, old: Item, new: Item) = shop(shop, "Favourite updated") { Rules.favourite(it.copy(favourites = it.favourites.filterNot { i -> i.id == old.id }), new) }
    fun custom(item: Item, shop: String, favourite: Boolean) = update("${item.name} saved") { s ->
        val clean = Rules.checked(item)
        val product = Product(clean.name, "Other", "🛍", listOf(clean.variety), listOf(clean.size))
        val d = s.stores.getValue(shop)
        val added = d.copy(items = Rules.add(d.items, clean))
        s.copy(custom = if (Catalogue.find(clean.name) != null) s.custom else s.custom.filterNot { it.name.equals(clean.name, true) } + product, stores = s.stores + (shop to if (favourite) Rules.favourite(added, clean) else added))
    }
    fun clearCompleted(shop: String) = shop(shop, "Completed items cleared") { d ->
        val cleared = d.items.filter { it.done }.map { it.copy(done = false) }
        d.copy(items = d.items.filterNot { it.done }, previous = cleared.fold(d.previous) { all, i -> if (all.any { it.key() == i.key() }) all.map { if (it.key() == i.key()) i else it } else all + i })
    }
    fun fresh(shop: String) = shop(shop, "Fresh list ready. Previous items are saved.", Rules::fresh)
    fun reuse(shop: String) = shop(shop, "Previous items added", Rules::reuse)
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
