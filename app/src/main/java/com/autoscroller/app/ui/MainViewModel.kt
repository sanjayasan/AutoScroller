package com.autoscroller.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.autoscroller.app.util.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PermissionsState(
    val hasOverlay: Boolean = false,
    val hasAccessibility: Boolean = false,
    val isBatteryIgnored: Boolean = false,
    val hasNotification: Boolean = false
) {
    val canLaunch: Boolean get() = hasOverlay && hasAccessibility
}

class MainViewModel : ViewModel() {

    private val _permissionsState = MutableStateFlow(PermissionsState())
    val permissionsState: StateFlow<PermissionsState> = _permissionsState.asStateFlow()

    fun refreshPermissions(context: Context) {
        val overlay = PermissionHelper.hasOverlayPermission(context)
        val accessibility = PermissionHelper.isAccessibilityServiceEnabled(context)
        val battery = PermissionHelper.isBatteryOptimizationIgnored(context)
        val notification = PermissionHelper.hasNotificationPermission(context)

        _permissionsState.update {
            it.copy(
                hasOverlay = overlay,
                hasAccessibility = accessibility,
                isBatteryIgnored = battery,
                hasNotification = notification
            )
        }
    }
}
