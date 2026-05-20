/*
 * Copyright 2021 FormDev Software GmbH
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

import org.fife.ui.rsyntaxtextarea.RSyntaxTextAreaUI
import org.fife.ui.rtextarea.ConfigurableCaret
import java.awt.Graphics
import java.awt.Rectangle
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.text.BadLocationException
import javax.swing.text.Caret
import javax.swing.text.DefaultEditorKit
import javax.swing.text.Utilities
import kotlin.math.max
import kotlin.math.min

/**
 * @author Karl Tauber
 */
internal class FlatRSyntaxTextAreaUI
    (rSyntaxTextArea: JComponent) : RSyntaxTextAreaUI(rSyntaxTextArea) {
    override fun createCaret(): Caret {
        val caret: Caret = FlatConfigurableCaret()
        caret.setBlinkRate(500)
        return caret
    }

    override fun paintCurrentLineHighlight(g: Graphics, visibleRect: Rectangle) {
        if (!textArea.getHighlightCurrentLine()) return

        // paint current line highlight always in the line where the caret is
        try {
            val dot = textArea.getCaret().getDot()
            val dotRect = textArea.modelToView(dot)
            val height = textArea.getLineHeight()

            g.setColor(textArea.getCurrentLineHighlightColor())
            g.fillRect(visibleRect.x, dotRect.y, visibleRect.width, height)
        } catch (ex: BadLocationException) {
            super.paintCurrentLineHighlight(g, visibleRect)
        }
    }

    //---- class FlatConfigurableCaret ----------------------------------------
    private class FlatConfigurableCaret

        : ConfigurableCaret() {
        private var isWordSelection = false
        private var isLineSelection = false
        private var dragSelectionStart = 0
        private var dragSelectionEnd = 0

        override fun mousePressed(e: MouseEvent) {
            super.mousePressed(e)

            val c = getComponent()

            // left double-click starts word selection
            isWordSelection = e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e) && !e.isConsumed()

            // left triple-click starts line selection
            isLineSelection =
                e.getClickCount() == 3 && SwingUtilities.isLeftMouseButton(e) && (!e.isConsumed() || c.getDragEnabled())

            // select line
            // (this is also done in DefaultCaret.mouseClicked(), but this event is
            // sent when the mouse is released, which is too late for triple-click-and-drag)
            if (isLineSelection) {
                val actionMap = c.getActionMap()
                val selectLineAction = if (actionMap != null)
                    actionMap.get(DefaultEditorKit.selectLineAction)
                else
                    null
                if (selectLineAction != null) {
                    selectLineAction.actionPerformed(
                        ActionEvent(
                            c,
                            ActionEvent.ACTION_PERFORMED, null, e.getWhen(), e.getModifiers()
                        )
                    )
                }
            }

            // remember selection where word/line selection starts to keep it always selected while dragging
            if (isWordSelection || isLineSelection) {
                val mark = getMark()
                val dot = getDot()
                dragSelectionStart = min(dot, mark)
                dragSelectionEnd = max(dot, mark)
            }
        }

        override fun mouseReleased(e: MouseEvent?) {
            isWordSelection = false
            isLineSelection = false
            super.mouseReleased(e)
        }

        override fun mouseDragged(e: MouseEvent) {
            if ((isWordSelection || isLineSelection) && !e.isConsumed() && SwingUtilities.isLeftMouseButton(e)) {
                // fix Swing's double/triple-click-and-drag behavior so that dragging after
                // a double/triple-click extends selection by whole words/lines
                val c = getComponent()
                val pos = c.viewToModel(e.getPoint())
                if (pos < 0) return

                try {
                    if (pos > dragSelectionEnd) select(
                        dragSelectionStart,
                        if (isWordSelection) Utilities.getWordEnd(c, pos) else Utilities.getRowEnd(c, pos)
                    )
                    else if (pos < dragSelectionStart) select(
                        dragSelectionEnd,
                        if (isWordSelection) Utilities.getWordStart(c, pos) else Utilities.getRowStart(c, pos)
                    )
                    else select(dragSelectionStart, dragSelectionEnd)
                } catch (ex: BadLocationException) {
                    UIManager.getLookAndFeel().provideErrorFeedback(c)
                }
            } else super.mouseDragged(e)
        }

        fun select(mark: Int, dot: Int) {
            if (mark != getMark()) setDot(mark)
            if (dot != getDot()) moveDot(dot)
        }
    }
}
