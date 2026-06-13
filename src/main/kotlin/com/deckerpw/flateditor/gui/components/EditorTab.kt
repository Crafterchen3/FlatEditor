package com.deckerpw.flateditor.gui.components

import com.deckerpw.flateditor.data.Project
import com.deckerpw.flateditor.lang.TypeRegistry
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import java.io.File
import javax.swing.JPanel

class EditorTab(val project: Project, val file: File): FlatEditorPane() {


    init {
        textArea.text = file.readText()
        textArea.syntaxEditingStyle = TypeRegistry.getSyntax(file.extension)
    }

    fun save(){
        file.writeText(textArea.text)
    }

}