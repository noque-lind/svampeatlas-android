package com.noque.svampeatlas.utilities

import android.util.Log

class DispatchGroup(private val name: String?) {

    private var count = 0
    private var runnables = mutableListOf<(Runnable)>()


    @Synchronized
    fun enter() {
        count++
        Log.d("DispatchGroup ${name}", "Count is now $count")
    }

    @Synchronized
    fun leave() {
        count--
        Log.d("DispatchGroup ${name}", "Count is now $count")
        notifyGroup()
    }

    fun notify(runnable: Runnable) {
        runnables.add(runnable)
        notifyGroup()
    }

    private fun notifyGroup() {
        if (count == 0) {
            runnables.forEach { it.run() }
            runnables.clear()
        }
    }
    @Synchronized
    fun clear() {
        runnables.clear()
    }
}