package com.deckerpw.flateditor.gui.frames

import com.formdev.flatlaf.extras.FlatSVGIcon
import java.awt.BorderLayout
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class StartFrame: JFrame("Flat Editor") {

    init {
        iconImage = FlatSVGIcon("com/deckerpw/flateditor/icons/logo.svg").image
        rootPane.putClientProperty("JRootPane.titleBarHeight", 40)
        defaultCloseOperation = DISPOSE_ON_CLOSE
        setSize(400,400)
        setLocationRelativeTo(null)
        layout = BorderLayout()

        add(JPanel(BorderLayout(10,10)).apply {
            border = EmptyBorder(10,10,10,10)
            add(JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
                add(JButton("New Project").apply {
                    this@StartFrame.rootPane.defaultButton = this
                })
                add(JButton("Open Folder"))
            }, BorderLayout.NORTH)
            add(JList(arrayOf("Picasso","Hello World", "Multi-Test")).apply {

            })
        }, BorderLayout.CENTER)

        isVisible = true
    }

}