package com.deckerpw.flateditor.lang

import com.formdev.flatlaf.extras.FlatSVGIcon
import org.fife.ui.autocomplete.BasicCompletion
import org.fife.ui.autocomplete.CompletionProvider
import org.fife.ui.autocomplete.DefaultCompletionProvider
import org.fife.ui.autocomplete.FunctionCompletion
import org.fife.ui.autocomplete.ParameterizedCompletion
import org.fife.ui.autocomplete.ShorthandCompletion
import org.fife.ui.autocomplete.VariableCompletion
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import java.io.File
import javax.swing.Icon


class TypeRegistry private constructor(){

    companion object{

        private val fileTypeMap = mutableMapOf<String, FileType>()

        init {
            put(SyntaxConstants.SYNTAX_STYLE_MARKDOWN, FlatSVGIcon("com/deckerpw/flateditor/icons/markdownCell.svg"),
                "md","markdown")
            put(SyntaxConstants.SYNTAX_STYLE_JSON, FlatSVGIcon("com/deckerpw/flateditor/icons/json.svg"),
                "json")
            put(SyntaxConstants.SYNTAX_STYLE_JAVA, FlatSVGIcon("com/deckerpw/flateditor/icons/class.svg"),
                "java",)
            put(SyntaxConstants.SYNTAX_STYLE_NONE, FlatSVGIcon("com/deckerpw/flateditor/icons/text.svg"),
                "txt")
            put(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE, FlatSVGIcon("com/deckerpw/flateditor/icons/properties.svg"),
                "props","properties")
            put(SyntaxConstants.SYNTAX_STYLE_CSS, FlatSVGIcon("com/deckerpw/flateditor/icons/any_type.svg"),
                "css")
            put(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT, FlatSVGIcon("com/deckerpw/flateditor/icons/any_type.svg"),
                "js")
            putDisabled(FlatSVGIcon("com/deckerpw/flateditor/icons/javaClass.svg"),
                "class")
            putDisabled(FlatSVGIcon("com/deckerpw/flateditor/icons/archive.svg"),
                "jar","zip","rar","7z","tar","xz")
        }

        private fun put(syntax: String, icon: Icon, vararg extensions: String, providerCode: DefaultCompletionProvider.() -> Unit = {}){
            val type = FileType(syntax,icon,true,DefaultCompletionProvider().apply { providerCode() })
            extensions.forEach {
                fileTypeMap[it] = type
            }
        }

        private fun put(syntax: String, provider: CompletionProvider, icon: Icon, vararg extensions: String){
            val type = FileType(syntax,icon,true,provider)
            extensions.forEach {
                fileTypeMap[it] = type
            }
        }

        private fun putDisabled(icon: Icon, vararg extensions: String){
            val type = FileType(null,icon,false)
            extensions.forEach {
                fileTypeMap[it] = type
            }
        }

        fun getSyntax(ext: String): String = fileTypeMap[ext.lowercase()]?.syntax ?: SyntaxConstants.SYNTAX_STYLE_NONE
        fun getIcon(ext: String): Icon = fileTypeMap[ext.lowercase()]?.icon ?: FlatSVGIcon("com/deckerpw/flateditor/icons/any_type.svg")
        fun getCompletionProvider(ext: String): CompletionProvider? = fileTypeMap[ext.lowercase()]?.completionProvider
        fun isEnabled(ext: String): Boolean = fileTypeMap[ext.lowercase()]?.enabled ?: false
    }

}

class FileType(val syntax: String?, val icon: Icon, val enabled: Boolean = true, var completionProvider: CompletionProvider? = null){

}