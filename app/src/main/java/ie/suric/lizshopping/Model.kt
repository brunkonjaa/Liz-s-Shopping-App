package ie.suric.lizshopping

import java.security.MessageDigest
import java.util.Locale
import java.util.UUID

/** Display names are presentation only; persisted relationships use these stable shop IDs. */
data class Shop(val id: String, val name: String, val builtIn: Boolean, val archived: Boolean = false)

object ShopIds {
    fun builtIn(name: String) = "shop:${slug(name)}"
    fun custom() = "shop:custom:${UUID.randomUUID()}"
    fun forName(name: String) = builtIn(name)
    private fun slug(value: String) = value.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
}

val shops = listOf("Lidl", "Aldi", "Dunnes Stores", "SuperValu")
val suggestedShops = listOf("Dealz", "Euro Shop", "European Shop", "Spar", "Centra")
val defaultShops = shops.map { Shop(ShopIds.builtIn(it), it, builtIn = true) }

data class Category(val id: String, val name: String, val builtIn: Boolean = false)

object CategoryIds {
    fun fromName(name: String): String {
        val normalized = name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        val slug = normalized.replace(Regex("[^a-z0-9]+"), "-").trim('-').take(48)
        val hash = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8)).take(6).joinToString("") { "%02x".format(it) }
        return "category:$slug:$hash"
    }
}

data class ProductImageReference(val kind: String, val key: String) {
    fun checked(): ProductImageReference {
        require(kind in setOf("bundled", "local", "category")) { "Unsupported product image reference." }
        require(key.length in 1..200 && key.none { it.isISOControl() || it == '/' || it == '\\' }) { "Invalid product image reference." }
        if (kind == "local") require(key.matches(Regex("[a-fA-F0-9-]{36}\\.(jpg|png|webp)"))) { "Invalid local image key." }
        return this
    }
}

object ProductIds {
    fun bundled(name: String) = "product:builtin:${slug(name)}"
    fun custom() = "product:custom:${UUID.randomUUID()}"
    /** Stable, deterministic identity for products carried over from version-1 records. */
    fun legacy(name: String): String {
        val normalized = name.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
        val hash = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
            .take(12).joinToString("") { "%02x".format(it) }
        return "product:legacy:$hash"
    }
    private fun slug(value: String) = value.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
}

/** Shared product definition. List, favourite and previous-item quantities live in Item instead. */
data class Product(
    val name: String,
    val category: String,
    val symbol: String,
    val varieties: List<String>,
    val sizes: List<String>,
    val id: String = ProductIds.legacy(name),
    val categoryId: String = CategoryIds.fromName(category),
    val units: List<String> = emptyList(),
    val image: ProductImageReference? = null,
    val description: String? = null,
    val builtIn: Boolean = false
)

/** A shop-specific entry snapshot referencing a shared product. Package count is quantity; size is per package. */
data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val variety: String = "",
    val size: String = "",
    val quantity: Int = 1,
    val done: Boolean = false,
    val productId: String = Catalogue.find(name)?.id ?: ProductIds.legacy(name),
    val shopId: String = "",
    val notes: String? = null
) {
    fun key() = listOf(productId, variety, size).map { it.trim().lowercase(Locale.ROOT) }
    fun detail() = listOf(variety, size).filter { it.isNotBlank() }.joinToString(" · ")
}

data class ShopData(
    val items: List<Item> = emptyList(),
    val favourites: List<Item> = emptyList(),
    val previous: List<Item> = emptyList(),
    /** Explicitly selected associations; never interpreted as verified stock. */
    val associatedProductIds: Set<String> = emptySet()
)

data class ShoppingState(
    val selectedShopId: String? = null,
    val shopDirectory: List<Shop> = defaultShops,
    /** Keyed by stable Shop.id, never by the display label. */
    val stores: Map<String, ShopData> = defaultShops.associate { it.id to ShopData() },
    /** User-created definitions; built-ins are supplied by the bundled catalogue. */
    val custom: List<Product> = emptyList(),
    val customCategories: List<Category> = emptyList()
) {
    val allProducts: List<Product> get() = Catalogue.products + custom
    val allCategories: List<Category> get() = Catalogue.categoryDefinitions + customCategories
    val selectedShop: Shop? get() = shopDirectory.firstOrNull { it.id == selectedShopId }
    fun shop(reference: String): Shop? = shopDirectory.firstOrNull { it.id == reference || it.name.equals(reference.trim(), true) }
    fun shopData(reference: String): ShopData = stores[shop(reference)?.id ?: reference] ?: ShopData()
}

