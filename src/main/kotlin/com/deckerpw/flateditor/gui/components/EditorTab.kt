package com.deckerpw.flateditor.gui.components

import com.deckerpw.flateditor.data.Project
import com.deckerpw.flateditor.lang.TypeRegistry
import com.deckerpw.flateditor.lang.java.JavaSupportConfig
import com.formdev.flatlaf.util.UIScale
import org.fife.ui.autocomplete.AutoCompletion
import java.io.File

class EditorTab(val project: Project, val file: File) : FlatEditorPane() {


    init {
        textArea.text = file.readText()
        textArea.syntaxEditingStyle = TypeRegistry.getSyntax(file.extension)
        val provider = TypeRegistry.getCompletionProvider(file.extension)

        if (provider != null)
            AutoCompletion(provider).apply {
                autoCompleteSingleChoices = false
                isAutoActivationEnabled = true
                isParameterAssistanceEnabled = true
                setChoicesWindowSize(UIScale.scale(300), UIScale.scale(400))
                setDescriptionWindowSize(UIScale.scale(300), UIScale.scale(400))
            }.install(textArea)
    }

    fun save() {
        file.writeText(textArea.text)
    }

}