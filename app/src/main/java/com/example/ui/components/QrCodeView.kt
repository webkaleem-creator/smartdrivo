package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    darkColor: Color = Color.Black,
    lightColor: Color = Color.White
) {
    val qrBitmap = remember(content, darkColor, lightColor) {

        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8"
        )

        val matrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            512,
            512,
            hints
        )

        val width = matrix.width
        val height = matrix.height

        val pixels = IntArray(width * height)

        val dark = darkColor.toArgb()
        val light = lightColor.toArgb()

        for (y in 0 until height) {
            for (x in 0 until width) {
                pixels[(y * width) + x] =
                    if (matrix[x, y]) dark else light
            }
        }

        Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        ).apply {
            setPixels(
                pixels,
                0,
                width,
                0,
                0,
                width,
                height
            )
        }.asImageBitmap()
    }

    Image(
        bitmap = qrBitmap,
        contentDescription = "UPI Payment QR Code",
        modifier = modifier.then(
            Modifier
        )
    )
}