package com.sippy.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sippy.app.R

/**
 * First run. A personalised link fills the name in already, so most people will
 * only ever see this screen with the answer pre-typed and one tap left to do.
 */
@Composable
fun OnboardingScreen(
    suggestedName: String,
    onDone: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var name by remember { mutableStateOf(suggestedName) }
    val keyboard = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(colors.background, colors.primaryContainer.copy(alpha = 0.45f))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Mascot(
                progress = 0.35f,
                celebrating = false,
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .aspectRatio(0.82f),
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.displaySmall,
                color = colors.onBackground,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.onboarding_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { if (it.length <= 24) name = it },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                placeholder = { Text(stringResource(R.string.onboarding_placeholder)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    keyboard?.hide()
                    onDone(name)
                }),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    keyboard?.hide()
                    onDone(name)
                },
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WaterMid,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_cta),
                    style = MaterialTheme.typography.labelLarge,
                )
            }

            TextButton(onClick = { onDone("") }) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
