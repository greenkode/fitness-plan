package com.krachtix.identity.core.repository

import com.krachtix.identity.core.entity.OAuthAuthenticationMethod
import com.krachtix.identity.core.entity.OAuthGrantType
import com.krachtix.identity.core.entity.OAuthScope
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface OAuthScopeRepository : JpaRepository<OAuthScope, Int> {
    fun findByName(name: String): OAuthScope?
}

@Repository
interface OAuthAuthenticationMethodRepository : JpaRepository<OAuthAuthenticationMethod, Int> {
    fun findByName(name: String): OAuthAuthenticationMethod?
}

@Repository
interface OAuthGrantTypeRepository : JpaRepository<OAuthGrantType, Int> {
    fun findByName(name: String): OAuthGrantType?
}
