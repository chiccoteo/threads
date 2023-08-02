package uz.threads.auth

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.security.oauth2.common.OAuth2AccessToken
import org.springframework.security.oauth2.common.OAuth2RefreshToken
import org.springframework.security.oauth2.provider.OAuth2Authentication

import java.util.*

@Document(collection = "auth_client")
data class AuthClient(
    @Indexed(unique = true) val clientId: String,
    val clientSecret: String,
    val accessTokenValidity: Int,
    val refreshTokenValidity: Int,
    val grantTypes: MutableSet<String> = mutableSetOf(),
    val scopes: MutableSet<String> = mutableSetOf(),
    val resources: MutableSet<String> = mutableSetOf(),
    val redirectUris: MutableSet<String> = mutableSetOf(),
    val additionalInformation: MutableMap<String, Any> = mutableMapOf(),
    @Id val id: String? = null
)

@Document
data class AccessToken(
    val user: UserAuthDto,
    @Indexed(unique = true) val tokenId: String? = null,
    var token: OAuth2AccessToken? = null,
    val authenticationId: String? = null,
    val username: String? = null,
    val clientId: String? = null,
    var refreshToken: String? = null,
    var ipAddress: String? = null,
    var authentication: String? = null,
    @Id val id: String? = null,
    @CreatedDate var createdDate: Date? = null,
    @LastModifiedDate var modifiedDate: Date? = null
) {
    fun getAuthentication(): OAuth2Authentication {
        return SerializableObjectConverter.deserialize(this.authentication)
    }

    fun setAuthentication(authentication: OAuth2Authentication) {
        this.authentication = SerializableObjectConverter.serialize(authentication)
    }
}

@Document
data class RefreshToken(
    @Indexed(unique = true) val tokenId: String? = null,
    val token: OAuth2RefreshToken? = null,
    var authentication: String? = null,
    @Id val id: String? = null
) {
    fun getAuthentication(): OAuth2Authentication {
        return SerializableObjectConverter.deserialize(this.authentication)
    }

    fun setAuthentication(authentication: OAuth2Authentication) {
        this.authentication = SerializableObjectConverter.serialize(authentication)
    }
}