package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.OrderStatus
import com.example.ui.theme.KiranaAmber
import com.example.ui.theme.KiranaAmberLight
import com.example.ui.theme.KiranaBlue
import com.example.ui.theme.KiranaBlueLight
import com.example.ui.theme.KiranaGreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaGreenLight
import com.example.ui.theme.KiranaRed
import com.example.ui.theme.KiranaRedLight
import com.example.ui.theme.KiranaSurfaceWhite
import com.example.util.ImageCompressionUtil
import com.example.util.QrCodeGenerator
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun KiranaTopHeader(
    title: String,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    showSignOut: Boolean = false,
    onSignOut: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.get(language)
    Surface(
        color = KiranaGreen,
        contentColor = KiranaSurfaceWhite,
        shadowElevation = 4.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = KiranaSurfaceWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onToggleLanguage,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KiranaSurfaceWhite,
                    contentColor = KiranaGreenDark
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .testTag("language_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = "Switch language",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.switchLangButton,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            if (showSignOut && onSignOut != null) {
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = strings.signOut,
                        tint = KiranaSurfaceWhite
                    )
                }
            }
        }
    }
}

@Composable
fun DeliveryBannerStrip(
    bannerText: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = KiranaGreenLight,
        contentColor = KiranaGreenDark,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bolt,
                contentDescription = null,
                tint = KiranaGreenDark,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = bannerText.ifBlank { "Delivery in 30 minutes" },
                style = MaterialTheme.typography.titleMedium,
                color = KiranaGreenDark,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun KiranaImage(
    imageBase64: String?,
    imageKey: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val bitmap: Bitmap? = remember(imageBase64) {
        ImageCompressionUtil.decodeBase64ToBitmap(imageBase64)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(id = ImageCompressionUtil.drawableForKey(imageKey)),
            contentDescription = contentDescription,
            contentScale = contentScale,
            modifier = modifier
        )
    }
}

@Composable
fun StarRatingRow(
    rating: Int,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        for (star in 1..5) {
            Icon(
                imageVector = if (star <= rating) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = null,
                tint = if (star <= rating) KiranaAmber else Color(0xFFBDBDBD),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$rating/5",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun OrderStatusChip(
    status: OrderStatus,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val (bg, fg) = when (status) {
        OrderStatus.WAITING_PAYMENT_CONFIRMATION -> KiranaAmberLight to KiranaAmber
        OrderStatus.PLACED, OrderStatus.PAYMENT_CONFIRMED -> KiranaBlueLight to KiranaBlue
        OrderStatus.PACKING, OrderStatus.READY, OrderStatus.OUT_FOR_DELIVERY -> KiranaGreenLight to KiranaGreenDark
        OrderStatus.DELIVERED -> KiranaGreen to KiranaSurfaceWhite
        OrderStatus.CANCELLED -> KiranaRedLight to KiranaRed
    }
    val label = if (language == AppLanguage.BN) status.bnLabel else status.enLabel
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun DeliveryCountdownTimer(
    confirmedAt: Timestamp?,
    deliveryMinutes: Int,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    if (confirmedAt == null) return
    val strings = AppStrings.get(language)
    var remainingSeconds by remember(confirmedAt, deliveryMinutes) {
        mutableLongStateOf(
            ((confirmedAt.seconds + deliveryMinutes * 60L) - (System.currentTimeMillis() / 1000L)).coerceAtLeast(0L)
        )
    }
    LaunchedEffect(confirmedAt, deliveryMinutes) {
        while (true) {
            val targetSec = confirmedAt.seconds + deliveryMinutes * 60L
            val nowSec = System.currentTimeMillis() / 1000L
            remainingSeconds = (targetSec - nowSec).coerceAtLeast(0L)
            if (remainingSeconds <= 0L) break
            delay(1000L)
        }
    }
    val mins = remainingSeconds / 60
    val secs = remainingSeconds % 60
    val formatted = "%02d:%02d".format(mins, secs)

    Surface(
        color = if (remainingSeconds > 0) KiranaAmberLight else KiranaGreenLight,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Schedule,
                contentDescription = null,
                tint = if (remainingSeconds > 0) KiranaAmber else KiranaGreenDark
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (remainingSeconds > 0) {
                    "${strings.deliveryTimerLabel}: $formatted"
                } else {
                    if (language == AppLanguage.BN) "ডেলিভারি যেকোনো মুহূর্তে পৌঁছে যাবে" else "Arriving shortly!"
                },
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = if (remainingSeconds > 0) Color(0xFF92400E) else KiranaGreenDark
            )
        }
    }
}

@Composable
fun UpiQrCodeDisplay(
    upiId: String,
    shopName: String,
    customQrBase64: String?,
    modifier: Modifier = Modifier
) {
    val customBitmap = remember(customQrBase64) {
        ImageCompressionUtil.decodeBase64ToBitmap(customQrBase64)
    }
    val qrMatrix = remember(upiId, shopName) {
        val upiUri = "upi://pay?pa=${upiId.trim()}&pn=${shopName.replace(" ", "")}&cu=INR"
        QrCodeGenerator.encode(upiUri)
    }
    Box(
        modifier = modifier
            .size(190.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(2.dp, KiranaGreen, RoundedCornerShape(16.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (customBitmap != null) {
            Image(
                bitmap = customBitmap.asImageBitmap(),
                contentDescription = "Shop UPI QR Code",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else if (qrMatrix != null) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = qrMatrix.size
                val cellSize = size.width / n.toFloat()
                for (r in 0 until n) {
                    for (c in 0 until n) {
                        if (qrMatrix[r][c]) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(c * cellSize, r * cellSize),
                                size = Size(cellSize + 0.5f, cellSize + 0.5f)
                            )
                        }
                    }
                }
            }
        } else {
            Text(text = upiId, fontWeight = FontWeight.Bold)
        }
    }
}

// --- JETPACK CREDENTIAL MANAGER GOOGLE SIGN-IN HELPERS ---

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for Google Sign-In")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOut(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun GoogleSignInButton(
    label: String,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { msg ->
                    isLoading = false
                    onAuthError(msg)
                },
                scope = scope,
                onAuthCancelled = { isLoading = false }
            )
        },
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = KiranaGreen,
            contentColor = KiranaSurfaceWhite
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .testTag("google_sign_in_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = KiranaSurfaceWhite,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
