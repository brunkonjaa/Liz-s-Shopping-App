package ie.suric.lizshopping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CatalogueTest {
    @Test fun researchedFoodCatalogueIsCompleteAndSearchable() {
        val products = ShoppingState().allProducts()
        assertEquals(products.size, products.map { it.id }.distinct().size)
        assertEquals(products.size, products.map { it.name.lowercase() }.distinct().size)
        assertTrue("Expected the expanded four-store food catalogue", products.size >= 850)
        assertTrue(products.all { it.description.isNotBlank() && it.symbol.isNotBlank() })
        assertTrue(products.all { it.sizes.isNotEmpty() && it.varieties.isNotEmpty() })
        assertEquals(ResearchCatalogue.products.size, ResearchCatalogue.products.map { it.id }.distinct().size)
        assertTrue(ResearchCatalogue.products.all { it.researchShops.isNotEmpty() && it.researchShops.all { shop -> shop in ShopDefaults.names } })

        listOf("Lamb chops", "Fresh basil", "Prawns", "Tinned tomatoes", "Frozen fish and seafood").forEach {
            assertNotNull("Expected researched item: $it", Catalogue.find(it))
        }
        assertTrue(Catalogue.find("Fresh basil")!!.researchShops.contains("Lidl"))
        val beef = ResearchCatalogue.products.first { it.name.startsWith("Beef diced / stewing") }
        assertTrue(beef.researchShops.containsAll(listOf("Lidl", "Aldi", "Dunnes Stores")))
    }

    @Test fun expandedCatalogueStillFitsValidatedOfflineBackup() {
        val encoded = Backup.encode(ShoppingState())
        assertTrue(encoded.toByteArray(Charsets.UTF_8).size <= Backup.MAX_BYTES)
        val decoded = Backup.decode(encoded)
        assertEquals(ShoppingState().allProducts(), decoded.allProducts())
        assertEquals(
            ResearchCatalogue.products.associate { it.id to it.researchShops },
            decoded.allProducts().filter { it.researchShops.isNotEmpty() }.associate { it.id to it.researchShops }
        )
    }
}
