package com.deckerpw.flateditor.gui.frames

import com.deckerpw.flateditor.Layout
import com.deckerpw.flateditor.closeApp
import com.deckerpw.flateditor.data.Project
import com.deckerpw.flateditor.gui.components.EditorTab
import com.deckerpw.flateditor.gui.components.FlatFileExplorer
import com.deckerpw.flateditor.gui.components.toolbarButton
import com.deckerpw.flateditor.lang.TypeRegistry
import com.deckerpw.flateditor.poolApp
import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatButton
import java.awt.BorderLayout
import java.awt.Font
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.util.function.IntConsumer
import javax.swing.*


class ProjectFrame(val project: Project) : JFrame("${project.dir.name} - Flat Editor") {

    val tabbedPane = JTabbedPane().apply {
        putClientProperty("JTabbedPane.tabCloseCallback", IntConsumer { tabIndex: Int ->
            (this.getComponent(tabIndex) as? EditorTab)?.save()
            this.remove(tabIndex)
        })
        putClientProperty("JTabbedPane.tabClosable", true)
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent?) {
                super.mouseClicked(e)
                if (e != null) {
                    val index = indexAtLocation(e.x,e.y)
                    if (index != -1 && e.button == MouseEvent.BUTTON2)
                        removeTabAt(index)
                }

            }
        })
    }
    var normalWidth = Layout.width
    var normalHeight = Layout.height

    val innerSplitPane = JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        FlatFileExplorer(project.dir, project),
        JPanel(BorderLayout()).apply {
            add(tabbedPane, BorderLayout.CENTER)
        }
    ).apply {
        dividerLocation = Layout.innerDivider
    }


    val outerSplitPane: JSplitPane = JSplitPane(
        JSplitPane.VERTICAL_SPLIT,
        innerSplitPane,
        JPanel(BorderLayout()).apply {
            add(JToolBar(JToolBar.VERTICAL).apply {

            }, BorderLayout.WEST)


            add(project.terminalTabbedPane.apply {
                putClientProperty(FlatClientProperties.TABBED_PANE_TRAILING_COMPONENT, JToolBar().apply {
                    add(Box.createGlue())
                    add(FlatButton().apply {

                        setIcon(FlatSVGIcon("com/deckerpw/flateditor/icons/hideToolWindow.svg"))
                        setButtonType(FlatButton.ButtonType.toolBarButton)
                        setFocusable(false)
                        addActionListener {
                            outerSplitPane.dividerLocation = outerSplitPane.maximumDividerLocation
                        }
                    })
                })
                putClientProperty(FlatClientProperties.TABBED_PANE_LEADING_COMPONENT, JLabel("  Logs:  ").apply {
                    font = font.deriveFont(font.getStyle() or Font.BOLD)
                })
            }, BorderLayout.CENTER)
        }
    ).apply {
        dividerLocation = Layout.outerDivider
    }

    init {
        iconImage = FlatSVGIcon("com/deckerpw/flateditor/icons/logo.svg").image
        rootPane.putClientProperty("JRootPane.titleBarHeight", 40)
        jMenuBar = JMenuBar().apply {
            add(JMenu("File").apply {
                add(JMenuItem("New Project").apply {
                    addActionListener {
                    }
                })
                add(JMenuItem("Open Project").apply {
                    addActionListener {
                    }
                })
                add(JMenuItem("Project Manager").apply {
                    addActionListener {
                        StartFrame()
                    }
                })
                add(JMenuItem("Close Project").apply {
                    addActionListener {
                        project.closeProject()
                    }
                })
                addSeparator()
                add(JMenuItem("Settings").apply {
                    addActionListener {
                        SettingsFrame(this@ProjectFrame)
                    }
                    icon = FlatSVGIcon("com/deckerpw/flateditor/icons/settings.svg")
                })
                addSeparator()
                add(JMenuItem("Exit").apply {
                    addActionListener {
                        closeApp()
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
                    project.saveAll()
                    project.javaCompiler.compileAndRunProject()
                }
            })
        }
        defaultCloseOperation = DISPOSE_ON_CLOSE
        setSize(normalWidth, normalHeight)
        extendedState = if (Layout.maximized) MAXIMIZED_BOTH else NORMAL
        setLocationRelativeTo(null)
        addComponentListener(object : ComponentListener {
            override fun componentResized(e: ComponentEvent?) {
                if (extendedState == NORMAL) {
                    normalWidth = width
                    normalHeight = height
                }
            }

            override fun componentMoved(e: ComponentEvent?) {
            }

            override fun componentShown(e: ComponentEvent?) {
            }

            override fun componentHidden(e: ComponentEvent?) {
            }
        })


        layout = BorderLayout()

        add(
            outerSplitPane, BorderLayout.CENTER
        )
        add(JToolBar(JToolBar.VERTICAL).apply {
            add(toolbarButton("com/deckerpw/flateditor/icons/project.svg") {

            })

            border = BorderFactory.createMatteBorder(0, 0, 0, 1, UIManager.getColor("Separator.foreground"))
        }, BorderLayout.WEST)
        isVisible = true
    }

    fun addFile(file: File) {
        tabbedPane.components.map { it as? EditorTab }.forEach { if (it?.file?.path == file.path) return }
        tabbedPane.addTab(file.nameWithoutExtension, TypeRegistry.getIcon(file.extension), EditorTab(project, file))
    }

    override fun dispose() {
        Layout.width = normalWidth
        Layout.height = normalHeight
        Layout.maximized = extendedState == MAXIMIZED_BOTH
        Layout.innerDivider = innerSplitPane.dividerLocation
        Layout.outerDivider = outerSplitPane.dividerLocation
        super.dispose()
    }

}