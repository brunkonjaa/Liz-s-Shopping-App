package ie.suric.lizshopping

import org.junit.Assert.*
import org.junit.Test

class RulesTest {
    private val lidlId = ShopIds.builtIn("Lidl")
    private val eggsProduct = Catalogue.find("Eggs")!!
    private val eggs = Item(name = "Eggs", variety = "Free Range", size = "12 pack", quantity = 2, productId = eggsProduct.id, shopId = lidlId)

    @Test fun packageSizeAndPackageCountRemainSeparate() { assertEquals(2, eggs.quantity); assertEquals("Free Range · 12 pack", eggs.detail()) }
    @Test fun productIdentityIsStableAndReusableAcrossShops() {
        assertEquals(eggsProduct.id, Catalogue.find("Eggs")!!.id)
        assertEquals(eggsProduct.id, eggs.copy(shopId = ShopIds.builtIn("Aldi")).productId)
    }
    @Test fun duplicateItemsMergeOnlyWithinSameProductAndShop() {
        val items = Rules.add(listOf(eggs), eggs.copy(id = "new", name = " eggs ", variety = "free range", quantity = 1))
        assertEquals(1, items.size); assertEquals(3, items.single().quantity)
        assertEquals(2, Rules.add(listOf(eggs), eggs.copy(id = "other", shopId = ShopIds.builtIn("Aldi"))).size)
    }
    @Test fun differentSizesAndVarietiesRemainSeparate() {
        assertEquals(2, Rules.add(listOf(eggs), eggs.copy(id = "new", size = "6 pack")).size)
        assertEquals(2, Rules.add(listOf(eggs), eggs.copy(id = "new", variety = "Organic")).size)
    }
    @Test fun quantityHasSafeBounds() {
        assertEquals(1, Rules.changeQuantity(eggs, -999).quantity)
        assertEquals(999, Rules.changeQuantity(eggs, 999).quantity)
    }
    @Test fun editingMergesCollisionWithoutChangingProductDefinition() {
        val milk = eggs.copy(id = "other", name = "Milk", productId = Catalogue.find("Milk")!!.id, quantity = 1)
        val result = Rules.edit(listOf(eggs, milk), milk.copy(name = "Eggs", productId = eggs.productId))
        assertEquals(1, result.size); assertEquals(3, result.single().quantity)
        assertEquals("Eggs", eggsProduct.name)
    }
    @Test fun shopFavouritesAndQuantitiesAreIndependent() {
        val lidl = Rules.favourite(ShopData(), eggs)
        val changed = lidl.copy(items = listOf(Rules.changeQuantity(eggs, 1)))
        assertEquals(2, changed.favourites.single().quantity)
        assertTrue(ShopData().favourites.isEmpty())
    }
    @Test fun freshAndReuseKeepExistingBehaviour() {
        val fresh = Rules.fresh(Rules.favourite(ShopData(items = listOf(eggs.copy(done = true))), eggs))
        assertTrue(fresh.items.isEmpty()); assertEquals(1, fresh.favourites.size); assertFalse(fresh.previous.single().done)
        assertEquals(eggs.quantity, Rules.reuse(fresh).items.single().quantity)
    }
    @Test fun addingCompletedProductReopensIt() { assertFalse(Rules.add(listOf(eggs.copy(done = true)), eggs).single().done) }
    @Test fun repeatedFavouriteSaveDoesNotDuplicate() { assertEquals(1, Rules.favourite(Rules.favourite(ShopData(), eggs), eggs.copy(quantity = 3)).favourites.size) }
    @Test fun catalogueKeepsExistingGroceryConfigurations() {
        assertEquals(listOf("6 pack", "12 pack", "18 pack", "24 pack"), Catalogue.find("Eggs")!!.sizes)
        assertTrue(Catalogue.find("Cooking oil")!!.sizes.contains("1.5 L"))
        assertTrue(Catalogue.find("Potatoes")!!.varieties.contains("Kerr's Pink"))
    }
    @Test fun searchSuggestsPluralAndPartialMatchesWithoutMerging() {
        val matches = ProductSearch.suggest(Catalogue.products, "  TOMATO ")
        assertTrue(matches.any { it.name == "Tomatoes" })
        assertTrue(ProductSearch.suggest(Catalogue.products, "Fresh tomatoes").any { it.name == "Tomatoes" })
        assertEquals(Catalogue.products.size, Catalogue.products.distinctBy { it.id }.size)
        assertEquals(Catalogue.products.size, Catalogue.products.size)
    }
    @Test fun creatingDistinctProductDoesNotChangeSuggestedProductAndExactDuplicateIsRejected() {
        val base = ShoppingState()
        val original = Catalogue.find("Tomatoes")!!
        val distinct = Product("Cherry Tomatoes", original.category, "🍅", listOf("Cherry"), listOf("250 g"), id = ProductIds.custom(), categoryId = original.categoryId)
        val expanded = Rules.createProduct(base, distinct)
        assertEquals(original, Catalogue.find("Tomatoes"))
        assertNotEquals(original.id, expanded.custom.single().id)
        try { Rules.createProduct(expanded, distinct.copy(id = ProductIds.custom())); fail("Duplicate was accepted") } catch (_: IllegalArgumentException) { }
    }
    @Test fun customShopHasStableIdentityAndArchivePreservesItsData() {
        val withShop = Rules.createShop(ShoppingState(), "Dealz")
        val dealz = withShop.shopDirectory.single { it.name == "Dealz" }
        val withData = withShop.copy(stores = withShop.stores + (dealz.id to ShopData(items = listOf(eggs.copy(shopId = dealz.id)))))
        val archived = Rules.archiveShop(withData, dealz.id)
        assertTrue(archived.shopDirectory.single { it.id == dealz.id }.archived)
        assertEquals(1, archived.stores.getValue(dealz.id).items.size)
        assertNotEquals(lidlId, dealz.id)
    }
    @Test(expected = IllegalArgumentException::class) fun duplicateShopNamesRejectedCaseInsensitively() { Rules.createShop(Rules.createShop(ShoppingState(), "Dealz"), "  DEALZ ") }
    @Test(expected = IllegalArgumentException::class) fun emptyShopNameRejected() { Rules.createShop(ShoppingState(), "  ") }
    @Test fun customCategoryAndProductKeepStableReferences() {
        val withCategory = Rules.createCategory(ShoppingState(), "Garden")
        val category = withCategory.customCategories.single()
        val product = Product("Compost", category.name, "🛍", listOf("Organic"), listOf("20 L"), id = ProductIds.custom(), categoryId = category.id)
        val updated = withCategory.copy(custom = listOf(Rules.checkedProduct(product)))
        assertEquals(category.id, updated.allProducts.single { it.id == product.id }.categoryId)
    }
    @Test fun customWeightIsPreserved() { assertEquals("375 g", Rules.checked(Item(name = "Olives", size = "375 g")).size) }
    @Test(expected = IllegalArgumentException::class) fun emptyNameRejected() { Rules.checked(Item(name = " ")) }
    @Test(expected = IllegalArgumentException::class) fun negativeQuantityRejected() { Rules.checked(eggs.copy(quantity = -1)) }
    @Test(expected = IllegalArgumentException::class) fun controlTextRejected() { Rules.checked(eggs.copy(name = "Eggs\nInjected")) }
    @Test(expected = IllegalArgumentException::class) fun overlongNameRejected() { Rules.checked(eggs.copy(name = "a".repeat(101))) }
}
