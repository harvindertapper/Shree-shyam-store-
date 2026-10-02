package com.sevenzenlabs.zenmart.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sevenzenlabs.zenmart.commerce.CommerceValidation
import com.sevenzenlabs.zenmart.data.ItemReturnRequest
import com.sevenzenlabs.zenmart.data.Sale
import com.sevenzenlabs.zenmart.data.SaleItem
import com.sevenzenlabs.zenmart.ui.theme.*
import com.sevenzenlabs.zenmart.utils.AppStrings
import com.sevenzenlabs.zenmart.utils.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReturnDialog(
    sale: Sale,
    saleItems: List<SaleItem>,
    remainingReturnables: Map<Long, Double>,
    strings: AppStrings,
    isProcessing: Boolean,
    onDismiss: () -> Unit,
    onConfirmReturn: (itemsToReturn: List<ItemReturnRequest>, refundMode: String, reason: String, note: String?) -> Unit
) {
    val selectedQuantities = remember { mutableStateMapOf<Long, Double>() }
    var selectedReason by remember { mutableStateOf(strings.returnReasonCustomerRequest) }
    var reasonDropdownExpanded by remember { mutableStateOf(false) }
    var customNote by remember { mutableStateOf("") }

    val defaultMode = remember(sale.paymentMode) {
        if (sale.paymentMode == "UDHAAR") "UDHAAR_REVERSAL" else if (sale.paymentMode == "UPI") "UPI" else "CASH"
    }
    var selectedRefundMode by remember { mutableStateOf(defaultMode) }

    val reasons = remember(strings) {
        listOf(
            strings.returnReasonCustomerRequest,
            strings.returnReasonDefective,
            strings.returnReasonWrongItem,
            strings.returnReasonExpired,
            strings.returnReasonOther
        )
    }

    val itemsToReturn = selectedQuantities.filter { it.value > 0.0 }.map { (itemId, qty) ->
        ItemReturnRequest(itemId, qty)
    }

    val totalRefundPaise = itemsToReturn.sumOf { req ->
        val line = saleItems.find { it.id == req.saleItemId }
        if (line != null) {
            CommerceValidation.calculateLineTotal(line.unitPrice, req.quantityReturned)
        } else 0L
    }

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            border = BorderStroke(1.5.dp, BorderStrong),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.returnDialogTitle,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = SaffronDark
                        )
                        Text(
                            text = "${strings.reportsBillNumber} ${sale.billNumber}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMediumGray
                        )
                    }
                }

                // Disclaimer / Merchant Notice
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF9E6),
                    border = BorderStroke(1.dp, Color(0xFFFFD54F)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = SaffronDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = strings.returnDisclaimerNotice,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = TextNearBlack
                        )
                    }
                }

                HorizontalDivider()

                // Items list with return steppers
                Text(
                    text = strings.returnSelectItemsPrompt,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMediumGray
                )

                Box(
                    modifier = Modifier
                        .heightIn(max = 190.dp)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(saleItems) { item ->
                            val remaining = remainingReturnables[item.id] ?: 0.0
                            val currentQty = selectedQuantities[item.id] ?: 0.0
                            val canReturn = remaining > 0.0

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (currentQty > 0.0) SaffronPrimary else BorderSubtle
                                ),
                                color = if (canReturn) Color.White else Color(0xFFF9F9F9),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1.3f)) {
                                        Text(
                                            text = item.productNameSnapshot,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (canReturn) TextNearBlack else TextMediumGray
                                        )
                                        Text(
                                            text = "${CurrencyUtils.formatRupees(item.unitPrice)} / ${item.unit}",
                                            fontSize = 11.sp,
                                            color = TextMediumGray
                                        )
                                        Text(
                                            text = strings.returnRemainingQuantity(remaining, item.unit),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (canReturn) SuccessGreen else TextMutedGray
                                        )
                                    }

                                    if (canReturn) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    val newQty = maxOf(0.0, currentQty - 1.0)
                                                    selectedQuantities[item.id] = newQty
                                                },
                                                enabled = currentQty > 0.0 && !isProcessing,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Remove,
                                                    contentDescription = "Decrease",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (currentQty > 0.0) ErrorRed else TextMutedGray
                                                )
                                            }

                                            Text(
                                                text = if (currentQty % 1.0 == 0.0) "${currentQty.toLong()}" else "%.1f".format(currentQty),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 13.sp,
                                                color = if (currentQty > 0.0) SaffronDark else TextNearBlack,
                                                modifier = Modifier.widthIn(min = 24.dp),
                                                textAlign = TextAlign.Center
                                            )

                                            IconButton(
                                                onClick = {
                                                    val newQty = minOf(remaining, currentQty + 1.0)
                                                    selectedQuantities[item.id] = newQty
                                                },
                                                enabled = currentQty < remaining && !isProcessing,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Add,
                                                    contentDescription = "Increase",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (currentQty < remaining) SuccessGreen else TextMutedGray
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = strings.returnAlreadyRefunded,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMutedGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                // Reason Selector
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.returnReasonLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMediumGray
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { if (!isProcessing) reasonDropdownExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, BorderStrong)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedReason,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextNearBlack
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }

                        DropdownMenu(
                            expanded = reasonDropdownExpanded,
                            onDismissRequest = { reasonDropdownExpanded = false }
                        ) {
                            reasons.forEach { reasonItem ->
                                DropdownMenuItem(
                                    text = { Text(reasonItem, fontSize = 12.sp) },
                                    onClick = {
                                        selectedReason = reasonItem
                                        reasonDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Refund Mode Selection
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = strings.returnRefundModeLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMediumGray
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (sale.paymentMode == "UDHAAR") {
                            FilterChip(
                                selected = selectedRefundMode == "UDHAAR_REVERSAL",
                                onClick = { selectedRefundMode = "UDHAAR_REVERSAL" },
                                label = { Text(strings.returnModeUdhaarReversal, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        FilterChip(
                            selected = selectedRefundMode == "CASH",
                            onClick = { selectedRefundMode = "CASH" },
                            label = { Text(strings.returnModeCash, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )

                        if (sale.paymentMode != "UDHAAR") {
                            FilterChip(
                                selected = selectedRefundMode == "UPI",
                                onClick = { selectedRefundMode = "UPI" },
                                label = { Text(strings.returnModeUpi, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Total refund display
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF6F8FA), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.returnTotalRefund,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.sp,
                        color = TextNearBlack
                    )
                    Text(
                        text = CurrencyUtils.formatRupees(totalRefundPaise),
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = SuccessGreen
                    )
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isProcessing,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text(strings.cancel, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Button(
                        onClick = {
                            onConfirmReturn(
                                itemsToReturn,
                                selectedRefundMode,
                                selectedReason,
                                customNote.ifBlank { null }
                            )
                        },
                        enabled = itemsToReturn.isNotEmpty() && totalRefundPaise > 0L && !isProcessing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SaffronPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(44.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                strings.returnConfirmButton,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
