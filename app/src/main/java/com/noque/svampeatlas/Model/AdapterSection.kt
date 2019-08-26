package com.noque.svampeatlas.Model

import android.util.Log

class Section<T>(private val title: String?, private val items: List<T>) {
    enum class ViewType {
        HEADER,
        ITEM;

        companion object {
            val values: List<ViewType> = values().toList()
        }
    }

    fun count(): Int {
        return if (title != null) items.count() + 1 else items.count()
    }

    fun viewType(position: Int): ViewType {
        return if (title != null && position == 0) ViewType.HEADER else ViewType.ITEM
    }

    fun title(): String? {
        return title
    }

    fun getItem(position: Int): T? {
        if (title != null && position == 0) {
            return null
        }

        return if (title != null) items[position - 1] else items[position]
    }
}