package ie.suric.lizshopping

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingJourneyTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Test fun selectShopAddTwoMilkAndTickOff() {
        rule.waitUntil(10000) { rule.onAllNodesWithText("Lidl").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Lidl").performClick()
        rule.onNodeWithText("Search for a product").performTextInput("Milk")
        rule.onNode(hasText("Milk") and hasText("Dairy and eggs")).performClick()
        rule.onNodeWithText("2 L").performClick()
        rule.onNodeWithContentDescription("Increase Milk quantity").performClick()
        rule.onNodeWithText("+ Add to shopping list").performScrollTo().performClick()
        rule.onNodeWithText("Shopping List").performClick()
        rule.onNodeWithText("×2").assertExists()
        rule.onNodeWithContentDescription("Mark Milk bought").performClick()
        rule.onNodeWithText("Bought ✓").assertExists()
        rule.onNodeWithContentDescription("Mark Milk still to buy").performClick()
        rule.onNodeWithText("Bought ✓").assertDoesNotExist()
        rule.onNodeWithText("All Items").performClick()
        rule.onNodeWithText("Search for a product").performTextInput("Eggs")
        rule.onNode(hasText("Eggs") and hasText("Dairy and eggs")).performClick()
        rule.onNodeWithText("Free Range").performClick()
        rule.onNodeWithText("12 pack").performClick()
        rule.onNodeWithText("♥  Save as a favourite for this shop").performScrollTo().performClick()
        rule.onNodeWithText("+ Add to shopping list").performScrollTo().performClick()
        rule.onNodeWithText("Shopping List").performClick()
        rule.onNodeWithText("Free Range · 12 pack").assertExists()
        rule.onNodeWithText("×1").assertExists()
        rule.onNodeWithContentDescription("Increase Eggs quantity").performScrollTo().performClick()
        rule.onNodeWithText("Favourites").performClick()
        rule.onNodeWithText("Free Range · 12 pack · ×1").assertExists()
        rule.onNodeWithText("+ Add").performClick()
        rule.onNodeWithText("Change shop ▾").performClick()
        rule.onNodeWithText("Aldi").performClick()
        rule.onNodeWithText("Shopping List").performClick()
        rule.onNodeWithText("Your shopping list is empty. Add everyday essentials or choose from favourites.").assertExists()
        rule.onNodeWithText("All Items").performClick()
        rule.onNodeWithText("+ Add custom item").performClick()
        rule.onNodeWithText("Product name (required)").performTextInput("Liz's special biscuits")
        rule.onNodeWithText("+ Add to shopping list").performScrollTo().performClick()
        rule.onNodeWithText("Shopping List").performClick()
        rule.onNodeWithText("Liz's special biscuits").assertExists()
        rule.onNodeWithText("Change shop ▾").performClick()
        rule.onNodeWithText("Lidl").performClick()
        rule.onNodeWithText("Shopping List").performClick()
        rule.onNodeWithText("Milk").assertExists()
        rule.onNodeWithText("Eggs").assertExists()
    }
}
