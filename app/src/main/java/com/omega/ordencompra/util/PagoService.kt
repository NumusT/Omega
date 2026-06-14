package com.omega.ordencompra.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

data class PagoData(
    val email: String,
    val notaEntrega: String,
    val cliente: String,
    val monto: String,
    val tipoPago: String,
    val modalidad: String,
    val referencia: String,
    val vendedor: String,
    val imagenBase64: String = ""
)

object PagoService {
    private const val WEBHOOK_URL =
        "https://script.google.com/macros/s/AKfycbwth3byoeyqlzS73uCi-1DDSQCrk4jidEXlouj4Btr97EKCt7NVEFHqh7AqvBx9256J0w/exec"

    suspend fun enviarPago(pago: PagoData): Result<String> = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val json = JSONObject().apply {
                put("email", pago.email)
                put("notaEntrega", pago.notaEntrega)
                put("cliente", pago.cliente)
                put("monto", pago.monto)
                put("tipoPago", pago.tipoPago)
                put("modalidad", pago.modalidad)
                put("imagenBase64", pago.imagenBase64)
                put("referencia", pago.referencia)
                put("vendedor", pago.vendedor)
            }

            conn = URL(WEBHOOK_URL).openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 30000
            conn.readTimeout = 30000

            OutputStreamWriter(conn.outputStream).use { it.write(json.toString()) }

            val response = conn.inputStream.bufferedReader().readText()
            val result = JSONObject(response)
            if (result.optBoolean("success", false)) {
                Result.success("Pago registrado correctamente")
            } else {
                val err = result.optString("error", "Error desconocido")
                Result.failure(Exception("Script: $err"))
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: e.toString()
            val responseBody = try {
                if (conn != null) conn.errorStream?.bufferedReader()?.readText() ?: "(no error body)" else "(conn null)"
            } catch (_: Exception) { "(error al leer body)" }
            Result.failure(Exception("$msg | Body: $responseBody"))
        }
    }
}
