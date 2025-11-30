package com.websmithing.gpstracker2.ui.features.home.components

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.websmithing.gpstracker2.R

@Composable
fun LocationPermissionFlow(
    onStartBackgroundService: () -> Unit
) {
    val context = LocalContext.current
    val isBackgroundLocationAvailable = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    var showForegroundDeniedDialog by remember { mutableStateOf(false) }
    var showForegroundRationaleDialog by remember { mutableStateOf(false) }

    var showPreBackgroundDialog by remember { mutableStateOf(false) }
    var showBackgroundDeniedDialog by remember { mutableStateOf(false) }

    val foregroundLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->

            val allGranted = result.values.all { it }
            val anyDenied = result.values.any { !it }

            if (allGranted) {
                if (isBackgroundLocationAvailable) {
                    showPreBackgroundDialog = true
                } else {
                    onStartBackgroundService()
                }
            } else {
                val shouldShow = result.entries.any { (permission, granted) ->
                    !granted && shouldShowRationale(context, permission)
                }

                if (shouldShow) showForegroundRationaleDialog = true
                else showForegroundDeniedDialog = true
            }
        }

    val backgroundLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                onStartBackgroundService()
            } else {
                val shouldShow = shouldShowRationale(
                    context,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                )

                if (shouldShow) {
                    showBackgroundDeniedDialog = true
                } else {
                    showBackgroundDeniedDialog = true
                }
            }
        }

    LaunchedEffect(true) {
        foregroundLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    //---------------------------------------------------------
    //                FOREGROUND DENIED / RATIONALE
    //---------------------------------------------------------

    if (showForegroundRationaleDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(context.getString(R.string.permission_rationale_foreground_location_title)) },
            text = { Text(context.getString(R.string.permission_rationale_foreground_location_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showForegroundRationaleDialog = false
                    foregroundLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }) {
                    Text(context.getString(R.string.permission_button_grant))
                }
            },
            dismissButton = {
                TextButton(onClick = { showForegroundRationaleDialog = false }) {
                    Text(context.getString(R.string.permission_button_deny))
                }
            }
        )
    }

    if (showForegroundDeniedDialog) {
        DeniedDialog(
            text = context.getString(R.string.permission_denied_foreground_location),
            onDismissRequest = { showForegroundDeniedDialog = false }
        )
    }

    //---------------------------------------------------------
    //                BACKGROUND PERMISSION
    //---------------------------------------------------------

    if (showPreBackgroundDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(context.getString(R.string.permission_rationale_background_location_title)) },
            text = { Text(context.getString(R.string.permission_rationale_background_location_pre_request)) },
            confirmButton = {
                TextButton(onClick = {
                    showPreBackgroundDialog = false
                    backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }) {
                    Text(context.getString(R.string.permission_button_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPreBackgroundDialog = false }) {
                    Text(context.getString(R.string.permission_button_cancel))
                }
            }
        )
    }

    if (showBackgroundDeniedDialog) {
        DeniedDialog(
            text = context.getString(R.string.permission_denied_background_location),
            onDismissRequest = { showBackgroundDeniedDialog = false }
        )
    }
}

@Composable
private fun DeniedDialog(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    text: String
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = {},
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                }
            ) {
                Text(context.getString(R.string.permission_button_settings))
            }
        },
        modifier = modifier,
    )
}

/**
 * Helper to check if we should show rationale.
 */
private fun shouldShowRationale(context: android.content.Context, permission: String): Boolean {
    return androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
        context.findActivity(),
        permission
    )
}

/**
 * Extract Activity from Context.
 */
private tailrec fun android.content.Context.findActivity(): android.app.Activity {
    return when (this) {
        is android.app.Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> throw IllegalStateException("Context is not an Activity")
    }
}
