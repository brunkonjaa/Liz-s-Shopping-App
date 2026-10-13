package ie.suric.lizshopping

import android.app.Application
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.json.JSONObject

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StorageTest {
    private val item = Item(name = "Eggs", variety = "Free Range", size = "12 pack", quantity = 2, done = true)
    private fun sample() = ShoppingState(selected = "Lidl", stores = ShoppingState().stores + ("Lidl" to ShopData(items = listOf(item), favourites = listOf(item.copy(done = false)))), custom = listOf(Product("Special soap", "Other", "🛍", listOf(""), listOf(""))))
    @Test fun backupRoundTripKeepsEverything() { assertEquals(sample(), Backup.decode(Backup.encode(sample()))) }
    @Test fun shopsAreIndependent() { val s = Backup.decode(Backup.encode(sample())); assertTrue(s.stores.getValue("Aldi").items.isEmpty()); assertTrue(s.stores.getValue("Aldi").favourites.isEmpty()) }
    @Test fun sqliteSurvivesConnectionRestart() {
        val app = RuntimeEnvironment.getApplication()
        val name = "test-${System.nanoTime()}.db"
        LocalStore(app, name).use { it.save(sample()) }
        LocalStore(app, name).use { assertEquals(sample(), it.load()) }
        app.deleteDatabase(name)
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
}
