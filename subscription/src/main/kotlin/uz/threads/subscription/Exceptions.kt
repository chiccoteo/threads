package uz.threads.subscription

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource
import java.util.*

sealed class SubscriptionServiceException(message: String? = null) : RuntimeException(message) {
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

class UserNotFoundException : SubscriptionServiceException() {
    override fun errorType() = ErrorCode.USER_NOT_FOUND
}

class SubscriptionNotFoundException : SubscriptionServiceException() {
    override fun errorType() = ErrorCode.SUBSCRIPTION_NOT_FOUND
}

class GeneralApiException(val msg: String) : SubscriptionServiceException() {
    override fun errorType(): ErrorCode = ErrorCode.GENERAL_API_EXCEPTION
}

class FeignErrorException(val code: Int?, val errorMessage: String?) : SubscriptionServiceException() {
    override fun errorType() = ErrorCode.GENERAL_API_EXCEPTION
}