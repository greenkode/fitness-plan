package com.krachtix.identity.core.currency.service

import com.krachtix.commons.exception.InvalidRequestException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.currency.domain.OrganizationCurrencyRepository
import com.krachtix.identity.core.organization.entity.Organization
import com.krachtix.identity.core.organization.entity.SubscriptionTier
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class CurrencyLimitServiceTest {

    @Mock
    private lateinit var organizationCurrencyRepository: OrganizationCurrencyRepository

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var currencyLimitService: CurrencyLimitService

    @Test
    fun `should return 1 for TRIAL tier`() {
        assertThat(currencyLimitService.getMaxCurrencies(SubscriptionTier.TRIAL)).isEqualTo(1)
    }

    @Test
    fun `should return 3 for STARTER tier`() {
        assertThat(currencyLimitService.getMaxCurrencies(SubscriptionTier.STARTER)).isEqualTo(3)
    }

    @Test
    fun `should return 10 for PROFESSIONAL tier`() {
        assertThat(currencyLimitService.getMaxCurrencies(SubscriptionTier.PROFESSIONAL)).isEqualTo(10)
    }

    @Test
    fun `should return max value for ENTERPRISE tier`() {
        assertThat(currencyLimitService.getMaxCurrencies(SubscriptionTier.ENTERPRISE)).isEqualTo(Int.MAX_VALUE)
    }

    @Test
    fun `should allow currency addition within limit`() {
        val organization = Organization(name = "Test Org", slug = "test-org").apply {
            plan = SubscriptionTier.STARTER
        }
        val clientId = "test-client"

        `when`(organizationCurrencyRepository.countByClientIdAndEnabledTrue(clientId)).thenReturn(1L)

        currencyLimitService.validateCurrencyAddition(clientId, organization, 2)
    }

    @Test
    fun `should throw when currency limit exceeded for TRIAL tier`() {
        val organization = Organization(name = "Test Org", slug = "test-org").apply {
            plan = SubscriptionTier.TRIAL
        }
        val clientId = "test-client"

        `when`(organizationCurrencyRepository.countByClientIdAndEnabledTrue(clientId)).thenReturn(1L)
        `when`(messageService.getMessage("currency.limit.exceeded")).thenReturn("Limit exceeded")

        assertThatThrownBy {
            currencyLimitService.validateCurrencyAddition(clientId, organization, 1)
        }.isInstanceOf(InvalidRequestException::class.java)
    }

    @Test
    fun `should throw when currency limit exceeded for STARTER tier`() {
        val organization = Organization(name = "Test Org", slug = "test-org").apply {
            plan = SubscriptionTier.STARTER
        }
        val clientId = "test-client"

        `when`(organizationCurrencyRepository.countByClientIdAndEnabledTrue(clientId)).thenReturn(3L)
        `when`(messageService.getMessage("currency.limit.exceeded")).thenReturn("Limit exceeded")

        assertThatThrownBy {
            currencyLimitService.validateCurrencyAddition(clientId, organization, 1)
        }.isInstanceOf(InvalidRequestException::class.java)
    }

    @Test
    fun `should allow unlimited currencies for ENTERPRISE tier`() {
        val organization = Organization(name = "Test Org", slug = "test-org").apply {
            plan = SubscriptionTier.ENTERPRISE
        }
        val clientId = "test-client"

        `when`(organizationCurrencyRepository.countByClientIdAndEnabledTrue(clientId)).thenReturn(100L)

        currencyLimitService.validateCurrencyAddition(clientId, organization, 50)
    }
}
