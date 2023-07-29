package uz.threads.user

import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedDate
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
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var modifiedDate: Date? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false
)

@Entity
class Category(
    @Column(length = 128, nullable = false, unique = true) var name: String
) : BaseEntity()

@Entity
class Gender(
    @Enumerated(EnumType.STRING) val name: GenderName
) : BaseEntity()

@Entity(name = "threads_user")
class User(
    @Column(length = 12) var phoneNumber: String? = null,
    @Column(length = 128) var email: String? = null,
    @Column(length = 128) var bio: String? = null,
    @ManyToOne var profileCategory: Category? = null,
    @ManyToOne var gender: Gender? = null,
    @Column(length = 128, nullable = false) var name: String,
    @Column(length = 128, unique = true, nullable = false) var username: String,
    @Column(length = 128, nullable = false) var password: String
) : BaseEntity() {
    constructor(phoneNumber: String?, email: String?, name: String, username: String, password: String) : this(
        phoneNumber, email, null, null, null, name, username, password
    )
}
