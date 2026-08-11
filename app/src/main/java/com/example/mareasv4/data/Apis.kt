package com.example.mareasv4.data
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
interface IhmApi {
 @GET("api-ihm/getmarea") suspend fun stations(@Query("request") request:String="getlist",@Query("format") format:String="json"):StationEnvelope
 @GET("api-ihm/getmarea") suspend fun tides(@Query("request") request:String="gettide",@Query("id") id:String,@Query("format") format:String="json"):IhmResponse
}
interface WeatherApi { @GET("v1/forecast") suspend fun current(@Query("latitude")lat:Double,@Query("longitude")lon:Double,@Query("current")current:String="temperature_2m,apparent_temperature,relative_humidity_2m,surface_pressure,uv_index,wind_speed_10m,wind_gusts_10m",@Query("timezone")tz:String="auto"):WeatherResponse }
interface MarineApi { @GET("v1/marine") suspend fun current(@Query("latitude")lat:Double,@Query("longitude")lon:Double,@Query("current")current:String="wave_height,wave_period,wave_direction,swell_wave_height",@Query("timezone")tz:String="auto"):MarineResponse }
object Apis {
 private val client=OkHttpClient.Builder().addInterceptor{c->c.proceed(c.request().newBuilder().header("User-Agent","MareasV4Oficial/4.0").build())}.build()
 private fun r(url:String)=Retrofit.Builder().baseUrl(url).client(client).addConverterFactory(GsonConverterFactory.create()).build()
 val ihm=r("https://ideihm.covam.es/").create(IhmApi::class.java)
 val weather=r("https://api.open-meteo.com/").create(WeatherApi::class.java)
 val marine=r("https://marine-api.open-meteo.com/").create(MarineApi::class.java)
}
