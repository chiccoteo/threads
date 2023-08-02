package uz.threads.post

import org.springframework.cloud.openfeign.FeignClient
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.multipart.MultipartFile
import javax.persistence.EntityManager

@FeignClient(name = "user", configuration = [Auth2TokenConfiguration::class])
interface UserService {
    @GetMapping("internal/exists/{id}")
    fun existsById(@PathVariable id: Long): Boolean

    @GetMapping("{id}")
    fun getUserById(@PathVariable id: Long): UserGetDto

    @GetMapping("get")
    fun getUser(): UserGetDto
}

@FeignClient(name = "subscription", configuration = [Auth2TokenConfiguration::class])
interface SubscriptionService {
    @GetMapping("internal/{userId}")
    fun getFollowers(@PathVariable userId: Long): List<UserGetDto>?
}

interface ThreadService {
    fun create(dto: ThreadCreateDto)
    fun getByUser(userId: Long, pageable: Pageable): Page<ThreadGetDto>?
    fun delete(id: Long)
}

interface AttachmentService {
    fun upload(file: MultipartFile): Long
    fun getFile(id: Long): ResponseEntity<ByteArray>?
}

interface LikeService {
    fun create(dto: LikeDto)
    fun getLikesByThreadId(threadId: Long, pageable: Pageable): Page<UserGetDto>?
    fun getLikesByReplyId(replyId: Long, pageable: Pageable): Page<UserGetDto>?
    fun delete(dto: LikeDto)
}

interface ReplyService {
    fun create(dto: ReplyContentDto)
    fun getRepliesByThreadId(threadId: Long, pageable: Pageable): Page<ReplyContentGetDto>?
    fun getRepliesByReplyId(replyId: Long, pageable: Pageable): Page<ReplyContentGetDto>?
    fun delete(id: Long)
}

@Service
class ThreadServiceImpl(
    private val threadRepo: ThreadRepository,
    private val permissionForReplyRepo: PermissionForReplyRepository,
    private val attachmentRepo: AttachmentRepository,
    private val userService: UserService,
    private val subscriptionService: SubscriptionService,
    private val entityManager: EntityManager,
    private val seenThreadRepo: SeenThreadRepository
) : ThreadService {

    @Transactional
    override fun create(dto: ThreadCreateDto) {
        if (!userService.existsById(dto.ownerId)) throw UserNotFoundException()
        val savedThread = dto.permissionForReplyId?.let {
            if (!permissionForReplyRepo.existsByIdAndDeletedFalse(it))
                throw PermissionForReplyNotFound()
            threadRepo.save(dto.toEntity(entityManager.getReference(PermissionForReply::class.java, it)))
        } ?: threadRepo.save(
            dto.toEntity(
                permissionForReplyRepo.findByPermissionForReplyNameAndDeletedFalse(
                    PermissionForReplyName.ANYONE
                )
            )
        )
        dto.attachmentsId?.forEach {
            attachmentRepo.findByIdAndDeletedFalse(it)?.run {
                thread = savedThread
                attachmentRepo.save(this)
            } ?: throw AttachmentNotFoundException()
        }
    }

    override fun getByUser(userId: Long, pageable: Pageable): Page<ThreadGetDto>? {
//        if (!userService.existsById(userId)) throw UserNotFoundException()
        val followersId = mutableListOf<Long>()
        val attachmentsId = mutableListOf<Long>()
        subscriptionService.getFollowers(userId)?.forEach {
            followersId.add(it.id)
        }
        return threadRepo.getThreadsByOwner(userId, followersId, pageable).map {
            attachmentRepo.findAllByThreadIdAndDeletedFalse(it.getId())?.forEach { attachment ->
                attachmentsId.add(attachment.id!!)
            }
            seenThread(userId, it.getId())
            ThreadGetDto.toDto(it, userService.getUser().username, attachmentsId)
        }
    }

    private fun seenThread(userId: Long, threadId: Long) {
        val thread = threadRepo.findByIdAndDeletedFalse(threadId) ?: throw ThreadNotFoundException()
        seenThreadRepo.save(SeenThread(thread, userId))
    }

    override fun delete(id: Long) {
        threadRepo.trash(id) ?: throw ThreadNotFoundException()
    }
}

