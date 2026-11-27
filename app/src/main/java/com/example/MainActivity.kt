package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.KiranaRepository
import com.example.i18n.AppLanguage
import com.example.i18n.LanguagePreferences
import com.example.model.UserRole
import com.example.ui.admin.AdminScreen
import com.example.ui.admin.AdminViewModel
import com.example.ui.auth.AuthScreen
import com.example.ui.components.signOut
import com.example.ui.customer.CustomerRootScreen
import com.example.ui.customer.CustomerViewModel
import com.example.ui.packer.PackerScreen
import com.example.ui.packer.PackerViewModel
import com.example.ui.theme.GraminKiranaTheme
import com.example.ui.theme.KiranaGreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GraminKiranaTheme {
                GraminKiranaApp()
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun GraminKiranaApp(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val languagePrefs = remember(context) { LanguagePreferences(context.applicationContext) }
    val language by languagePrefs.languageFlow.collectAsStateWithLifecycle(initialValue = AppLanguage.EN)

    val toggleLanguage: () -> Unit = {
        val next = if (language == AppLanguage.EN) AppLanguage.BN else AppLanguage.EN
        scope.launch { languagePrefs.setLanguage(next) }
    }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(
            language = language,
            onToggleLanguage = toggleLanguage,
            onAuthSuccess = { }
        )
    } else {
        key(user.uid) {
            AuthenticatedKiranaShell(
                uid = user.uid,
                displayName = user.displayName ?: "",
                language = language,
                onToggleLanguage = toggleLanguage,
                onSignOut = {
                    signOut(credentialManager, scope)
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedKiranaShell(
    uid: String,
    displayName: String,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { KiranaRepository(context.applicationContext) }
    var role by remember(uid) { mutableStateOf<UserRole?>(null) }

    LaunchedEffect(uid) {
        role = repository.resolveCurrentUserRole()
    }

    when (role) {
        null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = KiranaGreen)
            }
        }
        UserRole.ADMIN -> {
            val adminVm: AdminViewModel = viewModel(
                key = "admin_$uid",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        AdminViewModel(KiranaRepository(app))
                    }
                }
            )
            AdminScreen(
                viewModel = adminVm,
                language = language,
                onToggleLanguage = onToggleLanguage,
                onSignOut = onSignOut
            )
        }
        UserRole.PACKER -> {
            val packerVm: PackerViewModel = viewModel(
                key = "packer_$uid",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        PackerViewModel(KiranaRepository(app))
                    }
                }
            )
            PackerScreen(
                viewModel = packerVm,
                language = language,
                onToggleLanguage = onToggleLanguage,
                onSignOut = onSignOut
            )
        }
        UserRole.CUSTOMER -> {
            val customerVm: CustomerViewModel = viewModel(
                key = "customer_$uid",
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        CustomerViewModel(KiranaRepository(app), uid)
                    }
                }
            )
            CustomerRootScreen(
                viewModel = customerVm,
                defaultDisplayName = displayName,
                language = language,
                onToggleLanguage = onToggleLanguage,
                onSignOut = onSignOut
            )
        }
    }
}
