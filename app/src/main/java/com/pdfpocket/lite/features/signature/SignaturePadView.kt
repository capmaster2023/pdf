package com.pdfpocket.lite.features.signature

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.ceil

class SignaturePadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 7f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    private val paths = mutableListOf<Path>()

    private var currentPath: Path? = null
    private var activePointerId = MotionEvent.INVALID_POINTER_ID

    private var lastX = 0f
    private var lastY = 0f
    private var strokeMoved = false

    private val touchTolerance = 1.5f

    init {
        setBackgroundColor(Color.WHITE)
        isClickable = true
        isFocusable = true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)

                activePointerId = event.getPointerId(0)
                lastX = event.x
                lastY = event.y
                strokeMoved = false

                currentPath = Path().apply {
                    moveTo(lastX, lastY)
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val pointerIndex = event.findPointerIndex(activePointerId)

                if (pointerIndex < 0) {
                    return true
                }

                for (historyIndex in 0 until event.historySize) {
                    addPoint(
                        event.getHistoricalX(pointerIndex, historyIndex),
                        event.getHistoricalY(pointerIndex, historyIndex)
                    )
                }

                addPoint(
                    event.getX(pointerIndex),
                    event.getY(pointerIndex)
                )

                invalidate()
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex = event.actionIndex

                if (event.getPointerId(pointerIndex) == activePointerId) {
                    addPoint(
                        event.getX(pointerIndex),
                        event.getY(pointerIndex)
                    )

                    finishStroke()
                    parent?.requestDisallowInterceptTouchEvent(false)
                }

                return true
            }

            MotionEvent.ACTION_UP -> {
                val pointerIndex =
                    event.findPointerIndex(activePointerId)
                        .takeIf { it >= 0 }
                        ?: 0

                addPoint(
                    event.getX(pointerIndex),
                    event.getY(pointerIndex)
                )

                finishStroke()
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()

                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                finishStroke()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }

        return true
    }

    private fun addPoint(x: Float, y: Float) {
        val path = currentPath ?: return

        val dx = abs(x - lastX)
        val dy = abs(y - lastY)

        if (dx < touchTolerance && dy < touchTolerance) {
            return
        }

        val middleX = (lastX + x) / 2f
        val middleY = (lastY + y) / 2f

        path.quadTo(
            lastX,
            lastY,
            middleX,
            middleY
        )

        lastX = x
        lastY = y
        strokeMoved = true
    }

    private fun finishStroke() {
        val path = currentPath ?: return

        if (strokeMoved) {
            path.lineTo(lastX, lastY)
        } else {
            path.addCircle(
                lastX,
                lastY,
                paint.strokeWidth / 2f,
                Path.Direction.CW
            )
        }

        paths.add(Path(path))

        currentPath = null
        activePointerId = MotionEvent.INVALID_POINTER_ID
        strokeMoved = false

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        paths.forEach { path ->
            canvas.drawPath(path, paint)
        }

        currentPath?.let { path ->
            canvas.drawPath(path, paint)
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    fun clearPad() {
        paths.clear()
        currentPath = null
        activePointerId = MotionEvent.INVALID_POINTER_ID
        invalidate()
    }

    fun undo() {
        if (paths.isNotEmpty()) {
            paths.removeAt(paths.lastIndex)
            invalidate()
        }
    }

    fun toPng(): ByteArray {
        val drawablePaths = buildList {
            addAll(paths)
            currentPath?.let(::add)
        }

        if (drawablePaths.isEmpty()) {
            return emptyTransparentPng()
        }

        val contentBounds = RectF()
        var firstPath = true

        drawablePaths.forEach { path ->
            val pathBounds = RectF()
            path.computeBounds(pathBounds, true)

            if (firstPath) {
                contentBounds.set(pathBounds)
                firstPath = false
            } else {
                contentBounds.union(pathBounds)
            }
        }

        val padding = 28f

        val outputWidth =
            ceil(contentBounds.width() + padding * 2)
                .toInt()
                .coerceAtLeast(1)

        val outputHeight =
            ceil(contentBounds.height() + padding * 2)
                .toInt()
                .coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(
            outputWidth,
            outputHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)

        canvas.drawColor(
            Color.TRANSPARENT,
            PorterDuff.Mode.CLEAR
        )

        canvas.translate(
            -contentBounds.left + padding,
            -contentBounds.top + padding
        )

        drawablePaths.forEach { path ->
            canvas.drawPath(path, paint)
        }

        return bitmapToPng(bitmap)
    }

    private fun emptyTransparentPng(): ByteArray {
        val bitmap = Bitmap.createBitmap(
            1,
            1,
            Bitmap.Config.ARGB_8888
        )

        return bitmapToPng(bitmap)
    }

    private fun bitmapToPng(bitmap: Bitmap): ByteArray {
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(
                Bitmap.CompressFormat.PNG,
                100,
                output
            )

            bitmap.recycle()
            output.toByteArray()
        }
    }
}
