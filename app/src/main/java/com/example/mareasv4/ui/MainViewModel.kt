package com.example.mareasv4.ui
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.*
import com.example.mareasv4.data.*
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
sealed interface UiState { data object Gps:UiState;data object Loading:UiState;data class Ready(val data:AppData):UiState;data class Error(val text:String):UiState }
class MainViewModel(a:Application):AndroidViewModel(a){private val repo=Repository();private val gps=LocationServices.getFusedLocationProviderClient(a);private val _state=MutableStateFlow<UiState>(UiState.Gps);val state:StateFlow<UiState> = _state
 fun refresh()=viewModelScope.launch { if(ContextCompat.checkSelfPermission(getApplication(),android.Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){_state.value=UiState.Error("Permiso GPS no concedido");return@launch};_state.value=UiState.Gps;try{val r=CurrentLocationRequest.Builder().setPriority(Priority.PRIORITY_HIGH_ACCURACY).setDurationMillis(25000).setMaxUpdateAgeMillis(0).build();val l=gps.getCurrentLocation(r,CancellationTokenSource().token).await()?:error("GPS no disponible");_state.value=UiState.Loading;_state.value=UiState.Ready(repo.load(l.latitude,l.longitude))}catch(e:Exception){_state.value=UiState.Error(e.message?:"Error al cargar")}}
}
