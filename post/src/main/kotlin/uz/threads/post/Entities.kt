package uz.threads.post

import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.data.jpa.repository.Temporal
import java.util.*
import javax.persistence.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdDate: Date? = null,
    @CreatedBy @Column(updatable = false) var createdBy: Long? = null,
    @LastModifiedBy var modifiedBy: Long? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false
)

@Entity
class PermissionForReply(
    @Enumerated(EnumType.STRING) val permissionForReplyName: PermissionForReplyName
) : BaseEntity()

@Entity
class Thread(
    @Column(length = 1024) val text: String? = null,
    @Column(nullable = false) val ownerId: Long,
    @ManyToOne val permissionForReply: PermissionForReply
) : BaseEntity()

@Entity
class Attachment(
    @ManyToOne var thread: Thread?,
    @ManyToOne var reply: Reply?,
    @Column(length = 128, nullable = false) var originalName: String,
    @Column(nullable = false) var size: Long,
    @Column(length = 16, nullable = false) var contentType: String
) : BaseEntity() {
    constructor(originalName: String, size: Long, contentType: String) : this(
        null, null, originalName, size, contentType
    )
}

@Entity
class AttachmentContent(
    @Column(nullable = false) val content: ByteArray,
    @OneToOne val attachment: Attachment
) : BaseEntity()

@Entity
class ReplyContent(
    @ManyToOne val thread: Thread? = null,
    @ManyToOne val parentReply: Reply? = null,
    @OneToOne val replyNode: ReplyNode,
    @Column(nullable = false) val ownerId: Long,
) : BaseEntity()

@Entity
class Reply(
    @Column(length = 1024) val text: String? = null,
    @ManyToOne val permissionForReply: PermissionForReply
) : BaseEntity()

@Entity
class ReplyNode(
    @OneToOne var next: ReplyNode? = null,
    @OneToOne var value: Reply,
) : BaseEntity()

@Entity
class Like(
    @ManyToOne val thread: Thread? = null,
    @ManyToOne val reply: Reply? = null,
    @Column(nullable = false) val userId: Long
) : BaseEntity()

@Entity
class SeenThread(
    @ManyToOne val thread: Thread,
    @Column(nullable = false) val userId: Long
) : BaseEntity()