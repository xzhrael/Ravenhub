package com.ravenhub.app.ui.security

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ravenhub.app.security.MasterKeyManager

enum class LockMode {
    SETUP,
    UNLOCK,
    REAUTH,
    CHANGE_PIN
}

@Composable
fun LockScreen(
    mode: LockMode = LockMode.UNLOCK,
    onUnlocked: () -> Unit,
    onCancel: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var pinText by remember { mutableStateOf("") }
    var confirmPinText by remember { mutableStateOf("") }
    var newPinText by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var changeStep by remember { mutableIntStateOf(0) } // 0: current, 1: new, 2: confirm
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isInputValid = pinText.length in 4..8 && pinText.all { it.isDigit() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(80.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = when (mode) {
                        LockMode.SETUP -> if (isConfirmStep) "Confirm PIN" else "Set Security PIN"
                        LockMode.UNLOCK -> "Unlock RavenHub"
                        LockMode.REAUTH -> "Security Re-Authentication"
                        LockMode.CHANGE_PIN -> when (changeStep) {
                            0 -> "Enter Current PIN"
                            1 -> "Enter New PIN"
                            else -> "Confirm New PIN"
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (mode) {
                        LockMode.SETUP -> if (isConfirmStep) "Re-enter 4-8 digit numeric PIN" else "Enter a 4-8 digit numeric PIN"
                        LockMode.UNLOCK -> "Enter PIN to access your encrypted suite"
                        LockMode.REAUTH -> "Enter PIN to confirm sensitive action"
                        LockMode.CHANGE_PIN -> when (changeStep) {
                            0 -> "Verify your existing master security PIN"
                            1 -> "Enter a new 4-8 digit numeric PIN"
                            else -> "Re-enter your new PIN to confirm"
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = pinText,
                onValueChange = { input ->
                    if (input.length <= 8 && input.all { it.isDigit() }) {
                        pinText = input
                        errorMessage = null
                    }
                },
                label = { Text("PIN (Numeric Only)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = errorMessage != null,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            errorMessage?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    if (!isInputValid) {
                        errorMessage = "PIN must be between 4 and 8 digits"
                        return@Button
                    }

                    when (mode) {
                        LockMode.SETUP -> {
                            if (!isConfirmStep) {
                                confirmPinText = pinText
                                pinText = ""
                                isConfirmStep = true
                            } else {
                                if (pinText == confirmPinText) {
                                    MasterKeyManager.getOrCreateMasterKey(context, pinText)
                                    Toast.makeText(context, "PIN setup successful", Toast.LENGTH_SHORT).show()
                                    onUnlocked()
                                } else {
                                    errorMessage = "PINs do not match. Try again."
                                    pinText = ""
                                    confirmPinText = ""
                                    isConfirmStep = false
                                }
                            }
                        }
                        LockMode.UNLOCK, LockMode.REAUTH -> {
                            if (MasterKeyManager.verifyPin(context, pinText)) {
                                onUnlocked()
                            } else {
                                errorMessage = "Incorrect PIN. Try again."
                                pinText = ""
                            }
                        }
                        LockMode.CHANGE_PIN -> {
                            when (changeStep) {
                                0 -> {
                                    if (MasterKeyManager.verifyPin(context, pinText)) {
                                        pinText = ""
                                        changeStep = 1
                                    } else {
                                        errorMessage = "Incorrect current PIN. Try again."
                                        pinText = ""
                                    }
                                }
                                1 -> {
                                    newPinText = pinText
                                    pinText = ""
                                    changeStep = 2
                                }
                                2 -> {
                                    if (pinText == newPinText) {
                                        MasterKeyManager.getOrCreateMasterKey(context, pinText)
                                        Toast.makeText(context, "PIN changed successfully", Toast.LENGTH_SHORT).show()
                                        onUnlocked()
                                    } else {
                                        errorMessage = "New PINs do not match. Try again."
                                        pinText = ""
                                        newPinText = ""
                                        changeStep = 1
                                    }
                                }
                            }
                        }
                    }
                },
                enabled = isInputValid,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = when (mode) {
                        LockMode.SETUP -> if (isConfirmStep) "Confirm & Encrypt" else "Continue"
                        LockMode.UNLOCK -> "Unlock App"
                        LockMode.REAUTH -> "Confirm Re-Auth"
                        LockMode.CHANGE_PIN -> when (changeStep) {
                            0 -> "Verify Current PIN"
                            1 -> "Continue"
                            else -> "Save New PIN"
                        }
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            onCancel?.let { cancelAction ->
                TextButton(onClick = cancelAction) {
                    Text("Cancel")
                }
            }
        }
    }
}
