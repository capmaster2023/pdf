package com.pdfpocket.lite.features.signature

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import java.io.ByteArrayOutputStream

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
    private val current = Path()
    private val paths = mutableListOf<Path>()

    init { setBackgroundColor(Color.WHITE) }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> { current.reset(); current.moveTo(event.x, event.y); invalidate(); return true }
            MotionEvent.ACTION_MOVE -> { current.lineTo(event.x, event.y); invalidate() }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                current.lineTo(event.x, event.y)
                paths += Path(current)
                current.reset()
                invalidate()
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        paths.forEach { canvas.drawPath(it, paint) }
        canvas.drawPath(current, paint)
    }

    fun clearPad() { paths.clear(); current.reset(); invalidate() }
    fun undo() { if (paths.isNotEmpty()) { paths.removeAt(paths.lastIndex); invalidate() } }

    fun toPng(): ByteArray {
        val bitmap = Bitmap.createBitmap(width.coerceAtLeast(800), height.coerceAtLeast(300), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        paths.forEach { canvas.drawPath(it, paint) }
        return ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }
}
