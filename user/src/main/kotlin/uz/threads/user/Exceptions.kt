package uz.threads.user

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import java.util.*

sealed class UserServiceException(message: String? = null) : RuntimeException(message) {
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

class CategoryNotFoundException : UserServiceException() {
    override fun errorType() = ErrorCode.CATEGORY_NOT_FOUND
}

class GenderNotFoundException : UserServiceException() {
    override fun errorType() = ErrorCode.GENDER_NOT_FOUND
}

class CategoryAlreadyExistsException : UserServiceException() {
    override fun errorType() = ErrorCode.CATEGORY_ALREADY_EXISTS
}

class UserNotFoundException : UserServiceException() {
    override fun errorType() = ErrorCode.USER_NOT_FOUND
}

class UsernameAlreadyExistsException : UserServiceException() {
    override fun errorType() = ErrorCode.USERNAME_ALREADY_EXISTS
}

class EmailOrPhoneNotNullException : UserServiceException() {
    override fun errorType() = ErrorCode.EMAIL_OR_PHONE_NOT_NULL
}
