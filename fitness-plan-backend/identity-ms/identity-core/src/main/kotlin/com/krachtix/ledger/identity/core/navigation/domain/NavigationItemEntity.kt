package com.krachtix.identity.core.navigation.domain

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "navigation_item")
class NavigationItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int? = null,

    @Column(nullable = false)
    val publicId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val platform: String = "WEB",

    @Column(nullable = false)
    val key: String,

    @Column(nullable = false)
    val label: String,

    val icon: String? = null,

    val path: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    val parent: NavigationItemEntity? = null,

    @Column(nullable = false)
    val sortOrder: Int = 0,

    @Column(nullable = false)
    val active: Boolean = true,

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "navigation_item_role", joinColumns = [JoinColumn(name = "navigation_item_id")])
    @Column(name = "role", nullable = false)
    val roles: Set<String> = emptySet()
)
