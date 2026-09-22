package com.deckerpw.flateditor

import com.deckerpw.poolbox.PoolApp
import com.deckerpw.poolbox.config.booleanConfig
import com.deckerpw.poolbox.config.intConfig
import com.deckerpw.poolbox.config.stringConfig
import com.deckerpw.poolbox.updater.GithubUpdateProvider
import com.formdev.flatlaf.extras.FlatSVGIcon

val VERSION = (System.getProperty("VERSION") ?: "0").apply {
    println("Version: $this")
}
const val IDENTIFIER = "com.deckerpw.flateditor"
val poolApp = PoolApp(
    VERSION, IDENTIFIER,
    GithubUpdateProvider(
        "Crafterchen3/FlatEditor",
        { assetName ->
            assetName.startsWith("FlatEditor") && assetName.endsWith(".msi")
        }),
    PoolApp.AppInfo(
        name = "FlatEditor",
        description = "A lightweight code editor built entirely in Java and Kotlin. Its fast, customizable and simple to use.",
        copyright = "© 2026 Paul Decker. All Rights Reserved.",
        icon = FlatSVGIcon("com/deckerpw/flateditor/icons/logo.svg")
    )
)
private val config = poolApp.config

var theme: String by config.stringConfig("theme", "com.formdev.flatlaf.FlatDarkLaf")
var fontScale: Int by config.intConfig("fontScale", 2)

class Layout private constructor() {

    companion object {
        var width by config.intConfig("layout.width", 1600)
        var height by config.intConfig("layout.height", 1200)
        var maximized by config.booleanConfig("layout.maximized", false)
        var innerDivider by config.intConfig("layout.innerDivider", 300)
        var outerDivider by config.intConfig("layout.outerDivider", 300)
    }

}