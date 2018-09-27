package com.rollncode.utility

import kotlinx.coroutines.experimental.CoroutineScope
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.launch

internal interface ICoroutines {

    val jobs: MutableSet<Job>

    fun execute(block: suspend CoroutineScope.() -> Unit) {
        jobs += GlobalScope.launch(block = block)
    }

    fun cancelJobs() {
        jobs.filter { !it.isCompleted }.forEach { it.cancel() }
    }
}