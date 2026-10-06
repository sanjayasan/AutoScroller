package com.autoscroller.app.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.autoscroller.app.service.FloatingOverlayService
import com.autoscroller.app.ui.theme.AccentBlue
import com.autoscroller.app.ui.theme.AutoScrollerTheme
import com.autoscroller.app.ui.theme.DarkBackground
import com.autoscroller.app.ui.theme.DarkSurface
import com.autoscroller.app.ui.theme.DarkSurfaceBorder
import com.autoscroller.app.ui.theme.ErrorRed
import com.autoscroller.app.ui.theme.SuccessGreen
import com.autoscroller.app.util.PermissionHelper

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AutoScrollerTheme {
                MainScreen(
                    viewModel = viewModel,
                    onLaunchOverlay = {
                        FloatingOverlayService.startService(this)
                    },
                    onExitApp = {
                        finishAffinity()
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions(this)
    }
}

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onLaunchOverlay: () -> Unit,
    onExitApp: () -> Unit
) {
    val context = LocalContext.current
    val permissionsState by viewModel.permissionsState.collectAsState()

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissions(context)
    }

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                Text(
                    text = "AutoScroller",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Automated screen gestures via Accessibility & Floating Overlay",
                    fontSize = 14.sp,
                    color = Color(0xFFA6ADC8)
                )
            }

            Text(
                text = "Required Permissions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            // Permission 1: Overlay
            PermissionItemCard(
                title = "Overlay Permission",
                description = "Allows displaying floating controls over other applications.",
                isGranted = permissionsState.hasOverlay,
                isRequired = true,
                onClick = {
                    try {
                        context.startActivity(PermissionHelper.getOverlayPermissionIntent(context))
                    } catch (_: Exception) {}
                }
            )

            // Permission 2: Accessibility Service
            PermissionItemCard(
                title = "Accessibility Gesture Engine",
                description = "Required to dispatch swipe and scroll gestures linearly.",
                isGranted = permissionsState.hasAccessibility,
                isRequired = true,
                onClick = {
                    try {
                        context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                    } catch (_: Exception) {}
                }
            )

            Text(
                text = "Recommended Optimizations",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )

            // Permission 3: Battery Optimization Whitelist
            PermissionItemCard(
                title = "Ignore Battery Optimizations",
                description = "Prevents background killer from interrupting long-running loops.",
                isGranted = permissionsState.isBatteryIgnored,
                isRequired = false,
                onClick = {
                    try {
                        context.startActivity(PermissionHelper.getBatteryOptimizationIntent(context))
                    } catch (_: Exception) {}
                }
            )

            // Permission 4: Notification Permission (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionItemCard(
                    title = "Post Notifications",
                    description = "Displays the persistent foreground controller notification.",
                    isGranted = permissionsState.hasNotification,
                    isRequired = false,
                    onClick = {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Button(
                onClick = onLaunchOverlay,
                enabled = permissionsState.canLaunch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue,
                    disabledContainerColor = Color(0xFF334155)
                )
            ) {
                Text(
                    text = if (permissionsState.canLaunch) "🚀 Launch Floating Controller" else "⚠️ Grant Required Permissions Above",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (permissionsState.canLaunch) Color.White else Color(0xFF94A3B8)
                )
            }

            OutlinedButton(
                onClick = onExitApp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Exit App",
                    fontSize = 14.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }
    }
}

@Composable
fun PermissionItemCard(
    title: String,
    description: String,
    isGranted: Boolean,
    isRequired: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, DarkSurfaceBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator Circle
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) SuccessGreen else if (isRequired) ErrorRed else Color(0xFFEAB308))
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    if (isRequired) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "*Required",
                            fontSize = 11.sp,
                            color = if (isGranted) SuccessGreen else ErrorRed
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color(0xFFA6ADC8),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isGranted) SuccessGreen.copy(alpha = 0.2f) else ErrorRed.copy(alpha = 0.2f)
            ) {
                Text(
                    text = if (isGranted) "Granted" else "Grant",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) SuccessGreen else ErrorRed,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}
