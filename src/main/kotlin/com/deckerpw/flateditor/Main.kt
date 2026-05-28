package com.deckerpw.flateditor

import com.deckerpw.flateditor.data.Project
import com.deckerpw.flateditor.gui.components.EditorTab
import com.deckerpw.flateditor.gui.frames.ProjectFrame
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import java.io.File
import javax.swing.UIManager

fun main() {

    FlatLaf.registerCustomDefaultsSource("com.deckerpw.flateditor")
    applyLookAndFeel()
    UIManager.put( "TitlePane.unifiedBackground", false);

    //UIManager.put("Tree.showDefaultIcon", false)

    ProjectFrame(Project(File("run/picasso"),"App"))
//    main()
}

private fun applyLookAndFeel() {
    if (UIManager.getLookAndFeel().javaClass.getName() == theme) return

    try {
        UIManager.setLookAndFeel(theme)
        FlatLaf.updateUI()
    } catch (ex: Exception) {
        ex.printStackTrace()
    }
}