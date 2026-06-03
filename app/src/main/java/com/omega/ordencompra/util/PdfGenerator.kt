package com.omega.ordencompra.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.omega.ordencompra.R
import com.omega.ordencompra.data.db.entities.ClienteEntity
import com.omega.ordencompra.data.db.entities.OrdenEntity
import com.omega.ordencompra.data.db.entities.ProductoEntity
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object PdfGenerator {
    fun generateOrdenPdf(
        context: Context,
        orden: OrdenEntity,
        productos: List<ProductoEntity>,
        cliente: ClienteEntity? = null
    ): Uri {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Palette and Paints
        val accentColor = 0xFF001C53.toInt() // Corporate Deep Blue
        val textDark = 0xFF1C1B1F.toInt()
        val textMedium = 0xFF555555.toInt()
        val lineLightColor = 0xFFD2D9F4.toInt()

        // Page background
        canvas.drawColor(0xFFFFFFFF.toInt()) // White background

        val leftMargin = 40f
        val rightMargin = 555f
        var y = 40f

        // Draw Logo at top left
        try {
            val logoBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            val scaledLogo = Bitmap.createScaledBitmap(logoBitmap, 110, 55, true)
            canvas.drawBitmap(scaledLogo, leftMargin, y, null)
        } catch (e: Exception) {
            // Fallback: draw placeholder text in case logo resource fails to load
            val logoTextPaint = Paint().apply {
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = accentColor
            }
            canvas.drawText("OMEGA", leftMargin, y + 25f, logoTextPaint)
        }

        // Right side header metadata (ORDEN DE COMPRA)
        val rightTitlePaint = Paint().apply {
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = accentColor
            textAlign = Paint.Align.RIGHT
        }
        val rightSubPaint = Paint().apply {
            textSize = 9f
            color = textMedium
            textAlign = Paint.Align.RIGHT
        }
        val rightValuePaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = accentColor
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("ORDEN DE", rightMargin, y + 18f, rightTitlePaint)
        canvas.drawText("COMPRA", rightMargin, y + 38f, rightTitlePaint)
        canvas.drawText("ORDEN N°", rightMargin, y + 54f, rightSubPaint)
        canvas.drawText(orden.numeroOrden, rightMargin, y + 68f, rightValuePaint)
        canvas.drawText("FECHA DE EMISIÓN", rightMargin, y + 84f, rightSubPaint)
        canvas.drawText(formatPdfDate(orden.fecha), rightMargin, y + 98f, rightValuePaint)

        // Buyer company info under the logo
        var companyY = 110f
        val companyNamePaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = accentColor
        }
        val companyInfoPaint = Paint().apply {
            textSize = 9f
            color = textMedium
        }

        canvas.drawText("REPRESENTACIONES OMEGA CERAMIC,", leftMargin, companyY, companyNamePaint)
        companyY += 12f
        canvas.drawText("C.A.", leftMargin, companyY, companyNamePaint)
        companyY += 12f
        canvas.drawText("RIF. J-40101363-5", leftMargin, companyY, companyInfoPaint)
        companyY += 12f
        canvas.drawText("Tel: +584125626147", leftMargin, companyY, companyInfoPaint)

        // Section: PROVEEDOR
        y = 175f
        val sectionTitlePaint = Paint().apply {
            textSize = 10f
            color = textMedium
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isUnderlineText = true
        }
        val supplierNamePaint = Paint().apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = accentColor
        }
        val supplierInfoPaint = Paint().apply {
            textSize = 9f
            color = textDark
        }

        canvas.drawText("PROVEEDOR", leftMargin, y, sectionTitlePaint)
        y += 18f
        canvas.drawText(cliente?.nombre ?: orden.clienteNombre, leftMargin, y, supplierNamePaint)
        y += 14f

        if (cliente != null) {
            if (cliente.rif.isNotBlank()) {
                canvas.drawText("RIF: ${cliente.rif}", leftMargin, y, supplierInfoPaint)
                y += 12f
            }
            val addressLines = cliente.direccion.split("\n", ", ")
            addressLines.forEach { line ->
                if (line.isNotBlank() && y < 240f) {
                    canvas.drawText(line.trim(), leftMargin, y, supplierInfoPaint)
                    y += 12f
                }
            }
            if (cliente.telefono.isNotBlank() && y < 252f) {
                canvas.drawText("Tel: ${cliente.telefono}", leftMargin, y, supplierInfoPaint)
                y += 12f
            }
            if (cliente.email.isNotBlank() && y < 264f) {
                canvas.drawText("Email: ${cliente.email}", leftMargin, y, supplierInfoPaint)
                y += 12f
            }
        } else {
            canvas.drawText("Dirección no disponible", leftMargin, y, supplierInfoPaint)
            y += 12f
        }

        // Table Header
        val tableY = 270f
        val headerBgPaint = Paint().apply {
            color = 0xFFF3F4F6.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(leftMargin, tableY - 14f, rightMargin, tableY + 10f, headerBgPaint)

        val tableHeaderPaint = Paint().apply {
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = textMedium
        }
        
        canvas.drawText("DESCRIPCIÓN DEL ÍTEM", leftMargin + 8f, tableY, tableHeaderPaint)
        
        tableHeaderPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("CANTIDAD", 310f, tableY, tableHeaderPaint)
        
        tableHeaderPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("P. UNITARIO", 430f, tableY, tableHeaderPaint)
        canvas.drawText("TOTAL", rightMargin - 8f, tableY, tableHeaderPaint)

        // Draw Table Items
        val itemTitlePaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = accentColor
        }
        val itemRefPaint = Paint().apply {
            textSize = 8f
            color = textMedium
        }
        val itemBodyCenterPaint = Paint().apply {
            textSize = 10f
            color = textDark
            textAlign = Paint.Align.CENTER
        }
        val itemBodyRightPaint = Paint().apply {
            textSize = 10f
            color = textDark
            textAlign = Paint.Align.RIGHT
        }
        val itemTotalRightPaint = Paint().apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = accentColor
            textAlign = Paint.Align.RIGHT
        }
        val lightLinePaint = Paint().apply {
            strokeWidth = 0.5f
            color = lineLightColor
        }

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "MX"))
        var rowY = tableY + 24f

        productos.forEach { prod ->
            canvas.drawText(prod.productoCatalogoId.uppercase(), leftMargin + 8f, rowY, itemTitlePaint)
            if (prod.nombre.isNotBlank()) {
                canvas.drawText(prod.nombre, leftMargin + 8f, rowY + 12f, itemRefPaint)
            }

            canvas.drawText(prod.cantidad.toString(), 310f, rowY + 6f, itemBodyCenterPaint)
            canvas.drawText(currencyFormat.format(prod.precioUnitario), 430f, rowY + 6f, itemBodyRightPaint)
            canvas.drawText(currencyFormat.format(prod.total), rightMargin - 8f, rowY + 6f, itemTotalRightPaint)

            rowY += 28f
            canvas.drawLine(leftMargin, rowY, rightMargin, rowY, lightLinePaint)
        }

        // Totals Section on the Right
        val startTotalsY = maxOf(rowY + 15f, 690f)
        var totalsY = startTotalsY
        val subtotal = orden.total

        val labelPaint = Paint().apply {
            textSize = 10f
            color = textMedium
        }
        val valuePaint = Paint().apply {
            textSize = 10f
            color = textDark
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("Subtotal", 340f, totalsY, labelPaint)
        canvas.drawText(currencyFormat.format(subtotal), rightMargin - 8f, totalsY, valuePaint)
        totalsY += 16f

        // Dotted separator line
        val dottedPaint = Paint().apply {
            strokeWidth = 1f
            color = lineLightColor
            pathEffect = DashPathEffect(floatArrayOf(4f, 4f), 0f)
            style = Paint.Style.STROKE
        }
        canvas.drawLine(340f, totalsY, rightMargin, totalsY, dottedPaint)
        totalsY += 12f

        // Total a Pagar filled box
        val boxBgPaint = Paint().apply {
            color = 0xFFF1F3F9.toInt()
            style = Paint.Style.FILL
        }
        val boxHeight = 44f
        canvas.drawRect(330f, totalsY, rightMargin, totalsY + boxHeight, boxBgPaint)

        val boxLabelPaint = Paint().apply {
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = accentColor
        }
        val boxValuePaint = Paint().apply {
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = accentColor
            textAlign = Paint.Align.RIGHT
        }

        canvas.drawText("TOTAL A", 340f, totalsY + 18f, boxLabelPaint)
        canvas.drawText("PAGAR", 340f, totalsY + 30f, boxLabelPaint)
        canvas.drawText(currencyFormat.format(orden.total), rightMargin - 8f, totalsY + 28f, boxValuePaint)

        // Draw Observations on the Left (aligned with Totals block)
        if (orden.observaciones.isNotBlank()) {
            var obsY = startTotalsY
            val obsPaint = Paint().apply {
                textSize = 9f
                color = textMedium
            }
            val obsTitlePaint = Paint().apply {
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = accentColor
            }
            canvas.drawText("Notas adicionales:", leftMargin, obsY, obsTitlePaint)
            obsY += 12f

            val obsText = orden.observaciones
            val maxWidth = 260
            val words = obsText.split(" ")
            var line = ""
            words.forEach { word ->
                val testLine = if (line.isEmpty()) word else "$line $word"
                val width = obsPaint.measureText(testLine)
                if (width > maxWidth) {
                    canvas.drawText(line, leftMargin, obsY, obsPaint)
                    obsY += 11f
                    line = word
                } else {
                    line = testLine
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line, leftMargin, obsY, obsPaint)
            }
        }

        document.finishPage(page)

        val dir = File(context.cacheDir, "pdfs")
        dir.mkdirs()
        val file = File(dir, "OC-${orden.numeroOrden}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun getProductRef(prod: ProductoEntity): String {
        return try {
            val words = prod.nombre.trim().split("\\s+".toRegex())
            val prefix = words.mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
            val suffix = if (prod.productoCatalogoId.length >= 4) {
                prod.productoCatalogoId.takeLast(4).uppercase()
            } else {
                prod.id.takeLast(4).uppercase()
            }
            "REF: $prefix-$suffix"
        } catch (e: Exception) {
            "REF: GEN-ITEM"
        }
    }

    private fun formatPdfDate(dateStr: String): String {
        return try {
            val inputFormat = if (dateStr.contains("-")) {
                java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
            } else {
                java.text.SimpleDateFormat("dd/MM/yyyy", Locale.US)
            }
            val date = inputFormat.parse(dateStr)
            if (date != null) {
                val outputFormat = java.text.SimpleDateFormat("dd 'de' MMMM, yyyy", Locale("es", "MX"))
                outputFormat.format(date)
            } else {
                dateStr
            }
        } catch (e: Exception) {
            dateStr
        }
    }
}
