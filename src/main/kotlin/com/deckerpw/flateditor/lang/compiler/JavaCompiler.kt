package com.deckerpw.flateditor.lang.compiler

import com.deckerpw.flateditor.data.Project
import java.io.File
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import javax.tools.ToolProvider


class JavaCompiler(val project: Project) {
        fun compile(file: File): CompletableFuture<Boolean>? {
            return project.taskScheduler.javaTask {
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
            return project.taskScheduler.javaTask {
                val log = project.buildLogViewer
                log.clear()
                log.addLine("Compiling Java")
                println("compiler start")
                try {
                    val javaFiles = dir.walkTopDown()
                        .filter { it.isFile && it.extension == "java" }
                        .toList()
                    val compiler = ToolProvider.getSystemJavaCompiler()
                    val fileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8)
                    val compilationUnits = fileManager.getJavaFileObjectsFromFiles(javaFiles)
                    val writer = StringWriter()
                    val task = compiler.getTask(
                        writer, fileManager, null, listOf(
                            "-d", Paths.get(dir.path, "target", "classes").apply { toFile().mkdirs() }.toString()
                        ), null, compilationUnits
                    )
                    val compilationResult = task.call()
                    log.addLine(writer.toString())

                    if (compilationResult)
                        log.addLine("Build completed successfully")
                    else
                        log.addLine("Build completed with errors")


                    return@javaTask compilationResult
                } catch (e: Exception) {
                    e.printStackTrace()
                    return@javaTask false
                }
            }
        }

        fun execute(dir: File, mainClass: String): CompletableFuture<Int>? {
            return project.taskScheduler.javaTask {
                val log = project.runLogViewer
                log.clear()
                log.addLine("Starting Process")
                try {
                    val processBuilder = ProcessBuilder(
                        listOf(
                            "java", "-cp", "../../lib/*;.", mainClass
                        )
                    )
                    processBuilder.directory(dir)
                    processBuilder.redirectErrorStream(true)

                    val process = processBuilder.start()

                    process.inputStream.bufferedReader().forEachLine { line ->
                        log.addLine(line)
                    }


                    val exitCode = process.waitFor()

                    log.addLine("\nProcess finished with exit code $exitCode")
                    return@javaTask exitCode
                } catch (e: Exception) {
                    return@javaTask -1
                }
            }
        }

        fun compileAndRunProject() {
            project.focusBuildLog()
            compileDir(project.dir)?.thenApply { success ->
                println(success)
                if (success) {
                    project.focusRunLog()
                    execute(File("${project.dir}/target/classes"), project.mainClass)?.thenApply { result ->
                        println(result)
                    }
                }
            }
        }
}