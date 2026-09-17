package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AvisoItem
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID = "jakero_lembretes"
    const val CHANNEL_NAME = "Lembretes e Cobranças"

    fun criarCanalNotificacao(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificações de entregas e cobranças de Ordens de Serviço"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun enviarNotificacaoLembretes(context: Context, avisos: List<AvisoItem>) {
        if (avisos.isEmpty()) return

        criarCanalNotificacao(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val urgentes = avisos.count { it.isUrgente }
        val titulo = if (urgentes > 0) {
            "Jakero Tudo: $urgentes pendência(s) urgente(s)"
        } else {
            "Jakero Tudo: ${avisos.size} lembrete(s)"
        }

        val corpo = avisos.take(3).joinToString("\n") { it.descricao }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.jakero_logo)
            .setContentTitle(titulo)
            .setContentText(avisos.first().descricao)
            .setStyle(NotificationCompat.BigTextStyle().bigText(corpo))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(1001, builder.build())
        } catch (_: SecurityException) {}
    }
}
