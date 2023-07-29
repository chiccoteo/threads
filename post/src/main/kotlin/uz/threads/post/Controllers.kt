package uz.threads.post

import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@ControllerAdvice
class ExceptionHandlers(
    private val errorMessageSource: ResourceBundleMessageSource
) {
    @ExceptionHandler(PostServiceException::class)
    fun handleException(exception: PostServiceException): ResponseEntity<*> {
        return when (exception) {
            is ThreadNotFoundException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is FileIsEmptyException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is PermissionForReplyNotFound -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is UserNotFoundException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is ReplyNotFoundException -> ResponseEntity.badRequest().body(
                exception.getErrorMessage(errorMessageSource, emptyArray<Any>())
            )

            is AttachmentNotFoundException -> ResponseEntity.badRequest().body(
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
@RequestMapping("thread")
class ThreadController(
    private val service: ThreadService
) {
    @PostMapping
    fun create(@RequestBody dto: ThreadCreateDto) = service.create(dto)

    @GetMapping("{userId}")
    fun getByUser(@PathVariable userId: Long, pageable: Pageable): Page<ThreadGetDto>? =
        service.getByUser(userId, pageable)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}

@RestController
@RequestMapping("attachment")
class AttachmentController(
    private val service: AttachmentService
) {
    @PostMapping
    fun upload(@RequestParam file: MultipartFile) = service.upload(file)

    @GetMapping("{id}")
    fun getFile(@PathVariable id: Long): ResponseEntity<ByteArray>? = service.getFile(id)
}

@RestController
@RequestMapping("like")
class LikeController(
    private val service: LikeService
) {
    @PostMapping
    fun create(@RequestBody dto: LikeDto) = service.create(dto)

    @GetMapping("thread/{threadId}")
    fun getLikesByThreadId(@PathVariable threadId: Long, pageable: Pageable): Page<UserGetDto>? =
        service.getLikesByThreadId(threadId, pageable)

    @GetMapping("reply/{replyId}")
    fun getLikesByReplyId(@PathVariable replyId: Long, pageable: Pageable): Page<UserGetDto>? =
        service.getLikesByReplyId(replyId, pageable)

    @DeleteMapping
    fun delete(@RequestBody dto: LikeDto) = service.delete(dto)
}

@RestController
@RequestMapping("reply")
class ReplyController(
    private val service: ReplyService
) {
    @PostMapping
    fun create(@RequestBody dto: ReplyContentDto) = service.create(dto)

    @GetMapping("threadId{threadId}")
    fun getRepliesByThreadId(@PathVariable threadId: Long, pageable: Pageable): Page<ReplyContentGetDto>? =
        service.getRepliesByThreadId(threadId, pageable)

    @GetMapping("replyId{replyId}")
    fun getRepliesByReplyId(@PathVariable replyId: Long, pageable: Pageable): Page<ReplyContentGetDto>? =
        service.getRepliesByReplyId(replyId, pageable)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}
