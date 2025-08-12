package com.destancase

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.destancase.core.AppConfig
import com.destancase.feature.home.HomeScreen
import com.destancase.feature.web.WebViewScreen
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    // Runtime notification izni için register
    private val notifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* izin sonucu burada işlenebilir */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+ için bildirim izni iste
        if (Build.VERSION.SDK_INT >= 33) {
            notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // FCM token log (kullanıcı bildirimi için loglandı.)
        FirebaseMessaging.getInstance().token.addOnSuccessListener {
            android.util.Log.d("FCM", "token=$it")
        }

        setContent {
            var showWeb by remember { mutableStateOf(false) }

            if (showWeb) {
                WebViewScreen(
                    url = AppConfig.WEB_URL,                 // URL AppConfig’ten
                    allowedHosts = AppConfig.ALLOWED_HOSTS,  // izinli host listesi
                    onCloseRequested = { showWeb = false }   // WebView kapanınca Home’a dön
                )
            } else {
                HomeScreen(onStartUpload = { showWeb = true })
            }
        }
    }
}
