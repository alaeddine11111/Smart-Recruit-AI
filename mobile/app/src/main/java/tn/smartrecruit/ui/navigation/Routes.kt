package tn.smartrecruit.ui.navigation

object Routes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val CHOOSE_ROLE = "choose_role"
    const val LOGIN = "login"
    const val REGISTER = "register/{role}"
    const val PENDING = "pending"
    const val CANDIDATE_HOME = "candidate"
    const val RECRUITER_HOME = "recruiter"

    fun register(role: String) = "register/$role"   // role = "candidate" | "recruiter"
}