object Rules {
    fun text(value: String, required: Boolean = false): String {
        val trimmed = value.trim()
        require(trimmed.length <= 100 && (!required || trimmed.isNotEmpty()) && trimmed.none { it.isISOControl() }) { "Use 1–100 ordinary characters for names; optional details may be blank." }
        return trimmed
    }

    fun checked(item: Item) = item.copy(name = text(item.name, true), variety = text(item.variety), size = text(item.size), notes = item.notes?.let { text(it) }).also {
        require(it.quantity in 1..999) { "Quantity must be between 1 and 999." }
        require(it.id.length in 1..100)
        require(it.productId.length in 1..200)
        require(it.shopId.isEmpty() || it.shopId.length in 1..200)
    }

    fun checkedShopName(value: String): String = text(value, true)
    fun createShop(state: ShoppingState, rawName: String): ShoppingState {
        val name = checkedShopName(rawName)
        require(state.shopDirectory.none { it.name.equals(name, true) }) { "A shop with this name already exists." }
        val shop = Shop(ShopIds.custom(), name, builtIn = false)
        return state.copy(shopDirectory = state.shopDirectory + shop, stores = state.stores + (shop.id to ShopData()))
    }
    fun renameShop(state: ShoppingState, shopId: String, rawName: String): ShoppingState {
        val shop = state.shopDirectory.firstOrNull { it.id == shopId } ?: error("Shop not found.")
        require(!shop.builtIn) { "Built-in shops cannot be renamed." }
        val name = checkedShopName(rawName)
        require(state.shopDirectory.none { it.id != shopId && it.name.equals(name, true) }) { "A shop with this name already exists." }
        return state.copy(shopDirectory = state.shopDirectory.map { if (it.id == shopId) it.copy(name = name) else it })
    }
    /** Deletion is an archive operation so lists, favourites, history and IDs remain recoverable. */
    fun archiveShop(state: ShoppingState, shopId: String): ShoppingState {
        val shop = state.shopDirectory.firstOrNull { it.id == shopId } ?: error("Shop not found.")
        require(!shop.builtIn) { "The original four shops cannot be removed." }
        return state.copy(shopDirectory = state.shopDirectory.map { if (it.id == shopId) it.copy(archived = true) else it }, selectedShopId = if (state.selectedShopId == shopId) null else state.selectedShopId)
    }
    fun createCategory(state: ShoppingState, rawName: String): ShoppingState {
        val name = checkedShopName(rawName)
        require(state.allCategories.none { it.name.equals(name, true) }) { "A category with this name already exists." }
        return state.copy(customCategories = state.customCategories + Category(CategoryIds.fromName(name) + ":custom", name))
    }
    fun associate(state: ShoppingState, shopId: String, productId: String): ShoppingState {
        require(state.shopDirectory.any { it.id == shopId } && state.allProducts.any { it.id == productId }) { "Unknown shop or product." }
        val data = state.stores[shopId] ?: ShopData()
        return state.copy(stores = state.stores + (shopId to data.copy(associatedProductIds = data.associatedProductIds + productId)))
    }
    fun disassociate(state: ShoppingState, shopId: String, productId: String): ShoppingState {
        val data = state.stores[shopId] ?: error("Shop not found.")
        require(data.items.none { it.productId == productId } && data.favourites.none { it.productId == productId } && data.previous.none { it.productId == productId }) {
            "Remove the product from this shop's lists, favourites and previous items before disassociating it."
        }
        return state.copy(stores = state.stores + (shopId to data.copy(associatedProductIds = data.associatedProductIds - productId)))
    }

    fun checkedProduct(product: Product): Product {
        val name = text(product.name, true)
        val category = text(product.category, true)
        val varieties = product.varieties.map { text(it) }.distinctBy { it.lowercase(Locale.ROOT) }
        val sizes = product.sizes.map { text(it) }.distinctBy { it.lowercase(Locale.ROOT) }
        require(product.id.length in 1..200 && product.categoryId.length in 1..200)
        require(varieties.size <= 30 && sizes.size <= 30 && product.units.size <= 20)
        require(product.description == null || text(product.description).length <= 500)
        product.image?.checked()
        return product.copy(name = name, category = category, varieties = varieties, sizes = sizes, units = product.units.map { text(it, true) }.distinct(), description = product.description?.let(::text))
    }
    fun createProduct(state: ShoppingState, product: Product): ShoppingState {
        val clean = checkedProduct(product)
        require(!clean.builtIn && clean.id.startsWith("product:custom:")) { "New products need a generated user-product identity." }
        require(state.allCategories.any { it.id == clean.categoryId }) { "Choose a saved category." }
        require(state.allProducts.none { it.id == clean.id || it.name.equals(clean.name, true) }) {
            "A matching product already exists. Select it from the catalogue to reuse it, or choose a distinct name."
        }
        return state.copy(custom = state.custom + clean)
    }

