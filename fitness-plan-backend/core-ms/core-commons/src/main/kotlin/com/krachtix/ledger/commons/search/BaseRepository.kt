package com.krachtix.commons.search

import com.krachtix.search.SearchRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.NoRepositoryBean
import java.io.Serializable

@NoRepositoryBean
interface BaseRepository<T : Any, ID : Serializable>
    : JpaRepository<T, ID>, SearchRepository<T, ID>