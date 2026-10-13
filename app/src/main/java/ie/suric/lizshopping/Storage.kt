package ie.suric.lizshopping

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

object Backup {
    const val MAX_BYTES = 1_048_576
    private fun item(i: Item) = JSONObject().put("id", i.id).put("name", i.name).put("variety", i.variety).put("size", i.size).put("quantity", i.quantity).put("done", i.done)
    private fun items(list: List<Item>) = JSONArray().also { a -> list.forEach { a.put(item(it)) } }
    fun encode(s: ShoppingState): String {
        val root = JSONObject().put("format", "liz-shopping-list").put("version", 1).put("selected", s.selected ?: JSONObject.NULL)
        val stores = JSONObject()
        shops.forEach { name ->
            val d = s.stores.getValue(name)
            stores.put(name, JSONObject().put("items", items(d.items)).put("favourites", items(d.favourites)).put("previous", items(d.previous)))
        }
        root.put("stores", stores)
        root.put("custom", JSONArray().also { a -> s.custom.forEach { p -> a.put(JSONObject().put("name", p.name).put("varieties", JSONArray(p.varieties)).put("sizes", JSONArray(p.sizes))) } })
        val encoded = root.toString()
        require(encoded.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Shopping data is too large. Clear old items before adding more." }
        decode(encoded) // Validate internal invariants before committing a snapshot.
        return encoded
    }
    private fun string(o: JSONObject, key: String, required: Boolean = false): String {
        require(o.get(key) is String) { "Invalid text field." }
        return Rules.text(o.getString(key), required)
    }
    private fun readItems(a: JSONArray): List<Item> {
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
    fun decode(raw: String): ShoppingState {
        require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Backup exceeds 1 MB." }
        val o = JSONObject(raw)
        require(o.get("version") == 1 && o.getString("format") == "liz-shopping-list") { "Unsupported backup format." }
        val selected = if (o.isNull("selected")) null else string(o, "selected", true)
        require(selected == null || selected in shops)
        val stores = o.getJSONObject("stores")
        require(stores.keys().asSequence().toSet() == shops.toSet())
        val data = shops.associateWith { shop ->
            val d = stores.getJSONObject(shop)
            ShopData(readItems(d.getJSONArray("items")), readItems(d.getJSONArray("favourites")).also { list -> require(list.none { it.done }) }, readItems(d.getJSONArray("previous")))
        }
        val c = o.getJSONArray("custom")
        require(c.length() <= 1000)
        fun options(a: JSONArray): List<String> {
            require(a.length() in 1..30)
            return (0 until a.length()).map { n -> require(a.get(n) is String); Rules.text(a.getString(n)) }
        }
        val custom = (0 until c.length()).map { n -> val p = c.getJSONObject(n); Product(string(p, "name", true), "Other", "🛍", options(p.getJSONArray("varieties")), options(p.getJSONArray("sizes"))) }
        require(custom.map { it.name.lowercase(java.util.Locale.ROOT) }.distinct().size == custom.size)
        return ShoppingState(selected, data, custom)
    }
}

class LocalStore(context: Context, name: String = "shopping.db") : SQLiteOpenHelper(context, name, null, 1) {
    override fun onCreate(db: SQLiteDatabase) { db.execSQL("CREATE TABLE state (id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL)") }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Explicit migration required") }
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
}
