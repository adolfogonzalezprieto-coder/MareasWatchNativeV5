package com.example.mareasv4

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mareasv4.data.AppData
import com.example.mareasv4.data.TidePoint
import com.example.mareasv4.ui.MainViewModel
import com.example.mareasv4.ui.UiState
import java.time.LocalTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
    }
}

private val Bg = Color(0xFF020617)
private val Panel = Color(0xFF0F172A)
private val Cyan = Color(0xFF22D3EE)
private val Muted = Color(0xFF94A3B8)

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val state by vm.state.collectAsState()
    val isWatch = LocalContext.current.packageManager
        .hasSystemFeature(PackageManager.FEATURE_WATCH)

    val permissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        vm.refresh()
    }

    LaunchedEffect(Unit) {
        permissionRequest.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Bg
        ) {
            when (val current = state) {
                UiState.Gps -> WaitingScreen("Buscando GPS...")
                UiState.Loading -> WaitingScreen("Consultando mareas oficiales...")
                is UiState.Error -> ErrorScreen(current.text, vm::refresh)
                is UiState.Ready -> Dashboard(current.data, isWatch, vm::refresh)
            }
        }
    }
}

@Composable
private fun WaitingScreen(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Cyan)
            Spacer(Modifier.height(10.dp))
            Text(text, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ErrorScreen(text: String, retry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Button(onClick = retry) { Text("Reintentar") }
        }
    }
}

@Composable
private fun Dashboard(data: AppData, isWatch: Boolean, refresh: () -> Unit) {
    val padding = if (isWatch) 14.dp else 28.dp
    val nextEvent = data.tide.next
    val nextEventLabel = if (
        nextEvent?.tipo?.lowercase()?.startsWith("pl") == true
    ) {
        "Próxima pleamar"
    } else {
        "Próxima bajamar"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Mareas V4 Oficial",
            color = Color.White,
            fontSize = if (isWatch) 13.sp else 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Estación IHM: ${data.tide.station}",
            color = Cyan,
            fontSize = if (isWatch) 10.sp else 16.sp
        )
        Text(
            "GPS ${"%.5f".format(data.latitude)}, ${"%.5f".format(data.longitude)}",
            color = Muted,
            fontSize = if (isWatch) 8.sp else 11.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "${"%.2f".format(data.tide.currentHeight)} m ${if (data.tide.rising) "↑" else "↓"}",
            color = Color.White,
            fontSize = if (isWatch) 27.sp else 44.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "Curva interpolada entre predicciones oficiales IHM",
            color = Color(0xFFFBBF24),
            fontSize = if (isWatch) 8.sp else 12.sp
        )

        TideGraph(
            points = data.tide.curve,
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isWatch) 125.dp else 245.dp)
        )
        GraphAxis(isWatch)

        if (nextEvent != null) {
            Text(
                "$nextEventLabel: ${nextEvent.hora} · ${"%.2f".format(nextEvent.altura)} m",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = if (isWatch) 9.sp else 15.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Metric("TEMP", "${"%.0f".format(data.temperature)}°")
            Metric("VIENTO", "${"%.0f".format(data.wind)} km/h")
            Metric("OLA", "${"%.1f".format(data.wave)} m")
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Horas y alturas: Instituto Hidrográfico de la Marina. No sustituye al Anuario de Mareas.",
            color = Color(0xFFFF8A80),
            fontSize = if (isWatch) 8.sp else 11.sp,
            textAlign = TextAlign.Center
        )
        Button(onClick = refresh) { Text("Actualizar") }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Muted, fontSize = 7.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
    }
}

@Composable
private fun GraphAxis(isWatch: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("00", "06", "12", "18", "24").forEach { hour ->
            Text(hour, color = Muted, fontSize = if (isWatch) 7.sp else 10.sp)
        }
    }
}

@Composable
private fun TideGraph(points: List<TidePoint>, modifier: Modifier) {
    Canvas(
        modifier = modifier
            .background(Panel, RoundedCornerShape(18.dp))
            .padding(10.dp)
    ) {
        if (points.size < 2) return@Canvas

        val minimum = points.minOf { it.height }
        val maximum = points.maxOf { it.height }
        val range = (maximum - minimum).coerceAtLeast(0.1)

        fun x(hour: Double): Float = ((hour / 24.0) * size.width).toFloat()
        fun y(height: Double): Float =
            size.height - (((height - minimum) / range) * size.height).toFloat()

        val path = Path()
        points.forEachIndexed { index, point ->
            if (index == 0) {
                path.moveTo(x(point.hour), y(point.height))
            } else {
                path.lineTo(x(point.hour), y(point.height))
            }
        }
        drawPath(path, Cyan, style = Stroke(3.dp.toPx()))

        val now = LocalTime.now()
        val currentHour = now.hour + now.minute / 60.0
        val index = (currentHour * 4).toInt().coerceIn(0, points.size - 2)
        val fraction = currentHour * 4 - index
        val currentHeight = points[index].height +
            (points[index + 1].height - points[index].height) * fraction
        val currentX = x(currentHour)
        val currentY = y(currentHeight)

        drawLine(
            Color.White.copy(alpha = 0.5f),
            Offset(currentX, 0f),
            Offset(currentX, size.height),
            1.dp.toPx()
        )
        drawCircle(Color.White, 5.dp.toPx(), Offset(currentX, currentY))
        drawCircle(Cyan, 3.dp.toPx(), Offset(currentX, currentY))
    }
}
