package uz.threads.auth

import org.springframework.security.oauth2.provider.OAuth2Authentication
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("user")
class UserController {

    @GetMapping("current")
    fun getUser(authentication: OAuth2Authentication): Map<*, *> {
        val user = if (authentication.userAuthentication is PreAuthenticatedAuthenticationToken) {
            (authentication.userAuthentication.principal as CustomUserDetails).userDto
        } else {
            authentication.userAuthentication.details as UserAuthDto
        }
        return mutableMapOf(
            "userId" to user.id,
            "username" to user.username,
            "name" to user.name,
            "authorities" to authentication.authorities,
            "clientId" to authentication.oAuth2Request.clientId
        )
    }

}

@RestController
@RequestMapping("token")
class TokenController(private val service: TokenService) {

    @DeleteMapping
    fun delete() = service.delete()
}