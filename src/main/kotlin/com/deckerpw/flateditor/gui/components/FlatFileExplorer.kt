package com.deckerpw.flateditor.gui.components

import com.deckerpw.flateditor.data.Project
import com.deckerpw.flateditor.lang.TypeRegistry
import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.icons.FlatTreeLeafIcon
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.File
import java.util.concurrent.CompletableFuture
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel


class FlatFileExplorer(val fileRoot: File, val project: Project) : JPanel(BorderLayout()) {

    var nodeCount = 0
    var cancelled = false
    var ignoreNodeCount = false

    init {
        val root = DefaultMutableTreeNode(FileNode(fileRoot))
        val treeModel = DefaultTreeModel(root)
        val tree = JTree(treeModel)
        tree.putClientProperty(
            FlatClientProperties.STYLE,
            "rowHeight: 20;"
        )
        val renderer = CellRenderer()
        val ml: MouseListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val row = e.y / tree.rowHeight
                val treeNode = tree.getPathForRow(row)?.lastPathComponent as? DefaultMutableTreeNode
                val node = treeNode?.userObject as? FileNode
                val file = node?.file ?: return
                tree.setSelectionRow(row)
                if (e.clickCount == 2 && e.button == MouseEvent.BUTTON1 &&
                    !file.isDirectory && TypeRegistry.isEnabled(file.extension)
                ) {
                    project.projectFrame.addFile(file)
                    project.projectFrame.tabbedPane.selectedIndex = project.projectFrame.tabbedPane.tabCount - 1
                }
                if (e.button == MouseEvent.BUTTON3) {
                    FileNodePopup(treeNode, file, tree, project).show(tree, e.x, e.y)
                }
            }
        }
        tree.addMouseListener(ml)
        renderer.setLeafIcon(FlatTreeLeafIcon())
        tree.setCellRenderer(renderer)
        add(JScrollPane(tree))
        CompletableFuture.runAsync { createChildren(fileRoot, root) }.thenRun {
            tree.expandRow(0)
            println("Finished Loading tree")
        }
    }

    private fun createChildren(
        fileRoot: File, node: DefaultMutableTreeNode
    ) {
        if (cancelled)
            return
        if (nodeCount >= 5000 && !ignoreNodeCount) {
            when (JOptionPane.showConfirmDialog(
                null,
                "There may be too many files in the project directory to realistically handle.\nDo you want to close the Project?",
                "Too many Files",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            )) {
                0 -> {
                    project.closeProject()
                    cancelled = true
                    return
                }

                else -> {
                    ignoreNodeCount = true
                }
            }
        }
        val files = fileRoot.listFiles()?.apply {
            sortBy { it.name }
            sortBy { it.extension }
            sortByDescending { it.isDirectory }
            nodeCount += size
        }
        if (files == null) return

        for (file in files) {
            val childNode = FileTreeNode(file)
            node.add(childNode)
            if (file.isDirectory()) {
                createChildren(file, childNode)
            }
        }
    }

    override fun getMinimumSize(): Dimension {
        return Dimension(200, 0)
    }

}

class FileNodePopup(
    treeNode: DefaultMutableTreeNode,
    file: File,
    tree: JTree,
    project: Project
) : JPopupMenu() {

    init {
        add(JMenu("New").apply {
            fun createFile(ext: String) {
                val name = JOptionPane.showInputDialog("Please enter File name")
                val filename = "$name.$ext"
                if (file.isDirectory) {
                    val file1 = file.resolve(filename)
                    file1.createNewFile()
                    treeNode.add(FileTreeNode(file1))
                    tree.updateUI()
                    project.projectFrame.addFile(file1)

                } else {
                    val file1 = file.resolveSibling(filename)
                    file1.createNewFile()
                    (treeNode.parent as? DefaultMutableTreeNode)?.add(FileTreeNode(file1))
                    tree.updateUI()
                    project.projectFrame.addFile(file1)
                }
            }

            fun createFolder() {
                val name = JOptionPane.showInputDialog("Please enter Folder name")
                if (file.isDirectory) {
                    val file1 = file.resolve(name)
                    file1.mkdir()
                    treeNode.add(FileTreeNode(file1))
                    tree.updateUI()
                } else {
                    val file1 = file.resolveSibling(name)
                    file1.mkdir()
                    (treeNode.parent as? DefaultMutableTreeNode)?.add(FileTreeNode(file1))
                    tree.updateUI()
                }
            }

            add(JMenuItem("Java Class", FlatSVGIcon("com/deckerpw/flateditor/icons/class.svg")).withActionListener {
                createFile("java")
            })
            add(JMenuItem("Text File", FlatSVGIcon("com/deckerpw/flateditor/icons/text.svg")).withActionListener {
                createFile("txt")
            })
            add(JMenuItem("Folder", FlatSVGIcon("com/deckerpw/flateditor/icons/folder.svg")).withActionListener {
                createFolder()
            })
        })
        if (!treeNode.isRoot) {
            add(JMenuItem("Delete").withActionListener {
                if (!treeNode.isLeaf && treeNode.childCount > 0) {
                    if (JOptionPane.showConfirmDialog(
                            null,
                            "Remove Folder and Children?",
                            "Confirm Deletion",
                            JOptionPane.YES_NO_OPTION,
                        ) == 0
                    ) {
                        file.deleteRecursively()
                        treeNode.removeFromParent()
                        tree.updateUI()
                    }
                } else {
                    file.delete()
                    treeNode.removeFromParent()
                    tree.updateUI()
                }
            })
        }
    }

}

class FileTreeNode(private val file: File) : DefaultMutableTreeNode(FileNode(file)) {


    override fun isLeaf(): Boolean {
        return !file.isDirectory
    }

}

class FileNode(val file: File) {
    override fun toString(): String {
        val name = file.getName()
        if (name == "") {
            return file.getAbsolutePath()
        } else {
            return name
        }
    }
}

class CellRenderer() : DefaultTreeCellRenderer() {

    override fun getTreeCellRendererComponent(
        tree: JTree?,
        value: Any?,
        sel: Boolean,
        expanded: Boolean,
        leaf: Boolean,
        row: Int,
        hasFocus: Boolean
    ): Component? {
        return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus).apply {
            if (leaf) {
                val ext = ((value as? DefaultMutableTreeNode)?.userObject as? FileNode)?.file?.extension ?: ""
                icon = TypeRegistry.getIcon(ext)
            }
        }
    }

}