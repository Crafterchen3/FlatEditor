package com.deckerpw.flateditor

import com.deckerpw.flateditor.data.Project
import com.deckerpw.flateditor.data.activeProjects
import com.deckerpw.flateditor.gui.frames.StartFrame
import com.formdev.flatlaf.FlatLaf
import java.io.File
import javax.swing.JFileChooser
import javax.swing.UIManager
import kotlin.properties.Delegates

private var _simple = false;
val simple: Boolean get() = _simple

fun main(args: Array<String>) {
    //System.setProperty("sun.java2d.uiScale","1.4")
    FlatLaf.registerCustomDefaultsSource("com.deckerpw.flateditor")
    applyLookAndFeel()
    UIManager.put( "TitlePane.unifiedBackground", false);
    _simple = args.contains("simple")
    StartFrame()
    //Project(File("run/picasso"), "App")
    //UIManager.put("Tree.showDefaultIcon", false)

//    main()
}

fun applyLookAndFeel() {
    if (UIManager.getLookAndFeel().javaClass.getName() == theme) return

    try {
        UIManager.setLookAndFeel(theme)
        FlatLaf.updateUI()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}

fun closeApp(){
    activeProjects.forEach { it.closeProject(false) }
}