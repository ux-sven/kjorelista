package no.svenway.kjorelista.ui.screens

import no.svenway.kjorelista.ui.theme.Brand
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import no.svenway.kjorelista.R
import no.svenway.kjorelista.data.AppStore
import no.svenway.kjorelista.data.BackendException
import no.svenway.kjorelista.ui.Haptic
import no.svenway.kjorelista.ui.rememberHaptics
import no.svenway.kjorelista.ui.theme.LocalAppColors

private enum class Mode { LOGIN, REGISTER, FORGOT }

/** Innlogging, registrering og glemt passord (port av src/app/login.tsx). */
@Composable
fun LoginScreen(store: AppStore, onOpenPrivacy: () -> Unit) {
    val colors = LocalAppColors.current
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var mode by rememberSaveable { mutableStateOf(Mode.LOGIN) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var info by remember { mutableStateOf("") }

    val s = object {
        val emailRequired = stringResource(R.string.auth_email_required)
        val passwordRequired = stringResource(R.string.auth_password_required)
        val passwordTooShort = stringResource(R.string.auth_password_too_short)
        val passwordsDoNotMatch = stringResource(R.string.auth_passwords_do_not_match)
        val firstNameRequired = stringResource(R.string.auth_first_name_required)
        val lastNameRequired = stringResource(R.string.auth_last_name_required)
        val invalidCredentials = stringResource(R.string.auth_invalid_credentials)
        val confirmEmail = stringResource(R.string.auth_confirm_email)
        val resetEmailSent = stringResource(R.string.auth_reset_email_sent)
    }

    fun switchMode(next: Mode) {
        mode = next
        error = ""
        info = ""
    }

    fun fail(message: String) {
        haptics(Haptic.ERROR)
        error = message
    }

    fun run(block: suspend () -> Unit) {
        focusManager.clearFocus()
        loading = true
        error = ""
        scope.launch {
            try {
                block()
            } catch (e: BackendException) {
                fail(if (e.invalidCredentials) s.invalidCredentials else e.message ?: "")
            } finally {
                loading = false
            }
        }
    }

    fun submit() {
        when (mode) {
            Mode.LOGIN -> {
                if (email.isBlank()) return fail(s.emailRequired)
                if (password.isEmpty()) return fail(s.passwordRequired)
                run {
                    store.backend.signIn(email.trim(), password)
                    haptics(Haptic.SUCCESS)
                }
            }
            Mode.REGISTER -> {
                if (firstName.isBlank()) return fail(s.firstNameRequired)
                if (lastName.isBlank()) return fail(s.lastNameRequired)
                if (email.isBlank()) return fail(s.emailRequired)
                if (password.length < 6) return fail(s.passwordTooShort)
                if (password != confirmPassword) return fail(s.passwordsDoNotMatch)
                run {
                    val needsConfirmation = store.backend.signUp(email.trim(), password, firstName.trim(), lastName.trim())
                    haptics(Haptic.SUCCESS)
                    if (needsConfirmation) {
                        mode = Mode.LOGIN
                        info = s.confirmEmail
                    }
                }
            }
            Mode.FORGOT -> {
                if (email.isBlank()) return fail(s.emailRequired)
                run {
                    store.backend.resetPassword(email.trim())
                    haptics(Haptic.SUCCESS)
                    info = s.resetEmailSent
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // Merkevare som i logoen: lilla gradient bak innloggingen
            .background(Brush.linearGradient(listOf(Brand.purpleLight, Brand.purpleDark)))
            .safeDrawingPadding()
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Image(
                    painter = painterResource(R.drawable.app_logo),
                    contentDescription = null,
                    modifier = Modifier.padding(bottom = 14.dp).size(96.dp).clip(RoundedCornerShape(22.dp)),
                )
                val title = stringResource(R.string.header_title)
                Image(
                    painter = painterResource(R.drawable.wordmark_gold),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.widthIn(max = 260.dp).fillMaxWidth().semantics { heading() },
                )
                Text(
                    stringResource(R.string.header_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Surface(shape = RoundedCornerShape(20.dp), color = colors.card, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (mode != Mode.FORGOT) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            listOf(Mode.LOGIN, Mode.REGISTER).forEachIndexed { i, m ->
                                SegmentedButton(
                                    selected = mode == m,
                                    onClick = { switchMode(m) },
                                    shape = SegmentedButtonDefaults.itemShape(index = i, count = 2),
                                ) {
                                    Text(stringResource(if (m == Mode.LOGIN) R.string.auth_sign_in else R.string.auth_sign_up))
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.auth_forgot_password_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(R.string.auth_forgot_password_description), style = MaterialTheme.typography.bodyMedium, color = colors.mutedForeground)
                        }
                    }

                    if (error.isNotEmpty()) Banner(error, colors.destructive)
                    if (info.isNotEmpty()) Banner(info, colors.success)

                    if (mode == Mode.REGISTER) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                label = { Text(stringResource(R.string.auth_first_name)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f).semantics { contentType = ContentType.PersonFirstName },
                            )
                            OutlinedTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                label = { Text(stringResource(R.string.auth_last_name)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                                modifier = Modifier.weight(1f).semantics { contentType = ContentType.PersonLastName },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.auth_email)) },
                        placeholder = { Text(stringResource(R.string.auth_email_placeholder)) },
                        leadingIcon = { Icon(Icons.Outlined.Mail, contentDescription = null) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            autoCorrectEnabled = false,
                            imeAction = if (mode == Mode.FORGOT) ImeAction.Send else ImeAction.Next,
                        ),
                        keyboardActions = KeyboardActions(onSend = { submit() }),
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentType = if (mode == Mode.REGISTER) ContentType.NewUsername + ContentType.EmailAddress
                            else ContentType.Username + ContentType.EmailAddress
                        },
                    )

                    if (mode != Mode.FORGOT) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text(stringResource(R.string.auth_password)) },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        if (showPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = stringResource(if (showPassword) R.string.auth_hide_password else R.string.auth_show_password),
                                    )
                                }
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                autoCorrectEnabled = false,
                                imeAction = if (mode == Mode.LOGIN) ImeAction.Go else ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(onGo = { submit() }),
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentType = if (mode == Mode.LOGIN) ContentType.Password else ContentType.NewPassword
                            },
                        )
                    }

                    if (mode == Mode.REGISTER) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it },
                            label = { Text(stringResource(R.string.auth_confirm_password)) },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false, imeAction = ImeAction.Go),
                            keyboardActions = KeyboardActions(onGo = { submit() }),
                            modifier = Modifier.fillMaxWidth().semantics { contentType = ContentType.NewPassword },
                        )
                    }

                    Button(
                        onClick = ::submit,
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.primaryForeground),
                        contentPadding = ButtonDefaults.ContentPadding,
                    ) {
                        if (loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primaryForeground)
                        } else {
                            Text(
                                stringResource(
                                    when (mode) {
                                        Mode.LOGIN -> R.string.auth_sign_in
                                        Mode.REGISTER -> R.string.auth_sign_up
                                        Mode.FORGOT -> R.string.auth_send_reset_email
                                    },
                                ),
                                modifier = Modifier.padding(vertical = 4.dp),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    if (mode == Mode.LOGIN) {
                        TextButton(onClick = { switchMode(Mode.FORGOT) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.auth_forgot_password))
                        }
                    }
                    if (mode == Mode.FORGOT) {
                        TextButton(onClick = { switchMode(Mode.LOGIN) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.auth_back_to_login))
                        }
                    }
                }
            }

            TextButton(onClick = onOpenPrivacy) {
                Text(stringResource(R.string.privacy_title), color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
private fun Banner(text: String, color: Color) {
    Text(
        text,
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
            .padding(12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
    )
}
