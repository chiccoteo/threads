package uz.threads.subscription

import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@ControllerAdvice
class ExceptionHandlers(
    private val errorMessageSource: ResourceBundleMessageSource
) {
    @ExceptionHandler(SubscriptionServiceException::class)
    fun handleException(exception: SubscriptionServiceException): ResponseEntity<*> {
        return when (exception) {
            is UserNotFoundException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is SubscriptionNotFoundException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is FeignErrorException -> ResponseEntity.badRequest().body(
                BaseMessage(exception.code, exception.errorMessage)
            )

            is GeneralApiException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, exception.msg)
            )
        }
    }
}

@RestController
class SubscriptionController(
    private val service: SubscriptionService
) {
    @PostMapping
    fun create(@RequestBody dto: SubscriptionDto) = service.create(dto)

    @GetMapping("followers")
    fun getFollowers(@RequestParam(name = "userId") userId: Long, pageable: Pageable): Page<UserGetDto>? =
        service.getFollowers(userId, pageable)

    @GetMapping("followings")
    fun getFollowings(@RequestParam(name = "userId") userId: Long, pageable: Pageable): Page<UserGetDto>? =
        service.getFollowings(userId, pageable)
}

@RestController
@RequestMapping("internal")
class SubscriptionInternalController(
    private val service: SubscriptionService
) {
    @GetMapping("{userId}")
    fun getFollowersByUserId(@PathVariable userId: Long): List<UserGetDto>? = service.getFollowersByUserId(userId)

    @DeleteMapping("{userId}")
    fun deleteSubscriptions(@PathVariable userId: Long) = service.deleteSubscriptions(userId)
}
