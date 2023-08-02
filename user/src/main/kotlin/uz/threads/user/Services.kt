package uz.threads.user

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import javax.persistence.EntityManager

interface CategoryService {
    fun create(name: String)
    fun update(dto: CategoryDto)
    fun get(pageable: Pageable): Page<CategoryDto>
    fun getById(id: Long): CategoryDto
    fun delete(id: Long)
}

interface UserService {
    fun create(dto: UserCreateDto)
    fun update(id: Long, dto: UserUpdateDto)
    fun get(pageable: Pageable): Page<UserGetDto>
    fun getById(id: Long): UserGetDto
    fun delete(id: Long)
    fun existsById(id: Long): Boolean
    fun findByUsername(username: String): UserAuthDto
    fun getUser(): UserGetDto
    fun isActive(username: String): Boolean
}

@FeignClient(name = "subscription")
interface SubscriptionService {
    @DeleteMapping("internal/{userId}")
    fun deleteSubscriptions(@PathVariable userId: Long)
}

@Service
class CategoryServiceImpl(
    private val repo: CategoryRepository
) : CategoryService {
    override fun create(name: String) {
        if (repo.existsByNameAndDeletedFalse(name))
            throw CategoryAlreadyExistsException()
        repo.save(Category(name))
    }

    override fun update(dto: CategoryDto) {
        repo.findByIdAndDeletedFalse(dto.id)?.run {
            if (repo.existsByNameAndDeletedFalse(dto.name))
                throw CategoryAlreadyExistsException()
            name = dto.name
            repo.save(this)
        } ?: throw CategoryNotFoundException()
    }

    override fun get(pageable: Pageable) = repo.findAllNotDeleted(pageable).map {
        CategoryDto.toDto(it)
    }

    override fun getById(id: Long): CategoryDto {
        repo.findByIdAndDeletedFalse(id)?.run {
            return CategoryDto.toDto(this)
        } ?: throw CategoryNotFoundException()
    }

    override fun delete(id: Long) {
        repo.trash(id) ?: throw CategoryNotFoundException()
    }
}

@Service
class UserServiceImpl(
    private val userRepo: UserRepository,
    private val roleRepo: RoleRepository,
    private val categoryRepo: CategoryRepository,
    private val genderRepo: GenderRepository,
    private val entityManager: EntityManager,
    private val subscriptionService: SubscriptionService,
    private val passwordEncoder: BCryptPasswordEncoder
) : UserService {
    override fun create(dto: UserCreateDto) {
        if (userRepo.existsByUsernameAndDeletedFalse(dto.username))
            throw UsernameAlreadyExistsException()
        if (dto.email == null && dto.phoneNumber == null)
            throw EmailOrPhoneNotNullException()

        dto.phoneNumber?.let {
            if (it.startsWith("+"))
                dto.phoneNumber = it.substring(1)
        }
        userRepo.save(dto.toEntity(passwordEncoder.encode(dto.password), roleRepo.findByNameAndDeletedFalse(RoleName.USER)!!))
    }

    override fun update(id: Long, dto: UserUpdateDto) {
        val user = userRepo.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        dto.run {
            profileCategoryId?.let {
                if (!categoryRepo.existsByIdAndDeletedFalse(it))
                    throw CategoryNotFoundException()
                user.profileCategory = entityManager.getReference(Category::class.java, it)
            }
            username?.let {
                if (userRepo.existsByUsernameAndDeletedFalse(it))
                    throw UsernameAlreadyExistsException()
                user.username = it
            }
            genderId?.let {
                if (!genderRepo.existsByIdAndDeletedFalse(it))
                    throw GenderNotFoundException()
                user.gender = entityManager.getReference(Gender::class.java, it)
            }
            name?.let {
                user.name = it
            }
            bio?.let {
                user.bio = it
            }
            phoneNumber?.let {
                user.phoneNumber = it
            }
            email?.let {
                user.email = it
            }
            password?.let {
                user.password = it
            }
            userRepo.save(user)
        }
    }

    override fun get(pageable: Pageable) = userRepo.findAllNotDeleted(pageable).map { UserGetDto.toDto(it) }

    override fun getById(id: Long): UserGetDto {
        val user = userRepo.findByIdAndDeletedFalse(id) ?: throw UserNotFoundException()
        return UserGetDto.toDto(user)
    }

    @Transactional
    override fun delete(id: Long) {
        userRepo.trash(id) ?: throw UserNotFoundException()
        subscriptionService.deleteSubscriptions(id)
    }

    override fun existsById(id: Long): Boolean {
        return userRepo.existsByIdAndDeletedFalse(id)
    }

    override fun findByUsername(username: String): UserAuthDto {
        return userRepo.findByUsernameAndDeletedFalse(username)?.run {
            UserAuthDto.toDto(this)
        } ?: throw UserNotFoundException()
    }

    override fun getUser(): UserGetDto {
        val user = userRepo.findByIdAndDeletedFalse(userId()) ?: throw UserNotFoundException()
        return UserGetDto.toDto(user)
    }

    override fun isActive(username: String): Boolean {
        return userRepo.findByUsernameAndDeletedFalse(username)?.run {
            this.active
        } ?: throw UserNotFoundException()
    }
}