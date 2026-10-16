package ie.suric.lizshopping

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.getOrNull
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun openTab(label: String) {
        // Current navigation exposes Tab semantics. Do not choose the first
        // text match, which could belong to a drawer item or another heading.
        rule.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsDisplayed()
            .assertIsEnabled()
            .performClick()
    }

    private fun waitForTransientMessageToClear() {
        // Captured Material snackbar: Polite LiveRegion + Dismiss action. On
        // CI it covers the centre of Add even when that button is displayed.
        val snackbar = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite) and
            SemanticsMatcher.keyIsDefined(SemanticsActions.Dismiss)
        try {
            rule.waitUntil(STATE_TIMEOUT_MS) { rule.onAllNodes(snackbar).fetchSemanticsNodes().isEmpty() }
        } catch (error: Throwable) {
            captureDiagnostics("transient-message-timeout", error)
            throw error
        }
    }

    private fun visibleControl(description: String): SemanticsNodeInteraction {
        waitForTransientMessageToClear()
        return rule.onNodeWithContentDescription(description)
            .performScrollTo()
            .assertIsDisplayed()
            .assertIsEnabled()
    }

    private fun startAtLidl() {
        rule.waitUntil(STATE_TIMEOUT_MS) {
            val vm = ViewModelProvider(rule.activity)[ShoppingViewModel::class.java]
            vm.state.value != null || vm.loadFailed.value
        }
        val current = requireNotNull(ViewModelProvider(rule.activity)[ShoppingViewModel::class.java].state.value) {
            "Saved state did not load; the journey must not replace it."
        }
        // Refuse household data instead of silently assuming a fresh install
        // or clearing it to make quantity-1 assertions pass.
        assertTrue("Use an empty disposable test installation; this journey never clears saved data.",
            current.custom.isEmpty() && current.stores.values.all {
                it.items.isEmpty() && it.favourites.isEmpty() && it.previous.isEmpty()
            })
        closeDrawerIfOpen()
        if (current.selected == null) {
            rule.onNode(hasText("Lidl") and hasText("0 items") and hasClickAction())
                .performScrollTo().assertIsDisplayed().performClick()
        } else if (current.selected != "Lidl") {
            rule.onNodeWithText("Change shop ▾").assertIsDisplayed().performClick()
            rule.onNodeWithContentDescription("Select Lidl shop").assertIsDisplayed().performClick()
        }
        waitForText("Shopping List")
        openTab("Shopping List")
        waitForText("Your shopping list for Lidl")
    }

    private fun logShoppingState(label: String) {
        rule.waitForIdle()
        diagnostics.event(label)
    }

    private fun waitForBreadQuantity(expected: Int) {
        try {
            rule.waitUntil(STATE_TIMEOUT_MS) {
                val current = ViewModelProvider(rule.activity)[ShoppingViewModel::class.java].state.value
                if (current?.selected != "Lidl") return@waitUntil false
                current.dataFor("Lidl").items.any { it.name.equals("Bread", true) && it.quantity == expected }
            }
            diagnostics.stopTrace()
        } catch (error: Throwable) {
            diagnostics.stopTrace()
            captureDiagnostics("bread-$expected-timeout", error)
            throw error
        }
    }

    private fun waitForPersistedShop(expected: String) {
        try {
            rule.waitUntil(STATE_TIMEOUT_MS) {
                // Independent committed readback, without constructing LocalStore
                // or relying on the ViewModel retained by activity recreation.
                val path = rule.activity.getDatabasePath("shopping.db")
                val saved = SQLiteDatabase.openDatabase(path.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                    db.rawQuery("SELECT payload FROM state WHERE id=1", null).use { cursor ->
                        if (cursor.moveToFirst()) Backup.decode(cursor.getString(0)) else null
                    }
                } ?: return@waitUntil false
                val lidl = saved.dataFor("Lidl")
                saved.selected == expected &&
                    lidl.items.singleOrNull { it.name.equals("Bread", true) }?.quantity == 7 &&
                    lidl.favourites.singleOrNull { it.name.equals("Bread", true) }?.quantity == 1 &&
                    saved.dataFor("Aldi").items.isEmpty()
            }
        } catch (error: Throwable) {
            captureDiagnostics("persisted-$expected-timeout", error)
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

    private fun addBreadToList(label: String) {
        if (!collectingDiagnostics) { diagnostics.start(); collectingDiagnostics = true }
        captureDiagnostics("$label-before")
        diagnostics.dispatch = "$label preparing"
        try {
            // Catalogue and favourite cards share this current primary action.
            visibleControl("Add Bread to shopping list")
            captureDiagnostics("$label-ready")
            if (label == "first-add") diagnostics.startTrace()
            diagnostics.dispatch = "$label started"
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
            visibleControl(saveDescription).performClick()
        } else {
            visibleControl("Remove Bread from favourites")
        }
    }

    @After
    fun captureFinalState() {
        if (collectingDiagnostics) diagnostics.close()
    }

    @Test
    fun directAddQuantityCompletionFavouritesIsolationAndRelaunch() {
        startAtLidl()

        // A catalogue product is added from its explicit primary control;
        // configuration remains available as a separate secondary action.
        openTab("All Items")
        waitForText("Search for a product")
        rule.onNodeWithText("Search for a product").performTextReplacement("Bread")
        dismissKeyboard()
        waitForText("Bread")
        addBreadToList("first-add")
        waitForBreadQuantity(1)
        captureDiagnostics("first-add-observed")
        diagnostics.stopTrace()
        logShoppingState("afterFirstBreadAdd")
        addBreadToList("second-add")
        waitForBreadQuantity(2)
        logShoppingState("afterSecondBreadAdd")

        openTab("Shopping List")
        waitForText("Bread")
        rule.onNodeWithText("×2").assertExists()

        // Quantity changes stay on the list and tolerate rapid repeated taps.
        val increase = visibleControl("Increase Bread quantity")
        repeat(5) { increase.performClick() }
        visibleControl("Decrease Bread quantity").performClick()
        waitForBreadQuantity(6)
        rule.onNodeWithText("×6").assertExists()
        visibleControl("Mark Bread bought").performClick()
        rule.onNodeWithContentDescription("Mark Bread still to buy").assertExists()
        visibleControl("Mark Bread still to buy").performClick()
        rule.onNodeWithContentDescription("Mark Bread bought").assertExists()

        // Favourites reuse the same direct-add interaction.
        openTab("All Items")
        waitForText("Search for a product")
        rule.onNodeWithText("Search for a product").performTextReplacement("Bread")
        dismissKeyboard()
        waitForText("Bread")
        ensureBreadFavourite()
        openTab("Favourites")
        waitForText("Bread")
        addBreadToList("favourite-add")
        waitForBreadQuantity(7)
        rule.runOnIdle {
            val data = requireNotNull(ViewModelProvider(rule.activity)[ShoppingViewModel::class.java].state.value).dataFor("Lidl")
            assertEquals("List edits must not change the favourite default", 1,
                data.favourites.single { it.name.equals("Bread", true) }.quantity)
        }
        openTab("Shopping List")
        waitForText("Your shopping list for Lidl")
        rule.onNodeWithText("×7").assertExists()

        // Switching shops keeps the lists independent.
        rule.onNodeWithText("Change shop ▾").performClick()
        rule.onNodeWithContentDescription("Select Aldi shop").performClick()
        waitForText("Your shopping list for Aldi")
        rule.onNodeWithText("Bread").assertDoesNotExist()
        // Commit Aldi first so a later Lidl read cannot match an older Lidl
        // snapshot while the final selection write is pending or has failed.
        waitForPersistedShop("Aldi")
        rule.onNodeWithText("Change shop ▾").performClick()
        rule.onNodeWithContentDescription("Select Lidl shop").performClick()
        waitForText("Your shopping list for Lidl")
        rule.onNodeWithText("Bread").assertExists()
        waitForPersistedShop("Lidl")

        // The current shop reopens directly into its list after recreation.
        rule.activityRule.scenario.recreate()
        waitForText("Your shopping list for Lidl")
        waitForBreadQuantity(7)
        rule.onNodeWithText("Bread").assertExists()
        rule.onNodeWithText("×7").assertExists()
        waitForPersistedShop("Lidl")
        captureDiagnostics("journey-final")
    }
}
