package ie.suric.lizshopping

import org.junit.Assert.*
import org.junit.Test

class RulesTest {
    private val eggs = Item(name = "Eggs", variety = "Free Range", size = "12 pack", quantity = 2)
    @Test fun packagesAreSeparateFromContents() { assertEquals(2, eggs.quantity); assertEquals("Free Range · 12 pack", eggs.detail()) }
    @Test fun duplicatesMergeWithWhitespaceAndCaseNormalisation() {
        val items = Rules.add(listOf(eggs), eggs.copy(id = "new", name = " eggs ", variety = "free range", quantity = 1))
        assertEquals(1, items.size); assertEquals(3, items.single().quantity)
    }
    @Test fun differentSizesRemainSeparate() { assertEquals(2, Rules.add(listOf(eggs), eggs.copy(id = "new", size = "6 pack")).size) }
    @Test fun differentVarietiesRemainSeparate() { assertEquals(2, Rules.add(listOf(eggs), eggs.copy(id = "new", variety = "Organic")).size) }
    @Test fun quantityNeverDropsBelowOne() { assertEquals(1, Rules.changeQuantity(eggs, -999).quantity) }
    @Test fun quantityHasSafeUpperBound() { assertEquals(999, Rules.changeQuantity(eggs, 999).quantity) }
    @Test fun editingMergesCollision() {
        val milk = eggs.copy(id = "other", name = "Milk", quantity = 1)
        assertEquals(3, Rules.edit(listOf(eggs, milk), milk.copy(name = "Eggs")).single().quantity)
    }
    @Test fun temporaryListQuantityDoesNotMutateFavourite() {
        val d = Rules.favourite(ShopData(items = listOf(eggs)), eggs)
        val changed = d.copy(items = d.items.map { Rules.changeQuantity(it, 1) })
        assertEquals(2, changed.favourites.single().quantity); assertEquals(3, changed.items.single().quantity)
    }
    @Test fun freshListPreservesFavouritesAndReusableItems() {
        val fresh = Rules.fresh(Rules.favourite(ShopData(items = listOf(eggs.copy(done = true))), eggs))
        assertTrue(fresh.items.isEmpty()); assertEquals(1, fresh.favourites.size); assertFalse(fresh.previous.single().done)
        assertEquals(eggs.quantity, Rules.reuse(fresh).items.single().quantity)
    }
    @Test fun addingCompletedProductReopensIt() { assertFalse(Rules.add(listOf(eggs.copy(done = true)), eggs).single().done) }
    @Test fun repeatedFavouriteSaveDoesNotDuplicate() { assertEquals(1, Rules.favourite(Rules.favourite(ShopData(), eggs), eggs.copy(quantity = 3)).favourites.size) }
    @Test fun catalogueIncludesCorePresets() {
        assertEquals(listOf("6 pack", "12 pack", "18 pack", "24 pack"), Catalogue.find("Eggs")!!.sizes)
        assertTrue(Catalogue.find("Cooking oil")!!.sizes.contains("1.5 L"))
        assertTrue(Catalogue.find("Potatoes")!!.varieties.contains("Kerr's Pink"))
    }
    @Test fun customWeightIsPreserved() { assertEquals("375 g", Rules.checked(Item(name = "Olives", size = "375 g")).size) }
    @Test(expected = IllegalArgumentException::class) fun emptyNameRejected() { Rules.checked(Item(name = " ")) }
    @Test(expected = IllegalArgumentException::class) fun negativeQuantityRejected() { Rules.checked(eggs.copy(quantity = -1)) }
    @Test(expected = IllegalArgumentException::class) fun controlTextRejected() { Rules.checked(eggs.copy(name = "Eggs\nInjected")) }
    @Test(expected = IllegalArgumentException::class) fun overlongNameRejected() { Rules.checked(eggs.copy(name = "a".repeat(101))) }
}
