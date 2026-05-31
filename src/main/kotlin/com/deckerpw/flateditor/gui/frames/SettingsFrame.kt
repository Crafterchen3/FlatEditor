package com.deckerpw.flateditor.gui.frames

import com.deckerpw.flateditor.applyLookAndFeel
import com.deckerpw.flateditor.closeApp
import com.deckerpw.flateditor.theme
import com.formdev.flatlaf.FlatClientProperties
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme
import org.intellij.lang.annotations.JdkConstants
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.UIManager
import javax.swing.border.EmptyBorder
import kotlin.system.exitProcess

class SettingsFrame(parent: JFrame): JFrame("Settings") {

    private val applyList = mutableListOf<() -> (Unit)>()
    private var needsRestart = false

    data class Theme(val name: String, val className: String)

    val themes = listOf(
        Theme("Light", FlatLightLaf::class.qualifiedName!!),
        Theme("Dark", FlatDarkLaf::class.qualifiedName!!),
        Theme("One Dark", FlatOneDarkIJTheme::class.qualifiedName!!),
    )

    val applyButton = JButton("Apply").apply {
        isEnabled = false
        addActionListener {
            applyChanges()
        }
    }

    init {
        rootPane.putClientProperty("JRootPane.titleBarHeight",40)
        rootPane.putClientProperty(FlatClientProperties.STYLE,
            "TitlePane.unifiedBackground: true;"
        )

        jMenuBar = JMenuBar().apply {
            add(JMenu("Export/Import Settings"))
        }

        layout = BorderLayout()
        background = background.darker()
        add(JTabbedPane().apply {
            border = BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground"))
            tabPlacement = JTabbedPane.LEFT
            putClientProperty(FlatClientProperties.STYLE,
                "tabInsets: 6,35,6,35;"
            )
            addTab("Project", JScrollPane(JPanel().apply {
                border = EmptyBorder(8,8,8,8)
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
            }))
            addTab("Themes", JPanel(BorderLayout()).apply {
                border = EmptyBorder(8,8,8,8)

                add(JList(themes.map { it.name }.toTypedArray()).apply {
                    selectedIndex = themes.indexOfFirst { it.className == theme }
                    addListSelectionListener {
                        addApplyFunction{
                            theme = themes[this.selectedIndex].className
                            needsRestart = true
                            applyLookAndFeel()
                        }
                    }
                })
            })
        }, BorderLayout.CENTER)
        add(JPanel(FlowLayout(FlowLayout.RIGHT)).apply {
            border = EmptyBorder(8,8,8,8)
            add(JButton("OK").apply {
                this@SettingsFrame.getRootPane().setDefaultButton(this)
                addActionListener {
                    applyChanges()
                    tryClose()
                }
            })
            add(JButton("Cancel").apply {
                addActionListener {
                    tryClose()
                }
            })
            add(applyButton)
        }, BorderLayout.SOUTH)

        defaultCloseOperation = DISPOSE_ON_CLOSE
        setSize(800,600)
        setLocationRelativeTo(parent)
        iconImage = parent.iconImage
        isVisible = true
    }

    fun applyChanges(){
        applyList.forEach { it() }
        applyList.clear()
        applyButton.isEnabled = false
    }

    fun addApplyFunction(func: () -> (Unit)){
        applyList.add(func)
        applyButton.isEnabled = true
    }

    fun tryClose(){
        if (needsRestart)
            when(JOptionPane.showConfirmDialog(this, "The App needs to restart to apply some changes!\nExit now?")){
                0 -> closeApp()
                1 ->  dispose()
                else -> {}
            }
        else
            dispose()
    }

}