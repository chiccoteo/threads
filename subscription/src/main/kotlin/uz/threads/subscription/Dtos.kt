package uz.threads.subscription

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class BaseMessage(val code: Int? = null, val message: String? = null)

data class SubscriptionDto(
    val userId: Long,
    val followerId: Long
) {
    fun toEntity() = Subscription(userId, followerId)
}

data class UserGetDto(
    val id: Long,
    val name: String,
    val username: String
)