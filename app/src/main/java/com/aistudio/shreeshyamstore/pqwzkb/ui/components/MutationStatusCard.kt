package com.aistudio.shreeshyamstore.pqwzkb.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.ErrorRed
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.SaffronPrimary
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.SuccessGreen
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.TextMediumGray
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.TextNearBlack
import com.aistudio.shreeshyamstore.pqwzkb.ui.theme.WarmCreamBg
import com.aistudio.shreeshyamstore.pqwzkb.utils.AppStrings
import com.aistudio.shreeshyamstore.pqwzkb.utils.MutationStage
import com.aistudio.shreeshyamstore.pqwzkb.utils.MutationStatus
import com.aistudio.shreeshyamstore.pqwzkb.utils.title

@Composable
fun AppMutationStatusCard(
    status: MutationStatus,
    strings: AppStrings,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    if (status.stage == MutationStage.IDLE) return

    val isBusy = status.stage == MutationStage.VALIDATING ||
        status.stage == MutationStage.SAVING_LOCALLY ||
        status.stage == MutationStage.SYNCING
    val isError = status.stage == MutationStage.VALIDATION_ERROR ||
        status.stage == MutationStage.AUTH_ERROR ||
        status.stage == MutationStage.RETRYABLE_ERROR ||
        status.stage == MutationStage.CONFLICT ||
        status.stage == MutationStage.FAILURE
    val accent = when {
        isError -> ErrorRed
        status.stage == MutationStage.SAVED_LOCALLY || status.stage == MutationStage.SUCCESS -> SuccessGreen
        else -> SaffronPrimary
    }
    val background = when {
        isError -> ErrorRed.copy(alpha = 0.08f)
        status.stage == MutationStage.SAVED_LOCALLY || status.stage == MutationStage.SUCCESS -> SuccessGreen.copy(alpha = 0.08f)
        else -> WarmCreamBg
    }

    Card(
        modifier = modifier.fillMaxWidth().testTag("mutation_status_card"),
        colors = CardDefaults.cardColors(containerColor = background),
        border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = accent,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Icon(
                        imageVector = when {
                            isError -> Icons.Default.ErrorOutline
                            status.stage == MutationStage.SAVED_LOCALLY || status.stage == MutationStage.SUCCESS -> Icons.Default.CheckCircle
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status.stage.title(strings),
                    color = TextNearBlack,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            status.message?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    color = if (isError) ErrorRed else TextMediumGray,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (status.canRetry && onRetry != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRetry,
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = SaffronPrimary),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("mutation_retry_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.retryAction, fontWeight = FontWeight.Bold)
                    }
                    if (onDismiss != null) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("mutation_dismiss_button")
                        ) {
                            Text(strings.dismissAction)
                        }
                    }
                }
            } else if (onDismiss != null && !isBusy) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .testTag("mutation_dismiss_button")
                ) {
                    Text(strings.dismissAction)
                }
            }
        }
    }
}
