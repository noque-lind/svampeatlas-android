package com.noque.svampeatlas.models

import com.noque.svampeatlas.adapters.NotebookAdapter
import javax.crypto.SealedObject


interface ViewType

open class Item<V>(private val viewType: V) where V : ViewType, V: Enum<V> {
    fun getViewType(): V {
        return viewType
    }
}


class Sections<V, I> where V : ViewType, V: Enum<V>, I: Item<V> {

    private var sections: MutableList<Section<I>> = mutableListOf()

    fun addSection(section: Section<I>) {
        sections.add(section)
    }

    fun setSections(sections: List<Section<I>>) {
        this.sections = sections.toMutableList()
    }

    fun getTitle(position: Int): String? {
        val section = getSection(position)
        return section.first.getTitle()
    }

    fun getError(position: Int): AppError? {
        return getSection(position).first.getError()
    }

    fun getCount(): Int {
        var count = 0

        sections.forEach {
            count += it.getCount()
        }

        return count
    }

    fun getViewTypeOrdinal(position: Int): Int {
        var currentPosition = 0

        sections.forEach {
            if (position >= currentPosition && position <= (currentPosition + it.getCount() - 1)) {
                return when (val viewType = it.getViewType(position - currentPosition)) {
                    Section.ViewType.ITEM -> {
                         it.getItem(position - currentPosition).getViewType().ordinal + Section.ViewType.values.count()
                    }
                    else -> { viewType.ordinal }
                }
            }

            currentPosition += it.getCount()
        }

        throw java.lang.IndexOutOfBoundsException("Index out of bound when trying to fetch view type ordinal at position $position")
    }

    fun getSectionViewType(viewType: Int): Section.ViewType {
        return if (viewType > Section.ViewType.values.lastIndex) {
            Section.ViewType.ITEM
        } else {
            Section.ViewType.values[viewType]
        }
    }


    fun getItem(position: Int): I {
        val section = getSection(position)
       return section.first.getItem(position - section.second)
    }

    fun deleteItem(adapterPosition: Int) {
        val section = getSection(adapterPosition)
        section.first.removeItem(adapterPosition - section.second)
    }

    fun getSectionPosition(section: Section<I>): Int {
        var currentPosition = 0

        sections.forEach {
            if (it == section) {
                return currentPosition
            }
            currentPosition += it.getCount()
        }

        throw java.lang.IndexOutOfBoundsException("Could not find the requested section.")
    }

    private fun getSection(index: Int): Pair<Section<I>, Int> {
        var currentIndex = 0

        sections.forEach {
            if (index >= currentIndex && index < (it.getCount() + currentIndex)) {
                return Pair(it, currentIndex)
            }
            currentIndex += it.getCount()
        }

        throw IndexOutOfBoundsException("Out of bounds when trying to fetch section at position: $index, with copulated position being: $index")
    }
}


class Section<T>(private var title: String?, private var state: State<List<T>> = State.Empty()) {

    enum class ViewType: com.noque.svampeatlas.models.ViewType {
        HEADER,
        ITEM,
        ERROR,
        LOADER;

        companion object {
            val values: List<ViewType> = values().toList()
        }
    }


    fun setState(state: State<List<T>>) {
        this.state = state
    }

    fun setTitle(title: String?) {
        this.title = title
    }


    fun getCount(): Int {
        return when (val state = state) {
            is State.Items -> {
                if (title != null) state.items.count() + 1 else state.items.count()
            }
            is State.Empty -> {
                if (title != null) 1 else 0
            }
            is State.Loading -> {
                if (title != null) 2 else 1
            }
            is State.Error -> {
                if (title != null) 2 else 1
            }
        }
    }

    fun getViewType(position: Int): ViewType {
        return when (state) {
            is State.Items -> {
                if (title != null && position == 0) ViewType.HEADER else ViewType.ITEM
            }
            is State.Empty -> {
                if (title != null && position == 0) ViewType.HEADER else throw java.lang.IndexOutOfBoundsException("Attempted to get ViewType for position over 0 when section State is empty.")
            }
            is State.Loading -> {
                if (title != null && position == 0) ViewType.HEADER else ViewType.LOADER
            }
            is State.Error -> {
                if (title != null && position == 0) ViewType.HEADER else ViewType.ERROR
            }
        }
    }

    fun getTitle(): String? {
        return title
    }

    fun getError(): AppError? {
        return when (val state = state) {
            is State.Error -> { state.error }
            else -> {
                return null
            }
        }
    }

    fun getItem(position: Int): T {
        return when (val state = state) {
            is State.Items -> {
                if (title != null && position == 0) { throw java.lang.IndexOutOfBoundsException("Index out of bounds when trying to fetch item at position 0 where title is nut null")
                } else if (title != null) {
                    state.items[position - 1]
                } else {
                    state.items[position]
                }
            }
            else -> { throw IllegalAccessError("Should not access item while state is not items.") }
        }
    }

    fun removeItem(position: Int) {
        when (val state = state) {
            is State.Items -> {
                if (title != null && position == 0) { throw java.lang.IndexOutOfBoundsException("Index out of bounds when trying to fetch item at position 0 where title is nut null")
                } else if (title != null) {
                    val items = state.items.toMutableList()
                    items.removeAt(position - 1)
                    setState(State.Items(items))
                } else {
                    val items = state.items.toMutableList()
                    items.removeAt(position - 1)
                    setState(State.Items(items))
                }
            }
            else -> { throw IllegalAccessError("Should not access item while state is not items.") }
        }
    }

    data class Builder<T>(
        var title: String? = null,
        var items: List<T>? = null) {

        fun title(title: String) = apply { this.title = title }
        fun items(items: List<T>) = apply { this.items = items }
        fun build(): Section<T> {
            val section = Section<T>(title)
            items?.let { section.setState(State.Items(it)) }
            return section
        }
    }
}