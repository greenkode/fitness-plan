package com.krachtix.identity.core.settings.query

import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.currency.domain.OrganizationCurrency
import com.krachtix.identity.core.currency.domain.OrganizationCurrencyRepository
import com.krachtix.identity.core.entity.OAuthRegisteredClient
import com.krachtix.identity.core.entity.OrganizationStatus
import com.krachtix.identity.core.organization.entity.Organization
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.repository.OAuthRegisteredClientRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class GetMerchantSettingsQueryHandlerTest {

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var oAuthRegisteredClientRepository: OAuthRegisteredClientRepository

    @Mock
    private lateinit var organizationCurrencyRepository: OrganizationCurrencyRepository

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: GetMerchantSettingsQueryHandler

    private lateinit var organization: Organization
    private lateinit var client: OAuthRegisteredClient
    private val merchantId = UUID.randomUUID()
    private val clientId = UUID.randomUUID().toString()
    private val chartTemplateId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        organization = Organization(
            id = merchantId,
            name = "Test Corp",
            slug = "test-corp",
            status = OrganizationStatus.ACTIVE
        )

        client = OAuthRegisteredClient().apply {
            id = merchantId
            this.clientId = this@GetMerchantSettingsQueryHandlerTest.clientId
            clientName = "Test Corp"
        }
    }

    @Test
    fun `should return merchant settings when setup is completed`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "true")
        organization.addProperty(OrganizationPropertyName.DEFAULT_CURRENCY, "USD")
        organization.addProperty(OrganizationPropertyName.CHART_TEMPLATE_ID, chartTemplateId)

        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(oAuthRegisteredClientRepository.findById(merchantId)).thenReturn(Optional.of(client))
        `when`(organizationCurrencyRepository.findByClientIdAndEnabledTrue(clientId)).thenReturn(
            listOf(
                OrganizationCurrency(id = 1, clientId = clientId, currencyCode = "USD", isPrimary = true),
                OrganizationCurrency(id = 2, clientId = clientId, currencyCode = "EUR", isPrimary = false)
            )
        )

        val result = handler.handle(GetMerchantSettingsQuery(merchantId))

        assertThat(result.setupCompleted).isTrue()
        assertThat(result.defaultCurrency).isEqualTo("USD")
        assertThat(result.chartTemplateId).isEqualTo(chartTemplateId)
        assertThat(result.additionalCurrencies).containsExactly("EUR")
    }

    @Test
    fun `should return empty additional currencies when only primary currency exists`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "true")
        organization.addProperty(OrganizationPropertyName.DEFAULT_CURRENCY, "USD")
        organization.addProperty(OrganizationPropertyName.CHART_TEMPLATE_ID, chartTemplateId)

        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(oAuthRegisteredClientRepository.findById(merchantId)).thenReturn(Optional.of(client))
        `when`(organizationCurrencyRepository.findByClientIdAndEnabledTrue(clientId)).thenReturn(
            listOf(
                OrganizationCurrency(id = 1, clientId = clientId, currencyCode = "USD", isPrimary = true)
            )
        )

        val result = handler.handle(GetMerchantSettingsQuery(merchantId))

        assertThat(result.additionalCurrencies).isEmpty()
    }

    @Test
    fun `should throw when organization not found`() {
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.empty())
        `when`(messageService.getMessage("settings.error.merchant_not_found")).thenReturn("Merchant not found")

        assertThatThrownBy { handler.handle(GetMerchantSettingsQuery(merchantId)) }
            .isInstanceOf(RecordNotFoundException::class.java)
    }

    @Test
    fun `should throw when setup is not completed`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "false")

        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(messageService.getMessage("settings.error.setup_not_completed")).thenReturn("Setup not completed")

        assertThatThrownBy { handler.handle(GetMerchantSettingsQuery(merchantId)) }
            .isInstanceOf(RecordNotFoundException::class.java)
    }

    @Test
    fun `should throw when setup completed property is missing`() {
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(messageService.getMessage("settings.error.setup_not_completed")).thenReturn("Setup not completed")

        assertThatThrownBy { handler.handle(GetMerchantSettingsQuery(merchantId)) }
            .isInstanceOf(RecordNotFoundException::class.java)
    }

    @Test
    fun `should throw when default currency is not configured`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "true")
        organization.addProperty(OrganizationPropertyName.CHART_TEMPLATE_ID, chartTemplateId)

        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(messageService.getMessage("settings.error.currency_not_configured")).thenReturn("Currency not configured")

        assertThatThrownBy { handler.handle(GetMerchantSettingsQuery(merchantId)) }
            .isInstanceOf(RecordNotFoundException::class.java)
    }

    @Test
    fun `should throw when chart template is not configured`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "true")
        organization.addProperty(OrganizationPropertyName.DEFAULT_CURRENCY, "USD")

        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(messageService.getMessage("settings.error.chart_template_not_configured")).thenReturn("Chart template not configured")

        assertThatThrownBy { handler.handle(GetMerchantSettingsQuery(merchantId)) }
            .isInstanceOf(RecordNotFoundException::class.java)
    }

    @Test
    fun `should throw when oauth client not found`() {
        organization.addProperty(OrganizationPropertyName.SETUP_COMPLETED, "true")
        organization.addProperty(OrganizationPropertyName.DEFAULT_CURRENCY, "USD")
        organization.addProperty(OrganizationPropertyName.CHART_TEMPLATE_ID, chartTemplateId)

        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(oAuthRegisteredClientRepository.findById(merchantId)).thenReturn(Optional.empty())
        `when`(messageService.getMessage("settings.error.merchant_not_found")).thenReturn("Merchant not found")

        assertThatThrownBy { handler.handle(GetMerchantSettingsQuery(merchantId)) }
            .isInstanceOf(RecordNotFoundException::class.java)
    }
}
