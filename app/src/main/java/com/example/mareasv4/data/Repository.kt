package com.example.mareasv4.data
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalTime
import kotlin.math.*
class Repository {
 suspend fun load(lat:Double,lon:Double):AppData=coroutineScope {
  val stations=Apis.ihm.stations().estaciones.puertos
  val nearest=stations.minByOrNull{ haversine(lat,lon,it.lat.toDouble(),it.lon.toDouble()) } ?: error("No hay estación IHM")
  val tideD=async{Apis.ihm.tides(id=nearest.id)}
  val weatherD=async{Apis.weather.current(lat,lon)}
  val marineD=async{Apis.marine.current(lat,lon)}
  val tide=tideD.await().mareas; val events=tide.datos.marea.sortedBy{toHour(it.hora)}
  val now=LocalTime.now().hour+LocalTime.now().minute/60.0
  val all=buildAnchors(events); val curve=(0..96).map{ i->val h=i/4.0;TidePoint(h,interpolate(all,h)) }
  val current=interpolate(all,now);val future=interpolate(all,(now+.1).coerceAtMost(24.0));val next=events.firstOrNull{toHour(it.hora)>now}
  val w=weatherD.await().current;val m=marineD.await().current
  AppData(lat,lon,TideView(tide.puerto,events,curve,current,future>=current,next),w?.temperature_2m?:0.0,w?.apparent_temperature?:0.0,w?.relative_humidity_2m?:0,w?.surface_pressure?:0.0,w?.uv_index?:0.0,w?.wind_speed_10m?:0.0,w?.wind_gusts_10m?:0.0,m?.wave_height?:0.0,m?.wave_period?:0.0)
 }
 private fun toHour(t:String):Double { val p=t.trim().split(":");return p[0].toDouble()+p.getOrElse(1){"0"}.toDouble()/60.0 }
 private fun buildAnchors(e:List<IhmEvent>):List<Pair<Double,Double>> { if(e.isEmpty())return listOf(0.0 to 0.0,24.0 to 0.0);val b=e.map{toHour(it.hora) to it.altura}.toMutableList();if(b.first().first>0)b.add(0,0.0 to b.first().second);if(b.last().first<24)b.add(24.0 to b.last().second);return b }
 private fun interpolate(a:List<Pair<Double,Double>>,h:Double):Double { val i=a.zipWithNext().firstOrNull{h>=it.first.first&&h<=it.second.first}?:return a.last().second;val x0=i.first.first;val x1=i.second.first;val y0=i.first.second;val y1=i.second.second;val f=((h-x0)/(x1-x0)).coerceIn(0.0,1.0);return y0+(y1-y0)*(1-cos(Math.PI*f))/2 }
 private fun haversine(a:Double,b:Double,c:Double,d:Double):Double { val r=6371.0;val p=Math.toRadians(c-a);val q=Math.toRadians(d-b);val x=sin(p/2).pow(2)+cos(Math.toRadians(a))*cos(Math.toRadians(c))*sin(q/2).pow(2);return 2*r*asin(sqrt(x)) }
}
