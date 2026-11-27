package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.KiranaRepository
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.model.OrderStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale

class AdminOrderAlertService : Service(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "AdminAlertService"
        private const val MONITOR_CHANNEL_ID = "kirana_admin_monitor"
        private const val ALARM_CHANNEL_ID = "kirana_new_order_alarm"
        private const val MONITOR_NOTIFICATION_ID = 1001
        private const val ALARM_NOTIFICATION_ID = 1002
        const val ACTION_START = "com.example.action.START_ADMIN_MONITOR"
        const val ACTION_STOP = "com.example.action.STOP_ADMIN_MONITOR"
        const val ACTION_SILENCE = "com.example.action.SILENCE_ALARM"

        fun start(context: Context) {
            val intent = Intent(context, AdminOrderAlertService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AdminOrderAlertService::class.java))
        }

        fun silence(context: Context) {
            val intent = Intent(context, AdminOrderAlertService::class.java).apply {
                action = ACTION_SILENCE
            }
            runCatching { context.startService(intent) }
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var listenerRegistration: ListenerRegistration? = null
    private var alarmLoopJob: Job? = null
    private var activeRingtone: Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createChannels()
        tts = TextToSpeech(applicationContext, this)
        val fgNotification = buildMonitorNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                MONITOR_NOTIFICATION_ID,
                fgNotification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(MONITOR_NOTIFICATION_ID, fgNotification)
        }
        startOrderListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlarmLoop()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SILENCE -> {
                stopAlarmLoop()
            }
        }
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val res = tts?.setLanguage(Locale.Builder().setLanguage("en").setRegion("IN").build())
            ttsReady = res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsReady) {
                tts?.setLanguage(Locale.US)
                ttsReady = true
            }
            tts?.setSpeechRate(0.95f)
        }
    }

    private fun startOrderListener() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            stopSelf()
            return
        }
        val db = KiranaRepository.createConfiguredFirestore(applicationContext)
        listenerRegistration?.remove()
        listenerRegistration = db.collection("orders")
            .whereEqualTo("adminAcknowledged", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    handleFirestoreError(error, OperationType.LIST, "orders")
                    return@addSnapshotListener
                }
                val unacknowledgedActive = snapshot?.documents?.count { doc ->
                    val status = doc.getString("status")
                    status == OrderStatus.PLACED.name || status == OrderStatus.WAITING_PAYMENT_CONFIRMATION.name
                } ?: 0

                if (unacknowledgedActive > 0) {
                    startAlarmLoop(unacknowledgedActive)
                } else {
                    stopAlarmLoop()
                }
            }
    }

    private fun startAlarmLoop(pendingCount: Int) {
        postAlarmNotification(pendingCount)
        if (alarmLoopJob?.isActive == true) return

        acquireWakeLock()
        alarmLoopJob = serviceScope.launch {
            while (isActive) {
                triggerLoudAlertAndSpeech()
                delay(6500L)
            }
        }
    }

    private fun triggerLoudAlertAndSpeech() {
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            if (activeRingtone == null && alarmUri != null) {
                activeRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)?.apply {
                    audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
            }
            activeRingtone?.let { ringtone ->
                if (!ringtone.isPlaying) ringtone.play()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Ringtone play error: ${e.message}")
        }

        serviceScope.launch {
            delay(1200L)
            try {
                activeRingtone?.stop()
            } catch (_: Exception) { }
            if (ttsReady) {
                val params = Bundle().apply {
                    putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_ALARM)
                    putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                }
                tts?.speak(
                    "Sir, new order has come",
                    TextToSpeech.QUEUE_FLUSH,
                    params,
                    "kirana_new_order_utterance"
                )
            }
        }
    }

    private fun stopAlarmLoop() {
        alarmLoopJob?.cancel()
        alarmLoopJob = null
        runCatching { activeRingtone?.stop() }
        runCatching { tts?.stop() }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(ALARM_NOTIFICATION_ID)
        releaseWakeLock()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GraminKirana::OrderAlertLock").apply {
            acquire(10 * 60 * 1000L)
        }
    }

    private fun releaseWakeLock() {
        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        }
        wakeLock = null
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val monitorChannel = NotificationChannel(
                MONITOR_CHANNEL_ID,
                "Order Monitor (Background)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Gramin Kirana ready to alert on new orders"
            }
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val alarmChannel = NotificationChannel(
                ALARM_CHANNEL_ID,
                "New Order Loud Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud ringing alert when a new grocery order arrives"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 600, 300, 600)
                setSound(
                    alarmUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
            }
            nm.createNotificationChannel(monitorChannel)
            nm.createNotificationChannel(alarmChannel)
        }
    }

    private fun buildMonitorNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, MONITOR_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shop)
            .setContentTitle("Gramin Kirana Admin Active")
            .setContentText("Listening for new village orders in background")
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    private fun postAlarmNotification(pendingCount: Int) {
        val openIntent = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val silenceIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, AdminOrderAlertService::class.java).apply { action = ACTION_SILENCE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, ALARM_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_shop)
            .setContentTitle("Sir, new order has come!")
            .setContentText("$pendingCount new order(s) waiting for confirmation")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .addAction(0, "Silence", silenceIntent)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(ALARM_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stopAlarmLoop()
        listenerRegistration?.remove()
        listenerRegistration = null
        tts?.shutdown()
        tts = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
