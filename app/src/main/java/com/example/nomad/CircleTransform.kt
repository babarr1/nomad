package com.example.nomad

import android.graphics.*
import com.squareup.picasso.Transformation

/**
 * Picasso transformation to create circular images.
 * This class is useful if not using the de.hdodenhof:circleimageview library.
 */
class CircleTransform : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        // Use Kotlin's minOf
        val size = minOf(source.width, source.height)

        val x = (source.width - size) / 2
        val y = (source.height - size) / 2

        val squaredBitmap = Bitmap.createBitmap(source, x, y, size, size)
        if (squaredBitmap != source) {
            source.recycle()
        }

        // Use ARGB_8888 as a safe default config if source.config is null
        val config = source.config ?: Bitmap.Config.ARGB_8888
        val bitmap = Bitmap.createBitmap(size, size, config)

        val canvas = Canvas(bitmap)
        val paint = Paint()
        val shader = BitmapShader(squaredBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.isAntiAlias = true

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        squaredBitmap.recycle()
        return bitmap
    }

    override fun key(): String {
        return "circle"
    }
}