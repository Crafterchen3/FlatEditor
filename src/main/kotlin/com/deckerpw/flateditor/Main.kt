package com.deckerpw.flateditor

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme
import javax.swing.UIManager

fun main() {

    FlatLaf.registerCustomDefaultsSource("com.deckerpw.flateditor")
    FlatOneDarkIJTheme.setup()
    //UIManager.put( "TitlePane.unifiedBackground", false);
    MainFrame()
}