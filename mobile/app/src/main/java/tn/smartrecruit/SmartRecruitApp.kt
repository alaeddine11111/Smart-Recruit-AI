package tn.smartrecruit

import android.app.Application
import tn.smartrecruit.di.AppContainer

class SmartRecruitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
