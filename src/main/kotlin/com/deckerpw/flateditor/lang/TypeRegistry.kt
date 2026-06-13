package com.deckerpw.flateditor.lang

import com.formdev.flatlaf.extras.FlatSVGIcon
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
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
                "java")
            put(SyntaxConstants.SYNTAX_STYLE_NONE, FlatSVGIcon("com/deckerpw/flateditor/icons/text.svg"),
                "txt")
            put(SyntaxConstants.SYNTAX_STYLE_PROPERTIES_FILE, FlatSVGIcon("com/deckerpw/flateditor/icons/properties.svg"),
                "props","properties")
            putDisabled(FlatSVGIcon("com/deckerpw/flateditor/icons/javaClass.svg"),
                "class")
            putDisabled(FlatSVGIcon("com/deckerpw/flateditor/icons/archive.svg"),
                "jar","zip","rar","7z","tar","xz")
        }

        private fun put(syntax: String, icon: Icon, vararg extensions: String){
            val type = FileType(syntax,icon)
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
        fun isEnabled(ext: String): Boolean = fileTypeMap[ext.lowercase()]?.enabled ?: false
    }

}

class FileType(val syntax: String?, val icon: Icon, val enabled: Boolean = true){

}