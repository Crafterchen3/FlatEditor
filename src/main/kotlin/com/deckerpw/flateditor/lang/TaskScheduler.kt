package com.deckerpw.flateditor.lang

import org.apache.logging.log4j.util.Supplier
import java.util.concurrent.CompletableFuture

class TaskScheduler{

    private val compilerScheduler = LimitedScheduler(1)

    fun <T> javaTask(
        supplier: Supplier<T>
    ): CompletableFuture<T>? = compilerScheduler.execute(supplier)


    private class LimitedScheduler(val maxTasks: Int){

        private var currentTasks = 0;

        fun <T> execute(
            supplier: Supplier<T>
        ): CompletableFuture<T>? {
            if (currentTasks >= maxTasks) {
                println("denied")
                return null
            }
            currentTasks += 1
            return CompletableFuture.supplyAsync {
                return@supplyAsync supplier.get().also {
                    currentTasks -= 1
                    println("finished")
                }
            }
        }

    }

}