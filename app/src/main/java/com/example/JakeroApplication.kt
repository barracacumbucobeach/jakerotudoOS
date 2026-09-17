package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import com.example.data.repository.OrdemRepository
import com.example.data.sync.SyncManager
import com.example.service.LembreteWorker
import com.example.service.NotificationHelper

class JakeroApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var syncManager: SyncManager
        private set

    lateinit var repository: OrdemRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getDatabase(this)
        preferencesManager = PreferencesManager(this)
        syncManager = SyncManager(this, database, preferencesManager)
        repository = OrdemRepository(database, syncManager)

        try {
            NotificationHelper.criarCanalNotificacao(this)
        } catch (_: Exception) { }

        try {
            LembreteWorker.agendarVerificacaoDiaria(this)
        } catch (_: Exception) { }

        try {
            syncManager.start()
        } catch (_: Exception) { }
    }

    companion object {
        lateinit var instance: JakeroApplication
            private set
    }
}
