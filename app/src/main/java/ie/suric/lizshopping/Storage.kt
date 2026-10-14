package ie.suric.lizshopping

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

object Backup {
    const val MAX_BYTES = 1_048_576
    const val CURRENT_VERSION = 2

    private fun item(i: Item) = JSONObject()
        .put("id", i.id).put("productId", i.productId).put("shopId", i.shopId)
        .put("name", i.name).put("variety", i.variety).put("size", i.size)
        .put("quantity", i.quantity).put("done", i.done).put("notes", i.notes ?: JSONObject.NULL)
    private fun items(list: List<Item>) = JSONArray().also { a -> list.forEach { a.put(item(it)) } }

    /** Local image bytes are private app data and intentionally excluded from portable JSON backups. */
    private fun portableImage(product: Product): ProductImageReference? = when (val image = product.image) {
        null -> null
        else -> if (image.kind == "local") ProductImageReference("category", product.categoryId) else image
    }
    private fun product(p: Product, portable: Boolean) = JSONObject()
        .put("id", p.id).put("name", p.name).put("categoryId", p.categoryId).put("category", p.category)
        .put("symbol", p.symbol).put("varieties", JSONArray(p.varieties)).put("sizes", JSONArray(p.sizes))
        .put("units", JSONArray(p.units)).put("image", (if (portable) portableImage(p) else p.image)?.let { JSONObject().put("kind", it.kind).put("key", it.key) } ?: JSONObject.NULL)
        .put("description", p.description ?: JSONObject.NULL).put("builtIn", p.builtIn)
    private fun category(c: Category) = JSONObject().put("id", c.id).put("name", c.name).put("builtIn", c.builtIn)

    fun encode(s: ShoppingState): String = encodeInternal(s, portable = true)

    /** SQLite snapshot codec retains app-private local image keys; external backup codec does not. */
    fun encodeSnapshot(s: ShoppingState): String = encodeInternal(s, portable = false)

