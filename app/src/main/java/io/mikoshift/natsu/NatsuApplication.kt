package io.mikoshift.natsu

import android.app.Application
import io.mikoshift.natsu.di.AppContainer

class NatsuApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
