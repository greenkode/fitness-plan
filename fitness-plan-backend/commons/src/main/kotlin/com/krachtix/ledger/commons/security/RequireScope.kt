package com.krachtix.commons.security

import org.springframework.security.access.prepost.PreAuthorize

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('SCOPE_fx:rates')")
annotation class RequireFxRatesScope

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('SCOPE_fx:convert')")
annotation class RequireFxConvertScope

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('SCOPE_internal:read')")
annotation class RequireInternalReadScope

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('SCOPE_krachtix:read') or hasAnyRole('MERCHANT_SUPER_ADMIN', 'MERCHANT_ADMIN', 'MERCHANT_FINANCE_ADMIN', 'MERCHANT_USER')")
annotation class RequireLedgerReadScope

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('SCOPE_krachtix:write') or hasAnyRole('MERCHANT_SUPER_ADMIN', 'MERCHANT_ADMIN', 'MERCHANT_FINANCE_ADMIN')")
annotation class RequireLedgerWriteScope

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@PreAuthorize("hasAuthority('SCOPE_krachtix:admin') or hasAnyRole('MERCHANT_SUPER_ADMIN', 'MERCHANT_ADMIN')")
annotation class RequireLedgerAdminScope
