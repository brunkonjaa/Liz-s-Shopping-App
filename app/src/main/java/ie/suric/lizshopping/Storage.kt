package ie.suric.lizshopping

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object Backup {
    const val MAX_BYTES = 1_048_576
    private const val FORMAT = "liz-shopping-list"
    private const val CURRENT_VERSION = 3

    private fun item(i: Item) = JSONObject()
        .put("id", i.id).put("name", i.name).put("variety", i.variety).put("size", i.size)
        .put("quantity", i.quantity).put("done", i.done).put("productId", i.productId ?: JSONObject.NULL)
        .put("notes", i.notes)

    private fun items(list: List<Item>) = JSONArray().also { a -> list.forEach { a.put(item(it)) } }

    private fun product(p: Product) = JSONObject().put("id", p.id).put("name", p.name).put("category", p.category)
        .put("symbol", p.symbol).put("varieties", JSONArray(p.varieties)).put("sizes", JSONArray(p.sizes))
        .put("imageRef", p.imageRef ?: JSONObject.NULL).put("imageDataBase64", p.imageDataBase64 ?: JSONObject.NULL)

    private fun shop(s: Shop) = JSONObject().put("id", s.id).put("name", s.name).put("builtIn", s.builtIn)
        .put("logoImageRef", s.logoImageRef ?: JSONObject.NULL).put("logoImageDataBase64", s.logoImageDataBase64 ?: JSONObject.NULL)

    fun encode(state: ShoppingState): String {
        val root = JSONObject().put("format", FORMAT).put("version", CURRENT_VERSION)
        val selected = state.selected?.let { state.shop(it).id }
        root.put("selectedShopId", selected ?: JSONObject.NULL)
        root.put("shops", JSONArray().also { a -> state.shopDefinitions.forEach { a.put(shop(it)) } })
        root.put("products", JSONArray().also { a -> state.allProducts().forEach { a.put(product(it)) } })
        root.put("stores", JSONObject().also { stores ->
            state.shopDefinitions.forEach { definition ->
                val data = state.stores[definition.id] ?: ShopData()
                stores.put(definition.id, JSONObject()
                    .put("items", items(data.items)).put("favourites", items(data.favourites)).put("previous", items(data.previous))
                    .put("associatedProductIds", JSONArray(data.associatedProductIds.toList())))
            }
        })
        val encoded = root.toString()
        require(encoded.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Shopping data is too large. Clear old items before adding more." }
        decode(encoded)
        return encoded
    }

    fun encodeLegacy(state: ShoppingState): String {
        val root = JSONObject().put("format", FORMAT).put("version", 1).put("selected", state.selected ?: JSONObject.NULL)
        root.put("stores", JSONObject().also { stores ->
            ShopDefaults.names.forEach { name ->
                val d = state.stores[name] ?: state.dataFor(name)
                stores.put(name, JSONObject().put("items", legacyItems(d.items)).put("favourites", legacyItems(d.favourites)).put("previous", legacyItems(d.previous)))
            }
        })
        root.put("custom", JSONArray().also { a -> state.custom.forEach { p -> a.put(JSONObject().put("name", p.name).put("varieties", JSONArray(p.varieties)).put("sizes", JSONArray(p.sizes))) } })
        return root.toString()
    }

    private fun legacyItems(list: List<Item>) = JSONArray().also { a -> list.forEach { i ->
        a.put(JSONObject().put("id", i.id).put("name", i.name).put("variety", i.variety).put("size", i.size).put("quantity", i.quantity).put("done", i.done))
    } }

    private fun string(o: JSONObject, key: String, required: Boolean = false): String {
        require(o.has(key) && o.get(key) is String) { "Invalid text field." }
        return Rules.text(o.getString(key), required)
    }

    private fun nullableString(o: JSONObject, key: String): String? {
        if (!o.has(key) || o.isNull(key)) return null
        return string(o, key)
    }

    private fun nullableImageData(o: JSONObject, key: String): String? {
        if (!o.has(key) || o.isNull(key)) return null
        val value = o.get(key)
        require(value is String) { "Invalid image data." }
        return value.also { validateImageData(it) }
    }

    private fun readItems(a: JSONArray, allowMissingProductId: Boolean): List<Item> {
        require(a.length() <= 1000) { "Too many items." }
        val result = (0 until a.length()).map { n ->
            val o = a.getJSONObject(n)
            val q = o.get("quantity")
            require(q is Int && o.get("done") is Boolean) { "Invalid quantity or completion field." }
            val productId = nullableString(o, "productId")
            if (!allowMissingProductId) require(productId != null) { "Missing product reference." }
            Rules.checked(Item(string(o, "id", true), string(o, "name", true), string(o, "variety"), string(o, "size"), q, o.getBoolean("done"), productId, if (o.has("notes")) string(o, "notes") else ""))
        }
        require(result.map { it.id }.distinct().size == result.size && result.map { it.key() }.distinct().size == result.size) { "Duplicate backup items." }
        return result
    }

    private fun readLegacyItems(a: JSONArray) = readItems(a, allowMissingProductId = true)

    private fun options(a: JSONArray): List<String> {
        require(a.length() in 1..30)
        return (0 until a.length()).map { n -> require(a.get(n) is String); Rules.text(a.getString(n)) }
    }

    private fun readProduct(o: JSONObject): Product {
        val imageRef = nullableString(o, "imageRef")?.also { validateImageRef(it) }
        val imageData = nullableImageData(o, "imageDataBase64")
        return Product(string(o, "name", true), string(o, "category", true), string(o, "symbol", true), options(o.getJSONArray("varieties")), options(o.getJSONArray("sizes")), string(o, "id", true), imageRef, imageData)
    }

    private fun readProducts(a: JSONArray): List<Product> {
        require(a.length() <= 2000) { "Too many catalogue products." }
        val products = (0 until a.length()).map { readProduct(a.getJSONObject(it)) }
        require(products.map { it.id }.distinct().size == products.size) { "Duplicate catalogue product ids." }
        require(products.map { it.name.lowercase() }.distinct().size == products.size) { "Duplicate catalogue products." }
        return products
    }

    private fun readShopDefinitions(a: JSONArray): List<Shop> {
        require(a.length() in ShopDefaults.definitions.size..2000)
        val result = (0 until a.length()).map { n ->
            val o = a.getJSONObject(n)
            val logoRef = nullableString(o, "logoImageRef")?.also { validateImageRef(it) }
            val logoData = nullableImageData(o, "logoImageDataBase64")
            Shop(string(o, "id", true), Rules.validateShopName(string(o, "name", true)), o.optBoolean("builtIn", false), logoRef, logoData)
        }
        require(result.map { it.id }.distinct().size == result.size)
        require(result.map { it.name.lowercase() }.distinct().size == result.size)
        ShopDefaults.definitions.forEach { required -> require(result.any { it.id == required.id && it.name == required.name && it.builtIn }) }
        return result
    }

    private fun readStore(o: JSONObject, allowMissingAssociations: Boolean): ShopData {
        val associations = if (!o.has("associatedProductIds")) {
            require(allowMissingAssociations)
            emptySet()
        } else {
            val a = o.getJSONArray("associatedProductIds")
            require(a.length() <= 2000)
            (0 until a.length()).map { Rules.text(a.getString(it), true) }.toSet()
        }
        return ShopData(readItems(o.getJSONArray("items"), allowMissingProductId = true), readItems(o.getJSONArray("favourites"), allowMissingProductId = true).also { list -> require(list.none { it.done }) }, readItems(o.getJSONArray("previous"), allowMissingProductId = true), associations)
    }

    private fun decodeV1(o: JSONObject): ShoppingState {
        require(o.get("version") == 1 && o.getString("format") == FORMAT) { "Unsupported backup format." }
        val selected = if (o.isNull("selected")) null else string(o, "selected", true)
        require(selected == null || selected in ShopDefaults.names)
        val stores = o.getJSONObject("stores")
        require(stores.keys().asSequence().toSet() == ShopDefaults.names.toSet())
        val data = ShopDefaults.names.associateWith { shop ->
            val d = stores.getJSONObject(shop)
            ShopData(readLegacyItems(d.getJSONArray("items")), readLegacyItems(d.getJSONArray("favourites")).also { list -> require(list.none { it.done }) }, readLegacyItems(d.getJSONArray("previous")))
        }
        val customArray = o.getJSONArray("custom")
        require(customArray.length() <= 1000)
        val custom = (0 until customArray.length()).map { n ->
            val p = customArray.getJSONObject(n)
            Product(string(p, "name", true), "Other", "🛍", options(p.getJSONArray("varieties")), options(p.getJSONArray("sizes")))
        }
        require(custom.map { it.name.lowercase() }.distinct().size == custom.size)
        return migrateLegacy(ShoppingState(selected, data, custom))
    }

    private fun migrateLegacy(legacy: ShoppingState): ShoppingState {
        val custom = legacy.custom.mapIndexed { index, product -> product.copy(id = "custom:${stableKey(product.name)}:$index") }
        val products = Catalogue.products + custom
        fun productId(item: Item): String {
            val match = products.firstOrNull { it.name.equals(item.name.trim(), true) }
            return item.productId ?: match?.id ?: "${stableLegacyProductId(item.name)}:${stableKey(item.variety)}:${stableKey(item.size)}"
        }
        fun migrateItems(items: List<Item>) = items.map { it.copy(productId = productId(it)) }
        val migratedStores = ShopDefaults.definitions.associate { definition ->
            val old = legacy.stores[definition.name] ?: ShopData()
            val items = migrateItems(old.items)
            val favourites = migrateItems(old.favourites)
            val previous = migrateItems(old.previous)
            definition.id to ShopData(items, favourites, previous, (items + favourites + previous).mapNotNull { it.productId }.toSet())
        }
        return ShoppingState(legacy.selected, migratedStores, custom, ShopDefaults.definitions)
    }

    private fun decodeV2(o: JSONObject): ShoppingState {
        require(o.getInt("version") in 2..CURRENT_VERSION && o.getString("format") == FORMAT) { "Unsupported backup format." }
        require(!o.has("selected")) { "Unsupported backup field." }
        val definitions = readShopDefinitions(o.getJSONArray("shops"))
        val products = readProducts(o.getJSONArray("products"))
        val builtinIds = Catalogue.products.map { it.id }.toSet()
        val custom = products.filterNot { it.id in builtinIds }
        val storesObject = o.getJSONObject("stores")
        require(storesObject.keys().asSequence().toSet() == definitions.map { it.id }.toSet())
        val stores = definitions.associate { definition -> definition.id to readStore(storesObject.getJSONObject(definition.id), allowMissingAssociations = false) }
        val selectedId = if (o.isNull("selectedShopId")) null else string(o, "selectedShopId", true)
        require(selectedId == null || definitions.any { it.id == selectedId })
        val validIds = (Catalogue.products + custom).map { it.id }.toSet()
        stores.values.flatMap { it.associatedProductIds }.forEach { require(it in validIds) }
        stores.values.flatMap { it.items + it.favourites + it.previous }.flatMap { listOfNotNull(it.productId) }.forEach { require(it in validIds) }
        return ShoppingState(selectedId?.let { id -> definitions.first { it.id == id }.name }, stores, custom, definitions)
    }

    fun decode(raw: String): ShoppingState {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 1 MB." }
        val o = JSONObject(raw)
        return when (o.getInt("version")) {
            1 -> decodeV1(o)
            in 2..CURRENT_VERSION -> decodeV2(o)
            else -> throw IllegalArgumentException("Unsupported backup format.")
        }
    }

    private fun validateImageRef(ref: String) {
        require(ref.length in 1..200 && !ref.contains('/') && !ref.contains('\\') && ref != "." && ref != ".." && !ref.contains("..")) { "Invalid image reference." }
    }

    private fun validateImageData(value: String) {
        require(value.length <= 400_000)
        try { require(Base64.decode(value, Base64.DEFAULT).size <= ImageStore.MAX_BYTES) } catch (_: IllegalArgumentException) { error("Invalid image data.") }
    }
}

class LocalStore(context: Context, name: String = "shopping.db") : SQLiteOpenHelper(context, name, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE state (id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL)") }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        require(oldVersion in 1..2 && newVersion == DATABASE_VERSION) { "Unsupported database migration $oldVersion->$newVersion" }
        db.rawQuery("SELECT payload FROM state WHERE id=1", null).use { cursor ->
            if (!cursor.moveToFirst()) return
            val migrated = Backup.encode(Backup.decode(cursor.getString(0)))
            db.update("state", ContentValues().apply { put("payload", migrated) }, "id=1", null).also { check(it == 1) }
        }
    }

    fun load(): ShoppingState = readableDatabase.rawQuery("SELECT payload FROM state WHERE id=1", null).use { if (it.moveToFirst()) Backup.decode(it.getString(0)) else ShoppingState() }

    fun save(state: ShoppingState) {
        val payload = Backup.encode(state)
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.insertWithOnConflict("state", null, ContentValues().apply { put("id", 1); put("payload", payload) }, SQLiteDatabase.CONFLICT_REPLACE).also { check(it != -1L) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    companion object { const val DATABASE_VERSION = 3 }
}

data class ImportedImage(val reference: String, val base64: String)

object ImageStore {
    const val MAX_BYTES = 256 * 1024
    private const val MAX_DIMENSION = 4096

    fun copyFromUri(context: Context, uri: Uri): ImportedImage {
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count == -1) break
                require(output.size() + count <= MAX_BYTES) { "Image is too large." }
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: error("Image could not be read.")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        require(BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds) != null || bounds.outWidth > 0) { "Unsupported image format." }
        require(bounds.outWidth in 1..MAX_DIMENSION && bounds.outHeight in 1..MAX_DIMENSION) { "Image dimensions are not supported." }
        val reference = "${UUID.randomUUID()}.img"
        val directory = File(context.filesDir, "product-images").apply { mkdirs() }
        FileOutputStream(File(directory, reference)).use { it.write(bytes) }
        return ImportedImage(reference, Base64.encodeToString(bytes, Base64.NO_WRAP))
    }

    fun file(context: Context, reference: String): File {
        require(reference.length in 1..200 && !reference.contains('/') && !reference.contains('\\') && !reference.contains(".."))
        return File(File(context.filesDir, "product-images"), reference)
    }
}
