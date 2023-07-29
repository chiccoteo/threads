package uz.threads.post

import com.fasterxml.jackson.annotation.JsonInclude
import java.util.*

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseMessage(val code: Int?, val message: String?)

data class ThreadCreateDto(
    val text: String?,
    val ownerId: Long,
    val attachmentsId: List<Long>?,
    val permissionForReplyId: Long?
) {
    fun toEntity(permissionForReply: PermissionForReply) = Thread(text, ownerId, permissionForReply)
}

data class ThreadGetDto(
    val id: Long,
    val text: String?,
    val ownerUsername: String,
    val attachmentsId: List<Long>?,
    val permissionForReplyId: Long,
    val countReplies: Int,
    val countLikes: Int,
    val createdDate: Date
) {
    companion object {
        fun toDto(threadGetDtoInterface: ThreadGetDtoInterface, ownerUsername: String, attachmentsId: List<Long>) =
            threadGetDtoInterface.run {
                ThreadGetDto(
                    getId(),
                    getText(),
                    ownerUsername,
                    attachmentsId,
                    getPermissionForReplyId(),
                    getCountReplies(),
                    getCountLikes(),
                    getCreatedDate()
                )
            }
    }
}

interface ThreadGetDtoInterface {
    fun getId(): Long
    fun getText(): String?
    fun getOwnerId(): Long
    fun getPermissionForReplyId(): Long
    fun getCountReplies(): Int
    fun getCountLikes(): Int
    fun getCreatedDate(): Date
}

data class UserGetDto(
    val id: Long,
    val username: String
)

data class LikeDto(
    val threadId: Long?,
    val replyId: Long?,
    val userId: Long
) {
    fun toEntity(thread: Thread?, reply: Reply?) = Like(thread, reply, userId)
}

data class ReplyDto(
    val id: Long?,
    val text: String?,
    val attachmentsId: List<Long>?,
    val permissionForReplyId: Long?
) {
    companion object {
        fun toDto(reply: Reply, attachmentsId: List<Long>?) = reply.run {
            ReplyDto(id, text, attachmentsId, permissionForReply.id)
        }
    }

    fun toEntity(permissionForReply: PermissionForReply) = Reply(text, permissionForReply)
}

data class ReplyContentDto(
    val threadId: Long?,
    val parentReplyId: Long?,
    val ownerId: Long,
    val replies: List<ReplyDto>
) {
    fun toEntity(thread: Thread?, parentReply: Reply?, replyNode: ReplyNode) =
        ReplyContent(thread, parentReply, replyNode, ownerId)
}

data class ReplyContentGetDto(
    val id: Long,
    val owner: UserGetDto,
    val replies: List<ReplyDto>
) {
    companion object {
        fun toDto(replyContent: ReplyContent, owner: UserGetDto, replies: List<ReplyDto>) = replyContent.run {
            ReplyContentGetDto(id!!, owner, replies)
        }
    }
}