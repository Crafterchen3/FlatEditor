package com.deckerpw.flateditor.gui.components

import com.deckerpw.flateditor.gui.components.FlatEditorPane.Companion.createEditorFont
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatButton
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.JToolBar
import javax.swing.UIManager

class LogViewer: JPanel(BorderLayout()) {

    val toolBar = JToolBar().apply {
        border = BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground"))
    }
    val textArea = JTextArea().apply {
        val font: Font = createEditorFont(2)
        setFont(font)
        setBackground(UIManager.getColor("FlatEditorPane.background"))
        isEditable = false
    }

    init {
        add(toolBar, BorderLayout.NORTH)
        add(JPanel(BorderLayout()).apply {
            add(JToolBar(JToolBar.VERTICAL).apply {
                border = BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Separator.foreground"))
                add(toolbarButton("com/deckerpw/flateditor/icons/toggleSoftWrap.svg"){

                })
                add(toolbarButton("com/deckerpw/flateditor/icons/scroll_up.svg"){

                })
                add(toolbarButton("com/deckerpw/flateditor/icons/scroll_down.svg"){

                })
            }, BorderLayout.WEST)
            add(JScrollPane(textArea), BorderLayout.CENTER)
        }, BorderLayout.CENTER)
    }

    fun clear(){
        textArea.text = ""
    }

    fun addLine(line: String){
        textArea.append(line+"\n")
    }

}