package org.fossify.home.views

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.provider.AlarmClock
import android.text.format.DateFormat
import android.view.MotionEvent
import android.view.ViewConfiguration
import org.fossify.home.activities.MainActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

@SuppressLint("ViewConstructor")
class BuiltInClockWidgetView(context: Context) : MyAppWidgetHostView(context) {

    private val timeStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val timeFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val dateStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLACK
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val dateFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private var timeText = ""
    private var dateText = ""
    private var isReceiverRegistered = false

    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            updateTime()
            postInvalidate()
        }
    }

    private var touchDownX = 0f
    private var touchDownY = 0f
    private var touchDownTime = 0L

    init {
        setWillNotDraw(false)
        updateTime()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerTimeReceiver()
        updateTime()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        unregisterTimeReceiver()
    }

    private fun registerTimeReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
            context.registerReceiver(timeReceiver, filter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterTimeReceiver() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(timeReceiver)
            } catch (ignored: Exception) {
            }
            isReceiverRegistered = false
        }
    }

    fun updateTime() {
        val now = Calendar.getInstance().time
        val is24Hour = DateFormat.is24HourFormat(context)
        val timeFormat = if (is24Hour) "HH:mm" else "h:mm"
        timeText = SimpleDateFormat(timeFormat, Locale.getDefault()).format(now)

        val skeleton = "EEEE, MMMM d"
        val pattern = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            DateFormat.getBestDateTimePattern(Locale.getDefault(), skeleton)
        } else {
            "EEEE, MMMM d"
        }
        dateText = SimpleDateFormat(pattern, Locale.getDefault()).format(now)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        val centerX = width / 2f

        val timeTextSize = (height * 0.44f).coerceIn(36f, 130f)
        val dateTextSize = (height * 0.16f).coerceIn(16f, 40f)

        val timeStrokeWidth = (timeTextSize * 0.12f).coerceIn(4f, 16f)
        val dateStrokeWidth = (dateTextSize * 0.14f).coerceIn(2f, 6f)

        timeStrokePaint.textSize = timeTextSize
        timeStrokePaint.strokeWidth = timeStrokeWidth

        timeFillPaint.textSize = timeTextSize

        dateStrokePaint.textSize = dateTextSize
        dateStrokePaint.strokeWidth = dateStrokeWidth

        dateFillPaint.textSize = dateTextSize

        val timeY = height * 0.52f
        val dateY = height * 0.78f

        // Draw time: high-contrast outline first, then fill
        canvas.drawText(timeText, centerX, timeY, timeStrokePaint)
        canvas.drawText(timeText, centerX, timeY, timeFillPaint)

        // Draw date: high-contrast outline first, then fill
        canvas.drawText(dateText, centerX, dateY, dateStrokePaint)
        canvas.drawText(dateText, centerX, dateY, dateFillPaint)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (ignoreTouches) {
            onIgnoreInterceptedListener?.invoke()
            return true
        }
        if (event == null) return super.onTouchEvent(event)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownX = event.rawX
                touchDownY = event.rawY
                touchDownTime = System.currentTimeMillis()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val dx = abs(event.rawX - touchDownX)
                val dy = abs(event.rawY - touchDownY)
                val duration = System.currentTimeMillis() - touchDownTime
                val slop = ViewConfiguration.get(context).scaledTouchSlop
                if (!hasLongPressed && dx < slop && dy < slop && duration < 500) {
                    performClick()
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        launchClockApp()
        return true
    }

    private fun launchClockApp() {
        val pm = context.packageManager

        // 1. Primary target requested: Version 1.2.8go (com.android.deskclock.go)
        val deskClockGoIntent = pm.getLaunchIntentForPackage("com.android.deskclock.go")
        if (deskClockGoIntent != null) {
            try {
                deskClockGoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(deskClockGoIntent)
                return
            } catch (e: Exception) {
                (context as? MainActivity)?.logKeeper?.log("BuiltInClockWidgetView", "Failed to launch com.android.deskclock.go", e)
            }
        }

        // 2. Standard system clock packages fallback
        val fallbackPackages = listOf(
            "com.google.android.deskclock",
            "com.android.deskclock",
            "com.sec.android.app.clockpackage",
            "com.oneplus.deskclock",
            "com.xiaomi.deskclock",
            "com.coloros.alarmclock"
        )
        for (pkg in fallbackPackages) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    return
                } catch (e: Exception) {
                    (context as? MainActivity)?.logKeeper?.log("BuiltInClockWidgetView", "Failed to launch $pkg", e)
                }
            }
        }

        // 3. System AlarmClock action intent
        try {
            val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (alarmIntent.resolveActivity(pm) != null) {
                context.startActivity(alarmIntent)
                return
            }
        } catch (e: Exception) {
            (context as? MainActivity)?.logKeeper?.log("BuiltInClockWidgetView", "Failed to resolve ACTION_SHOW_ALARMS", e)
        }
    }
}
