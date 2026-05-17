package com.deckerpw.flateditor

import com.deckerpw.poolbox.PoolApp

val VERSION = (System.getProperty("com.deckerpw.flateditor.VERSION") ?: "0").apply {
    println("Version: $this")
}
const val IDENTIFIER = "com.deckerpw.flateditor"
val poolApp = PoolApp(VERSION,IDENTIFIER)
private val config = poolApp.config