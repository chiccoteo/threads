package uz.threads.subscription

enum class Gender{
    MALE,
    FEMALE,
    CUSTOM
}

enum class ErrorCode(val code: Int) {
    SUBSCRIPTION_NOT_FOUND(300),
    USER_NOT_FOUND(301),
    GENERAL_API_EXCEPTION(302)
}