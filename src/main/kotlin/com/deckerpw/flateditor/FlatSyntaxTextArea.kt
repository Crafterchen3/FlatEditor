package com.deckerpw.flateditor

import org.fife.ui.rsyntaxtextarea.TextEditorPane
import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rtextarea.RTextAreaUI
import java.awt.Color
import javax.swing.UIManager
import javax.swing.text.BadLocationException
import kotlin.math.max
import kotlin.math.min

class FlatSyntaxTextArea: TextEditorPane() {

    private var useColorOfColorTokens = false


    private val parsedColorsMap: MutableMap<String?, Color?> = mutableMapOf()

    init {
        // this is necessary because RTextAreaBase.init() always sets foreground to black
        //setForeground(UIManager.getColor("TextArea.foreground"))
    }

    override fun createRTextAreaUI(): RTextAreaUI {
        return FlatRSyntaxTextAreaUI(this)
    }
}