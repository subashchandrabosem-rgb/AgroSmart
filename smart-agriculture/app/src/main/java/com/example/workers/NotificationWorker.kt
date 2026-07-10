package com.example.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.database.AppDatabase
import com.example.models.Notification

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: "AgroSmart Notification"
        val message = inputData.getString("message") ?: ""
        val type = inputData.getString("type") ?: "Order"
        val link = inputData.getString("link")

        // 1. Insert into local Room database
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            db.notificationDao().insertNotification(
                Notification(title = title, message = message, type = type)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Display System Status Bar Notification
        showSystemNotification(title, message, link)

        return Result.success()
    }

    private fun showSystemNotification(title: String, message: String, link: String?) {
        val channelId = "agrosmart_notifications"
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AgroSmart Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time updates for orders and crop diagnostics"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = if (!link.isNullOrEmpty()) {
            Intent(Intent.ACTION_VIEW, Uri.parse(link)).apply {
                setClass(applicationContext, com.example.MainActivity::class.java)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            Intent(applicationContext, com.example.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }
}
