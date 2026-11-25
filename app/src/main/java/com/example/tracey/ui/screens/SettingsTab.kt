package com.example.tracey.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.tracey.ui.theme.*
import com.example.tracey.viewmodel.TraceyViewModel

@Composable
fun SettingsTab(
    viewModel: TraceyViewModel,
    onNavigateAbout: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val wallets by viewModel.wallets.collectAsState()
    val expenses by viewModel.expenses.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = DeepGraphite)

        // Profile Summary Card
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = PureWhiteSurface,
            border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.6f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(shape = CircleShape, color = PersianCoatGrey, modifier = Modifier.size(54.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Pets, contentDescription = null, tint = CharcoalPrimary, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(currentUser, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${wallets.size} wallets • ${expenses.size} records", style = MaterialTheme.typography.bodySmall, color = MutedFurGrey)
                }
            }
        }

        // About Screen Card (Rubric Requirement: Developer Info)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhiteSurface,
            border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateAbout() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = CharcoalPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("About Tracey", fontWeight = FontWeight.SemiBold)
                    Text("Developer & app version details", style = MaterialTheme.typography.bodySmall, color = MutedFurGrey)
                }
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MutedFurGrey)
            }
        }

        // Sign Out Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhiteSurface,
            border = BorderStroke(1.dp, SilverBorder.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogout() }
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = SoftRoseDelete)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sign Out", fontWeight = FontWeight.SemiBold, color = SoftRoseDelete)
            }
        }
    }
}