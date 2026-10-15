package ie.suric.lizshopping

import java.util.Locale
import java.util.UUID

data class Shop(val id: String, val name: String, val builtIn: Boolean = false)

object ShopDefaults {
    val definitions = listOf(
        Shop("Lidl", "Lidl", builtIn = true),
        Shop("Aldi", "Aldi", builtIn = true),
        Shop("Dunnes Stores", "Dunnes Stores", builtIn = true),
        Shop("SuperValu", "SuperValu", builtIn = true)
    )
    val names = definitions.map { it.name }
    val suggestedNames = listOf("Dealz", "Euro Shop", "European Shop", "Spar", "Centra")
}

val shops = ShopDefaults.names

fun stableKey(value: String) = value.trim().lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]+"), "-").trim('-')
fun stableProductId(name: String, category: String = "Other") = "product:${stableKey(name)}:${stableKey(category)}"
fun stableLegacyProductId(name: String) = "legacy:${stableKey(name)}"

data class Product(
    val name: String,
    val category: String,
    val symbol: String,
    val varieties: List<String>,
    val sizes: List<String>,
    val id: String = stableProductId(name, category),
    val imageRef: String? = null,
    val imageDataBase64: String? = null
)

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val variety: String = "",
    val size: String = "",
    val quantity: Int = 1,
    val done: Boolean = false,
    val productId: String? = null,
    val notes: String = ""
) {
    fun key() = listOf(productId ?: name, variety, size).map { it.trim().lowercase(Locale.ROOT) }
    fun detail() = listOf(variety, size).filter { it.isNotBlank() }.joinToString(" · ")
}

data class ShopData(
    val items: List<Item> = emptyList(),
    val favourites: List<Item> = emptyList(),
    val previous: List<Item> = emptyList(),
    val associatedProductIds: Set<String> = emptySet()
)

data class ShoppingState(
    val selected: String? = null,
    val stores: Map<String, ShopData> = ShopDefaults.definitions.associate { it.id to ShopData() },
    val custom: List<Product> = emptyList(),
    val shopDefinitions: List<Shop> = ShopDefaults.definitions
)

fun ShoppingState.shop(name: String): Shop = shopDefinitions.firstOrNull { it.name == name || it.id == name }
    ?: error("Unknown shop")
fun ShoppingState.shopNames(): List<String> = shopDefinitions.map { it.name }
fun ShoppingState.dataFor(name: String): ShopData = stores[shop(name).id] ?: ShopData()
fun ShoppingState.allProducts(): List<Product> = (Catalogue.products + custom).distinctBy { it.id }
fun ShoppingState.productFor(item: Item): Product? = allProducts().firstOrNull { it.id == item.productId }
fun ShoppingState.categories(): List<String> = (Catalogue.categories + custom.map { it.category })
    .filter { it.isNotBlank() }
    .distinctBy { stableKey(it) }

object Rules {
    fun text(value: String, required: Boolean = false): String {
        val trimmed = value.trim()
        require(trimmed.length <= 100 && (!required || trimmed.isNotEmpty()) && trimmed.none { it.isISOControl() }) { "Use 1–100 ordinary characters for names; optional details may be blank." }
        return trimmed
    }
    fun checked(item: Item) = item.copy(name = text(item.name, true), variety = text(item.variety), size = text(item.size)).also {
        require(it.quantity in 1..999) { "Quantity must be between 1 and 999." }
        require(it.id.length in 1..100)
        require(it.productId == null || (it.productId.length in 1..150 && it.productId.none { c -> c.isISOControl() }))
        text(it.notes)
    }
    fun add(items: List<Item>, input: Item): List<Item> {
        val clean = checked(input)
        val match = items.firstOrNull { it.key() == clean.key() }
        return if (match == null) items + clean.copy(done = false) else items.map {
            if (it.id == match.id) it.copy(quantity = minOf(999, it.quantity + clean.quantity), done = false) else it
        }
    }

