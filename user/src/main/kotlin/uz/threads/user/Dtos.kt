package uz.threads.user

import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank
import javax.validation.constraints.Size


data class BaseMessage(val code: Int, val message: String?)

data class CategoryDto(
    val id: Long,
    @field:NotBlank(message = "name field is not blank") val name: String
) {
    companion object {
        fun toDto(category: Category) = category.run {
            CategoryDto(id!!, name)
        }
    }
}

data class UserCreateDto(
    @field:Size(
        message = "must be 9 and 13 between length of phoneNumber",
        min = 9,
        max = 13
    ) var phoneNumber: String?,
    @field:Email(message = "must be email formed") val email: String?,
    @field:NotBlank(message = "name field is not blank") val name: String,
    @field:NotBlank(message = "username field is not blank") val username: String,
    @field:NotBlank(message = "password field is not blank") val password: String
) {
    fun toEntity() = User(phoneNumber, email, name, username, password)
}

data class UserUpdateDto(
    @field:Size(
        message = "must be 9 and 13 between length of phoneNumber",
        min = 9,
        max = 13
    ) val phoneNumber: String?,
    @field:Email(message = "must be email formed") val email: String?,
    val bio: String?,
    val profileCategoryId: Long?,
    val genderId: Long?,
    val name: String?,
    val username: String?,
    val password: String?
)

data class UserGetDto(
    val id: Long,
    val phoneNumber: String?,
    val email: String?,
    val bio: String?,
    val category: CategoryDto?,
    val gender: String?,
    val name: String,
    val username: String
) {
    companion object {
        fun toDto(user: User) = user.run {
            UserGetDto(
                id!!,
                phoneNumber,
                email,
                bio,
                profileCategory?.let { CategoryDto.toDto(it) },
                gender.toString(),
                name,
                username
            )
        }
    }
}