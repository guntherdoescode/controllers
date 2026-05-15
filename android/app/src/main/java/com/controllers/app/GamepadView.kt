package com.controllers.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GamepadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val BUTTON_A = 1
        private const val BUTTON_B = 2
        private const val BUTTON_X = 4
        private const val BUTTON_Y = 8
        const val MASK_CROSS = 1
        const val MASK_CIRCLE = 2
        const val MASK_SQUARE = 4
        const val MASK_TRIANGLE = 8
        const val MASK_L1 = 16
        const val MASK_R1 = 32
        const val MASK_L2 = 64
        const val MASK_R2 = 128
        const val MASK_SHARE = 256
        const val MASK_OPTIONS = 512
        const val MASK_PS = 1024
        const val MASK_L3 = 2048
        const val MASK_R3 = 4096
        const val MASK_DPAD_UP = 16384
        const val MASK_DPAD_DOWN = 32768
        const val MASK_DPAD_LEFT = 65536
        const val MASK_DPAD_RIGHT = 131072
        private const val STICK_DEAD_ZONE = 0.15f
    }

    private var buttonsPressed = 0
    private var leftStickX = 0f
    private var leftStickY = 0f
    private var rightStickX = 0f
    private var rightStickY = 0f
    private var leftStickActive = false
    private var rightStickActive = false
    private var leftTrigger = 0f
    private var rightTrigger = 0f
    private var leftTriggerPressed = false
    private var rightTriggerPressed = false

    private var connected = false
    private var sendJob: Job? = null

    private val touchPoints = mutableMapOf<Int, PointF>()
    private val touchZone = mutableMapOf<Int, String>()

    // this is the bg color paint
    private val bgPaint = Paint().apply {
        color = Color.rgb(26, 26, 46)
        style = Paint.Style.FILL
    }
    // this one is for the stick bg
    private val stickBgPaint = Paint().apply {
        color = Color.rgb(45, 45, 68)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    // paint for the stick itself
    private val stickPaint = Paint().apply {
        color = Color.rgb(100, 100, 180)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val btnPaint = Paint().apply {
        color = Color.rgb(60, 60, 100)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val btnPressedPaint = Paint().apply {
        color = Color.rgb(120, 120, 200)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val triggerPaint = Paint().apply {
        color = Color.rgb(80, 40, 40)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val triggerPressedPaint = Paint().apply {
        color = Color.rgb(200, 60, 60)
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val smallTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 22f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val actionTextPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
        isAntiAlias = true
    }

    // Layout zones (fraction of width/height)
    private data class Zone(
        val cx: Float, val cy: Float, val radius: Float, val label: String
    )
    // zones for buttons, triggers, etc
    private val buttonZones = mutableListOf<Zone>()
    private val buttonLabels = mutableListOf<String>()
    private val triggerZones = mutableListOf<Zone>()
    private val dpadZones = mutableListOf<Zone>()

    private var stickRadius = 0f
    private var btnRadius = 0f

    // map for zones for quick lookup
    private val zoneMap = mutableMapOf<String, Int>()

    init {
        zoneMap["cross"] = MASK_CROSS
        zoneMap["circle"] = MASK_CIRCLE
        zoneMap["square"] = MASK_SQUARE
        zoneMap["triangle"] = MASK_TRIANGLE
        zoneMap["l1"] = MASK_L1
        zoneMap["r1"] = MASK_R1
        zoneMap["l2"] = MASK_L2
        zoneMap["r2"] = MASK_R2
        zoneMap["share"] = MASK_SHARE
        zoneMap["options"] = MASK_OPTIONS
        zoneMap["ps"] = MASK_PS
        zoneMap["dpad_up"] = MASK_DPAD_UP
        zoneMap["dpad_down"] = MASK_DPAD_DOWN
        zoneMap["dpad_left"] = MASK_DPAD_LEFT
        zoneMap["dpad_right"] = MASK_DPAD_RIGHT
    }

    fun setConnected(c: Boolean) {
        connected = c
        if (c) {
            startSendLoop()
        } else {
            stopSendLoop()
            buttonsPressed = 0
            leftStickX = 0f
            leftStickY = 0f
            rightStickX = 0f
            rightStickY = 0f
            leftStickActive = false
            rightStickActive = false
            leftTrigger = 0f
            rightTrigger = 0f
        }
        postInvalidate()
    }

    private fun applyDeadZone(value: Float): Float {
        return if (kotlin.math.abs(value) < STICK_DEAD_ZONE) 0f else value
    }

    private fun sendCurrentState() {
        if (!leftStickActive) { leftStickX = 0f; leftStickY = 0f }
        if (!rightStickActive) { rightStickX = 0f; rightStickY = 0f }
        val lx = (applyDeadZone(leftStickX) * 32767).toInt().coerceIn(-32768, 32767).toShort()
        val ly = (applyDeadZone(leftStickY) * -32767).toInt().coerceIn(-32768, 32767).toShort()
        val rx = (applyDeadZone(rightStickX) * 32767).toInt().coerceIn(-32768, 32767).toShort()
        val ry = (applyDeadZone(rightStickY) * -32767).toInt().coerceIn(-32768, 32767).toShort()
        val lt = (leftTrigger * 255).toInt().coerceIn(0, 255).toByte()
        val rt = (rightTrigger * 255).toInt().coerceIn(0, 255).toByte()
        NativeBridge.sendGamepadState(buttonsPressed, lx, ly, rx, ry, lt, rt)
    }

    private fun startSendLoop() {
        sendJob?.cancel()
        sendJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                if (connected) {
                    sendCurrentState()
                }
                delay(16)
            }
        }
    }

    private fun stopSendLoop() {
        sendJob?.cancel()
        sendJob = null
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutZones()
    }

    private fun layoutZones() {
        val w = width.toFloat()
        val h = height.toFloat()

        stickRadius = h * 0.12f
        btnRadius = h * 0.055f
        val triggerW = w * 0.12f
        val triggerH = h * 0.08f

        val topMargin = h * 0.12f
        val bottomMargin = h * 0.85f

        // Left stick (bottom-left)
        // Right stick (bottom-right)
        // D-pad (top-left area)
        // Action buttons (top-right area)
        // L1/R1 (top edge)
        // Share/Options/PS (center)

        buttonZones.clear()
        buttonLabels.clear()
        triggerZones.clear()
        dpadZones.clear()

        // Action buttons - right side, top area
        val actionCx = w * 0.82f
        val actionCy = h * 0.40f
        val spread = btnRadius * 2.4f

        buttonZones.add(Zone(actionCx, actionCy - spread, btnRadius, "△"))
        buttonLabels.add("triangle")
        buttonZones.add(Zone(actionCx - spread, actionCy, btnRadius, "□"))
        buttonLabels.add("square")
        buttonZones.add(Zone(actionCx + spread, actionCy, btnRadius, "○"))
        buttonLabels.add("circle")
        buttonZones.add(Zone(actionCx, actionCy + spread, btnRadius, "✕"))
        buttonLabels.add("cross")

        // D-pad - left side, top area
        val dpadCx = w * 0.18f
        val dpadCy = h * 0.40f
        val dpadSpread = btnRadius * 2.2f

        buttonZones.add(Zone(dpadCx, dpadCy - dpadSpread, btnRadius, "▲"))
        buttonLabels.add("dpad_up")
        buttonZones.add(Zone(dpadCx - dpadSpread, dpadCy, btnRadius, "◀"))
        buttonLabels.add("dpad_left")
        buttonZones.add(Zone(dpadCx + dpadSpread, dpadCy, btnRadius, "▶"))
        buttonLabels.add("dpad_right")
        buttonZones.add(Zone(dpadCx, dpadCy + dpadSpread, btnRadius, "▼"))
        buttonLabels.add("dpad_down")

        // Shoulder buttons
        val shoulderY = h * 0.05f
        triggerZones.add(Zone(w * 0.15f, shoulderY, btnRadius * 1.5f, "L1"))
        triggerZones.add(Zone(w * 0.85f, shoulderY, btnRadius * 1.5f, "R1"))
        triggerZones.add(Zone(w * 0.05f, shoulderY + h * 0.03f, btnRadius * 1.5f, "L2"))
        triggerZones.add(Zone(w * 0.95f, shoulderY + h * 0.03f, btnRadius * 1.5f, "R2"))

        // Center buttons
        val centerY = h * 0.03f
        buttonZones.add(Zone(w * 0.35f, centerY + h * 0.05f, btnRadius * 1.2f, "SHARE"))
        buttonLabels.add("share")
        buttonZones.add(Zone(w * 0.50f, centerY + h * 0.05f, btnRadius * 1.4f, "PS"))
        buttonLabels.add("ps")
        buttonZones.add(Zone(w * 0.65f, centerY + h * 0.05f, btnRadius * 1.2f, "OPT"))
        buttonLabels.add("options")
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawRect(0f, 0f, w, h, bgPaint)

        // Draw trigger zones
        for ((i, z) in triggerZones.withIndex()) {
            val pressed = when (i) {
                0 -> (buttonsPressed and MASK_L1) != 0
                1 -> (buttonsPressed and MASK_R1) != 0
                2 -> leftTriggerPressed
                3 -> rightTriggerPressed
                else -> false
            }
            val p = if (pressed) triggerPressedPaint else triggerPaint
            canvas.drawCircle(z.cx, z.cy, z.radius, p)
            canvas.drawText(
                when (i) { 0 -> "L1"; 1 -> "R1"; 2 -> "L2"; 3 -> "R2"; else -> "" },
                z.cx, z.cy + z.radius * 0.4f, smallTextPaint
            )
        }

        // Draw button zones
        for (i in buttonZones.indices) {
            val z = buttonZones[i]
            val label = buttonLabels[i]
            val mask = zoneMap[label] ?: 0
            val pressed = (buttonsPressed and mask) != 0

            val paint = if (pressed) btnPressedPaint else btnPaint
            canvas.drawCircle(z.cx, z.cy, z.radius, paint)
            canvas.drawText(z.label, z.cx, z.cy + z.radius * 0.35f, actionTextPaint)
        }

        // Draw left stick base
        val leftStickCX = w * 0.22f
        val leftStickCY = h * 0.78f
        canvas.drawCircle(leftStickCX, leftStickCY, stickRadius, stickBgPaint)
        canvas.drawCircle(
            leftStickCX + leftStickX * stickRadius * 0.7f,
            leftStickCY + leftStickY * stickRadius * 0.7f,
            stickRadius * 0.55f,
            stickPaint
        )

        // Draw right stick base
        val rightStickCX = w * 0.78f
        val rightStickCY = h * 0.78f
        canvas.drawCircle(rightStickCX, rightStickCY, stickRadius, stickBgPaint)
        canvas.drawCircle(
            rightStickCX + rightStickX * stickRadius * 0.7f,
            rightStickCY + rightStickY * stickRadius * 0.7f,
            stickRadius * 0.55f,
            stickPaint
        )

        // Connected indicator
        val statusColor = if (connected) Color.GREEN else Color.RED
        val sp = Paint().apply { color = statusColor; isAntiAlias = true }
        canvas.drawCircle(w * 0.02f, h * 0.03f, 12f, sp)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                touchPoints[pointerId] = PointF(x, y)
                touchZone[pointerId] = resolveZone(x, y)
                handleTouchStart(pointerId, x, y)
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pid = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)
                    touchPoints[pid] = PointF(x, y)
                    if (!touchZone.containsKey(pid)) {
                        touchZone[pid] = resolveZone(x, y)
                    }
                }
                updateSticks()
                updateTriggers()
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
                handleTouchEnd(pointerId)
                touchPoints.remove(pointerId)
                touchZone.remove(pointerId)
            }

            MotionEvent.ACTION_CANCEL -> {
                touchPoints.clear()
                touchZone.clear()
                buttonsPressed = 0
                leftStickX = 0f
                leftStickY = 0f
                rightStickX = 0f
                rightStickY = 0f
                leftStickActive = false
                rightStickActive = false
                leftTrigger = 0f
                rightTrigger = 0f
            }
        }

        postInvalidate()
        return true
    }

    private fun resolveZone(x: Float, y: Float): String {
        val w = width.toFloat()
        val h = height.toFloat()

        // Check triggers first
        for (i in triggerZones.indices) {
            val z = triggerZones[i]
            val dx = x - z.cx
            val dy = y - z.cy
            if (dx * dx + dy * dy < z.radius * z.radius * 2f) {
                return when (i) { 0 -> "l1"; 1 -> "r1"; 2 -> "l2"; 3 -> "r2"; else -> "" }
            }
        }

        // Check buttons
        for (i in buttonZones.indices) {
            val z = buttonZones[i]
            val dx = x - z.cx
            val dy = y - z.cy
            if (dx * dx + dy * dy < z.radius * z.radius * 2.5f) {
                return buttonLabels[i]
            }
        }

        // Check stick zones
        val leftStickCX = w * 0.22f
        val leftStickCY = h * 0.78f
        val rightStickCX = w * 0.78f
        val rightStickCY = h * 0.78f

        val dlx = x - leftStickCX
        val dly = y - leftStickCY
        if (dlx * dlx + dly * dly < stickRadius * stickRadius * 4f) {
            return "left_stick"
        }

        val drx = x - rightStickCX
        val dry = y - rightStickCY
        if (drx * drx + dry * dry < stickRadius * stickRadius * 4f) {
            return "right_stick"
        }

        return ""
    }

    private fun handleTouchStart(pointerId: Int, x: Float, y: Float) {
        val zone = touchZone[pointerId] ?: return

        if (zone == "left_stick") { leftStickActive = true; return }
        if (zone == "right_stick") { rightStickActive = true; return }
        if (zone == "l2") { leftTrigger = 1f; leftTriggerPressed = true }
        if (zone == "r2") { rightTrigger = 1f; rightTriggerPressed = true }

        val mask = zoneMap[zone] ?: return
        buttonsPressed = buttonsPressed or mask
    }

    private fun handleTouchEnd(pointerId: Int) {
        val zone = touchZone[pointerId] ?: return

        var needsFinalSend = false

        if (zone.startsWith("stick")) {
            if (zone == "left_stick") {
                leftStickX = 0f
                leftStickY = 0f
                leftStickActive = false
            } else {
                rightStickX = 0f
                rightStickY = 0f
                rightStickActive = false
            }
            needsFinalSend = true
        }

        val mask = zoneMap[zone] ?: 0
        buttonsPressed = buttonsPressed and mask.inv()

        if (zone == "l2") { leftTrigger = 0f; leftTriggerPressed = false }
        if (zone == "r2") { rightTrigger = 0f; rightTriggerPressed = false }

        if (needsFinalSend && connected) {
            sendCurrentState()
        }
    }

    private fun updateSticks() {
        val w = width.toFloat()
        val h = height.toFloat()
        val leftStickCX = w * 0.22f
        val leftStickCY = h * 0.78f
        val rightStickCX = w * 0.78f
        val rightStickCY = h * 0.78f

        for ((pid, zone) in touchZone) {
            val pt = touchPoints[pid] ?: continue
            when (zone) {
                "left_stick" -> {
                    val dx = pt.x - leftStickCX
                    val dy = pt.y - leftStickCY
                    val maxDist = stickRadius * 1.5f
                    var dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist > maxDist) dist = maxDist
                    val norm = if (dist > 5f) dist / maxDist else 0f
                    leftStickX = (dx / dist * norm).coerceIn(-1f, 1f)
                    leftStickY = (dy / dist * norm).coerceIn(-1f, 1f)
                }
                "right_stick" -> {
                    val dx = pt.x - rightStickCX
                    val dy = pt.y - rightStickCY
                    val maxDist = stickRadius * 1.5f
                    var dist = kotlin.math.sqrt(dx * dx + dy * dy)
                    if (dist > maxDist) dist = maxDist
                    val norm = if (dist > 5f) dist / maxDist else 0f
                    rightStickX = (dx / dist * norm).coerceIn(-1f, 1f)
                    rightStickY = (dy / dist * norm).coerceIn(-1f, 1f)
                }
            }
        }
    }

    private fun updateTriggers() {
        for ((_, zone) in touchZone) {
            if (zone == "l2") { leftTrigger = 1f; leftTriggerPressed = true }
            if (zone == "r2") { rightTrigger = 1f; rightTriggerPressed = true }
        }
    }
}
