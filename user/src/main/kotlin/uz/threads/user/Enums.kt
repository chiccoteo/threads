package uz.threads.user

enum class GenderName {
    MALE,
    FEMALE,
    CUSTOM
}

enum class ErrorCode(val code: Int) {
    CATEGORY_NOT_FOUND(101),
    CATEGORY_ALREADY_EXISTS(102),
    USER_NOT_FOUND(103),
    USERNAME_ALREADY_EXISTS(104),
    EMAIL_OR_PHONE_NOT_NULL(105),
    GENDER_NOT_FOUND(106)
}