@Service
class AttachmentServiceImpl(
    private val attachmentRepo: AttachmentRepository,
    private val attachmentContentRepo: AttachmentContentRepository,
) : AttachmentService {
    override fun upload(file: MultipartFile): Long {
        if (!file.isEmpty) {
            val attachment = attachmentRepo.save(Attachment(file.originalFilename!!, file.size, file.contentType!!))
            attachmentContentRepo.save(AttachmentContent(file.bytes, attachment))
            return attachment.id!!
        }
        throw FileIsEmptyException()
    }

    override fun getFile(id: Long): ResponseEntity<ByteArray>? {
        val responseEntity = attachmentRepo.findByIdAndDeletedFalse(id)?.let {
            attachmentContentRepo.findByAttachmentAndDeletedFalse(it)?.run {
                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(it.contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.originalName + "\"")
                    .body(content)
            }
        } ?: throw AttachmentNotFoundException()
        return responseEntity
    }
}

@Service
class LikeServiceImpl(
    private val likeRepo: LikeRepository,
    private val threadRepo: ThreadRepository,
    private val replyRepo: ReplyRepository,
    private val seenThreadRepo: SeenThreadRepository,
    private val userService: UserService
) : LikeService {
    override fun create(dto: LikeDto) {
        if (!userService.existsById(dto.userId)) throw UserNotFoundException()
        dto.run {
            val thread = threadId?.run {
                threadRepo.findByIdAndDeletedFalse(this) ?: throw ThreadNotFoundException()
            }
            val reply = replyId?.run {
                replyRepo.findByIdAndDeletedFalse(this) ?: throw ReplyNotFoundException()
            }
            likeRepo.save(toEntity(thread, reply))
            seenThread(userId, threadId!!)
        }
    }

    private fun seenThread(userId: Long, threadId: Long) {
        val thread = threadRepo.findByIdAndDeletedFalse(threadId) ?: throw ThreadNotFoundException()
        seenThreadRepo.save(SeenThread(thread, userId))
    }

    override fun getLikesByThreadId(threadId: Long, pageable: Pageable): Page<UserGetDto>? {
        return likeRepo.findAllByThreadIdAndDeletedFalse(threadId, pageable)?.map {
            userService.getUserById(it.userId)
        }
    }

    override fun getLikesByReplyId(replyId: Long, pageable: Pageable): Page<UserGetDto>? {
        return likeRepo.findAllByReplyIdAndDeletedFalse(replyId, pageable)?.map {
            userService.getUserById(it.userId)
        }
    }

    override fun delete(dto: LikeDto) {
        dto.run {
            threadId?.let {
                likeRepo.findByThreadIdAndUserIdAndDeletedFalse(it, userId)?.run {
                    deleted = true
                    likeRepo.save(this)
                }
            }
            replyId?.let {
                likeRepo.findByReplyIdAndUserIdAndDeletedFalse(it, userId)?.run {
                    deleted = true
                    likeRepo.save(this)
                }
            }
        }
    }

}

