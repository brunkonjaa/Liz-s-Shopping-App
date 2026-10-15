package ie.suric.lizshopping

import android.util.Log
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingJourneyTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        try {
            rule.waitUntil(10_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        } catch (error: ComposeTimeoutException) {
            rule.onRoot(useUnmergedTree = true).printToLog("ShoppingJourneyTimeout")
            val current = ViewModelProvider(rule.activity)[ShoppingViewModel::class.java].state.value
            val selected = current?.selected
            val items = selected?.let { current.dataFor(it).items }.orEmpty()
            Log.e("ShoppingJourneyTimeout", "expected=$text selected=$selected items=${items.map { it.name + " x" + it.quantity }} addCompleted=${items.any { it.name.equals("Bread", true) }}")
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("screencap -p /sdcard/shopping-journey-timeout.png").close()
            throw error
        }
    }

    private fun closeDrawerIfOpen() {
        if (rule.onAllNodesWithContentDescription("Close navigation menu").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithContentDescription("Close navigation menu").performClick()
        }
    }

    private fun logShoppingState(label: String) {
        rule.waitForIdle()
        val current = ViewModelProvider(rule.activity)[ShoppingViewModel::class.java].state.value
        val selected = current?.selected
        val items = selected?.let { current.dataFor(it).items }.orEmpty()
        Log.e("ShoppingJourneyState", "$label selected=$selected items=${items.map { it.name + " x" + it.quantity }} message=${ViewModelProvider(rule.activity)[ShoppingViewModel::class.java].message.value}")
    }

    private fun waitForBreadQuantity(expected: Int) {
        rule.waitUntil(10_000) {
            val current = ViewModelProvider(rule.activity)[ShoppingViewModel::class.java].state.value
            val selected = current?.selected ?: return@waitUntil false
            current.dataFor(selected).items.any { it.name.equals("Bread", true) && it.quantity == expected }
        }
    }

    private fun ensureBreadFavourite() {
        val saveDescription = "Save Bread as favourite"
        if (rule.onAllNodesWithContentDescription(saveDescription).fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithContentDescription(saveDescription).performClick()
        } else {
            rule.onNodeWithContentDescription("Remove Bread from favourites").assertExists()
        }
    }

    @After
    fun captureFinalState() {
        rule.waitForIdle()
    }

    @Test
    fun directAddQuantityCompletionFavouritesIsolationAndRelaunch() {
        closeDrawerIfOpen()
        if (rule.onAllNodesWithText("Shopping List").fetchSemanticsNodes().isEmpty()) {
            waitForText("Lidl")
            rule.onAllNodesWithText("Lidl").onFirst().performClick()
            waitForText("Shopping List")
        }

        // A catalogue product is added from its explicit primary control;
        // configuration remains available as a separate secondary action.
        rule.onAllNodesWithText("All Items").onFirst().performClick()
        waitForText("Search for a product")
        rule.onNodeWithText("Search for a product").performTextInput("Bread")
        waitForText("Bread")
        rule.onNodeWithContentDescription("Add Bread to shopping list").performClick()
        waitForBreadQuantity(1)
        logShoppingState("afterFirstBreadAdd")
        rule.onNodeWithContentDescription("Add Bread to shopping list").performClick()
        waitForBreadQuantity(2)
        logShoppingState("afterSecondBreadAdd")

        rule.onAllNodesWithText("Shopping List").onFirst().performClick()
        waitForText("Bread")
        rule.onNodeWithText("×2").assertExists()

        // Quantity changes stay on the list and tolerate rapid repeated taps.
        repeat(5) { rule.onNodeWithContentDescription("Increase Bread quantity").performClick() }
        rule.onNodeWithContentDescription("Decrease Bread quantity").performClick()
        rule.onNodeWithContentDescription("Mark Bread bought").performClick()
        rule.onNodeWithContentDescription("Mark Bread still to buy").assertExists()
        rule.onNodeWithContentDescription("Mark Bread still to buy").performClick()
        rule.onNodeWithContentDescription("Mark Bread bought").assertExists()

        // Favourites reuse the same direct-add interaction.
        rule.onAllNodesWithText("All Items").onFirst().performClick()
        waitForText("Search for a product")
        rule.onNodeWithText("Search for a product").performTextInput("Bread")
        waitForText("Bread")
        ensureBreadFavourite()
        rule.onAllNodesWithText("Favourites").onFirst().performClick()
        waitForText("Bread")
        rule.onNodeWithContentDescription("Add Bread to shopping list").performClick()
        waitForBreadQuantity(7)
        rule.onAllNodesWithText("Shopping List").onFirst().performClick()
        waitForText("Your shopping list for Lidl")

        // Switching shops keeps the lists independent.
        rule.onNodeWithText("Change shop ▾").performClick()
        rule.onNodeWithContentDescription("Select Aldi shop").performClick()
        waitForText("Your shopping list for Aldi")
        rule.onNodeWithText("Bread").assertDoesNotExist()
        rule.onNodeWithText("Change shop ▾").performClick()
        rule.onNodeWithContentDescription("Select Lidl shop").performClick()
        waitForText("Your shopping list for Lidl")
        rule.onNodeWithText("Bread").assertExists()

        // The current shop reopens directly into its list after recreation.
        rule.activityRule.scenario.recreate()
        waitForText("Your shopping list for Lidl")
        rule.onNodeWithText("Bread").assertExists()
    }
}
