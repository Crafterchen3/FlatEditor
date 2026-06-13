package com.deckerpw.flateditor

import com.deckerpw.flateditor.data.Project
import com.deckerpw.flateditor.data.activeProjects
import com.deckerpw.flateditor.gui.components.EditorTab
import com.deckerpw.flateditor.gui.frames.ProjectFrame
import com.deckerpw.flateditor.gui.frames.StartFrame
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import java.io.File
import javax.swing.UIManager

fun main() {
    //System.setProperty("sun.java2d.uiScale","1.4")
    FlatLaf.registerCustomDefaultsSource("com.deckerpw.flateditor")
    applyLookAndFeel()
    UIManager.put( "TitlePane.unifiedBackground", false);
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