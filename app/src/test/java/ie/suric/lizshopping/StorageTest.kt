package ie.suric.lizshopping

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StorageTest {
    private val lidlId = ShopIds.builtIn("Lidl")
    private val eggs = Item(name = "Eggs", variety = "Free Range", size = "12 pack", quantity = 2,
        done = true, productId = Catalogue.find("Eggs")!!.id, shopId = lidlId, notes = "Large carton")
    private fun sample(): ShoppingState {
        val customShopState = Rules.createShop(ShoppingState(), "Dealz")
        val dealz = customShopState.shopDirectory.single { it.name == "Dealz" }
        val categorized = Rules.createCategory(customShopState, "Garden")
        val category = categorized.customCategories.single()
        val product = Product("Special soap", category.name, "🛍", listOf("Sensitive"), listOf("375 g"), id = ProductIds.custom(), categoryId = category.id)
        val shopsData = categorized.stores +
            (lidlId to ShopData(items = listOf(eggs), favourites = listOf(eggs.copy(done = false)), previous = listOf(eggs.copy(done = false)), associatedProductIds = setOf(eggs.productId))) +
            (dealz.id to ShopData(associatedProductIds = setOf(product.id)))
        return categorized.copy(selectedShopId = lidlId, stores = shopsData,
            custom = listOf(product), customCategories = listOf(category))
    }

    @Test fun version2BackupRoundTripKeepsShopProductCategoryAndEntryIdentities() {
        val source = sample()
        val restored = Backup.decode(Backup.encode(source))
        assertEquals(source, restored)
        assertEquals(Backup.CURRENT_VERSION, JSONObject(Backup.encode(source)).getInt("version"))
    }
    @Test fun shopsHaveIndependentListsAssociationsAndFavourites() {
        val decoded = Backup.decode(Backup.encode(sample()))
        assertTrue(decoded.shopData("Aldi").items.isEmpty())
        assertTrue(decoded.shopData("Aldi").favourites.isEmpty())
        assertTrue(decoded.shopData("Aldi").associatedProductIds.isEmpty())
        assertEquals(1, decoded.shopData("Dealz").associatedProductIds.size)
    }
    @Test fun localImageReferencesBecomeSafeCategoryFallbacksInPortableBackup() {
        val source = sample()
        val product = source.custom.single().copy(image = ProductImageReference("local", "123e4567-e89b-12d3-a456-426614174000.jpg"))
        val restored = Backup.decode(Backup.encode(source.copy(custom = listOf(product))))
        assertEquals(ProductImageReference("category", product.categoryId), restored.custom.single().image)
    }
    @Test fun bundledImageReferenceRoundTripsWithoutNetworkOrUri() {
        val reference = ProductImageReference("bundled", "milk")
        val product = sample().custom.single().copy(image = reference)
        assertEquals(reference, Backup.decode(Backup.encode(sample().copy(custom = listOf(product)))).custom.single().image)
    }
    @Test fun sqliteSnapshotRetainsAppOwnedLocalImageReferenceAcrossRestart() {
        val app = RuntimeEnvironment.getApplication(); val name = "image-${System.nanoTime()}.db"
        val source = sample()
        val local = source.custom.single().copy(image = ProductImageReference("local", "123e4567-e89b-12d3-a456-426614174000.webp"))
        LocalStore(app, name).use { it.save(source.copy(custom = listOf(local))) }
        LocalStore(app, name).use { assertEquals(local.image, it.load().custom.single().image) }
        app.deleteDatabase(name)
    }
    @Test fun sqliteSnapshotSurvivesConnectionRestart() {
        val app = RuntimeEnvironment.getApplication(); val name = "test-${System.nanoTime()}.db"
        LocalStore(app, name).use { it.save(sample()) }
        LocalStore(app, name).use { assertEquals(sample(), it.load()) }
        app.deleteDatabase(name)
    }
    @Test fun version1BackupImportsAndPreservesAllLegacyFields() {
        val migrated = Backup.decode(version1Payload())
        val lidl = migrated.shopData("Lidl")
        assertEquals(ShopIds.builtIn("Lidl"), migrated.selectedShopId)
        assertEquals("legacy-eggs", lidl.items.single().id)
        assertEquals("Eggs", lidl.items.single().name)
        assertEquals("Free Range", lidl.items.single().variety)
        assertEquals("12 pack", lidl.items.single().size)
        assertEquals(2, lidl.items.single().quantity)
        assertTrue(lidl.items.single().done)
        assertEquals("legacy-favourite", lidl.favourites.single().id)
        assertEquals(3, lidl.favourites.single().quantity)
        assertEquals("legacy-previous", lidl.previous.single().id)
        assertEquals(1, migrated.custom.count { it.name == "Olives" }) // one shared identity reused in all old references
        assertEquals(lidl.items.single().productId, lidl.favourites.single().productId)
    }
    @Test fun sqliteVersion1MigrationIsTransactionalAndPreservesPayload() {
        val app = RuntimeEnvironment.getApplication(); val name = "v1-${System.nanoTime()}.db"
        val file = app.getDatabasePath(name); file.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        db.execSQL("CREATE TABLE state (id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL)")
        db.insert("state", null, ContentValues().apply { put("id", 1); put("payload", version1Payload()) })
        db.version = 1; db.close()
        LocalStore(app, name).use { store ->
            val migrated = store.load()
            assertEquals("legacy-eggs", migrated.shopData("Lidl").items.single().id)
            assertEquals(2, SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version })
        }
        app.deleteDatabase(name)
    }
    @Test fun failedSqliteMigrationLeavesVersionAndSourcePayloadIntact() {
        val app = RuntimeEnvironment.getApplication(); val name = "bad-v1-${System.nanoTime()}.db"
        val file = app.getDatabasePath(name); file.parentFile?.mkdirs()
        val db = SQLiteDatabase.openOrCreateDatabase(file, null)
        db.execSQL("CREATE TABLE state (id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL)")
        db.insert("state", null, ContentValues().apply { put("id", 1); put("payload", "not-json") })
        db.version = 1; db.close()
        try { LocalStore(app, name).use { it.load() }; fail("Expected migration to fail") } catch (_: Exception) { }
        SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY).use { check ->
            assertEquals(1, check.version)
            check.rawQuery("SELECT payload FROM state WHERE id=1", null).use { cursor -> assertTrue(cursor.moveToFirst()); assertEquals("not-json", cursor.getString(0)) }
        }
        app.deleteDatabase(name)
    }
    @Test(expected = IllegalArgumentException::class) fun unknownBackupVersionRejected() { Backup.decode(JSONObject(Backup.encode(sample())).put("version", 99).toString()) }
    @Test(expected = IllegalArgumentException::class) fun oversizedBackupRejected() { Backup.decode(" ".repeat(Backup.MAX_BYTES + 1)) }
    @Test(expected = IllegalArgumentException::class) fun invalidSelectedShopRejected() { Backup.decode(JSONObject(Backup.encode(sample())).put("selectedShopId", "unknown").toString()) }
    @Test(expected = IllegalArgumentException::class) fun fractionalQuantityRejected() {
        val json = JSONObject(Backup.encode(sample())); json.getJSONArray("stores").getJSONObject(0).getJSONArray("items").getJSONObject(0).put("quantity", 1.5); Backup.decode(json.toString())
    }
    @Test fun invalidRestoreLeavesStoredDataIntact() {
        val app = RuntimeEnvironment.getApplication(); val name = "invalid-restore-${System.nanoTime()}.db"
        LocalStore(app, name).use { db ->
            val before = sample(); db.save(before)
            try { val decoded = Backup.decode("{}"); db.save(decoded); fail() } catch (_: Exception) { }
            assertEquals(before, db.load())
        }
        app.deleteDatabase(name)
    }
    @Test fun v2RejectsUnknownProductReferenceWithoutReplacingState() {
        val valid = Backup.encode(sample())
        val json = JSONObject(valid)
        json.getJSONArray("stores").getJSONObject(0).getJSONArray("items").getJSONObject(0).put("productId", "missing-product")
        try { Backup.decode(json.toString()); fail("Unknown product reference accepted") } catch (_: IllegalArgumentException) { }
        assertEquals(sample(), Backup.decode(valid))
    }

    private fun version1Payload(): String {
        fun item(id: String, name: String, quantity: Int, done: Boolean, variety: String = "", size: String = "") =
            JSONObject().put("id", id).put("name", name).put("variety", variety).put("size", size).put("quantity", quantity).put("done", done)
        val stores = JSONObject()
        shops.forEach { name ->
            val list = when (name) { "Lidl" -> JSONArray().put(item("legacy-eggs", "Eggs", 2, true, "Free Range", "12 pack")); else -> JSONArray() }
            val favs = if (name == "Lidl") JSONArray().put(item("legacy-favourite", "Eggs", 3, false, "Free Range", "12 pack")) else JSONArray()
            val previous = if (name == "Lidl") JSONArray().put(item("legacy-previous", "Olives", 1, false, "Green", "375 g")) else JSONArray()
            stores.put(name, JSONObject().put("items", list).put("favourites", favs).put("previous", previous))
        }
        return JSONObject().put("format", "liz-shopping-list").put("version", 1).put("selected", "Lidl").put("stores", stores)
            .put("custom", JSONArray().put(JSONObject().put("name", "Olives").put("varieties", JSONArray().put("Green")).put("sizes", JSONArray().put("375 g")))).toString()
    }
}
