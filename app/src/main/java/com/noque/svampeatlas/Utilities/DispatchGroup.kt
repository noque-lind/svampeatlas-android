package com.noque.svampeatlas.Utilities

import android.util.Log

class DispatchGroup {
    private var count = 0
    private var runnable: (() -> Unit)? = null
    private var runnables = mutableListOf<(() -> Unit)>()


    init {
        count = 0
    }

    @Synchronized
    fun enter() {
        count++
        Log.d("DispatchGroup", "Count is now $count")
    }

    @Synchronized
    fun leave() {
        count--
        Log.d("DispatchGroup", "Count is now $count")
        notifyGroup()
    }

    fun notify(r: () -> Unit) {
        runnables.add(r)
        notifyGroup()
    }

    private fun notifyGroup() {
        if (count == 0) {
            runnables.forEach { it() }
            runnables.clear()
        }
    }

    fun clear() {
        runnables.clear()
    }
}