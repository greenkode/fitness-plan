package com.krachtix.search

import org.hibernate.search.engine.search.sort.dsl.SortOrder
import java.io.Serializable

interface SearchRepository<T, ID : Serializable> {
    fun fullTextSearch(
        text: String,
        offset: Int,
        limit: Int,
        fields: MutableList<String>,
        sortBy: String,
        sortOrder: SortOrder
    ): MutableList<T>

    fun fuzzySearch(
        text: String,
        offset: Int,
        limit: Int,
        fields: MutableList<String>,
        sortBy: String,
        sortOrder: SortOrder
    ): MutableList<T>

    fun wildcardSearch(
        pattern: String,
        offset: Int,
        limit: Int,
        fields: MutableList<String>,
        sortBy: String,
        sortOrder: SortOrder
    ): MutableList<T>
}