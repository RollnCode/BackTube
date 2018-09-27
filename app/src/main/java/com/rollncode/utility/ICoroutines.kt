package com.rollncode.utility

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.IO
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

internal interface ICoroutines {

    companion object {

        val COROUTINES = object : CoroutineScope {
            override val coroutineContext: CoroutineContext = Dispatchers.IO
        }

        fun <T> async(block: suspend CoroutineScope.() -> T) =
                COROUTINES.async(block = block)
    }

    val jobs: MutableSet<Job>

    fun execute(block: suspend CoroutineScope.() -> Unit) {
        jobs += COROUTINES.launch(block = block)
    }

    fun cancelJobs() {
        jobs.filter { !it.isCompleted }.forEach { it.cancel() }
    }
}