package com.deckerpw.flateditor.gui.components

import com.deckerpw.flateditor.gui.components.FlatEditorPane.Companion.createEditorFont
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.*

class LogViewer : JPanel(BorderLayout()) {

    val toolBar = JToolBar().apply {
        border = BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground"))
    }
    val textArea = JTextArea().apply {
        val font: Font = createEditorFont(2)
        setFont(font)
        setBackground(UIManager.getColor("FlatEditorPane.background"))
        isEditable = false
    }

    var autoscroll = true

    val scrollPane: JScrollPane = JScrollPane(textArea).apply {
        var verticalScrollBarMaximumValue = verticalScrollBar.maximum
        verticalScrollBar.addAdjustmentListener { e ->
            if (autoscroll) {
                if ((verticalScrollBarMaximumValue - e.adjustable.maximum) == 0) return@addAdjustmentListener
                e.adjustable.value = e.adjustable.maximum
                verticalScrollBarMaximumValue = scrollPane.verticalScrollBar.maximum
            }
        }
    }

    init {
        add(toolBar, BorderLayout.NORTH)
        add(JPanel(BorderLayout()).apply {

            add(JToolBar(JToolBar.VERTICAL).apply {
                border = BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Separator.foreground"))
                add(toolbarToggleButton("com/deckerpw/flateditor/icons/toggleSoftWrap.svg").apply {
                    isSelected = textArea.lineWrap
                    addActionListener {
                        textArea.lineWrap = !textArea.lineWrap
                    }
                })
                add(toolbarButton("com/deckerpw/flateditor/icons/top.svg") {
                    scrollPane.verticalScrollBar.value = 0
                })
                add(toolbarToggleButton("com/deckerpw/flateditor/icons/scroll_down.svg").apply {
                    isSelected = autoscroll
                    addActionListener {
                        autoscroll = !autoscroll
                    }
                })
            }, BorderLayout.WEST)
            add(scrollPane, BorderLayout.CENTER)
        }, BorderLayout.CENTER)
    }

    fun clear() {
        textArea.text = ""
    }

    fun addLine(line: String) {
        textArea.append(line + "\n")
    }

}