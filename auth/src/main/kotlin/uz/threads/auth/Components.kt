package uz.threads.auth

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import feign.Response
import feign.codec.ErrorDecoder
import org.apache.commons.codec.binary.Base64
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.common.OAuth2RefreshToken
import org.springframework.security.oauth2.common.util.SerializationUtils
import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.oauth2.provider.token.AuthenticationKeyGenerator
import org.springframework.security.oauth2.provider.token.DefaultAuthenticationKeyGenerator
import org.springframework.security.oauth2.provider.token.TokenStore
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*


object SerializableObjectConverter {
    fun serialize(`object`: OAuth2Authentication?): String {
        val bytes: ByteArray = SerializationUtils.serialize(`object`)
        return Base64.encodeBase64String(bytes)
    }

    fun deserialize(encodedObject: String?): OAuth2Authentication {
        val bytes: ByteArray = Base64.decodeBase64(encodedObject)
        return SerializationUtils.deserialize(bytes)
    }
}

@Component
class MongoTokenStore(
    private val accessTokenRepository: AccessTokenRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userService: UserService,
) : TokenStore {
    private val realIPHeader = "x-real-ip"
    private val authenticationKeyGenerator: AuthenticationKeyGenerator = DefaultAuthenticationKeyGenerator()

    override fun readAuthentication(token: OAuth2AccessToken?) = readAuthentication(token?.value)

    override fun readAuthentication(token: String?): OAuth2Authentication? {
        return extractTokenKey(token)?.let {
            val accessToken = accessTokenRepository.findByTokenId(it)
            accessToken?.let { currentAccessToken ->
                currentAccessToken.modifiedDate = Date()
                accessTokenRepository.save(currentAccessToken)
            }
            accessToken?.getAuthentication()
        }
    }

    @Transactional
    override fun storeAccessToken(token: OAuth2AccessToken, authentication: OAuth2Authentication) {
        val refreshToken = token.refreshToken?.value
//        readAccessToken(token.value)?.let { removeAccessToken(it) }
        val user = if (authentication.userAuthentication is UsernamePasswordAuthenticationToken) {
            authentication.userAuthentication.details as UserAuthDto
        } else {
            (authentication.userAuthentication.principal as CustomUserDetails).userDto
        }
        checkUserActive(user.username)
        val ipAddress = try {
            val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
            request.getHeader(realIPHeader) ?: request.remoteAddr
        } catch (e: java.lang.Exception) {
            null
        }

        val authenticationId = authenticationKeyGenerator.extractKey(authentication)
        var accessToken = accessTokenRepository.findByAuthenticationId(authenticationId)
        if (accessToken != null) {
            accessToken.modifiedDate = Date()
            accessToken.token?.expiration?.let {
                if (it.time < Date().time) {
                    accessToken!!.token = token
                    accessToken!!.refreshToken = refreshToken
                }
            }
        } else {
            accessToken = AccessToken(
                user,
                extractTokenKey(token.value),
                token,
                authenticationId,
                if (authentication.isClientOnly) null else authentication.name,
                authentication.oAuth2Request.clientId,
                refreshToken,
                ipAddress
            ).also { it.setAuthentication(authentication) }
        }
        accessTokenRepository.save(accessToken)
    }

    override fun readAccessToken(tokenValue: String): OAuth2AccessToken? {
        return extractTokenKey(tokenValue)?.let {
            val accessToken = accessTokenRepository.findByTokenId(it)
            accessToken?.let { currentAccessToken ->
                currentAccessToken.modifiedDate = Date()
                accessTokenRepository.save(currentAccessToken)
            }
            return accessToken?.token
        }
    }

    override fun removeAccessToken(token: OAuth2AccessToken) {
        extractTokenKey(token.value)?.let { accessTokenRepository.deleteByTokenId(it) }
    }

    override fun storeRefreshToken(token: OAuth2RefreshToken?, authentication: OAuth2Authentication?) {
        val refreshToken =
            RefreshToken(extractTokenKey(token?.value), token).also { it.setAuthentication(authentication!!) }
        refreshTokenRepository.save(refreshToken)
    }

    override fun readRefreshToken(tokenValue: String?): OAuth2RefreshToken? {
        return extractTokenKey(tokenValue)?.let { refreshTokenRepository.findByTokenId(it)?.token }
    }

    override fun readAuthenticationForRefreshToken(token: OAuth2RefreshToken?): OAuth2Authentication? {
        return extractTokenKey(token?.value)?.let { refreshTokenRepository.findByTokenId(it)?.getAuthentication() }
    }

    override fun removeRefreshToken(token: OAuth2RefreshToken?) {
        extractTokenKey(token?.value)?.let { refreshTokenRepository.deleteByTokenId(it) }
    }

    override fun removeAccessTokenUsingRefreshToken(refreshToken: OAuth2RefreshToken?) {
        refreshToken?.value?.let { accessTokenRepository.deleteByRefreshToken(it) }
    }

    override fun getAccessToken(authentication: OAuth2Authentication?): OAuth2AccessToken? {
        return authentication?.let {
            val authenticationId = authenticationKeyGenerator.extractKey(it)
            val accessToken = accessTokenRepository.findByAuthenticationId(authenticationId)
            if (accessToken?.token != null && authenticationId != authenticationKeyGenerator.extractKey(
                    this.readAuthentication(
                        accessToken.token
                    )
                )
            ) {
                checkUserActive(accessToken.username!!)
                this.removeAccessToken(accessToken.token!!)
                this.storeAccessToken(accessToken.token!!, authentication)
            }
            accessToken?.let { currentAccessToken ->
                checkUserActive(currentAccessToken.username!!)
                currentAccessToken.modifiedDate = Date()
                accessTokenRepository.save(currentAccessToken)
            }
            accessToken?.token
        }
    }

    override fun findTokensByClientIdAndUserName(
        clientId: String,
        userName: String,
    ): MutableCollection<OAuth2AccessToken> {
        return accessTokenRepository
            .findAllByClientIdAndUsername(clientId, userName)
            .filter { it.token != null }
            .map { it.token!! }
            .toMutableList()
    }

    override fun findTokensByClientId(clientId: String): MutableCollection<OAuth2AccessToken> {
        return accessTokenRepository
            .findAllByClientId(clientId)
            .filter { it.token != null }
            .map { it.token!! }
            .toMutableList()
    }

    private fun extractTokenKey(value: String?): String? {
        return if (value == null) {
            null
        } else {
            val digest: MessageDigest = try {
                MessageDigest.getInstance("MD5")
            } catch (var5: NoSuchAlgorithmException) {
                throw IllegalStateException("MD5 algorithm not available.  Fatal (should be in the JDK).")
            }
            try {
                val e = digest.digest(value.toByteArray(charset("UTF-8")))
                String.format("%032x", BigInteger(1, e))
            } catch (var4: UnsupportedEncodingException) {
                throw IllegalStateException("UTF-8 encoding not available.  Fatal (should be in the JDK).")
            }
        }
    }

    private fun checkUserActive(username: String) {
        if (!userService.isActive(username)) throw AuthenticationServiceException("User not active")
    }
}