    /**
     * Builds the one-tap catalogue action from the shop's saved configuration,
     * falling back to the first valid catalogue options when no saved choice
     * exists. A product with no valid default remains configurable instead of
     * receiving a guessed value.
     */
    fun defaultItem(product: Product, data: ShopData): Item? {
        val saved = (data.favourites + data.items + data.previous).firstOrNull {
            it.productId == product.id || it.name.equals(product.name, ignoreCase = true)
        }
        val variety = saved?.variety?.takeIf { it.isNotBlank() }
            ?: product.varieties.firstOrNull { it.isNotBlank() }.orEmpty()
        val size = saved?.size?.takeIf { it.isNotBlank() }
            ?: product.sizes.firstOrNull { it.isNotBlank() }.orEmpty()
        if (product.varieties.any { it.isNotBlank() } && variety.isBlank()) return null
        if (product.sizes.any { it.isNotBlank() } && size.isBlank()) return null
        return Item(
            name = product.name,
            variety = variety,
            size = size,
            quantity = 1,
            productId = product.id
        )
    }
    fun edit(items: List<Item>, input: Item): List<Item> {
        val clean = checked(input)
        val rest = items.filterNot { it.id == clean.id }
        val match = rest.firstOrNull { it.key() == clean.key() }
        return if (match == null) rest + clean else rest.map {
            if (it.id == match.id) it.copy(quantity = minOf(999, it.quantity + clean.quantity), done = it.done && clean.done) else it
        }
    }
    fun changeQuantity(item: Item, delta: Int) = item.copy(quantity = (item.quantity + delta).coerceIn(1, 999))
    fun favourite(data: ShopData, input: Item): ShopData {
        val checked = checked(input).copy(done = false)
        val old = data.favourites.firstOrNull { it.key() == checked.key() }
        val clean = checked.copy(id = old?.id ?: UUID.randomUUID().toString())
        return data.copy(favourites = data.favourites.filterNot { it.key() == clean.key() } + clean)
    }
    fun fresh(data: ShopData) = data.copy(previous = if (data.items.isEmpty()) data.previous else data.items.map { it.copy(done = false) }, items = emptyList())
    fun reuse(data: ShopData) = data.copy(items = data.previous.fold(data.items) { all, item -> add(all, item.copy(id = UUID.randomUUID().toString(), done = false)) })

    fun validateShopName(name: String) = text(name, required = true).also { require(it.length >= 2) { "Shop name must contain at least 2 characters." } }

    fun validateCategoryName(name: String) = text(name, required = true).also {
        require(it.length >= 2) { "Category name must contain at least 2 characters." }
    }

    fun category(existing: Iterable<String>, name: String): String {
        val clean = validateCategoryName(name)
        return existing.firstOrNull { it.trim().equals(clean, ignoreCase = true) } ?: clean
    }

    fun addShop(state: ShoppingState, name: String): ShoppingState {
        val clean = validateShopName(name)
        require(state.shopDefinitions.none { it.name.equals(clean, true) }) { "That shop already exists." }
        val shop = Shop("shop:${UUID.randomUUID()}", clean)
        return state.copy(shopDefinitions = state.shopDefinitions + shop, stores = state.stores + (shop.id to ShopData()), selected = clean)
    }

    fun renameShop(state: ShoppingState, id: String, name: String): ShoppingState {
        val old = state.shopDefinitions.firstOrNull { it.id == id } ?: error("Unknown shop")
        require(!old.builtIn) { "Built-in shops cannot be renamed." }
        val clean = validateShopName(name)
        require(state.shopDefinitions.none { it.id != id && it.name.equals(clean, true) }) { "That shop already exists." }
        return state.copy(shopDefinitions = state.shopDefinitions.map { if (it.id == id) it.copy(name = clean) else it }, selected = if (state.selected == old.name) clean else state.selected)
    }

    fun removeShop(state: ShoppingState, id: String): ShoppingState {
        val target = state.shopDefinitions.firstOrNull { it.id == id } ?: error("Unknown shop")
        require(!target.builtIn) { "The four default shops cannot be removed." }
        val data = state.stores[id] ?: ShopData()
        require(data.items.isEmpty() && data.favourites.isEmpty() && data.previous.isEmpty()) { "Clear this shop's lists and favourites before removing it." }
        val remaining = state.shopDefinitions.filterNot { it.id == id }
        return state.copy(shopDefinitions = remaining, stores = state.stores - id, selected = if (state.selected == target.name) remaining.firstOrNull()?.name else state.selected)
    }

    fun withAssociation(data: ShopData, productId: String) = data.copy(associatedProductIds = data.associatedProductIds + productId)
}
