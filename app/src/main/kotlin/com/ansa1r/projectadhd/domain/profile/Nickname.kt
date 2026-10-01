package com.ansa1r.projectadhd.domain.profile

object Nickname {
    const val MAX_LENGTH = 32
    fun valid(value: String): Boolean {
        val text = value.trim()
        return text.isNotEmpty() && text.codePointCount(0, text.length) <= MAX_LENGTH && text.none { it.isISOControl() }
    }
    fun normalize(value: String): String {
        require(valid(value))
        return value.trim()
    }
    fun restore(value: String?): String? = value?.takeIf(::valid)?.trim()
}
