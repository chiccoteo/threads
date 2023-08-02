package uz.threads.auth

import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataLoader(
    private val passwordEncoder: BCryptPasswordEncoder,
    private val authClientRepository: AuthClientRepository
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        authClientRepository.findByClientId("default") ?: run {
            authClientRepository.save(
                AuthClient(
                    "default",
                    passwordEncoder.encode("default"),
                    1000000000,
                    1000000000,
                    mutableSetOf("password", "refresh_token"),
                    mutableSetOf("web")
                )
            )
        }

    }

}