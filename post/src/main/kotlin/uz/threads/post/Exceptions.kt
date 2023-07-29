package uz.threads.post

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import java.util.*

sealed class PostServiceException(message: String? = null) : RuntimeException(message) {
    abstract fun errorType(): ErrorCode

    fun getErrorMessage(errorMessageSource: ResourceBundleMessageSource, vararg array: Any?): BaseMessage {
        return BaseMessage(
            errorType().code,
            errorMessageSource.getMessage(
                errorType().toString(),
                array,
                Locale(LocaleContextHolder.getLocale().language)
            )
        )
    }
}

class ThreadNotFoundException : PostServiceException() {
    override fun errorType() = ErrorCode.THREAD_NOT_FOUND
}

class ReplyNotFoundException : PostServiceException() {
    override fun errorType() = ErrorCode.REPLY_NOT_FOUND
}

class AttachmentNotFoundException : PostServiceException() {
    override fun errorType() = ErrorCode.ATTACHMENT_NOT_FOUND
}

class UserNotFoundException : PostServiceException() {
    override fun errorType() = ErrorCode.USER_NOT_FOUND
}

class PermissionForReplyNotFound : PostServiceException() {
    override fun errorType() = ErrorCode.PERMISSION_FOR_REPLY_NOT_FOUND
}

class FileIsEmptyException : PostServiceException() {
    override fun errorType() = ErrorCode.FILE_IS_EMPTY
}

class GeneralApiException(val msg: String) : PostServiceException() {
    override fun errorType(): ErrorCode = ErrorCode.GENERAL_API_EXCEPTION
}

class FeignErrorException(val code: Int?, val errorMessage: String?) : PostServiceException() {
    override fun errorType() = ErrorCode.GENERAL_API_EXCEPTION
}