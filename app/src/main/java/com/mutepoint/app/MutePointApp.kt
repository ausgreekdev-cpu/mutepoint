package com.mutepoint.app

import android.app.Application
import com.mutepoint.app.core.automation.DefaultWorkflows
import com.mutepoint.app.core.automation.engine.WorkflowTriggerListener
import dagger.hilt.android.HiltAndroidApp
import io.sentry.android.core.SentryAndroid
import javax.inject.Inject

@HiltAndroidApp
class MutePointApp : Application() {

    @Inject
    lateinit var workflowTriggerListener: WorkflowTriggerListener

    @Inject
    lateinit var defaultWorkflows: DefaultWorkflows

    override fun onCreate() {
        super.onCreate()
        runCatching { System.loadLibrary("sqlcipher") }

        if (!BuildConfig.DEBUG) {
            SentryAndroid.init(this) { options ->
                options.dsn = BuildConfig.SENTRY_DSN
                options.isEnableAutoSessionTracking = true
                options.tracesSampleRate = 0.2
            }
        }

        workflowTriggerListener.start()

        // Create default workflows on first launch
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            defaultWorkflows.createDefaults()
        }
    }
}