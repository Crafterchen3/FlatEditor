package com.deckerpw.flateditor.gui.components

import com.deckerpw.flateditor.gui.frames.ProjectFrame
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
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath


class FlatFileExplorer(val fileRoot: File, val projectFrame: ProjectFrame) : JPanel(BorderLayout()) {

    init {
        val root = DefaultMutableTreeNode(FileNode(fileRoot))
        val treeModel = DefaultTreeModel(root)
        val tree = JTree(treeModel)
        val renderer = CellRenderer()
        val ml: MouseListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val selRow = tree.getRowForLocation(e.getX(), e.getY())
                val selPath: TreePath? = tree.getPathForLocation(e.getX(), e.getY())
                if (selRow != -1) {
                    if (e.clickCount == 2) {
                        val file = ((selPath?.lastPathComponent as? FileTreeNode)?.userObject as? FileNode)?.file
                        if (file != null && !file.isDirectory && file.extension == "java")
                        projectFrame.tabbedPane.addTab(file.nameWithoutExtension, EditorTab(projectFrame.project,file))
                    }
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
        val files = fileRoot.listFiles().apply {
            sortBy { it.name }
            sortByDescending { it.isDirectory }
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

class CellRenderer(): DefaultTreeCellRenderer(){

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
            icon = when (((value as? DefaultMutableTreeNode)?.userObject as? FileNode)?.file?.extension ?: ""){
                "java" -> FlatSVGIcon("com/deckerpw/flateditor/icons/class.svg")
                "class" -> FlatSVGIcon("com/deckerpw/flateditor/icons/javaClass.svg")
                else -> icon
            }
        }
    }

}