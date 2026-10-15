package ie.suric.lizshopping

import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingJourneyTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val diagnostics by lazy { JourneyDiagnostics { rule.activity } }
    private var collectingDiagnostics = false

    private companion object {
        const val STATE_TIMEOUT_MS = 30_000L
    }

    private fun waitForText(text: String) {
        try {
            rule.waitUntil(10_000) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
        } catch (error: ComposeTimeoutException) {
            captureDiagnostics("text-timeout", error)
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
        diagnostics.event(label)
    }

    private fun waitForBreadQuantity(expected: Int) {
        try {
            rule.waitUntil(STATE_TIMEOUT_MS) {
                val current = ViewModelProvider(rule.activity)[ShoppingViewModel::class.java].state.value
                val selected = current?.selected ?: return@waitUntil false
                current.dataFor(selected).items.any { it.name.equals("Bread", true) && it.quantity == expected }
            }
            diagnostics.stopTrace()
        } catch (error: Throwable) {
            diagnostics.stopTrace()
            captureDiagnostics("bread-$expected-timeout", error)
            throw error
        }
    }

    private fun captureDiagnostics(label: String, error: Throwable? = null) {
        // Even output-directory initialization must not replace the test failure.
        try {
            diagnostics.capture(label, error)
            diagnostics.attempt("$label-semantics") {
                fun tree(unmerged: Boolean): String {
                    val roots = rule.onAllNodes(isRoot(), useUnmergedTree = unmerged)
                    return roots.fetchSemanticsNodes().indices.joinToString("\n") { roots[it].printToString() }
                }
                java.io.File(diagnostics.directory, "$label-semantics.txt").writeText(
                    "MERGED\n" + tree(false) + "\nUNMERGED\n" + tree(true)
                )
            }
            diagnostics.attempt("$label-button") {
                val matches = rule.onAllNodesWithContentDescription("Add Bread to shopping list").fetchSemanticsNodes()
                val report = buildString {
                    appendLine("matchingAddButtons=${matches.size}")
                    matches.forEach { node ->
                        appendLine("id=${node.id} rootBounds=${node.boundsInRoot} windowBounds=${node.boundsInWindow}")
                        appendLine("centerInRoot=${node.boundsInRoot.center} centerInWindow=${node.boundsInWindow.center}")
                        appendLine("enabled=${!node.config.contains(SemanticsProperties.Disabled)} hasClickAction=${node.config.getOrNull(SemanticsActions.OnClick)?.action != null}")
                        var ancestor = node.parent
                        while (ancestor != null) {
                            val range = ancestor.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)
                            if (range != null) appendLine("scrollAncestor=${ancestor.id} bounds=${ancestor.boundsInRoot} value=${range.value()} max=${range.maxValue()} reverse=${range.reverseScrolling}")
                            ancestor = ancestor.parent
                        }
                    }
                    if (matches.size == 1) appendLine("displayed=${rule.onNodeWithContentDescription("Add Bread to shopping list").isDisplayed()}")
                    appendLine("A returned performClick proves dispatch completed, not callback delivery. Bounds/visibility do not prove absence of overlays.")
                }
                java.io.File(diagnostics.directory, "$label-button.txt").writeText(report)
            }
        } catch (captureError: Throwable) {
            Log.e("ShoppingJourneyEvidence", "Diagnostic setup failed for $label", captureError)
        }
    }

    private fun addBreadFromCatalogue(label: String) {
        if (!collectingDiagnostics) { diagnostics.start(); collectingDiagnostics = true }
        captureDiagnostics("$label-before")
        // CI's shorter dp viewport composes Bread while its Add button is
        // fully clipped below the LazyColumn. Existence/enabled is insufficient.
        rule.onNodeWithContentDescription("Add Bread to shopping list")
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
        captureDiagnostics("$label-ready")
        if (label == "first-add") diagnostics.startTrace()
        diagnostics.dispatch = "$label started"
        try {
            rule.onNodeWithContentDescription("Add Bread to shopping list").performClick()
            diagnostics.dispatch = "$label returned"
            // Keep the optional trace focused on injection/state update;
            // screenshot and semantics work otherwise overwhelms its buffer.
            if (!diagnostics.isTracing) captureDiagnostics("$label-after-dispatch")
        } catch (error: Throwable) {
            diagnostics.stopTrace()
            diagnostics.dispatch = "$label threw ${error.javaClass.simpleName}"
            captureDiagnostics("$label-click-failure", error)
            throw error
        }
    }

    private fun dismissKeyboard() {
        rule.runOnUiThread {
            val focused = rule.activity.currentFocus
            val token = focused?.windowToken ?: rule.activity.window.decorView.windowToken
            focused?.clearFocus()
            rule.activity.getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(token, 0)
        }
        rule.waitForIdle()
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
        if (collectingDiagnostics) diagnostics.close()
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
        dismissKeyboard()
        waitForText("Bread")
        addBreadFromCatalogue("first-add")
        waitForBreadQuantity(1)
        captureDiagnostics("first-add-observed")
        diagnostics.stopTrace()
        logShoppingState("afterFirstBreadAdd")
        addBreadFromCatalogue("second-add")
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
        dismissKeyboard()
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
        captureDiagnostics("journey-final")
    }
}
