package com.deckerpw.flateditor

import com.deckerpw.flateditor.gui.MainFrame
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme

fun main() {

    FlatLaf.registerCustomDefaultsSource("com.deckerpw.flateditor")
    FlatOneDarkIJTheme.setup()
    //UIManager.put( "TitlePane.unifiedBackground", false);
    MainFrame()
}