package com.deckerpw.flateditor.gui.components

import com.deckerpw.flateditor.data.Project
import java.io.File
import javax.swing.JPanel

class EditorTab(val project: Project, val file: File): FlatEditorPane() {


    init {
        textArea.text = file.readText()

    }

}