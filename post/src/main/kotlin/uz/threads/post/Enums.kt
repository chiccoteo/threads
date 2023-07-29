package uz.threads.post

enum class PermissionForReplyName{
    ANYONE,
    FOLLOWERS,
    MENTIONS
}

enum class ErrorCode(val code: Int) {
    THREAD_NOT_FOUND(200),
    FILE_IS_EMPTY(201),
    USER_NOT_FOUND(202),
    PERMISSION_FOR_REPLY_NOT_FOUND(203),
    REPLY_NOT_FOUND(204),
    ATTACHMENT_NOT_FOUND(205),
    GENERAL_API_EXCEPTION(206)
}