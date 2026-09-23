package com.deckerpw.flateditor.gui.components

import com.deckerpw.flateditor.data.Project
import com.deckerpw.flateditor.jdk.JmodsSetup
import com.deckerpw.flateditor.lang.TypeRegistry
import com.deckerpw.flateditor.lang.java.JavaLanguageSupport
import java.io.File

class EditorTab(val project: Project, val file: File) : FlatEditorPane() {

    companion object {
        val tabs = mutableListOf<EditorTab>()

        fun updateThemeForAll() {
            tabs.forEach { it.updateTheme() }
        }
    }

    init {
        tabs.add(this)
        textArea.text = file.readText()
        textArea.syntaxEditingStyle = TypeRegistry.getSyntax(file.extension)
//        val provider = TypeRegistry.getCompletionProvider(file.extension)
//
//        if (provider != null)
//            AutoCompletion(provider).apply {
//                autoCompleteSingleChoices = false
//                isAutoActivationEnabled = true
//                isParameterAssistanceEnabled = true
//                setChoicesWindowSize(UIScale.scale(300), UIScale.scale(400))
//                setDescriptionWindowSize(UIScale.scale(300), UIScale.scale(400))
//            }.install(textArea)

//        LanguageSupportFactory.get().apply {
//            println((getSupportFor(SyntaxConstants.SYNTAX_STYLE_JAVA) as JavaLanguageSupport).jarManager.addCurrentJreClassFileSource())
//        }.register(textArea)

        if (file.extension == "java") {
            JavaLanguageSupport().apply {
                // Try system jmods first, fallback to bundled resources
                // LibraryInfo.getMainJreJarInfo() already falls back to bundled, but we prefer
                // explicit handling via JmodsSetup to ensure progress dialog is shown correctly
                val jdkInfo = JmodsSetup.getMainJdkLibraryInfo()
                if (jdkInfo != null) {
                    try {
                        jarManager.addClassFileSource(jdkInfo)
                    } catch (e: Exception) {
                        e.printStackTrace(); jarManager.addCurrentJreClassFileSource()
                    }
                } else {
                    jarManager.addCurrentJreClassFileSource()
                }
                install(textArea)
            }
        }

    }

    fun save() {
        file.writeText(textArea.text)
    }

    fun dispose() {
        save()
        tabs.remove(this)
    }

}