package com.example.mareasv4
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.*
import androidx.activity.compose.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mareasv4.data.*
import com.example.mareasv4.ui.*
import java.time.LocalTime
class MainActivity:ComponentActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);setContent{App()}}}
private val Bg=Color(0xFF020617);private val Panel=Color(0xFF0F172A);private val Cyan=Color(0xFF22D3EE);private val Muted=Color(0xFF94A3B8)
@Composable fun App(vm:MainViewModel= viewModel()){val s by vm.state.collectAsState();val watch=LocalContext.current.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH);val req=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){vm.refresh()};LaunchedEffect(Unit){req.launch(Manifest.permission.ACCESS_FINE_LOCATION)};MaterialTheme{Surface(Modifier.fillMaxSize(),color=Bg){when(val x=s){UiState.Gps->Wait("Buscando GPS...");UiState.Loading->Wait("Consultando mareas oficiales...");is UiState.Error->Error(x.text,vm::refresh);is UiState.Ready->Dashboard(x.data,watch,vm::refresh)}}}}
@Composable fun Wait(t:String)=Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){CircularProgressIndicator(color=Cyan);Spacer(Modifier.height(10.dp));Text(t,color=Color.White,fontWeight=FontWeight.Bold)}}
@Composable fun Error(t:String,r:()->Unit)=Box(Modifier.fillMaxSize().padding(20.dp),contentAlignment=Alignment.Center){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(t,color=Color.White,textAlign=TextAlign.Center);Button(onClick=r){Text("Reintentar")}}}
@Composable fun Dashboard(d:AppData,w:Boolean,r:()->Unit){val p=if(w)14.dp else 28.dp;Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(p),horizontalAlignment=Alignment.CenterHorizontally){Text("Mareas V4 Oficial",color=Color.White,fontSize=if(w)13.sp else 24.sp,fontWeight=FontWeight.Bold);Text("Estación IHM: ${d.tide.station}",color=Cyan,fontSize=if(w)10.sp else 16.sp);Text("GPS ${"%.5f".format(d.latitude)}, ${"%.5f".format(d.longitude)}",color=Muted,fontSize=if(w)8.sp else 11.sp);Spacer(Modifier.height(8.dp));Text("${"%.2f".format(d.tide.currentHeight)} m  ${if(d.tide.rising)"↑" else "↓"}",color=Color.White,fontSize=if(w)27.sp else 44.sp,fontWeight=FontWeight.Black);Text("Curva interpolada entre predicciones oficiales IHM",color=Color(0xFFFBBF24),fontSize=if(w)8.sp else 12.sp);Graph(d.tide.curve,Modifier.fillMaxWidth().height(if(w)125.dp else 245.dp));Axis(w);d.tide.next?.let{Text("${if(it.tipo.lowercase().startsWith("pl"))"Próxima pleamar" else "Próxima bajamar"}: ${it.hora} · ${"%.2f".format(it.altura)} m",color=Color.White,fontWeight=FontWeight.Bold,fontSize=if(w)9.sp else 15.sp)};Spacer(Modifier.height(10.dp));Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly){Metric("TEMP","${"%.0f".format(d.temperature)}°");Metric("VIENTO","${"%.0f".format(d.wind)} km/h");Metric("OLA","${"%.1f".format(d.wave)} m")};Spacer(Modifier.height(10.dp));Text("Horas y alturas: Instituto Hidrográfico de la Marina. No sustituye al Anuario de Mareas.",color=Color(0xFFFF8A80),fontSize=if(w)8.sp else 11.sp,textAlign=TextAlign.Center);Button(onClick=r){Text("Actualizar")}}}
@Composable fun Metric(a:String,b:String)=Column(horizontalAlignment=Alignment.CenterHorizontally){Text(a,color=Muted,fontSize=7.sp);Text(b,color=Color.White,fontWeight=FontWeight.Bold,fontSize=10.sp)}
@Composable fun Axis(w:Boolean)=Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){listOf("00","06","12","18","24").forEach{Text(it,color=Muted,fontSize=if(w)7.sp else 10.sp)}}
@Composable fun Graph(p:List<TidePoint>,m:Modifier){Canvas(m.background(Panel,RoundedCornerShape(18.dp)).padding(10.dp)){if(p.size<2)return@Canvas;val lo=p.minOf{it.height};val hi=p.maxOf{it.height};val rg=(hi-lo).coerceAtLeast(.1);fun x(h:Double)=((h/24)*size.width).toFloat();fun y(v:Double)=size.height-(((v-lo)/rg)*size.height).toFloat();val path=Path();p.forEachIndexed{i,q->if(i==0)path.moveTo(x(q.hour),y(q.height))else path.lineTo(x(q.hour),y(q.height))};drawPath(path,Cyan,style=Stroke(3.dp.toPx()));val h=LocalTime.now().hour+LocalTime.now().minute/60.0;val ix=(h*4).toInt().coerceIn(0,p.size-2);val f=h*4-ix;val v=p[ix].height+(p[ix+1].height-p[ix].height)*f;drawLine(Color.White.copy(.5f),Offset(x(h),0f),Offset(x(h),size.height),1.dp.toPx());drawCircle(Color.White,5.dp.toPx(),Offset(x(h),y(v)));drawCircle(Cyan,3.dp.toPx(),Offset(x(h),y(v)))}}
