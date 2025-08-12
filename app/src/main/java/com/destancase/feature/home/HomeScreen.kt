package com.destancase.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// Basit giriş ekranı, WebViewScreen'e geçiş başlatır
@Composable
fun HomeScreen(onStartUpload: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("DestanCase", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Text("Web sayfası üzerinden birden fazla dosya yükleyin.")
            Spacer(Modifier.height(32.dp))
            Button(onClick = onStartUpload) {
                Text("Dosya Yükle")
            }
        }
    }
}
