package com.deckerpw.flateditor.lang.compiler

import com.deckerpw.flateditor.data.Project
import com.deckerpw.flateditor.lang.taskScheduler
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import javax.tools.ToolProvider


class JavaCompiler {
    companion object {
        fun compile(file: File): CompletableFuture<Boolean>? {
            return taskScheduler.javaTask {
                println("compiler start")
                try {
                    val compiler = ToolProvider.getSystemJavaCompiler()
                    val compilationResult = compiler.run(null, null, null, file.path)

                    println("compiler finished")
                    return@javaTask compilationResult == 0
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@javaTask false
                }
            }
        }

        fun compileDir(dir: File): CompletableFuture<Boolean>? {
            return taskScheduler.javaTask {
                println("compiler start")
                try {
                    val javaFiles = dir.walkTopDown()
                        .filter { it.isFile && it.extension == "java" }
                        .toList()
                    val compiler = ToolProvider.getSystemJavaCompiler()
                    val fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)
                    val compilationUnits = fileManager.getJavaFileObjectsFromFiles(javaFiles)
                    val task = compiler.getTask(
                        null, fileManager, null, listOf(
                            "-d", Paths.get(dir.path, "target", "classes").apply { toFile().mkdirs() }.toString()
                        ), null, compilationUnits
                    )
                    val compilationResult = task.call()

                    println("compiler finished")
                    return@javaTask compilationResult
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@javaTask false
                }
            }
        }

        fun execute(dir: File, mainClass: String): CompletableFuture<Int>? {
            return taskScheduler.javaTask {
                try {
                    val processBuilder = ProcessBuilder(
                        listOf(
                            "java", "-cp", ".", mainClass
                        )
                    )
                    processBuilder.directory(dir)
                    processBuilder.redirectErrorStream(true)

                    val process = processBuilder.start()

                    process.inputStream.bufferedReader().forEachLine { line ->
                        println("[Class Output] $line")
                    }

                    return@javaTask process.waitFor()
                } catch (e: Exception) {
                    return@javaTask -1
                }
            }
        }

        fun compileAndRunProject(project: Project) {
            compileDir(project.dir)?.thenApply { success ->
                println(success)
                if (success)
                    execute(File("${project.dir}/target/classes"), project.mainClass)?.thenApply { result ->
                        println(result)
                    }
            }
        }
    }
}