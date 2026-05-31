package com.deckerpw.flateditor.gui.components

import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatButton
import java.awt.Insets
import javax.swing.border.EmptyBorder


fun toolbarButton(icon: String, action:() -> (Unit)): FlatButton{
    return FlatButton().apply {

        setIcon(FlatSVGIcon(icon))
        buttonType = FlatButton.ButtonType.toolBarButton
        isFocusable = false
        margin = Insets(3,3,3,3)
        addActionListener { action() }
    }
}