    fun checked(item: Item, shopId: String, products: List<Product>): Item {
        val clean = checked(item.copy(shopId = shopId))
        val product = products.firstOrNull { it.id == clean.productId } ?: throw IllegalArgumentException("The selected catalogue product no longer exists.")
        require(clean.name.equals(product.name, true)) { "Shopping item name must match its catalogue product." }
        require(clean.variety.isBlank() || product.varieties.isEmpty() || product.varieties.any { it.equals(clean.variety, true) }) { "Choose a supported product variety." }
        require(clean.size.isBlank() || product.sizes.isEmpty() || product.sizes.any { it.equals(clean.size, true) }) { "Choose a supported size or unit." }
        return clean
    }

    fun add(items: List<Item>, input: Item): List<Item> {
        val clean = checked(input)
        val match = items.firstOrNull { it.key() == clean.key() && it.shopId == clean.shopId }
        return if (match == null) items + clean.copy(done = false) else items.map {
            if (it.id == match.id) it.copy(quantity = minOf(999, it.quantity + clean.quantity), done = false) else it
        }
    }
    fun edit(items: List<Item>, input: Item): List<Item> {
        val clean = checked(input)
        val rest = items.filterNot { it.id == clean.id }
        val match = rest.firstOrNull { it.key() == clean.key() && it.shopId == clean.shopId }
        return if (match == null) rest + clean else rest.map {
            if (it.id == match.id) it.copy(quantity = minOf(999, it.quantity + clean.quantity), done = it.done && clean.done) else it
        }
    }
    fun changeQuantity(item: Item, delta: Int) = item.copy(quantity = (item.quantity + delta).coerceIn(1, 999))
    fun favourite(data: ShopData, input: Item): ShopData {
        val checked = checked(input).copy(done = false)
        val old = data.favourites.firstOrNull { it.key() == checked.key() }
        val clean = checked.copy(id = old?.id ?: UUID.randomUUID().toString())
        return data.copy(favourites = data.favourites.filterNot { it.key() == clean.key() } + clean, associatedProductIds = data.associatedProductIds + clean.productId)
    }
    fun fresh(data: ShopData) = data.copy(previous = if (data.items.isEmpty()) data.previous else data.items.map { it.copy(done = false) }, items = emptyList())
    fun reuse(data: ShopData) = data.copy(items = data.previous.fold(data.items) { all, item -> add(all, item.copy(id = UUID.randomUUID().toString(), done = false)) })
}

/** Offline suggestions rank likely reuse candidates; they never merge or mutate catalogue records. */
object ProductSearch {
    fun suggest(products: List<Product>, rawQuery: String, limit: Int = 20): List<Product> {
        val q = normalize(rawQuery)
        if (q.isBlank()) return emptyList()
        return products.mapNotNull { product -> score(product, q)?.let { product to it } }
            .sortedWith(compareByDescending<Pair<Product, Int>> { it.second }.thenBy { it.first.name.lowercase(Locale.ROOT) })
            .take(limit.coerceIn(1, 100)).map { it.first }
    }
    private fun score(product: Product, query: String): Int? {
        val name = normalize(product.name)
        val variants = listOf(name, normalize(product.name.removeSuffix("s")), singular(name))
        if (variants.any { it == query }) return 100
        if (variants.any { it.startsWith(query) || it.contains(query) }) return 80
        val words = name.split(' ')
        val searchWords = query.split(' ').filterNot { it in setOf("fresh", "the", "product", "regular") }
        if (searchWords.isNotEmpty() && searchWords.all { token -> words.any { it.startsWith(token) || singular(it) == token } }) return 65
        return null
    }
    private fun normalize(value: String) = value.trim().lowercase(Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")
    private fun singular(value: String): String = when {
        value.endsWith("ies") && value.length > 4 -> value.dropLast(3) + "y"
        value.endsWith("oes") && value.length > 4 -> value.dropLast(2)
        value.endsWith("s") && value.length > 3 -> value.dropLast(1)
        else -> value
    }
}
