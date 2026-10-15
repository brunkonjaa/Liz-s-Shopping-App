package ie.suric.lizshopping

import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Debug
import android.os.SystemClock
import android.util.Log
import android.util.Xml
import android.view.WindowInsets
import android.view.accessibility.AccessibilityNodeInfo
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Test-only, read-only diagnostics. Never serialize the full household database. */
internal class JourneyDiagnostics(private val activity: () -> MainActivity) {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    // Android 11+ restricts mkdir beneath Android/data until the framework
    // initializes the app-owned directory through this API.
    private val externalDirectory = context.getExternalFilesDir(null) ?: context.filesDir
    val directory = File(
        InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
            ?: externalDirectory.absolutePath,
        "journey-diagnostics"
    ).let { requested ->
        if (requested.mkdirs() || requested.isDirectory) requested
        else File(context.filesDir, "journey-diagnostics").apply {
            mkdirs()
            Log.e("ShoppingJourneyEvidence", "Requested diagnostic output unavailable: $requested; fallback=$this")
        }
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tracing = false
    private var events = 0
    var dispatch = "not attempted"

    fun start() {
        // Collect on Main so optimistic publication and rollback are recorded as
        // they occur. StateFlow can conflate; absence of an event is not proof
        // that a callback or persistence operation was never invoked.
        instrumentation.runOnMainSync {
            val vm = ViewModelProvider(activity())[ShoppingViewModel::class.java]
            scope.launch { vm.state.collect { event("viewModel ${summary(it)}") } }
            scope.launch { vm.message.collect { event("message=${it?.take(200)}") } }
        }
        // Optional exact method entry/exit evidence for LocalStore.save and
        // SQLite transaction completion, without adding hooks to production.
        // Enable separately: tracing affects scheduling and is not a baseline.
        if (InstrumentationRegistry.getArguments().getString("journeyMethodTrace") == "true") {
            attempt("method-trace-start") {
                Debug.startMethodTracing(File(directory, "first-add.trace").absolutePath, 16 * 1024 * 1024)
                tracing = true
            }
        }
    }

    @Synchronized fun event(value: String) {
        if (events++ >= 200) return
        val line = "${SystemClock.elapsedRealtime()} $value"
        runCatching { File(directory, "events.txt").appendText("$line\n") }
            .onFailure { Log.e("ShoppingJourneyEvidence", "Could not append diagnostic event", it) }
        Log.i("ShoppingJourneyEvidence", line)
    }

    fun capture(label: String, error: Throwable? = null) {
        event("capture=$label dispatch=$dispatch")
        // Independent attempts: a dead activity or accessibility failure must
        // not hide the original exception or prevent other evidence capture.
        attempt("$label-state") {
            val vm = ViewModelProvider(activity())[ShoppingViewModel::class.java]
            File(directory, "$label-state.txt").writeText(
                "elapsedRealtime=${SystemClock.elapsedRealtime()}\ndispatch=$dispatch\n" +
                    "viewModel=${summary(vm.state.value)}\nmessage=${vm.message.value?.take(200)}\n" +
                    "loadFailed=${vm.loadFailed.value}\n" + durableState()
            )
        }
        attempt("$label-window") {
            instrumentation.runOnMainSync {
                val current = activity()
                val decor = current.window.decorView
                val visible = Rect().also(decor::getWindowVisibleDisplayFrame)
                val ime = if (Build.VERSION.SDK_INT >= 30) decor.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) else null
                File(directory, "$label-window.txt").writeText(
                    "activity=${current.componentName.flattenToShortString()}\n" +
                        "lifecycle=${current.lifecycle.currentState}\nwindowFocus=${decor.hasWindowFocus()}\n" +
                        "decor=${decor.width}x${decor.height}\nvisibleFrame=$visible\nimeVisible=$ime\n" +
                        "density=${context.resources.displayMetrics.density}\n" +
                        "densityDpi=${context.resources.displayMetrics.densityDpi}\n" +
                        "focusedView=${current.currentFocus?.javaClass?.name}\n"
                )
            }
        }
        attempt("$label-hierarchy") {
            File(directory, "$label-hierarchy.xml").outputStream().use { output ->
                val xml = Xml.newSerializer().apply { setOutput(output, "UTF-8"); startDocument("UTF-8", true) }
                var remaining = 1000
                fun visit(node: AccessibilityNodeInfo, depth: Int) {
                    if (--remaining < 0 || depth > 50) return
                    xml.startTag(null, "node")
                    val bounds = Rect().also(node::getBoundsInScreen)
                    mapOf("class" to node.className, "text" to node.text, "description" to node.contentDescription,
                        "bounds" to bounds, "visible" to node.isVisibleToUser, "enabled" to node.isEnabled,
                        "clickable" to node.isClickable, "scrollable" to node.isScrollable).forEach { (key, value) ->
                        xml.attribute(null, key, value?.toString().orEmpty())
                    }
                    repeat(node.childCount) { index -> node.getChild(index)?.let { visit(it, depth + 1) } }
                    xml.endTag(null, "node")
                }
                instrumentation.uiAutomation.rootInActiveWindow?.let { visit(it, 0) }
                    ?: error("No active accessibility window")
                xml.endDocument()
            }
        }
        attempt("$label-screenshot") {
            val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
            try { File(directory, "$label.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
            finally { bitmap.recycle() }
        }
        attempt("$label-logcat") {
            // Limit to the app process: avoid collecting unrelated phone logs.
            val descriptor = instrumentation.uiAutomation.executeShellCommand("logcat -d -t 1500 --pid=${android.os.Process.myPid()} -v threadtime")
            android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { input ->
                File(directory, "$label-logcat.txt").outputStream().use { input.copyTo(it) }
            }
        }
        if (error != null) attempt("$label-stack") { File(directory, "$label-stack.txt").writeText(error.stackTraceToString()) }
    }

    private fun durableState(): String {
        val path = context.getDatabasePath("shopping.db")
        if (!path.exists()) return "persistence=database absent; save attempt not observed\n"
        // No LocalStore construction, load, migration, transaction or write.
        return SQLiteDatabase.openDatabase(path.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT payload FROM state WHERE id=1", null).use { cursor ->
                val saved = if (cursor.moveToFirst()) Backup.decode(cursor.getString(0)) else null
                "durable=${summary(saved)}\n" +
                    "persistence=committed readback only; pending/failed attempt needs events or optional method trace\n"
            }
        }
    }

    private fun summary(state: ShoppingState?): String {
        val selected = state?.selected
        val data = selected?.let { state.dataFor(it) }
        return "loaded=${state != null} selectedBuiltIn=${selected?.takeIf { it in ShopDefaults.names }} " +
            "itemCount=${data?.items?.size} bread=${data?.items?.filter { it.name.equals("Bread", true) }?.map { it.quantity }} " +
            "breadFavourite=${data?.favourites?.filter { it.name.equals("Bread", true) }?.map { it.quantity }}"
    }

    fun attempt(label: String, block: () -> Unit) {
        try { block() } catch (error: Throwable) {
            Log.e("ShoppingJourneyEvidence", "Diagnostic capture failed: $label", error)
            runCatching { File(directory, "capture-errors.txt").appendText("$label: ${error.stackTraceToString()}\n") }
        }
    }

    fun stopTrace() {
        if (tracing) { tracing = false; attempt("method-trace-stop") { Debug.stopMethodTracing() } }
    }

    fun close() { stopTrace(); scope.cancel() }
}
