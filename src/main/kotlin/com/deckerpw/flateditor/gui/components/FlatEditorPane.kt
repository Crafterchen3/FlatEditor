/*
 * Copyright 2020 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.deckerpw.flateditor.gui.components

import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont
import com.formdev.flatlaf.util.FontUtils
import org.fife.ui.rsyntaxtextarea.ErrorStrip
import org.fife.ui.rsyntaxtextarea.Style
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.SyntaxScheme
import org.fife.ui.rsyntaxtextarea.TextEditorPane
import org.fife.ui.rtextarea.RTextArea
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.BorderLayout
import java.awt.Font
import java.beans.PropertyChangeEvent
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.UIManager

/**
 * A pane that supports editing FlatLaf themes.
 *
 * @author Karl Tauber
 */
open class FlatEditorPane

    : JPanel(BorderLayout()) {
    private val editorPanel: JPanel
    private val scrollPane: RTextScrollPane
    // create text area
    val textArea: FlatSyntaxTextArea = FlatSyntaxTextArea()
    private val errorStrip: ErrorStrip

    init {
        textArea.markOccurrences = true
        textArea.tabSize = 4
        textArea.isAutoIndentEnabled = true
        textArea.tabsEmulated = true

        //		textArea.addParser( new FlatThemeParser() );
//		textArea.setUseColorOfColorTokens( true );
        textArea.addPropertyChangeListener(
            TextEditorPane.DIRTY_PROPERTY
        ) { e: PropertyChangeEvent? ->
            firePropertyChange(
                DIRTY_PROPERTY, e!!.oldValue, e.newValue
            )
        }

        //		// autocomplete
//		CompletionProvider provider = new FlatCompletionProvider();
//		AutoCompletion ac = new AutoCompletion( provider );
//		ac.setAutoCompleteSingleChoices( false );
//		ac.setAutoActivationEnabled( true );
//		ac.setParameterAssistanceEnabled( true );
//		ac.setChoicesWindowSize( UIScale.scale( 300 ), UIScale.scale( 400 ) );
//		ac.setDescriptionWindowSize( UIScale.scale( 300 ), UIScale.scale( 400 ) );
//		ac.install( textArea );

        // create scroll pane
        scrollPane = RTextScrollPane(textArea)
        scrollPane.setBorder(BorderFactory.createEmptyBorder())
        scrollPane.setLineNumbersEnabled(true)

        // map Ctrl+PageUp/Down to a not-existing action to avoid that the scrollpane catches them
        val inputMap = scrollPane.getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
        inputMap.put(KeyStroke.getKeyStroke("ctrl PAGE_UP"), "__dummy__")
        inputMap.put(KeyStroke.getKeyStroke("ctrl PAGE_DOWN"), "__dummy__")

        // create error strip
        errorStrip = ErrorStrip(textArea)

        // create editor panel
        editorPanel = JPanel(BorderLayout())
        editorPanel.add(scrollPane)
        editorPanel.add(errorStrip, BorderLayout.LINE_END)
        add(editorPanel, BorderLayout.CENTER)

        updateTheme()
    }

    fun updateTheme() {
        val font: Font = createEditorFont(2)

        textArea.setFont(font)
        textArea.setBackground(UIManager.getColor("FlatEditorPane.background"))
        textArea.setCaretColor(UIManager.getColor("FlatEditorPane.caretColor"))
        textArea.setSelectionColor(UIManager.getColor("FlatEditorPane.selectionBackground"))
        textArea.setCurrentLineHighlightColor(UIManager.getColor("FlatEditorPane.currentLineHighlight"))
        textArea.setMarkAllHighlightColor(UIManager.getColor("FlatEditorPane.markAllHighlightColor"))
        textArea.setMarkOccurrencesColor(UIManager.getColor("FlatEditorPane.markOccurrencesColor"))
        textArea.setMatchedBracketBGColor(UIManager.getColor("FlatEditorPane.matchedBracketBackground"))
        textArea.setMatchedBracketBorderColor(UIManager.getColor("FlatEditorPane.matchedBracketBorderColor"))
        textArea.paintMatchedBracketPair = true
        textArea.animateBracketMatching = false
        textArea.syntaxScheme = FlatSyntaxScheme(font)

        // gutter
        val gutter = scrollPane.gutter
        gutter.setBackground(UIManager.getColor("FlatEditorPane.gutter.background"))
        gutter.setBorderColor(UIManager.getColor("FlatEditorPane.gutter.borderColor"))
        gutter.lineNumberColor = UIManager.getColor("FlatEditorPane.gutter.lineNumberColor")
        gutter.setLineNumberFont(font)


        // error strip
        errorStrip.caretMarkerColor = UIManager.getColor("FlatEditorPane.errorstrip.caretMarkerColor")
    }

    override fun requestFocusInWindow(): Boolean {
        return textArea.requestFocusInWindow()
    }

    companion object {
        const val DIRTY_PROPERTY: String = TextEditorPane.DIRTY_PROPERTY

        fun createEditorFont(sizeIncr: Int): Font {
            val size = UIManager.getFont("defaultFont").getSize() + sizeIncr
            var font = FontUtils.getCompositeFont(FlatJetBrainsMonoFont.FAMILY, Font.PLAIN, size)
            if (isFallbackFont(font)) {
                val defaultFont = RTextArea.getDefaultFont()
                font = defaultFont.deriveFont(size.toFloat())
            }
            return font
        }

        private fun isFallbackFont(font: Font): Boolean {
            return Font.DIALOG.equals(font.family, ignoreCase = true)
        }
    }

    class FlatSyntaxScheme
    internal constructor(baseFont: Font) : SyntaxScheme(false) {
        init {
            val styles = getStyles()
            for (i in styles.indices) styles[i] = Style(UIManager.getColor("FlatEditorPane.caretColor"))

            init("variable", VARIABLE, baseFont)
            init("number", LITERAL_NUMBER_FLOAT, baseFont)
            init("number", LITERAL_NUMBER_HEXADECIMAL, baseFont)
            init("number", LITERAL_NUMBER_DECIMAL_INT, baseFont)
            init("string", LITERAL_STRING_DOUBLE_QUOTE, baseFont)
            init("string", LITERAL_CHAR, baseFont)
            init("function", FUNCTION, baseFont)
            init("type", DATA_TYPE, baseFont)
            init("reservedWord", RESERVED_WORD, baseFont)
            init("literalBoolean", LITERAL_BOOLEAN, baseFont)
            init("operator", OPERATOR, baseFont)
            init("separator", SEPARATOR, baseFont)
            init("whitespace", WHITESPACE, baseFont)
            init("comment", COMMENT_EOL, baseFont)
        }

        fun init(key: String?, token: Int, baseFont: Font) {
            val prefix = "FlatEditorPane.style."
            val fg = UIManager.getColor(prefix + key)
            val bg = UIManager.getColor(prefix + key + ".background")
            val italic = UIManager.getBoolean(prefix + key + ".italic")
            var font = Style.DEFAULT_FONT
            if (italic) font = baseFont.deriveFont(Font.ITALIC)
            getStyles()[token] = Style(fg, bg, font)
        }
    }
}
