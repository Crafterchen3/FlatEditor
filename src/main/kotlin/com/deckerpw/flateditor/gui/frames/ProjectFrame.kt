package com.deckerpw.flateditor.gui.frames

import com.deckerpw.flateditor.data.Project
import com.deckerpw.flateditor.gui.components.FlatEditorPane
import com.deckerpw.flateditor.gui.components.FlatFileExplorer
import com.deckerpw.flateditor.lang.compiler.JavaCompiler
import com.deckerpw.flateditor.poolApp
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatButton
import java.awt.BorderLayout
import javax.swing.*

class ProjectFrame(val project: Project) : JFrame("Flat Editor") {

    val tabbedPane = JTabbedPane()

    init {
        rootPane.putClientProperty("JRootPane.titleBarHeight", 40)
        jMenuBar = JMenuBar().apply {
            add(JMenu("File").apply {
                add(JMenuItem("Settings").apply {
                    addActionListener {
                        SettingsFrame()
                    }
                })
            })
            add(JMenu("Help").apply {
                add(JMenuItem("Check for Updates...").apply {
                    addActionListener {
                        poolApp.updater.checkForUpdates(this@ProjectFrame)
                    }
                })
            })
            add(Box.createGlue())
            add(FlatButton().apply {

                setIcon(FlatSVGIcon("com/deckerpw/flateditor/icons/execute.svg"))
                setButtonType(FlatButton.ButtonType.toolBarButton)
                setFocusable(false)
                addActionListener {
                    JavaCompiler.compileAndRunProject(project)
                }
            })
        }
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(800 * 2, 450 * 2)
        setLocationRelativeTo(null)

        layout = BorderLayout()
        add(
            JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                JSplitPane(
                    JSplitPane.HORIZONTAL_SPLIT,
                    FlatFileExplorer(project.dir, this),
                    JPanel(BorderLayout()).apply {
                        add(tabbedPane, BorderLayout.CENTER)
                    }
                ),
                JPanel(BorderLayout()).apply {
                    add(JToolBar(JToolBar.VERTICAL).apply {

                    }, BorderLayout.WEST)
                    add(JTabbedPane().apply {
                        addTab("Build", FlatEditorPane())
                        addTab("Run", FlatEditorPane())
                    }, BorderLayout.CENTER)
                }
            ), BorderLayout.CENTER)
//        add(textArea, BorderLayout.CENTER)
        isVisible = true
    }


}