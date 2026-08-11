package com.example.mareasv4.data

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class Repository {
    private val madridZone = ZoneId.of("Europe/Madrid")

    suspend fun load(lat: Double, lon: Double): AppData = coroutineScope {
        val stations = Apis.ihm.stations().estaciones.puertos
        val nearest = stations.minByOrNull {
            haversine(lat, lon, it.lat.toDouble(), it.lon.toDouble())
        } ?: error("No se encontró una estación IHM")

        val tideDeferred = async { Apis.ihm.tides(id = nearest.id) }
        val weatherDeferred = async { Apis.weather.current(lat, lon) }
        val marineDeferred = async { Apis.marine.current(lat, lon) }

        val tideResponse = tideDeferred.await().mareas
        val today = LocalDate.now(madridZone)
        val now = LocalTime.now(madridZone)
        val currentHour = now.hour + now.minute / 60.0

        val rawEvents = tideResponse.datos.marea
        val datedEvents = rawEvents.filter { event ->
            event.fecha?.let { isSameDate(it, today) } == true
        }

        val todayEvents = when {
            datedEvents.isNotEmpty() -> datedEvents
            rawEvents.all { it.fecha.isNullOrBlank() } &&
                tideResponse.fecha?.let { isSameDate(it, today) } == true -> rawEvents
            else -> error(
                "La API IHM no devolvió mareas para hoy ${today.format(DateTimeFormatter.ISO_DATE)}"
            )
        }.sortedBy { toHour(it.hora) }

        if (todayEvents.size < 2) {
            error("La API IHM devolvió datos incompletos para hoy")
        }

        val anchors = buildAnchors(todayEvents)
        val curve = (0..96).map { index ->
            val hour = index / 4.0
            TidePoint(hour, interpolate(anchors, hour))
        }
        val currentHeight = interpolate(anchors, currentHour)
        val futureHeight = interpolate(anchors, (currentHour + 0.1).coerceAtMost(24.0))
        val nextEvent = todayEvents.firstOrNull { toHour(it.hora) > currentHour }

        val weather = weatherDeferred.await().current
        val marine = marineDeferred.await().current

        AppData(
            latitude = lat,
            longitude = lon,
            tide = TideView(
                station = "${tideResponse.puerto} (IHM ${nearest.id})",
                events = todayEvents,
                curve = curve,
                currentHeight = currentHeight,
                rising = futureHeight >= currentHeight,
                next = nextEvent
            ),
            temperature = weather?.temperature_2m ?: 0.0,
            apparent = weather?.apparent_temperature ?: 0.0,
            humidity = weather?.relative_humidity_2m ?: 0,
            pressure = weather?.surface_pressure ?: 0.0,
            uv = weather?.uv_index ?: 0.0,
            wind = weather?.wind_speed_10m ?: 0.0,
            gusts = weather?.wind_gusts_10m ?: 0.0,
            wave = marine?.wave_height ?: 0.0,
            wavePeriod = marine?.wave_period ?: 0.0
        )
    }

    private fun isSameDate(value: String, expected: LocalDate): Boolean {
        val clean = value.trim().substringBefore("T").substringBefore(" ")
        val formats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM/dd")
        )
        return formats.any { formatter ->
            runCatching { LocalDate.parse(clean, formatter) == expected }.getOrDefault(false)
        }
    }

    private fun toHour(value: String): Double {
        val parts = value.trim().split(":")
        require(parts.size >= 2) { "Hora IHM no válida: $value" }
        return parts[0].toDouble() + parts[1].toDouble() / 60.0
    }

    private fun buildAnchors(events: List<IhmEvent>): List<Pair<Double, Double>> {
        val anchors = events.map { toHour(it.hora) to it.altura }.toMutableList()
        if (anchors.first().first > 0.0) {
            anchors.add(0, 0.0 to anchors.first().second)
        }
        if (anchors.last().first < 24.0) {
            anchors.add(24.0 to anchors.last().second)
        }
        return anchors
    }

    private fun interpolate(anchors: List<Pair<Double, Double>>, hour: Double): Double {
        val interval = anchors.zipWithNext().firstOrNull {
            hour >= it.first.first && hour <= it.second.first
        } ?: return anchors.last().second

        val x0 = interval.first.first
        val x1 = interval.second.first
        val y0 = interval.first.second
        val y1 = interval.second.second
        val fraction = ((hour - x0) / (x1 - x0)).coerceIn(0.0, 1.0)
        return y0 + (y1 - y0) * (1 - cos(PI * fraction)) / 2
    }

    private fun haversine(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val radiusKm = 6371.0
        val deltaLat = Math.toRadians(lat2 - lat1)
        val deltaLon = Math.toRadians(lon2 - lon1)
        val value = sin(deltaLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(deltaLon / 2).pow(2)
        return 2 * radiusKm * asin(sqrt(value))
    }
}
