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
    @Test fun categoryNamesReuseCaseAndWhitespaceEquivalentValues() {
        assertEquals("Household cleaning", Rules.category(listOf("Household cleaning"), " household CLEANING "))
        assertEquals("Pet care", Rules.category(listOf("Household cleaning"), " Pet care "))
    }
    @Test(expected = IllegalArgumentException::class) fun shortCategoryRejected() { Rules.validateCategoryName("x") }
    @Test fun suggestedShopNamesAreDistinctFromBuiltIns() { assertTrue(ShopDefaults.suggestedNames.none { it in ShopDefaults.names }) }
    @Test(expected = IllegalArgumentException::class) fun emptyNameRejected() { Rules.checked(Item(name = " ")) }
    @Test(expected = IllegalArgumentException::class) fun negativeQuantityRejected() { Rules.checked(eggs.copy(quantity = -1)) }
    @Test(expected = IllegalArgumentException::class) fun controlTextRejected() { Rules.checked(eggs.copy(name = "Eggs\nInjected")) }
    @Test(expected = IllegalArgumentException::class) fun overlongNameRejected() { Rules.checked(eggs.copy(name = "a".repeat(101))) }

    @Test fun catalogueProductsHaveStableIds() {
        assertEquals(Catalogue.find("Milk")!!.id, Catalogue.find("milk")!!.id)
        assertTrue(Catalogue.find("Milk")!!.id.startsWith("product:"))
    }

    @Test fun sameProductCanBeUsedByIndependentShops() {
        val product = Catalogue.find("Milk")!!
        val item = Item(name = product.name, productId = product.id, size = "2 L")
        val state = ShoppingState().copy(
            stores = ShoppingState().stores +
                ("Lidl" to ShopData(items = listOf(item), associatedProductIds = setOf(product.id))) +
                ("Aldi" to ShopData(items = listOf(item.copy(id = "aldi")), associatedProductIds = setOf(product.id)))
        )
        assertEquals(1, state.dataFor("Lidl").items.size)
        assertEquals(1, state.dataFor("Aldi").items.size)
        assertNotEquals(state.dataFor("Lidl").items.single().id, state.dataFor("Aldi").items.single().id)
    }

    @Test fun catalogueDefaultReusesSavedConfigurationAndQuantity() {
        val product = Catalogue.find("Milk")!!
        val savedSize = product.sizes.last { it.isNotBlank() }
        val saved = Item(name = product.name, productId = product.id, variety = product.varieties.first(), size = savedSize, quantity = 3)
        val item = Rules.defaultItem(product, ShopData(favourites = listOf(saved)))
        assertEquals(savedSize, item!!.size)
        assertEquals(3, item.quantity)
        assertEquals(product.id, item.productId)
    }

    @Test fun oneTapCatalogueAddMergesInsteadOfDuplicating() {
        val product = Catalogue.find("Milk")!!
        val data = ShopData(items = listOf(Item(name = product.name, productId = product.id, variety = product.varieties.first(), size = product.sizes.first(), quantity = 2)))
        val item = Rules.defaultItem(product, data)!!
        val result = Rules.add(data.items, item)
        assertEquals(1, result.size)
        assertEquals(4, result.single().quantity)
    }

    @Test fun rapidQuantityChangesRemainBoundedAndSequential() {
        var item = Item(name = "Milk", quantity = 1)
        repeat(25) { item = Rules.changeQuantity(item, 1) }
        repeat(10) { item = Rules.changeQuantity(item, -1) }
        assertEquals(16, item.quantity)
        repeat(1_000) { item = Rules.changeQuantity(item, 1) }
        assertEquals(999, item.quantity)
    }

    @Test fun customShopIsStableAndDeletionIsGuarded() {
        val added = Rules.addShop(ShoppingState(), "Spar")
        assertEquals("Spar", added.selected)
        val spar = added.shop("Spar")
        val renamed = Rules.renameShop(added, spar.id, "Local Spar")
        assertEquals(spar.id, renamed.shop("Local Spar").id)
        val withData = renamed.copy(stores = renamed.stores + (spar.id to ShopData(items = listOf(eggs))))
        try { Rules.removeShop(withData, spar.id); fail("shop with data must not be removed") } catch (_: IllegalArgumentException) { }
        val empty = Rules.removeShop(renamed, spar.id)
        assertFalse(empty.shopNames().contains("Local Spar"))
    }
}
