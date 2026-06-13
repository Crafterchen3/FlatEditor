package com.deckerpw.flateditor.gui.frames

import com.deckerpw.flateditor.data.ProjectData
import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.util.ColorFunctions
import java.awt.BorderLayout
import java.awt.Color
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

class StartFrame : JFrame("Flat Editor") {

    init {
        iconImage = FlatSVGIcon("com/deckerpw/flateditor/icons/logo.svg").image
        rootPane.putClientProperty("JRootPane.titleBarHeight", 40)
        defaultCloseOperation = DISPOSE_ON_CLOSE
        setSize(600, 600)
        setLocationRelativeTo(null)
        layout = BorderLayout()
        jMenuBar = JMenuBar().apply {
            add(JMenu("About"))
        }

        val root = HistoryTreeNode(HistoryNode("Projects", true))
        val treeModel = DefaultTreeModel(root)
        root.folder("Personal Projects") {
            project(ProjectData("Picasso", File("run/picasso"),"App"))
        }
        root.folder("Test Projects") {
            project(ProjectData("Hello World", File("run/helloworld"),"HelloWorld"))
            project(ProjectData("Multi File", File("run/multi-file"),"Main"))
        }
        add(JPanel(BorderLayout(10, 10)).apply {
            border = EmptyBorder(10, 10, 10, 10)
            add(JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
                add(JButton("New Project").apply {
                    this@StartFrame.rootPane.defaultButton = this
                })
                add(JButton("Open Folder"))
            }, BorderLayout.SOUTH)
            add(JScrollPane(JTree(treeModel).apply {
                val tree = this
                var alternateColor: Color = background
                if (FlatLaf.isLafDark())
                    alternateColor = ColorFunctions.lighten(background, 0.05f)
                else
                    alternateColor = ColorFunctions.darken(background, 0.05f)

                putClientProperty(
                    FlatClientProperties.STYLE,
                    $$"rowHeight: 30;  selectionArc: 10; selectionBackground: $Tree.selectionInactiveBackground; selectionForeground: $Tree.selectionInactiveForeground"
                )
                val renderer = DefaultTreeCellRenderer()
                renderer.leafIcon = FlatSVGIcon("com/deckerpw/flateditor/icons/java.svg")
                renderer.openIcon = FlatSVGIcon("com/deckerpw/flateditor/icons/folder.svg")
                renderer.closedIcon = FlatSVGIcon("com/deckerpw/flateditor/icons/folder.svg")
                cellRenderer = renderer
                showsRootHandles = true
                isRootVisible = true

                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent?) {
                        if (e == null)
                            return
                        val row = e.y / rowHeight
                        val treeNode = getPathForRow(row)?.lastPathComponent as? DefaultMutableTreeNode
                        val node = treeNode?.userObject as? HistoryNode ?: return
                        if (e.clickCount >= 2 && e.button == MouseEvent.BUTTON1 && !node.isDirectory) {
                            node.project?.toProject()
                            dispose()
                        }
                        if (e.button == MouseEvent.BUTTON3) {
                            setSelectionRow(row)
                            JPopupMenu().apply {
                                if (node.isDirectory) {
                                    add(JMenuItem("New Project...").apply {
                                        addActionListener {

                                        }
                                    })
                                    add(JMenuItem("Add Folder...").apply {
                                        addActionListener {
                                            val name = JOptionPane.showInputDialog("Enter Folder Name")
                                            if (!name.isNullOrEmpty()) {
                                                treeNode.folder(name)
                                                tree.updateUI()
                                            }
                                        }
                                    })
                                }
                                if (!treeNode.isRoot) {
                                    add(JMenuItem("Remove").apply {
                                        addActionListener {
                                            if (node.isDirectory && treeNode.childCount > 0) {
                                                if (JOptionPane.showConfirmDialog(
                                                        tree,
                                                        "Remove Folder and Children?",
                                                        "Confirm Deletion",
                                                        JOptionPane.YES_NO_OPTION,
                                                    ) == 0
                                                ) {
                                                    treeNode.removeFromParent()
                                                    tree.updateUI()
                                                }
                                            }else {
                                                treeNode.removeFromParent()
                                                tree.updateUI()
                                            }
                                        }
                                    })
                                }
                            }.show(tree, e.x, e.y)
                        }
                    }
                })
            }).apply {
                border = BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground"))
            })
        }, BorderLayout.CENTER)

        isVisible = true
    }

    class HistoryTreeNode(private val node: HistoryNode) : DefaultMutableTreeNode(node) {

        override fun isLeaf(): Boolean {
            return !node.isDirectory
        }

    }

    class HistoryNode(val name: String , val isDirectory: Boolean = false) {

        var project: ProjectData? = null

        constructor(project: ProjectData) : this(project.name,false) {
            this.project = project
        }

        override fun toString(): String = name
    }

    fun DefaultMutableTreeNode.project(project: ProjectData) = add(HistoryTreeNode(HistoryNode(project)))
    fun DefaultMutableTreeNode.folder(name: String, block: DefaultMutableTreeNode.() -> Unit = {}) =
        add(HistoryTreeNode(HistoryNode(name, true)).apply { block() })

}

fun Color.toHexString(): String {
    return String.format("#%02x%02x%02x", red, green, blue).apply {
        println(this)
    }
}