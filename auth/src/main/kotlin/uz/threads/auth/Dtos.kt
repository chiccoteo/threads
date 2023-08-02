package uz.threads.auth

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.oauth2.provider.ClientDetails
import java.io.Serializable

data class UserAuthDto(
    var id: Long,
    var username: String,
    var name: String? = null,
    var password: String,
    var role: String,
    var active: Boolean
) : Serializable

class CustomUserDetails(val userDto: UserAuthDto) : UserDetails, Serializable {
    override fun getAuthorities() = listOf(SimpleGrantedAuthority(userDto.role))
    override fun isEnabled() = userDto.active
    override fun getUsername() = userDto.username
    override fun isCredentialsNonExpired() = userDto.active
    override fun getPassword() = userDto.password
    override fun isAccountNonExpired() = userDto.active
    override fun isAccountNonLocked() = userDto.active
}

class CustomClientDetails(private val authClient: AuthClient) : ClientDetails {
    override fun isSecretRequired() = true
    override fun getAdditionalInformation(): MutableMap<String, Any> = mutableMapOf()
    override fun getAccessTokenValiditySeconds() = authClient.accessTokenValidity
    override fun getResourceIds() = authClient.resources
    override fun getClientId() = authClient.clientId
    override fun isAutoApprove(scope: String?) = true
    override fun getAuthorities() = mutableListOf<GrantedAuthority>()
    override fun getRefreshTokenValiditySeconds() = authClient.refreshTokenValidity
    override fun getClientSecret() = authClient.clientSecret
    override fun getRegisteredRedirectUri() = authClient.redirectUris
    override fun isScoped() = false
    override fun getScope() = authClient.scopes
    override fun getAuthorizedGrantTypes() = authClient.grantTypes
}