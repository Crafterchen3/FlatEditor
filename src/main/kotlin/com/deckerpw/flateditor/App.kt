package com.deckerpw.flateditor

import com.deckerpw.poolbox.PoolApp
import com.deckerpw.poolbox.config.booleanConfig
import com.deckerpw.poolbox.config.intConfig
import com.deckerpw.poolbox.config.stringConfig

val VERSION = (System.getProperty("com.deckerpw.flateditor.VERSION") ?: "0").apply {
    println("Version: $this")
}
const val IDENTIFIER = "com.deckerpw.flateditor"
val poolApp = PoolApp(VERSION,IDENTIFIER)
private val config = poolApp.config

var theme: String by config.stringConfig("theme","com.formdev.flatlaf.FlatDarkLaf")

class Layout private constructor(){

    companion object{
        var width by config.intConfig("layout.width",1600)
        var height by config.intConfig("layout.height",1200)
        var maximized by config.booleanConfig("layout.maximized",false)
        var innerDivider by config.intConfig("layout.innerDivider",300)
        var outerDivider by config.intConfig("layout.outerDivider",300)
    }

}