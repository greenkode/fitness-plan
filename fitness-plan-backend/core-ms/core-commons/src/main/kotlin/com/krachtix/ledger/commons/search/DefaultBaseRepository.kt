package com.krachtix.commons.search

import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.CriteriaQuery
import jakarta.persistence.criteria.Root
import org.hibernate.search.engine.search.sort.dsl.SortOrder
import org.hibernate.search.mapper.orm.Search
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import java.io.Serializable
import java.util.Optional

@Suppress("UNCHECKED_CAST")
class DefaultBaseRepository<T : Any, ID : Serializable>
    : SimpleJpaRepository<T, ID>, BaseRepository<T, ID> {
    private val entityManager: EntityManager
    private val idAttributeName: String

    constructor(domainClass: Class<T>, entityManager: EntityManager) : super(domainClass, entityManager) {
        this.entityManager = entityManager
        this.idAttributeName = resolveIdAttributeName(domainClass, entityManager)
    }

    constructor(entityInformation: JpaEntityInformation<T, ID>, entityManager: EntityManager) : super(
        entityInformation,
        entityManager
    ) {
        this.entityManager = entityManager
        this.idAttributeName = entityInformation.idAttribute?.name
            ?: resolveIdAttributeName(entityInformation.javaType, entityManager)
    }

    private fun resolveIdAttributeName(domainClass: Class<T>, em: EntityManager): String {
        val entityType = em.metamodel.entity(domainClass)
        return entityType.idClassAttributes?.firstOrNull()?.name
            ?: entityType.declaredSingularAttributes.firstOrNull { it.isId }?.name
            ?: "id"
    }

    override fun findById(id: ID): Optional<T> {
        val cb: CriteriaBuilder = entityManager.criteriaBuilder
        val cq: CriteriaQuery<T> = cb.createQuery(domainClass)
        val root: Root<T> = cq.from(domainClass)
        cq.select(root).where(cb.equal(root.get<Any>(idAttributeName), id))
        val result = entityManager.createQuery(cq).resultList.firstOrNull()
        return Optional.ofNullable(result) as Optional<T>
    }

    override fun deleteById(id: ID) {
        findById(id).ifPresent { entity -> delete(entity) }
    }

    override fun fullTextSearch(
        text: String, offset: Int, limit: Int,
        fields: MutableList<String>, sortBy: String, sortOrder: SortOrder
    ): MutableList<T> {

        if (text.isEmpty()) {
            return mutableListOf<T>()
        }

        return Search.session(entityManager)
            .search<T>(domainClass)
            .where { f -> f.match().fields().matching(text) }
            .sort { f -> f.field(sortBy).order(sortOrder) }
            .fetchHits(offset, limit)
    }

    override fun fuzzySearch(
        text: String,
        offset: Int,
        limit: Int,
        fields: MutableList<String>,
        sortBy: String,
        sortOrder: SortOrder
    ): MutableList<T> {
        return mutableListOf<T>()
    }

    override fun wildcardSearch(
        pattern: String,
        offset: Int,
        limit: Int,
        fields: MutableList<String>,
        sortBy: String,
        sortOrder: SortOrder
    ): MutableList<T> {
        return mutableListOf<T>()
    }
}
