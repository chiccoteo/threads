package uz.threads.subscription

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

interface SubscriptionService {
    fun create(dto: SubscriptionDto)
    fun getFollowers(userId: Long, pageable: Pageable): Page<UserGetDto>?
    fun getFollowings(userId: Long, pageable: Pageable): Page<UserGetDto>?
    fun deleteSubscriptions(userId: Long)
    fun getFollowersByUserId(userId: Long): List<UserGetDto>?
}

@FeignClient(name = "user", configuration = [Auth2TokenConfiguration::class])
interface UserService {
    @GetMapping("internal/exists/{id}")
    fun existsById(@PathVariable id: Long): Boolean

    @GetMapping("{id}")
    fun getUserById(@PathVariable id: Long): UserGetDto
}

@Service
class SubscriptionServiceImpl(
    private val userService: UserService,
    private val subscriptionRepo: SubscriptionRepository
) : SubscriptionService {

    @Transactional
    override fun create(dto: SubscriptionDto) {
        if (!userService.existsById(dto.userId)) throw UserNotFoundException()
        if (!userService.existsById(dto.followerId)) throw UserNotFoundException()
        subscriptionRepo.save(dto.toEntity())
    }

    override fun getFollowers(userId: Long, pageable: Pageable): Page<UserGetDto>? {
        if (!userService.existsById(userId)) throw UserNotFoundException()
        return subscriptionRepo.findAllByUserIdAndDeletedFalse(userId, pageable)?.map {
            userService.getUserById(it.followerId)
        }
    }

    override fun getFollowings(userId: Long, pageable: Pageable): Page<UserGetDto>? {
        if (!userService.existsById(userId)) throw UserNotFoundException()
        return subscriptionRepo.findAllByFollowerIdAndDeletedFalse(userId, pageable)?.map {
            userService.getUserById(it.userId)
        }
    }

    override fun deleteSubscriptions(userId: Long) {
        subscriptionRepo.findAllByFollowerId(userId)?.map {
            it.deleted = true
            subscriptionRepo.save(it)
        }
        subscriptionRepo.findAllByUserId(userId)?.map {
            it.deleted = true
            subscriptionRepo.save(it)
        }
    }

    override fun getFollowersByUserId(userId: Long): List<UserGetDto>? {
        if (!userService.existsById(userId)) throw UserNotFoundException()
        return subscriptionRepo.findAllByUserIdAndDeletedFalse(userId)?.map {
            userService.getUserById(it.followerId)
        }
    }

}