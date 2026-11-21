package tn.smartrecruit.data.model

/** Roles known by auth-service. Enum + companion object (course: enum, companion object). */
enum class Role {
    CANDIDAT, RECRUTEUR, ADMIN, UNKNOWN;

    companion object {
        fun from(value: String?): Role {
            val clean = value?.removePrefix("ROLE_")
            return entries.firstOrNull { it.name.equals(clean, ignoreCase = true) } ?: UNKNOWN
        }
    }
}
