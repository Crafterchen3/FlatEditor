package com.deckerpw.flateditor.gui.components

import com.formdev.flatlaf.extras.FlatSVGIcon
import com.formdev.flatlaf.extras.components.FlatButton
import com.formdev.flatlaf.extras.components.FlatToggleButton
import java.awt.Insets
import java.awt.event.ActionListener
import javax.swing.AbstractButton


fun toolbarButton(icon: String, action: (() -> (Unit))? = null): FlatButton {
    return FlatButton().apply {

        setIcon(FlatSVGIcon(icon))
        buttonType = FlatButton.ButtonType.toolBarButton
        isFocusable = false
        margin = Insets(3, 3, 3, 3)
        if (action != null)
            addActionListener { action() }
    }
}

fun toolbarToggleButton(icon: String, action: (() -> (Unit))? = null): FlatToggleButton {
    return FlatToggleButton().apply {

        setIcon(FlatSVGIcon(icon))
        buttonType = FlatButton.ButtonType.toolBarButton
        isFocusable = false
        margin = Insets(3, 3, 3, 3)
        if (action != null)
            addActionListener { action() }
    }
}

fun <T: AbstractButton>  T.withActionListener(listener: ActionListener): T{
    addActionListener(listener)
    return this
}