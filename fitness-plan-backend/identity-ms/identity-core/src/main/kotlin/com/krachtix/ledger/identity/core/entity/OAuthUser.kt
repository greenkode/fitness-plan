package com.krachtix.identity.core.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import com.krachtix.commons.dto.Email
import com.krachtix.commons.dto.PhoneNumber
import com.krachtix.commons.kyc.TrustLevel
import com.krachtix.commons.model.AuditableEntity
import com.krachtix.identity.core.totp.domain.TotpSecretConverter
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class UserType {
    INDIVIDUAL, BUSINESS
}

enum class RegistrationSource {
    INVITATION, OAUTH_GOOGLE, OAUTH_MICROSOFT, SELF_REGISTRATION
}

enum class OAuthProvider {
    GOOGLE, MICROSOFT
}

@Entity
@Table(name = "oauth_user")
class OAuthUser() : AuditableEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id: UUID? = null

    @Column(nullable = false)
    var username: String = ""

    @Column(nullable = false)
    var password: String = ""

    @Convert(converter = EmailConverter::class)
    var email: Email? = null

    var firstName: String? = null

    var middleName: String? = null

    var lastName: String? = null

    var pictureUrl: String? = null

    @Convert(converter = PhoneNumberConverter::class)
    var phoneNumber: PhoneNumber? = null

    var merchantId: UUID? = null

    var organizationId: UUID? = null

    @Enumerated(EnumType.STRING)
    var userType: UserType? = null

    @Enumerated(EnumType.STRING)
    var trustLevel: TrustLevel? = null

    @Column(nullable = false)
    var emailVerified: Boolean = false

    @Column(nullable = false)
    var phoneNumberVerified: Boolean = false

    var dateOfBirth: LocalDate? = null

    var taxIdentificationNumber: String? = null

    @Column(nullable = false)
    var enabled: Boolean = true

    @Column(nullable = false)
    var accountNonExpired: Boolean = true

    @Column(nullable = false)
    var accountNonLocked: Boolean = true

    @Column(nullable = false)
    var credentialsNonExpired: Boolean = true

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "oauth_user_authority",
        joinColumns = [JoinColumn(name = "user_id")]
    )
    @Column(name = "authority", nullable = false)
    var authorities: MutableSet<String> = mutableSetOf()

    @Column(nullable = false)
    var failedLoginAttempts: Int = 0

    var lockedUntil: Instant? = null

    var lastFailedLogin: Instant? = null

    @Enumerated(EnumType.STRING)
    var registrationSource: RegistrationSource = RegistrationSource.INVITATION

    @Convert(converter = TotpSecretConverter::class)
    var totpSecret: String? = null

    @Column(nullable = false)
    var totpEnabled: Boolean = false

    var twoFactorLastVerified: Instant? = null

    @OneToMany(mappedBy = "user", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    val settings: MutableSet<OAuthUserSetting> = mutableSetOf()

    var invitationStatus: Boolean
        get() = getSetting(OAuthUserSettingName.INVITATION_STATUS)?.toBoolean() ?: false
        set(value) = addSetting(OAuthUserSettingName.INVITATION_STATUS, value.toString())

    var locale: String
        get() = getSetting(OAuthUserSettingName.LOCALE) ?: "en"
        set(value) = addSetting(OAuthUserSettingName.LOCALE, value)

    var environmentPreference: EnvironmentMode
        get() = getSetting(OAuthUserSettingName.ENVIRONMENT_PREFERENCE)
            ?.let { EnvironmentMode.valueOf(it) } ?: EnvironmentMode.SANDBOX
        set(value) = addSetting(OAuthUserSettingName.ENVIRONMENT_PREFERENCE, value.name)

    var environmentLastSwitchedAt: Instant?
        get() = getSetting(OAuthUserSettingName.ENVIRONMENT_LAST_SWITCHED_AT)?.let { Instant.parse(it) }
        set(value) {
            value?.let { addSetting(OAuthUserSettingName.ENVIRONMENT_LAST_SWITCHED_AT, it.toString()) }
                ?: removeSetting(OAuthUserSettingName.ENVIRONMENT_LAST_SWITCHED_AT)
        }

    var registrationComplete: Boolean
        get() = getSetting(OAuthUserSettingName.REGISTRATION_COMPLETE)?.toBoolean() ?: false
        set(value) = addSetting(OAuthUserSettingName.REGISTRATION_COMPLETE, value.toString())

    constructor(username: String, password: String) : this() {
        this.username = username
        this.password = password
    }

    constructor(
        username: String,
        password: String,
        email: Email,
        enabled: Boolean = true,
        authorities: MutableSet<String> = mutableSetOf()
    ) : this() {
        this.username = username
        this.password = password
        this.email = email
        this.enabled = enabled
        this.authorities = authorities
    }

    fun isCurrentlyLocked(): Boolean {
        return lockedUntil != null && Instant.now().isBefore(lockedUntil)
    }

    fun recordFailedLogin() {
        val now = Instant.now()
        failedLoginAttempts++
        lastFailedLogin = now

        if (failedLoginAttempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = now.plusSeconds(LOCKOUT_DURATION_MINUTES * 60)
            accountNonLocked = false
        }
    }

    fun resetFailedLoginAttempts() {
        failedLoginAttempts = 0
        lastFailedLogin = null
        lockedUntil = null
        accountNonLocked = true
    }

    fun checkAndUnlockIfExpired(): Boolean {
        if (lockedUntil != null && Instant.now().isAfter(lockedUntil)) {
            resetFailedLoginAttempts()
            return true
        }
        return false
    }

    fun fullName(): String {
        val parts = listOfNotNull(firstName, lastName)
        return if (parts.isNotEmpty()) parts.joinToString(" ") else username
    }

    fun addSetting(name: OAuthUserSettingName, value: String) {
        settings.removeIf { it.settingName == name }
        settings.add(OAuthUserSetting(this, name, value))
    }

    fun getSetting(name: OAuthUserSettingName): String? = settings.find { it.settingName == name }?.settingValue

    fun removeSetting(name: OAuthUserSettingName) {
        settings.removeIf { it.settingName == name }
    }

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MINUTES = 30L
    }
}
