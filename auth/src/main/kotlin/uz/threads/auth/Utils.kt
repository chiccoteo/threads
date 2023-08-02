package uz.threads.auth

import org.springframework.security.core.context.SecurityContextHolder

fun getUserId() = SecurityContextHolder.getContext().getUserId()

fun currentUserName() = SecurityContextHolder.getContext().authentication.principal as String
