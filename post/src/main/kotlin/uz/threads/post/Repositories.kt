package uz.threads.post

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): Page<T>
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>, entityManager: EntityManager,
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllNotDeleted(pageable: Pageable): Page<T> = findAll(isNotDeletedSpecification, pageable)

    @Transactional
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }
}

interface ThreadRepository : BaseRepository<Thread> {
    @Query(
        value = """select t.id,
       t.text,
       t.owner_id,
       (select count(id)
        from t_post."like"
        where thread_id = t.id) as countLikes,
       (select count(id)
        from t_post.reply_content
        where thread_id = t.id) as countReplies,
       t.created_date,
       t.permission_for_reply_id
from threads.t_post.thread as t
         join t_post.seen_thread st on st.thread_id != t.id or st.user_id != :userId
where t.owner_id in (:ids)
group by t.id, t.text, t.created_date, t.permission_for_reply_id, t.owner_id
order by t.created_date""", nativeQuery = true
    )
    fun getThreadsByOwner(
        @Param(value = "userId") userId: Long,
        @Param(value = "ids") ids: List<Long>,
        pageable: Pageable
    ): Page<ThreadGetDtoInterface>?

    fun existsByIdAndDeletedFalse(id: Long): Boolean
}

interface PermissionForReplyRepository : BaseRepository<PermissionForReply> {
    fun findByPermissionForReplyNameAndDeletedFalse(permissionForReplyName: PermissionForReplyName): PermissionForReply
    fun existsByIdAndDeletedFalse(id: Long): Boolean
    fun existsByPermissionForReplyNameAndDeletedFalse(permissionForReplyName: PermissionForReplyName): Boolean
}

interface AttachmentRepository : BaseRepository<Attachment> {
    fun findAllByThreadIdAndDeletedFalse(threadId: Long): List<Attachment>?
    fun findAllByReplyIdAndDeletedFalse(replyId: Long): List<Attachment>?
}

interface AttachmentContentRepository : BaseRepository<AttachmentContent> {
    fun findByAttachmentAndDeletedFalse(attachment: Attachment): AttachmentContent?
}

interface LikeRepository : BaseRepository<Like> {
    fun findAllByThreadIdAndDeletedFalse(threadId: Long, pageable: Pageable): Page<Like>?
    fun findAllByReplyIdAndDeletedFalse(replyId: Long, pageable: Pageable): Page<Like>?
    fun findByThreadIdAndUserIdAndDeletedFalse(threadId: Long, userId: Long): Like?
    fun findByReplyIdAndUserIdAndDeletedFalse(replyId: Long, userId: Long): Like?
}

interface ReplyRepository : BaseRepository<Reply> {
    fun existsByIdAndDeletedFalse(id: Long): Boolean
}

interface ReplyNodeRepository : BaseRepository<ReplyNode> {
    fun findByValue(value: Reply): ReplyNode?
    fun existsByNextValue(nextValue: Reply): Boolean
}

interface ReplyContentRepository : BaseRepository<ReplyContent> {
    fun findAllByThreadIdAndDeletedFalse(threadId: Long, pageable: Pageable): Page<ReplyContent>?
    fun findAllByParentReplyIdAndDeletedFalse(parentReplyId: Long, pageable: Pageable): Page<ReplyContent>?
    fun findByReplyNodeValue(replyNodeValue: Reply): ReplyContent?
}

interface SeenThreadRepository : BaseRepository<SeenThread> {

}