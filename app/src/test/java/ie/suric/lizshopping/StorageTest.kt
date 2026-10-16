package ie.suric.lizshopping

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject
import android.database.sqlite.SQLiteDatabase
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StorageTest {
    private val item = Item(name = "Eggs", variety = "Free Range", size = "12 pack", quantity = 2, done = true)
    private fun sample() = ShoppingState(selected = "Lidl", stores = ShoppingState().stores + ("Lidl" to ShopData(items = listOf(item), favourites = listOf(item.copy(done = false)))), custom = listOf(Product("Special soap", "Other", "🛍", listOf(""), listOf(""))))
    @Test fun backupRoundTripKeepsEverything() { assertEquals(sample(), Backup.decode(Backup.encode(sample()))) }
    @Test fun selectedShopRoundTripKeepsCurrentShop() { assertEquals("Aldi", Backup.decode(Backup.encode(ShoppingState(selected = "Aldi"))).selected) }
    @Test fun shopsAreIndependent() { val s = Backup.decode(Backup.encode(sample())); assertTrue(s.stores.getValue("Aldi").items.isEmpty()); assertTrue(s.stores.getValue("Aldi").favourites.isEmpty()) }
    @Test fun sqliteSurvivesConnectionRestart() {
        val app = RuntimeEnvironment.getApplication()
        val name = "test-${System.nanoTime()}.db"
        LocalStore(app, name).use { it.save(sample()) }
        LocalStore(app, name).use { assertEquals(sample(), it.load()) }
        app.deleteDatabase(name)
    }
    @Test fun repeatedCatalogueAddsPersistCorrectQuantity() {
        val app = RuntimeEnvironment.getApplication()
        val name = "catalogue-add-${System.nanoTime()}.db"
        try {
            val product = Catalogue.find("Rolls")!!
            var state = ShoppingState(selected = "Lidl")
            repeat(10) {
                val definition = state.shop("Lidl")
                val data = state.stores.getValue(definition.id)
                val item = Rules.defaultItem(product, data)!!
                state = state.copy(stores = state.stores + (definition.id to data.copy(
                    items = Rules.add(data.items, item),
                    associatedProductIds = data.associatedProductIds + product.id
                )))
            }
            LocalStore(app, name).use { it.save(state) }
            LocalStore(app, name).use {
                val restored = it.load()
                assertEquals(1, restored.dataFor("Lidl").items.size)
                assertEquals(10, restored.dataFor("Lidl").items.single().quantity)
                assertEquals(product.id, restored.dataFor("Lidl").items.single().productId)
            }
        } finally {
            app.deleteDatabase(name)
        }
    }
    @Test(expected = IllegalArgumentException::class) fun unknownVersionRejected() { val json = JSONObject(Backup.encode(sample())).put("version", 99); Backup.decode(json.toString()) }
    @Test(expected = IllegalArgumentException::class) fun oversizedBackupRejected() { Backup.decode(" ".repeat(Backup.MAX_BYTES + 1)) }
    @Test(expected = IllegalArgumentException::class) fun invalidShopRejected() { Backup.decode(JSONObject(Backup.encode(sample())).put("selected", "Unknown").toString()) }
    @Test(expected = IllegalArgumentException::class) fun fractionalQuantityRejected() {
        val json = JSONObject(Backup.encode(sample())); json.getJSONObject("stores").getJSONObject("Lidl").getJSONArray("items").getJSONObject(0).put("quantity", 1.5); Backup.decode(json.toString())
    }
    @Test fun invalidRestoreLeavesStoredDataIntact() {
        val app = RuntimeEnvironment.getApplication(); val name = "test-${System.nanoTime()}.db"
        LocalStore(app, name).use { db -> db.save(sample()); try { val decoded = Backup.decode("{}"); db.save(decoded); fail() } catch (_: Exception) { }; assertEquals(sample(), db.load()) }
        app.deleteDatabase(name)
    }
    @Test fun customHtmlIsPlainText() { val s = sample().copy(custom = listOf(Product("<script>alert(1)</script>", "Other", "🛍", listOf(""), listOf("")))); assertEquals(s, Backup.decode(Backup.encode(s))) }

    @Test fun legacyBackupMigratesToStableReferences() {
        val legacy = Backup.encodeLegacy(sample())
        val migrated = Backup.decode(legacy)
        val migratedItem = migrated.dataFor("Lidl").items.single()
        assertEquals(Catalogue.find("Eggs")!!.id, migratedItem.productId)
        assertTrue(migrated.dataFor("Lidl").associatedProductIds.contains(Catalogue.find("Eggs")!!.id))
        assertTrue(migrated.dataFor("Lidl").items.single().done)
        assertEquals(2, migrated.dataFor("Lidl").items.single().quantity)
        assertEquals(migrated, Backup.decode(Backup.encode(migrated)))
    }

    @Test fun v2RoundTripKeepsCustomProductIdentityAndImageMetadata() {
        val image = Base64.getEncoder().encodeToString(byteArrayOf(1, 2, 3))
        val product = Product("Homemade soap", "Other", "🛍", listOf(""), listOf(""), "custom:soap", "soap.img", image)
        val item = Item(name = product.name, productId = product.id, quantity = 2)
        val state = ShoppingState(selected = "Lidl", custom = listOf(product), stores = ShoppingState().stores + ("Lidl" to ShopData(items = listOf(item), associatedProductIds = setOf(product.id))))
        val restored = Backup.decode(Backup.encode(state))
        assertEquals(product, restored.custom.single())
        assertEquals(product.id, restored.dataFor("Lidl").items.single().productId)
    }

    @Test(expected = IllegalArgumentException::class) fun unsafeImageReferenceRejected() {
        val product = Product("Unsafe", "Other", "🛍", listOf(""), listOf(""), "custom:unsafe", "../outside")
        Backup.decode(Backup.encode(ShoppingState(custom = listOf(product))))
    }

    @Test fun sqliteVersionOneIsMigratedOnceWithoutLosingData() {
        val app = RuntimeEnvironment.getApplication()
        val name = "legacy-${System.nanoTime()}.db"
        val legacyPayload = Backup.encodeLegacy(sample())
        app.openOrCreateDatabase(name, SQLiteDatabase.CREATE_IF_NECESSARY, null).use { db ->
            db.execSQL("CREATE TABLE state (id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL)")
            db.execSQL("INSERT INTO state(id, payload) VALUES(1, ?)", arrayOf(legacyPayload))
            db.version = 1
        }
        LocalStore(app, name).use { store ->
            val first = store.load()
            assertEquals(2, first.dataFor("Lidl").items.single().quantity)
            assertEquals(Catalogue.find("Eggs")!!.id, first.dataFor("Lidl").items.single().productId)
        }
        LocalStore(app, name).use { store -> assertEquals(Catalogue.find("Eggs")!!.id, store.load().dataFor("Lidl").items.single().productId) }
        app.deleteDatabase(name)
    }

    @Test fun failedLegacyMigrationLeavesOriginalPayloadAvailable() {
        val app = RuntimeEnvironment.getApplication()
        val name = "broken-legacy-${System.nanoTime()}.db"
        val payload = "{\"format\":\"liz-shopping-list\",\"version\":1,\"selected\":null,\"stores\":{},\"custom\":[]}"
        app.openOrCreateDatabase(name, SQLiteDatabase.CREATE_IF_NECESSARY, null).use { db ->
            db.execSQL("CREATE TABLE state (id INTEGER PRIMARY KEY CHECK(id=1), payload TEXT NOT NULL)")
            db.execSQL("INSERT INTO state(id, payload) VALUES(1, ?)", arrayOf(payload))
            db.version = 1
        }
        try { LocalStore(app, name).use { it.load() }; fail("broken legacy data must not be accepted") } catch (_: Exception) { }
        app.openOrCreateDatabase(name, SQLiteDatabase.CREATE_IF_NECESSARY, null).use { db ->
            assertEquals(1, db.version)
            db.rawQuery("SELECT payload FROM state WHERE id=1", null).use { cursor -> assertTrue(cursor.moveToFirst()); assertEquals(payload, cursor.getString(0)) }
        }
        app.deleteDatabase(name)
    }
}
