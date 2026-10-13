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
    }
}
