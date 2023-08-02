package uz.threads.auth

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.oauth2.provider.ClientDetails
import org.springframework.security.oauth2.provider.ClientDetailsService
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam


@Service
class AuthClientDetailsService(val authClientRepository: AuthClientRepository) : ClientDetailsService {
    override fun loadClientByClientId(clientId: String?): ClientDetails {
        val authClient = authClientRepository.findByClientId(clientId)
        if (authClient != null) {
            return CustomClientDetails(authClient)
        } else {
            throw IllegalArgumentException()
        }
    }
}

@Service
class CustomUserDetailsService(val userService: UserService) : UserDetailsService {
    override fun loadUserByUsername(username: String?): UserDetails {
        return CustomUserDetails(userService.find(username!!))
    }
}

@FeignClient("user")
interface UserService {

    @GetMapping("internal/find")
    fun find(@RequestParam username: String): UserAuthDto

//    @PostMapping("find/pinfl")
//    fun findByPinfl(@RequestParam pinfl: String): UserAuthDto

    @GetMapping("internal/is-active")
    fun isActive(@RequestParam username: String): Boolean
}

interface TokenService {
    fun deleteByUsername(username: String)
    fun delete()
}

@Service
class TokenServiceImpl(private val repository: AccessTokenRepository) : TokenService {

    override fun deleteByUsername(username: String) {
        repository.deleteByUsername(username)
    }

    override fun delete() {
        repository.deleteByUsername(currentUserName())
    }
}
