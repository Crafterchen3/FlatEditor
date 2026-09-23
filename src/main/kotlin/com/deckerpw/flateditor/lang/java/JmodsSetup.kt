package com.deckerpw.flateditor.lang.java

import com.deckerpw.flateditor.lang.java.buildpath.LibraryInfo
import java.io.File
import javax.swing.SwingUtilities

object JmodsSetup {

    private const val REQUIRED_JMOD = "java.base.jmod"

    /**
     * Checks whether jmods are present in the current Java runtime.
     * Looks in java.home/jmods and java.home/lib/jmods
     */
    fun isJmodsPresent(): Boolean {
        val javaHome = File(System.getProperty("java.home"))
        val candidates = listOf(
            File(javaHome, "jmods"),
            File(javaHome, "lib/jmods"),
            File(javaHome.parentFile ?: javaHome, "jmods")
        )
        for (dir in candidates) {
            if (isValidJmodsDir(dir)) return true
        }
        return false
    }

    private fun isValidJmodsDir(dir: File): Boolean {
        if (!dir.isDirectory) return false
        val base = File(dir, REQUIRED_JMOD)
        if (base.isFile && base.length() > 0) return true
        val jmods = dir.listFiles { f -> f.extension == "jmod" }
        return jmods != null && jmods.isNotEmpty()
    }

    /**
     * Returns the primary JDK LibraryInfo to use.
     * First tries the current java.home (if jmods are present), otherwise
     * falls back to the bundled jmods in resources (com/deckerpw/flateditor/jdk/jmods).
     * This method will trigger extraction of bundled jmods to a cache directory
     * with a progress dialog if necessary.
     */
    fun getMainJdkLibraryInfo(): LibraryInfo? {
        // Try system JDK first
        val systemInfo = try {
            LibraryInfo.getMainJreJarInfo()
        } catch (_: Exception) { null }

        // If systemInfo is a Jdk9LibraryInfo with files, it means jmods were found.
        // LibraryInfo.getMainJreJarInfo already falls back to bundled internally,
        // but we keep this as explicit check for isJmodsPresent to avoid double work.
        if (systemInfo != null) return systemInfo

        // System jmods not available -> use bundled
        return getBundledLibraryInfo()
    }

    /**
     * Returns LibraryInfo for the bundled jmods in resources.
     * Ensures bundled jmods are extracted to a cache directory (with progress dialog if needed)
     * and returns a Jdk9LibraryInfo pointing there.
     */
    fun getBundledLibraryInfo(): LibraryInfo? {
        return try {
            // Delegate to LibraryInfo's bundled handling (implemented in Java)
            // This will handle extraction with progress dialog
            LibraryInfo.getBundledJmodsInfo()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Ensures JDK library is ready. Returns true if system jmods are present
     * or bundled jmods have been successfully prepared.
     * Shows a progress dialog only if bundled extraction is required.
     */
    fun ensureJmods(): Boolean {
        if (isJmodsPresent()) {
            println("[JmodsSetup] System jmods found in java.home, no bundled extraction needed")
            return true
        }
        println("[JmodsSetup] System jmods NOT found, preparing bundled jmods from resources...")
        // This will show progress dialog if extraction is needed and we are on EDT / have UI
        val info = getBundledLibraryInfo()
        val ok = info != null
        if (ok) println("[JmodsSetup] Bundled jmods ready")
        else System.err.println("[JmodsSetup] Failed to prepare bundled jmods")
        return ok
    }

    /**
     * Ensures bundled jmods are prepared, showing progress dialog if this is the first run.
     * Safe to call from EDT. If called off EDT, extraction will run synchronously without dialog.
     */
    fun ensureBundledReadyWithDialog(): Boolean {
        // If not on EDT, just prepare silently
        if (!SwingUtilities.isEventDispatchThread()) {
            return getBundledLibraryInfo() != null
        }
        return ensureJmods()
    }
}
