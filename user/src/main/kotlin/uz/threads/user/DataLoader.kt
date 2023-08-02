package uz.threads.user

import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataLoader(
    private val passwordEncoder: BCryptPasswordEncoder,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val genderRepo: GenderRepository
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        if (!genderRepo.existsByNameAndDeletedFalse(GenderName.CUSTOM))
            genderRepo.save(Gender(GenderName.CUSTOM))
        if (!genderRepo.existsByNameAndDeletedFalse(GenderName.MALE))
            genderRepo.save(Gender(GenderName.MALE))
        if (!genderRepo.existsByNameAndDeletedFalse(GenderName.FEMALE))
            genderRepo.save(Gender(GenderName.FEMALE))

        val developer = roleRepository.findByNameAndDeletedFalse(RoleName.DEVELOPER)
            ?: run { roleRepository.save(Role(RoleName.DEVELOPER)) }
        val admin = roleRepository.findByNameAndDeletedFalse(RoleName.ADMIN)
            ?: run { roleRepository.save(Role(RoleName.ADMIN)) }
        val user = roleRepository.findByNameAndDeletedFalse(RoleName.USER)
            ?: run { roleRepository.save(Role(RoleName.USER)) }


        userRepository.findByUsernameAndDeletedFalse("dev") ?: run {
            userRepository.save(
                User(
                    "998979516170",
                    "chicco@gmail.com",
                    "Developeriddin",
                    "dev",
                    passwordEncoder.encode("12345678"),
                    developer
                )
            )
        }
    }
}