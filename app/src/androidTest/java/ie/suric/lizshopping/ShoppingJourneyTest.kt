package ie.suric.lizshopping

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.After
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

@RunWith(AndroidJUnit4::class)
class ShoppingJourneyTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun screenshot(name: String) {
        rule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        val outputDirectory = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?.let(::File)
            ?: File(instrumentation.targetContext.externalMediaDirs.first(), "additional_test_output")
        check(outputDirectory.isDirectory || outputDirectory.mkdirs())
        File(outputDirectory, "$name-semantics.txt").writeText(rule.onRoot().printToString())
        FileOutputStream(File(outputDirectory, "$name.png")).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        bitmap.recycle()
    }
    private fun waitForText(text: String) {
        rule.waitUntil(10_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }
    @After fun captureFinalState() { screenshot("after-test") }
    @Test fun selectShopAddTwoMilkAndTickOff() {
        rule.waitUntil(10000) { rule.onAllNodesWithText("Lidl").fetchSemanticsNodes().isNotEmpty() }
        screenshot("shops")
        rule.onNodeWithText("Lidl").performClick()
        waitForText("Search for a product")
        rule.onNodeWithText("Search for a product").performTextInput("Milk")
        rule.onNode(hasText("Milk") and hasText("Dairy and eggs")).performClick()
        rule.onNodeWithText("2 L").performScrollTo().performClick()
        rule.onNodeWithContentDescription("Increase Milk quantity").performScrollTo().performClick()
        waitForText("×2")
        screenshot("milk-editor")
        rule.onNodeWithText("+ Add to shopping list").performScrollTo().performClick()
        waitForText("Shopping List")
        rule.onNodeWithText("Shopping List").assertIsDisplayed().performClick()
        waitForText("×2")
        rule.onNodeWithText("×2").assertExists()
        rule.onNodeWithContentDescription("Mark Milk bought").performClick()
        waitForText("Bought ✓")
        rule.onNodeWithText("Bought ✓").assertExists()
        rule.onNodeWithContentDescription("Mark Milk still to buy").performClick()
        rule.waitUntil(10_000) { rule.onAllNodesWithText("Bought ✓").fetchSemanticsNodes().isEmpty() }
        rule.onNodeWithText("Bought ✓").assertDoesNotExist()
        rule.onNodeWithText("All Items").performClick()
        rule.onNodeWithText("Search for a product").performTextInput("Eggs")
        rule.onNode(hasText("Eggs") and hasText("Dairy and eggs")).performClick()
        rule.onNodeWithText("Free Range").performScrollTo().performClick()
        rule.onNodeWithText("12 pack").performScrollTo().performClick()
        rule.onNodeWithText("♥  Save as a favourite for this shop").performScrollTo().performClick()
        rule.onNodeWithText("+ Add to shopping list").performScrollTo().performClick()
        waitForText("Shopping List")
        rule.onNodeWithText("Shopping List").assertIsDisplayed().performClick()
        waitForText("Free Range · 12 pack")
        rule.onNodeWithText("Free Range · 12 pack").assertExists()
        rule.onNodeWithText("×1").assertExists()
        screenshot("shopping-list")
        rule.onNodeWithContentDescription("Increase Eggs quantity").performScrollTo().performClick()
        rule.onNodeWithText("Favourites").performClick()
        waitForText("Free Range · 12 pack · ×1")
        rule.onNodeWithText("Free Range · 12 pack · ×1").assertExists()
        rule.onNodeWithText("+ Add").performClick()
        rule.onNodeWithText("Change shop ▾").performClick()
        rule.onNodeWithText("Aldi").performClick()
        waitForText("Search for a product")
        waitForText("Shopping List")
        rule.onNodeWithText("Shopping List").assertIsDisplayed().performClick()
        rule.onNodeWithText("Your shopping list is empty. Add everyday essentials or choose from favourites.").assertExists()
        rule.onNodeWithText("All Items").performClick()
        rule.onNodeWithText("+ Add custom item").performClick()
        rule.onNodeWithText("Product name (required)").performTextInput("Liz's special biscuits")
        rule.onNodeWithText("+ Add to shopping list").performScrollTo().performClick()
        waitForText("Shopping List")
        rule.onNodeWithText("Shopping List").assertIsDisplayed().performClick()
        waitForText("Liz's special biscuits")
        rule.onNodeWithText("Liz's special biscuits").assertExists()
        rule.onNodeWithText("Change shop ▾").performClick()
        rule.onNodeWithText("Lidl").performClick()
        waitForText("Shopping List")
        rule.onNodeWithText("Shopping List").assertIsDisplayed().performClick()
        waitForText("Milk")
        rule.onNodeWithText("Milk").assertExists()
        rule.onNodeWithText("Eggs").assertExists()
    }
}
