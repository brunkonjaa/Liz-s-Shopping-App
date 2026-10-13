package ie.suric.lizshopping

import java.util.Locale
import java.util.UUID

val shops = listOf("Lidl", "Aldi", "Dunnes Stores", "SuperValu")
data class Product(val name: String, val category: String, val symbol: String, val varieties: List<String>, val sizes: List<String>)
data class Item(val id: String = UUID.randomUUID().toString(), val name: String, val variety: String = "", val size: String = "", val quantity: Int = 1, val done: Boolean = false) {
    fun key() = listOf(name, variety, size).map { it.trim().lowercase(Locale.ROOT) }
    fun detail() = listOf(variety, size).filter { it.isNotBlank() }.joinToString(" · ")
}
data class ShopData(val items: List<Item> = emptyList(), val favourites: List<Item> = emptyList(), val previous: List<Item> = emptyList())
data class ShoppingState(val selected: String? = null, val stores: Map<String, ShopData> = shops.associateWith { ShopData() }, val custom: List<Product> = emptyList())

object Rules {
    fun text(value: String, required: Boolean = false): String {
        val trimmed = value.trim()
        require(trimmed.length <= 100 && (!required || trimmed.isNotEmpty()) && trimmed.none { it.isISOControl() }) { "Use 1–100 ordinary characters for names; optional details may be blank." }
        return trimmed
    }
    fun checked(item: Item) = item.copy(name = text(item.name, true), variety = text(item.variety), size = text(item.size)).also {
        require(it.quantity in 1..999) { "Quantity must be between 1 and 999." }
        require(it.id.length in 1..100)
    }
    fun add(items: List<Item>, input: Item): List<Item> {
        val clean = checked(input)
        val match = items.firstOrNull { it.key() == clean.key() }
        return if (match == null) items + clean.copy(done = false) else items.map {
            if (it.id == match.id) it.copy(quantity = minOf(999, it.quantity + clean.quantity), done = false) else it
        }
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
}