@Component
class CustomAuthenticationProvider(private val userService: UserService) : AuthenticationProvider {

    private val passwordEncoder = BCryptPasswordEncoder()
    private val grantTypeKey = "grant_type"
    override fun authenticate(authentication: Authentication?): Authentication {
        if (authentication is UsernamePasswordAuthenticationToken && authentication.details is Map<*, *>) {
            val details = authentication.details as Map<*, *>
            return when (details[grantTypeKey] as String) {
                GrantType.PASSWORD.value -> authenticateByPassword(authentication, authentication.details as Map<*, *>)
                else -> throw BadCredentialsException("Illegal authentication grant_type")
            }
        } else throw BadCredentialsException("Illegal authentication token")
    }

    private fun authenticateByPassword(
        authentication: Authentication,
        details: Map<*, *>,
    ): UsernamePasswordAuthenticationToken {
        val username = details["username"] as String
        val password = authentication.credentials as String
        val user = userService.find(username)
        if (!passwordEncoder.matches(password, user.password)) throw BadCredentialsException("Incorrect password")
        val result = UsernamePasswordAuthenticationToken(
            username, null,
            listOf(SimpleGrantedAuthority(user.role))
        )
        result.details = user
        return result
    }

    override fun supports(authentication: Class<*>?): Boolean {
        return authentication == UsernamePasswordAuthenticationToken::class.java
    }
}

@Component
class FeignErrorDecoder : ErrorDecoder {

    val mapper = ObjectMapper()
    override fun decode(methodKey: String?, response: Response?): Exception {
        response?.apply {
            when (status()) {
                400 -> {
                    if (request().url().contains("find?username")) {
                        return AuthenticationServiceException("User not found by username")
                    }
                }
            }
            val message = (mapper.readValue(this.body().asInputStream(), ErrorMessage::class.java))
            return Exception(message.toString())
        }
        return Exception("Not handled")
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorMessage(
    var code: Int? = null,
    var message: String? = null,
)