@Service
class ReplyServiceImpl(
    private val replyRepo: ReplyRepository,
    private val replyNodeRepo: ReplyNodeRepository,
    private val replyContentRepo: ReplyContentRepository,
    private val threadRepo: ThreadRepository,
    private val permissionForReplyRepo: PermissionForReplyRepository,
    private val attachmentRepo: AttachmentRepository,
    private val userService: UserService
) : ReplyService {

    @Transactional
    override fun create(dto: ReplyContentDto) {
        if (!userService.existsById(dto.ownerId)) throw UserNotFoundException()
        val thread = dto.threadId?.run {
            threadRepo.findByIdAndDeletedFalse(this) ?: throw ThreadNotFoundException()
        }
        val reply = dto.parentReplyId?.run {
            replyRepo.findByIdAndDeletedFalse(this) ?: throw ReplyNotFoundException()
        }
        var replyNode: ReplyNode
        val replies = mutableListOf<Reply>()
        dto.replies.forEach {
            val savedReply = it.permissionForReplyId?.run {
                replyRepo.save(
                    it.toEntity(
                        permissionForReplyRepo.findByIdAndDeletedFalse(this) ?: throw PermissionForReplyNotFound()
                    )
                )
            } ?: replyRepo.save(
                it.toEntity(
                    permissionForReplyRepo.findByPermissionForReplyNameAndDeletedFalse(
                        PermissionForReplyName.ANYONE
                    )
                )
            )
            it.attachmentsId?.forEach { attachmentId ->
                attachmentRepo.findByIdAndDeletedFalse(attachmentId)?.run {
                    this.reply = savedReply
                    attachmentRepo.save(this)
                } ?: throw AttachmentNotFoundException()
            }
            replies.add(savedReply)
        }
        var i = replies.size - 1
        replyNode = replyNodeRepo.save(ReplyNode(null, replies[i]))
        i -= 1
        while (i >= 0) {
            replyNode = replyNodeRepo.save(ReplyNode(replyNode, replies[i]))
            i -= 1
        }
        replyContentRepo.save(dto.toEntity(thread, reply, replyNode))
    }

    override fun getRepliesByThreadId(threadId: Long, pageable: Pageable): Page<ReplyContentGetDto>? {
        if (!threadRepo.existsByIdAndDeletedFalse(threadId)) throw ThreadNotFoundException()
        return replyContentRepo.findAllByThreadIdAndDeletedFalse(threadId, pageable)?.map {
            var replyNode = it.replyNode
            val replies = mutableListOf<ReplyDto>()
            val attachmentsId = mutableListOf<Long>()
            attachmentRepo.findAllByReplyIdAndDeletedFalse(replyNode.value.id!!)?.forEach { attachment ->
                attachmentsId.add(attachment.id!!)
            }
            if (replyNode.value.deleted)
                ReplyContentGetDto.toDto(it, userService.getUserById(it.ownerId), replies)
            else {
                replies.add(ReplyDto.toDto(replyNode.value, attachmentsId))
                while (replyNode.next != null) {
                    replyNode = replyNode.next!!
                    if (replyNode.value.deleted)
                        break
                    replies.add(ReplyDto.toDto(replyNode.value, attachmentsId))
                }
                ReplyContentGetDto.toDto(it, userService.getUserById(it.ownerId), replies)
            }
        }
    }

    override fun getRepliesByReplyId(replyId: Long, pageable: Pageable): Page<ReplyContentGetDto>? {
        if (!replyRepo.existsByIdAndDeletedFalse(replyId)) throw ReplyNotFoundException()
        return replyContentRepo.findAllByParentReplyIdAndDeletedFalse(replyId, pageable)?.map {
            var replyNode = it.replyNode
            val replies = mutableListOf<ReplyDto>()
            val attachmentsId = mutableListOf<Long>()
            attachmentRepo.findAllByReplyIdAndDeletedFalse(replyNode.value.id!!)?.forEach { attachment ->
                attachmentsId.add(attachment.id!!)
            }
            if (replyNode.value.deleted)
                ReplyContentGetDto.toDto(it, userService.getUserById(it.ownerId), replies)
            else {
                replies.add(ReplyDto.toDto(replyNode.value, attachmentsId))
                while (replyNode.next != null) {
                    replyNode = replyNode.next!!
                    if (replyNode.value.deleted)
                        break
                    replies.add(ReplyDto.toDto(replyNode.value, attachmentsId))
                }
                ReplyContentGetDto.toDto(it, userService.getUserById(it.ownerId), replies)
            }
        }
    }

    override fun delete(id: Long) {
        val reply = replyRepo.trash(id) ?: throw ReplyNotFoundException()
        if (!replyNodeRepo.existsByNextValue(reply))
            replyContentRepo.findByReplyNodeValue(reply)?.run {
                deleted = true
                replyContentRepo.save(this)
            }
        replyNodeRepo.findByValue(reply)?.run {
            deleted = true
            replyNodeRepo.save(this)
            while (next != null) {
                next?.deleted = true
                replyNodeRepo.save(next!!)
                next?.value?.deleted = true
                replyRepo.save(next?.value!!)
                next = next?.next
            }
        }
    }
}