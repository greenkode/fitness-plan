package com.krachtix.user.security

import com.hazelcast.core.HazelcastInstance
import com.hazelcast.map.IMap
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import java.lang.reflect.Field
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class AccessTokenRevocationFilterTest {

    @Mock
    private lateinit var jwtDecoder: JwtDecoder

    @Mock
    private lateinit var filterChain: FilterChain

    @Mock
    private lateinit var hazelcastInstance: HazelcastInstance

    private lateinit var filter: AccessTokenRevocationFilter
    private lateinit var request: MockHttpServletRequest
    private lateinit var response: MockHttpServletResponse

    private val userId = "user-123"

    @BeforeEach
    fun setUp() {
        filter = AccessTokenRevocationFilter(jwtDecoder)
        request = MockHttpServletRequest()
        response = MockHttpServletResponse()
    }

    private fun setHazelcastInstance(instance: HazelcastInstance?) {
        val field: Field = AccessTokenRevocationFilter::class.java.getDeclaredField("hazelcastInstance")
        field.isAccessible = true
        field.set(filter, instance)
    }

    private fun buildJwt(
        subject: String = userId,
        issuedAt: Instant = Instant.ofEpochSecond(1000),
        expiresAt: Instant = Instant.ofEpochSecond(2000)
    ): Jwt = Jwt.withTokenValue("test-token")
        .header("alg", "RS256")
        .subject(subject)
        .issuedAt(issuedAt)
        .expiresAt(expiresAt)
        .build()

    private fun mockRevocationMap(entries: Map<String, Long>): IMap<String, Long> {
        val map = mock<IMap<String, Long>>()
        entries.forEach { (key, value) -> whenever(map.get(key)).thenReturn(value) }
        return map
    }

    @Nested
    @DisplayName("No Authorization Header")
    inner class NoAuthorizationHeader {

        @Test
        fun `should pass through when no authorization header present`() {
            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
            assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        }
    }

    @Nested
    @DisplayName("Non-Bearer Authorization Header")
    inner class NonBearerAuthorizationHeader {

        @Test
        fun `should pass through when authorization header is not bearer`() {
            request.addHeader("Authorization", "Basic dXNlcjpwYXNz")

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
            assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        }

        @Test
        fun `should pass through when authorization header is empty`() {
            request.addHeader("Authorization", "")

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
            assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        }
    }

    @Nested
    @DisplayName("Token Not Revoked")
    inner class TokenNotRevoked {

        @Test
        fun `should pass through when revocation map returns null for user`() {
            request.addHeader("Authorization", "Bearer test-token")
            val jwt = buildJwt()
            whenever(jwtDecoder.decode("test-token")).thenReturn(jwt)

            val map = mockRevocationMap(emptyMap())
            whenever(hazelcastInstance.getMap<String, Long>("user-token-revocation")).thenReturn(map)
            setHazelcastInstance(hazelcastInstance)

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
            assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        }
    }

    @Nested
    @DisplayName("Token Revoked")
    inner class TokenRevoked {

        @Test
        fun `should return 401 when token was issued before revocation timestamp`() {
            request.addHeader("Authorization", "Bearer test-token")
            val jwt = buildJwt(issuedAt = Instant.ofEpochSecond(1000))
            whenever(jwtDecoder.decode("test-token")).thenReturn(jwt)

            val map = mockRevocationMap(mapOf(userId to 1500L))
            whenever(hazelcastInstance.getMap<String, Long>("user-token-revocation")).thenReturn(map)
            setHazelcastInstance(hazelcastInstance)

            filter.doFilter(request, response, filterChain)

            verify(filterChain, never()).doFilter(request, response)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
            assertThat(response.contentType).isEqualTo(MediaType.APPLICATION_JSON_VALUE)
            assertThat(response.contentAsString).contains("token_revoked")
            assertThat(response.contentAsString).contains("Token has been revoked")
        }

        @Test
        fun `should return 401 when token issuedAt equals revocation timestamp`() {
            request.addHeader("Authorization", "Bearer test-token")
            val jwt = buildJwt(issuedAt = Instant.ofEpochSecond(1000))
            whenever(jwtDecoder.decode("test-token")).thenReturn(jwt)

            val map = mockRevocationMap(mapOf(userId to 1000L))
            whenever(hazelcastInstance.getMap<String, Long>("user-token-revocation")).thenReturn(map)
            setHazelcastInstance(hazelcastInstance)

            filter.doFilter(request, response, filterChain)

            verify(filterChain, never()).doFilter(request, response)
            assertThat(response.status).isEqualTo(HttpStatus.UNAUTHORIZED.value())
            assertThat(response.contentAsString).contains("token_revoked")
        }
    }

    @Nested
    @DisplayName("Token Issued After Revocation")
    inner class TokenIssuedAfterRevocation {

        @Test
        fun `should pass through when token was issued after revocation timestamp`() {
            request.addHeader("Authorization", "Bearer test-token")
            val jwt = buildJwt(issuedAt = Instant.ofEpochSecond(2000), expiresAt = Instant.ofEpochSecond(3000))
            whenever(jwtDecoder.decode("test-token")).thenReturn(jwt)

            val map = mockRevocationMap(mapOf(userId to 1500L))
            whenever(hazelcastInstance.getMap<String, Long>("user-token-revocation")).thenReturn(map)
            setHazelcastInstance(hazelcastInstance)

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
            assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        }
    }

    @Nested
    @DisplayName("Hazelcast Unavailable")
    inner class HazelcastUnavailable {

        @Test
        fun `should pass through when hazelcast instance is null`() {
            request.addHeader("Authorization", "Bearer test-token")
            val jwt = buildJwt()
            whenever(jwtDecoder.decode("test-token")).thenReturn(jwt)

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
            assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        }
    }

    @Nested
    @DisplayName("JWT Decode Failure")
    inner class JwtDecodeFailure {

        @Test
        fun `should pass through when jwt decoder throws exception`() {
            request.addHeader("Authorization", "Bearer invalid-token")
            whenever(jwtDecoder.decode("invalid-token")).thenThrow(BadJwtException("Invalid token"))

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
            assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        }

        @Test
        fun `should pass through when jwt decoder throws runtime exception`() {
            request.addHeader("Authorization", "Bearer bad-token")
            whenever(jwtDecoder.decode("bad-token")).thenThrow(RuntimeException("Unexpected error"))

            filter.doFilter(request, response, filterChain)

            verify(filterChain).doFilter(request, response)
            assertThat(response.status).isEqualTo(HttpStatus.OK.value())
        }
    }
}
