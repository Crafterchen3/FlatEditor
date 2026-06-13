package com.deckerpw.flateditor.data

import java.io.File

data class ProjectData(val name: String, val path: File, val mainClass: String){

    fun toProject(): Project = Project(path,mainClass)

}