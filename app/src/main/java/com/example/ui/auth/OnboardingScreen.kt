package com.example.ui.auth

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.UserProfile
import com.example.ui.components.DeliveryBannerStrip
import com.example.ui.components.KiranaTopHeader
import com.example.ui.theme.KiranaGreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaGreenLight
import com.example.ui.theme.KiranaRed
import com.example.ui.theme.KiranaRedLight
import com.example.ui.theme.KiranaSurfaceTint
import com.example.ui.theme.KiranaSurfaceWhite
import com.example.util.LocationHelper
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    existingProfile: UserProfile?,
    defaultName: String,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onSignOut: () -> Unit,
    onSaveProfile: suspend (name: String, mobile: String, address: String, lat: Double?, lng: Double?) -> Result<Unit>
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val strings = AppStrings.get(language)

    var name by remember(existingProfile) {
        mutableStateOf(existingProfile?.name?.ifBlank { defaultName } ?: defaultName)
    }
    var mobile by remember(existingProfile) {
        mutableStateOf(existingProfile?.mobile ?: "")
    }
    var address by remember(existingProfile) {
        mutableStateOf(existingProfile?.address ?: "")
    }
    var latitude by remember(existingProfile) { mutableStateOf(existingProfile?.latitude) }
    var longitude by remember(existingProfile) { mutableStateOf(existingProfile?.longitude) }
    var fetchingLocation by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun fetchLocationNow() {
        fetchingLocation = true
        errorMsg = null
        scope.launch {
            LocationHelper.fetchCurrentLocation(context)
                .onSuccess { coords ->
                    latitude = coords.latitude
                    longitude = coords.longitude
                }
                .onFailure { err ->
                    errorMsg = err.localizedMessage ?: "Could not get GPS location"
                }
            fetchingLocation = false
        }
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            fetchLocationNow()
        } else {
            errorMsg = if (language == AppLanguage.BN) {
                "লোকেশন অনুমতি দেওয়া হয়নি। ঠিকানা লিখে এগিয়ে যান।"
            } else {
                "Location permission denied. You can still type your full address."
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                KiranaTopHeader(
                    title = strings.appTitle,
                    language = language,
                    onToggleLanguage = onToggleLanguage,
                    showSignOut = true,
                    onSignOut = onSignOut
                )
                DeliveryBannerStrip(
                    bannerText = if (language == AppLanguage.BN) "৩০ মিনিটে ডেলিভারি" else "Delivery in 30 minutes"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KiranaSurfaceTint)
                .padding(innerPadding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = KiranaSurfaceWhite),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = strings.completeProfileTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.completeProfileSubtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(strings.fullName, fontSize = 16.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_name_input")
                    )

                    OutlinedTextField(
                        value = mobile,
                        onValueChange = { mobile = it.filter { ch -> ch.isDigit() || ch == '+' }.take(15) },
                        label = { Text(strings.mobileNumber, fontSize = 16.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_mobile_input")
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(strings.fullAddress, fontSize = 16.sp) },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("onboarding_address_input")
                    )

                    OutlinedButton(
                        onClick = {
                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                            if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                                fetchLocationNow()
                            } else {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .testTag("use_live_location_button")
                    ) {
                        if (fetchingLocation) {
                            CircularProgressIndicator(modifier = Modifier.height(22.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.useLiveLocation,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }

                    if (latitude != null && longitude != null) {
                        Surface(
                            color = KiranaGreenLight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KiranaGreenDark)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${strings.locationSaved} (${"%.4f".format(latitude)}, ${"%.4f".format(longitude)})",
                                    color = KiranaGreenDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (!errorMsg.isNullOrBlank()) {
                        Surface(
                            color = KiranaRedLight,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMsg ?: "",
                                color = KiranaRed,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            when {
                                name.trim().isEmpty() -> errorMsg = "Please enter your name"
                                mobile.trim().length < 6 -> errorMsg = "Please enter a valid mobile number"
                                address.trim().length < 3 -> errorMsg = "Please enter your full address"
                                else -> {
                                    saving = true
                                    errorMsg = null
                                    scope.launch {
                                        onSaveProfile(name, mobile, address, latitude, longitude)
                                            .onFailure { err ->
                                                errorMsg = err.localizedMessage ?: "Failed to save details"
                                            }
                                        saving = false
                                    }
                                }
                            }
                        },
                        enabled = !saving,
                        colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .testTag("save_profile_button")
                    ) {
                        if (saving) {
                            CircularProgressIndicator(color = KiranaSurfaceWhite, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = strings.saveContinue,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
