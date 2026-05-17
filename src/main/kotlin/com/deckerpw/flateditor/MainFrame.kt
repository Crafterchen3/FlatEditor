package com.deckerpw.flateditor

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Insets
import javax.swing.*


class MainFrame: JFrame("Flat Editor") {

    val textArea = FlatEditorPane()

    init {
        jMenuBar = JMenuBar().apply {
            add(JMenu("File").apply {
                add(JMenuItem("New...").apply {
                    addActionListener {
                    }
                })
            })
            add(JMenu("View").apply {
                val group = ButtonGroup()
                add(JRadioButtonMenuItem("Light Mode").apply {
                    addActionListener {
                        applyLookAndFeel(FlatLightLaf::class.qualifiedName)
                    }
                    group.add(this)
                })
                add(JRadioButtonMenuItem("Dark Mode").apply {
                    isSelected = true
                    addActionListener {
                        applyLookAndFeel(FlatOneDarkIJTheme::class.qualifiedName)
                    }
                    group.add(this)
                })
            })
            add(JMenu("Help").apply {
                add(JMenuItem("Check for Updates...").apply {
                    addActionListener {
                        poolApp.updater.checkForUpdates(this@MainFrame)
                    }
                })
            })
        }
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(800, 600)
        setLocationRelativeTo(null)

        layout = BorderLayout()
        val toolBar = JToolBar().apply {
            margin = Insets(4, 4, 4, 4)
        }
        add(toolBar, BorderLayout.NORTH)
        val minimumSize = Dimension(100, 50)
        val panel = JPanel()
        textArea.minimumSize = minimumSize
        panel.minimumSize = minimumSize
//        add(JSplitPane(
//            JSplitPane.HORIZONTAL_SPLIT,
//            textArea,
//            panel
//
//        ), BorderLayout.CENTER)
        add(textArea, BorderLayout.CENTER)
        isVisible = true
    }


    private fun applyLookAndFeel(lafClassName: String?) {
        if (UIManager.getLookAndFeel().javaClass.getName() == lafClassName) return

        try {
            UIManager.setLookAndFeel(lafClassName)
            FlatLaf.updateUI()
            textArea.updateTheme()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

}