    private fun encodeInternal(s: ShoppingState, portable: Boolean): String {
        validate(s)
        val storeArray = JSONArray()
        s.shopDirectory.forEach { shop ->
            val data = s.stores.getValue(shop.id)
            storeArray.put(JSONObject().put("shopId", shop.id).put("items", items(data.items))
                .put("favourites", items(data.favourites)).put("previous", items(data.previous))
                .put("associatedProductIds", JSONArray(data.associatedProductIds.sorted())))
        }
        val root = JSONObject().put("format", "liz-shopping-list").put("version", CURRENT_VERSION)
            .put("selectedShopId", s.selectedShopId ?: JSONObject.NULL)
            .put("shops", JSONArray().also { itArray -> s.shopDirectory.forEach { itArray.put(JSONObject().put("id", it.id).put("name", it.name).put("builtIn", it.builtIn).put("archived", it.archived)) } })
            .put("products", JSONArray().also { itArray -> s.allProducts.forEach { itArray.put(product(it, portable)) } })
            .put("categories", JSONArray().also { itArray -> s.allCategories.forEach { itArray.put(category(it)) } })
            .put("stores", storeArray)
        val encoded = root.toString()
        require(encoded.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Shopping data is too large. Clear old items before adding more." }
        decodeV2(JSONObject(encoded), sanitizeLocalImages = portable)
        return encoded
    }

    private fun string(o: JSONObject, key: String, required: Boolean = false): String {
        require(o.has(key) && o.get(key) is String) { "Invalid text field: $key." }
        return Rules.text(o.getString(key), required)
    }
    private fun stringList(a: JSONArray, max: Int = 30): List<String> {
        require(a.length() <= max) { "Too many product options." }
        return (0 until a.length()).map { n -> require(a.get(n) is String); Rules.text(a.getString(n)) }
    }
    private fun readV1Items(a: JSONArray): List<Item> {
        require(a.length() <= 1000) { "Too many items." }
        val result = (0 until a.length()).map { n ->
            val o = a.getJSONObject(n)
            val q = o.get("quantity")
            require(q is Int && o.get("done") is Boolean) { "Invalid quantity or completion field." }
            Rules.checked(Item(string(o, "id", true), string(o, "name", true), string(o, "variety"), string(o, "size"), q, o.getBoolean("done")))
        }
        require(result.map { it.id }.distinct().size == result.size && result.map { it.key() }.distinct().size == result.size) { "Duplicate backup items." }
        return result
    }

    /** Explicit pure conversion of the existing version-1 JSON payload and external backups. */
    fun migrateV1(raw: String): ShoppingState {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 1 MB." }
        val root = JSONObject(raw)
        require(root.get("version") == 1 && root.getString("format") == "liz-shopping-list") { "Unsupported backup format." }
        val selectedName = if (root.isNull("selected")) null else string(root, "selected", true)
        require(selectedName == null || selectedName in shops) { "Unknown shop." }
        val oldStores = root.getJSONObject("stores")
        require(oldStores.keys().asSequence().toSet() == shops.toSet()) { "Incomplete or unknown shop data." }
        val legacyCustom = root.getJSONArray("custom")
        require(legacyCustom.length() <= 1000) { "Too many custom products." }
        val custom = (0 until legacyCustom.length()).map { index ->
            val p = legacyCustom.getJSONObject(index)
            val name = string(p, "name", true)
            val varieties = stringList(p.getJSONArray("varieties"))
            val sizes = stringList(p.getJSONArray("sizes"))
            Product(name, "Other", "🛍", varieties, sizes, id = ProductIds.legacy(name), categoryId = CategoryIds.fromName("Other"))
        }
        require(custom.map { it.name.lowercase(Locale.ROOT) }.distinct().size == custom.size) { "Duplicate custom products." }
        val all = Catalogue.products + custom
        val shopsV2 = defaultShops
        val stores = shopsV2.associate { shop ->
            val rawShop = oldStores.getJSONObject(shop.name)
            fun migrate(a: JSONArray): List<Item> = readV1Items(a).map { old ->
                val product = all.firstOrNull { it.name.equals(old.name, true) } ?: Product(
                    old.name, "Other", "🛍", listOf(old.variety).filter { it.isNotBlank() }, listOf(old.size).filter { it.isNotBlank() },
                    id = ProductIds.legacy(old.name), categoryId = CategoryIds.fromName("Other")
                )
                Rules.checked(old.copy(productId = product.id, shopId = shop.id), shop.id, (all + product).distinctBy { it.id })
            }
            val current = migrate(rawShop.getJSONArray("items"))
            val favourites = migrate(rawShop.getJSONArray("favourites"))
            require(favourites.none { it.done }) { "Favourites cannot be completed." }
            val previous = migrate(rawShop.getJSONArray("previous"))
            shop.id to ShopData(current, favourites, previous,
                (current + favourites + previous).map { it.productId }.toSet())
        }
        val migratedCustom = (custom + shopsV2.flatMap { shop ->
            val old = oldStores.getJSONObject(shop.name)
            (listOf("items", "favourites", "previous").flatMap { key -> readV1Items(old.getJSONArray(key)) })
                .filter { item -> all.none { it.name.equals(item.name, true) } }
                .map { item -> Product(item.name, "Other", "🛍", listOf(item.variety).filter(String::isNotBlank), listOf(item.size).filter(String::isNotBlank), id = ProductIds.legacy(item.name), categoryId = CategoryIds.fromName("Other")) }
        }).distinctBy { it.id }
        val categoryNames = (Catalogue.categories + migratedCustom.map { it.category }).distinctBy { it.lowercase(Locale.ROOT) }
        val result = ShoppingState(
            selectedShopId = selectedName?.let(ShopIds::builtIn),
            shopDirectory = shopsV2,
            stores = stores,
            custom = migratedCustom,
            customCategories = categoryNames.filterNot { name -> Catalogue.categories.any { it.equals(name, true) } }.map { Category(CategoryIds.fromName(it) + ":custom", it) }
        )
        validate(result)
        return result
    }

    private fun readItemV2(o: JSONObject): Item {
        val q = o.get("quantity")
        require(q is Int && o.get("done") is Boolean) { "Invalid quantity or completion field." }
        val notes = if (!o.has("notes") || o.isNull("notes")) null else string(o, "notes")
        return Rules.checked(Item(
            id = string(o, "id", true), name = string(o, "name", true), variety = string(o, "variety"), size = string(o, "size"),
            quantity = q, done = o.getBoolean("done"), productId = string(o, "productId", true), shopId = string(o, "shopId", true), notes = notes
        ))
    }
    private fun readItemsV2(a: JSONArray, products: List<Product>, shopId: String): List<Item> {
        require(a.length() <= 1000) { "Too many items." }
        val result = (0 until a.length()).map { index -> Rules.checked(readItemV2(a.getJSONObject(index)), shopId, products) }
        require(result.all { it.shopId == shopId }) { "Shopping item belongs to a different shop." }
        require(result.map { it.id }.distinct().size == result.size && result.map { it.key() }.distinct().size == result.size) { "Duplicate backup items." }
        return result
    }
    private fun decodeV2(root: JSONObject, sanitizeLocalImages: Boolean): ShoppingState {
        require(root.get("version") == CURRENT_VERSION && root.getString("format") == "liz-shopping-list") { "Unsupported backup format." }
        val shopArray = root.getJSONArray("shops")
        require(shopArray.length() in shops.size..100) { "Invalid number of shops." }
        val shopList = (0 until shopArray.length()).map { i ->
            val s = shopArray.getJSONObject(i)
            require(s.get("builtIn") is Boolean && s.get("archived") is Boolean)
            Shop(string(s, "id", true), Rules.checkedShopName(string(s, "name", true)), s.getBoolean("builtIn"), s.getBoolean("archived"))
        }
        require(shopList.map { it.id }.distinct().size == shopList.size && shopList.map { it.name.lowercase(Locale.ROOT) }.distinct().size == shopList.size) { "Duplicate shop IDs or names." }
        require(defaultShops.all { builtin -> shopList.any { it == builtin } }) { "Required built-in shops are missing or changed." }
        require(shopList.filterNot { it.builtIn }.all { it.id.startsWith("shop:custom:") }) { "Invalid custom shop identity." }
        val categoryArray = root.getJSONArray("categories")
        require(categoryArray.length() <= 200) { "Too many categories." }
        val categories = (0 until categoryArray.length()).map { i ->
            val c = categoryArray.getJSONObject(i)
            require(c.get("builtIn") is Boolean)
            Category(string(c, "id", true), Rules.checkedShopName(string(c, "name", true)), c.getBoolean("builtIn"))
        }
        require(categories.map { it.id }.distinct().size == categories.size && categories.map { it.name.lowercase(Locale.ROOT) }.distinct().size == categories.size) { "Duplicate categories." }
        require(Catalogue.categoryDefinitions.all { expected -> categories.any { it == expected } }) { "Required built-in categories are missing or changed." }
        val productArray = root.getJSONArray("products")
        require(productArray.length() in Catalogue.products.size..3000) { "Invalid catalogue size." }
        val importedProducts = (0 until productArray.length()).map { i ->
            val p = productArray.getJSONObject(i)
            val name = string(p, "name", true)
            val catId = string(p, "categoryId", true)
            val image = if (p.isNull("image")) null else p.getJSONObject("image").let {
                val reference = ProductImageReference(string(it, "kind", true), string(it, "key", true)).checked()
                if (sanitizeLocalImages && reference.kind == "local") ProductImageReference("category", catId) else reference
            }
            val desc = if (p.isNull("description")) null else string(p, "description")
            val parsed = Rules.checkedProduct(Product(name, string(p, "category", true), string(p, "symbol"), stringList(p.getJSONArray("varieties")), stringList(p.getJSONArray("sizes")),
                id = string(p, "id", true), categoryId = catId, units = stringList(p.getJSONArray("units"), 20), image = image, description = desc, builtIn = p.getBoolean("builtIn")))
            require(parsed.categoryId in categories.map { it.id }) { "Product references an unknown category." }
            parsed
        }
        require(importedProducts.map { it.id }.distinct().size == importedProducts.size) { "Duplicate product IDs." }
        val bundledIds = Catalogue.products.map { it.id }.toSet()
        require(importedProducts.filter { it.builtIn }.all { it.id in bundledIds } && bundledIds.all { id -> importedProducts.any { it.id == id && it.builtIn } }) { "Invalid bundled catalogue entries." }
        require(importedProducts.filterNot { it.builtIn }.all { it.id.startsWith("product:custom:") || it.id.startsWith("product:legacy:") }) { "Invalid user product identity." }
        val products = Catalogue.products + importedProducts.filterNot { it.builtIn }
        val storeArray = root.getJSONArray("stores")
        require(storeArray.length() == shopList.size) { "Missing shop data." }
        val storeMap = (0 until storeArray.length()).associate { i ->
            val d = storeArray.getJSONObject(i)
            val shopId = string(d, "shopId", true)
            require(shopList.any { it.id == shopId }) { "Store references an unknown shop." }
            val associationsArray = d.getJSONArray("associatedProductIds")
            val associations = stringList(associationsArray, 3000).toSet()
            require(associations.size == associationsArray.length() && associations.all { id -> products.any { it.id == id } }) { "Invalid shop product association." }
            val favs = readItemsV2(d.getJSONArray("favourites"), products, shopId)
            require(favs.none { it.done }) { "Favourites cannot be completed." }
            shopId to ShopData(readItemsV2(d.getJSONArray("items"), products, shopId), favs,
                readItemsV2(d.getJSONArray("previous"), products, shopId), associations)
        }
        require(storeMap.size == shopList.size) { "Duplicate or missing shop data." }
        val selected = if (root.isNull("selectedShopId")) null else string(root, "selectedShopId", true)
        require(selected == null || shopList.any { it.id == selected && !it.archived }) { "Invalid selected shop." }
        val customCategories = categories.filterNot { it.builtIn }
        val state = ShoppingState(selected, shopList, storeMap, products.filterNot { it.builtIn }, customCategories)
        validate(state)
        return state
    }

    private fun decodeInternal(raw: String, sanitizeLocalImages: Boolean): ShoppingState {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 1 MB." }
        val root = JSONObject(raw)
        require(root.getString("format") == "liz-shopping-list") { "Unsupported backup format." }
        return when (root.getInt("version")) {
            1 -> migrateV1(raw)
            CURRENT_VERSION -> decodeV2(root, sanitizeLocalImages)
            else -> throw IllegalArgumentException("Unsupported backup format.")
        }
    }
    /** Decode a portable user-selected backup; local image keys always fall back safely. */
    fun decode(raw: String): ShoppingState = decodeInternal(raw, sanitizeLocalImages = true)
    /** Decode an app-private SQLite snapshot where local image files live under filesDir. */
    fun decodeSnapshot(raw: String): ShoppingState = decodeInternal(raw, sanitizeLocalImages = false)

    fun validate(state: ShoppingState) {
        require(state.shopDirectory.size in shops.size..100)
        require(state.shopDirectory.map { it.id }.distinct().size == state.shopDirectory.size)
        require(state.shopDirectory.map { it.name.lowercase(Locale.ROOT) }.distinct().size == state.shopDirectory.size)
        require(defaultShops.all { expected -> state.shopDirectory.any { it == expected } })
        require(state.stores.keys == state.shopDirectory.map { it.id }.toSet()) { "Every shop must have exactly one independent state." }
        val products = state.allProducts.map(Rules::checkedProduct)
        require(products.map { it.id }.distinct().size == products.size)
        require(products.map { it.name.lowercase(Locale.ROOT) }.distinct().size == products.size) { "Duplicate product names must be resolved explicitly." }
        val productIds = products.map { it.id }.toSet()
        val categoryIds = state.allCategories.map { it.id }.toSet()
        require(state.allCategories.map { it.id }.distinct().size == state.allCategories.size)
        require(Catalogue.categoryDefinitions.all { it in state.allCategories })
        require(products.all { it.categoryId in categoryIds })
        require(state.selectedShopId == null || state.shopDirectory.any { it.id == state.selectedShopId && !it.archived })
        state.stores.forEach { (shopId, data) ->
            require(state.shopDirectory.any { it.id == shopId })
            require(data.associatedProductIds.all { it in productIds })
            listOf(data.items, data.favourites, data.previous).forEach { list ->
                require(list.map { it.id }.distinct().size == list.size && list.map { it.key() }.distinct().size == list.size)
                list.forEach { Rules.checked(it, shopId, products) }
            }
            require(data.favourites.none { it.done })
        }
    }
}

class LocalStore(context: Context, name: String = "shopping.db") : SQLiteOpenHelper(context, name, null, 2) {
    override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE state (id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL)") }

    /** SQLiteOpenHelper wraps this callback in a transaction; any failed decode/write rolls back version and row. */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion == 1 && newVersion == 2) { "No migration exists for database $oldVersion → $newVersion." }
        db.rawQuery("SELECT payload FROM state WHERE id=1", null).use { cursor ->
            if (cursor.moveToFirst()) {
                val oldPayload = cursor.getString(0)
                val migrated = Backup.migrateV1(oldPayload)
                val payload = Backup.encodeSnapshot(migrated)
                val updated = db.update("state", ContentValues().apply { put("payload", payload) }, "id=1", null)
                check(updated == 1) { "Version-1 snapshot migration did not update its row." }
            }
        }
    }
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Downgrading saved shopping data is not supported.") }

    fun load(): ShoppingState = readableDatabase.rawQuery("SELECT payload FROM state WHERE id=1", null).use {
        if (it.moveToFirst()) Backup.decodeSnapshot(it.getString(0)) else ShoppingState()
    }
    fun save(state: ShoppingState) {
        val payload = Backup.encodeSnapshot(state)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict("state", null, ContentValues().apply { put("id", 1); put("payload", payload) }, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }
}
