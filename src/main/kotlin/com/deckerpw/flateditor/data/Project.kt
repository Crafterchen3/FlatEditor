package com.deckerpw.flateditor.data

import com.deckerpw.flateditor.gui.components.LogViewer
import com.deckerpw.flateditor.gui.components.toolbarButton
import com.deckerpw.flateditor.gui.frames.ProjectFrame
import com.deckerpw.flateditor.lang.TaskScheduler
import com.deckerpw.flateditor.lang.compiler.JavaCompiler
import java.io.File
import javax.swing.JTabbedPane

val activeProjects = mutableListOf<Project>()

class Project(val dir: File, val mainClass: String){

    val taskScheduler = TaskScheduler()
    val buildLogViewer = LogViewer().apply {
        toolBar.apply {
            add(toolbarButton("com/deckerpw/flateditor/icons/compile.svg"){

            })
            add(toolbarButton("com/deckerpw/flateditor/icons/suspend.svg"){

            }.apply {
                isEnabled = false
            })
        }
    }
    val runLogViewer = LogViewer().apply {
        toolBar.apply {
            add(toolbarButton("com/deckerpw/flateditor/icons/execute.svg"){

            })
            add(toolbarButton("com/deckerpw/flateditor/icons/suspend.svg"){

            }.apply {
                isEnabled = false
            })
        }
    }
    val javaCompiler = JavaCompiler(this)
    val terminalTabbedPane = JTabbedPane().apply {
        addTab("Build", buildLogViewer)
        addTab("Run", runLogViewer)
    }

    val projectFrame = ProjectFrame(this)

    init {
        activeProjects.add(this)
    }

    fun focusBuildLog(){
        terminalTabbedPane.selectedIndex = 0
    }

    fun focusRunLog(){
        terminalTabbedPane.selectedIndex = 1
    }

    fun closeProject() {
        projectFrame.dispose()
        activeProjects.remove(this)
    }
}
