package com.krachtix.identity.core.settings.command

import com.krachtix.commons.dto.Email
import com.krachtix.commons.exception.RecordNotFoundException
import com.krachtix.commons.i18n.MessageService
import com.krachtix.identity.core.entity.OAuthUser
import com.krachtix.identity.core.entity.OrganizationStatus
import com.krachtix.identity.core.organization.entity.Organization
import com.krachtix.identity.core.organization.entity.OrganizationPropertyName
import com.krachtix.identity.core.organization.repository.OrganizationRepository
import com.krachtix.identity.core.service.CacheEvictionService
import com.krachtix.identity.core.service.UserService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class UpdateAlertsCommandHandlerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var organizationRepository: OrganizationRepository

    @Mock
    private lateinit var cacheEvictionService: CacheEvictionService

    @Mock
    private lateinit var messageService: MessageService

    @InjectMocks
    private lateinit var handler: UpdateAlertsCommandHandler

    private lateinit var user: OAuthUser
    private lateinit var organization: Organization
    private val merchantId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        user = OAuthUser(
            username = "test@example.com",
            password = "encoded",
            email = Email("test@example.com")
        ).apply {
            id = userId
            this.merchantId = this@UpdateAlertsCommandHandlerTest.merchantId
        }

        organization = Organization(
            id = merchantId,
            name = "Test Corp",
            slug = "test-corp",
            status = OrganizationStatus.ACTIVE
        )
    }

    @Test
    fun `should update alert settings and evict caches`() {
        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.of(organization))
        `when`(messageService.getMessage("settings.success.alerts_updated")).thenReturn("Alerts updated")

        val command = UpdateAlertsCommand(
            failureLimit = BigDecimal("5"),
            lowBalance = BigDecimal("100.00")
        )

        val result = handler.handle(command)

        assertThat(result.success).isTrue()
        assertThat(result.message).isEqualTo("Alerts updated")

        assertThat(organization.getProperty(OrganizationPropertyName.FAILURE_LIMIT)).isEqualTo("5")
        assertThat(organization.getProperty(OrganizationPropertyName.LOW_BALANCE)).isEqualTo("100.00")

        verify(organizationRepository).save(organization)
        verify(cacheEvictionService).evictMerchantCaches(merchantId.toString())
    }

    @Test
    fun `should throw when user has no merchant`() {
        val userWithoutMerchant = OAuthUser(
            username = "test@example.com",
            password = "encoded",
            email = Email("test@example.com")
        ).apply {
            id = userId
            merchantId = null
        }

        `when`(userService.getCurrentUser()).thenReturn(userWithoutMerchant)
        `when`(messageService.getMessage("settings.error.no_merchant")).thenReturn("No merchant")

        val command = UpdateAlertsCommand(
            failureLimit = BigDecimal("5"),
            lowBalance = BigDecimal("100.00")
        )

        assertThatThrownBy { handler.handle(command) }
            .isInstanceOf(RecordNotFoundException::class.java)
            .hasMessage("No merchant")
    }

    @Test
    fun `should throw when organization not found`() {
        `when`(userService.getCurrentUser()).thenReturn(user)
        `when`(organizationRepository.findByIdWithProperties(merchantId)).thenReturn(Optional.empty())
        `when`(messageService.getMessage("settings.error.merchant_not_found")).thenReturn("Merchant not found")

        val command = UpdateAlertsCommand(
            failureLimit = BigDecimal("5"),
            lowBalance = BigDecimal("100.00")
        )

        assertThatThrownBy { handler.handle(command) }
            .isInstanceOf(RecordNotFoundException::class.java)
            .hasMessage("Merchant not found")
    }
}
