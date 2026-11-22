package tn.smartrecruit.ui.screens.recruiter

val CONTRACT_TYPES = listOf("CDI", "CDD", "STAGE", "FREELANCE", "ALTERNANCE")
val OFFER_STATUSES = listOf("DRAFT", "PUBLISHED", "CLOSED")

fun statusLabel(status: String): String = when (status) {
    "DRAFT" -> "Brouillon"
    "PUBLISHED" -> "Publiée"
    "CLOSED" -> "Clôturée"
    else -> status
}

fun formatSalary(min: Double?, max: Double?, currency: String?): String? {
    if (min == null && max == null) return null
    fun n(v: Double) = if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
    val unit = currency ?: "TND"
    return when {
        min != null && max != null -> "${n(min)} - ${n(max)} $unit"
        min != null -> "À partir de ${n(min)} $unit"
        else -> "Jusqu'à ${n(max ?: 0.0)} $unit"
    }
}

/** "2026-11-07T10:15:30Z" -> "2026-11-07" */
fun shortDate(iso: String?): String? = iso?.take(10)

fun numberToText(v: Double?): String =
    if (v == null) "" else if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
