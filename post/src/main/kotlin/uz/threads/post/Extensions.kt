package uz.threads.post

import org.springframework.security.core.context.SecurityContext
import org.springframework.security.oauth2.provider.OAuth2Authentication

fun SecurityContext.getUserId(): Long? {
    if (authentication is OAuth2Authentication) {
        val details = (authentication as OAuth2Authentication).userAuthentication.details as Map<*, *>
        val userId = details["userId"]
        if (userId is Int) return userId.toLong()
        return userId as Long
    }
    return null
}