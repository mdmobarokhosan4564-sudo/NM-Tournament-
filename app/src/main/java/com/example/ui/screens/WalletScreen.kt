package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.AppConfig
import com.example.data.model.PaymentMethod
import com.example.data.model.UserProfile
import com.example.data.model.WalletRequest
import com.example.data.model.WalletRequestStatus
import com.example.data.model.WalletRequestType
import com.example.ui.components.TelegramHelpCard
import com.example.ui.theme.BlazeGold
import com.example.ui.theme.DangerCrimson
import com.example.ui.theme.GlooCyan
import com.example.ui.theme.MonoFontFamily
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianCard
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.OutlineSubtle
import com.example.ui.theme.TacticalGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.UiState

@Composable
fun WalletScreen(
    config: AppConfig,
    profileState: UiState<UserProfile?>,
    walletRequestsState: UiState<List<WalletRequest>>,
    isBusy: Boolean,
    onSubmitAddMoney: (method: PaymentMethod, amount: Long, senderNumber: String, txnId: String, onSuccess: () -> Unit) -> Unit,
    onSubmitWithdraw: (method: PaymentMethod, accountNumber: String, amount: Long, onSuccess: () -> Unit) -> Unit,
    onCopyFeedback: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val profile = (profileState as? UiState.Success)?.data
    val balance = profile?.walletBalance ?: 0L
    val requests = (walletRequestsState as? UiState.Success)?.data ?: emptyList()
    val pendingWithdrawSum = requests
        .filter { it.type == WalletRequestType.WITHDRAW.name && it.status == WalletRequestStatus.PENDING.name }
        .sumOf { it.amount }
    val availableToWithdraw = (balance - pendingWithdrawSum).coerceAtLeast(0L)
    val paymentNumber = config.walletPaymentNumber.ifBlank { "01772902980" }

    var showAddMoneyDialog by remember { mutableStateOf(false) }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                color = ObsidianCard,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    ObsidianCard,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    GlooCyan.copy(alpha = 0.1f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "ARENA WALLET BALANCE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "$balance ${config.currencyLabel}",
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = MonoFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = BlazeGold,
                                    modifier = Modifier.testTag("wallet_balance_text")
                                )
                            }
                            Surface(
                                color = BlazeGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, BlazeGold.copy(alpha = 0.45f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AccountBalanceWallet,
                                    contentDescription = "Wallet",
                                    tint = BlazeGold,
                                    modifier = Modifier.padding(12.dp).size(26.dp)
                                )
                            }
                        }

                        if (pendingWithdrawSum > 0L) {
                            Text(
                                text = "Available to Withdraw: $availableToWithdraw ${config.currencyLabel} ($pendingWithdrawSum ${config.currencyLabel} pending)",
                                style = MaterialTheme.typography.labelSmall,
                                color = GlooCyan
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showAddMoneyDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = TacticalGreen,
                                    contentColor = ObsidianBg
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("wallet_add_money_button")
                            ) {
                                Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ADD MONEY", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { showWithdrawDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BlazeGold,
                                    contentColor = ObsidianBg
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("wallet_withdraw_button")
                            ) {
                                Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WITHDRAW", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        item {
            Surface(
                color = ObsidianCard,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, GlooCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("BKASH & NAGAD PAYMENT NUMBER", style = MaterialTheme.typography.labelSmall, color = GlooCyan)
                        Text(
                            text = paymentNumber,
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                            color = TextPrimary
                        )
                        Text(
                            text = "Send Money to this number, then enter your TxnID in Add Money.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    IconButton(onClick = {
                        clipboard.setText(AnnotatedString(paymentNumber))
                        onCopyFeedback("Number copied: $paymentNumber")
                    }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Payment Number", tint = BlazeGold)
                    }
                }
            }
        }

        item {
            TelegramHelpCard(
                supportContact = config.supportContact,
                onFallbackMessage = onCopyFeedback
            )
        }

        item {
            Text("WALLET TRANSACTIONS", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        }

        when (walletRequestsState) {
            is UiState.Loading -> {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            is UiState.Error -> {
                item {
                    Text(walletRequestsState.message, color = DangerCrimson, style = MaterialTheme.typography.bodyMedium)
                }
            }
            is UiState.Success -> {
                if (requests.isEmpty()) {
                    item {
                        Surface(
                            color = ObsidianCard,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, OutlineSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(36.dp))
                                Text("No Wallet Transactions Yet", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                Text(
                                    text = "Tap Add Money to deposit via bKash/Nagad or Withdraw your balance anytime.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(requests, key = { it.id }) { req ->
                        WalletTransactionRow(request = req, currencyLabel = config.currencyLabel)
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    if (showAddMoneyDialog) {
        AddMoneyDialog(
            paymentNumber = paymentNumber,
            currencyLabel = config.currencyLabel,
            isBusy = isBusy,
            onCopyNumber = {
                clipboard.setText(AnnotatedString(paymentNumber))
                onCopyFeedback("Copied $paymentNumber")
            },
            onDismiss = { showAddMoneyDialog = false },
            onSubmit = { method, amount, sender, txnId ->
                onSubmitAddMoney(method, amount, sender, txnId) {
                    showAddMoneyDialog = false
                }
            }
        )
    }

    if (showWithdrawDialog) {
        WithdrawDialog(
            availableBalance = availableToWithdraw,
            currencyLabel = config.currencyLabel,
            isBusy = isBusy,
            onDismiss = { showWithdrawDialog = false },
            onSubmit = { method, accountNum, amount ->
                onSubmitWithdraw(method, accountNum, amount) {
                    showWithdrawDialog = false
                }
            }
        )
    }
}

@Composable
private fun WalletTransactionRow(
    request: WalletRequest,
    currencyLabel: String
) {
    val isDeposit = WalletRequestType.fromRaw(request.type) == WalletRequestType.DEPOSIT
    val statusEnum = WalletRequestStatus.fromRaw(request.status)
    val methodLabel = PaymentMethod.fromRaw(request.method).label
    val statusColor = when (statusEnum) {
        WalletRequestStatus.APPROVED -> TacticalGreen
        WalletRequestStatus.REJECTED -> DangerCrimson
        WalletRequestStatus.PENDING -> BlazeGold
    }

    Surface(
        color = ObsidianCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, OutlineSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = (if (isDeposit) TacticalGreen else BlazeGold).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (isDeposit) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        tint = if (isDeposit) TacticalGreen else BlazeGold,
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "${if (isDeposit) "Add Money" else "Withdraw"} • $methodLabel",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    if (isDeposit && request.txnId.isNotBlank()) {
                        Text(
                            text = "TxnID: ${request.txnId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlooCyan
                        )
                    }
                    if (!isDeposit && request.accountNumber.isNotBlank()) {
                        Text(
                            text = "To $methodLabel: ${request.accountNumber}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlooCyan
                        )
                    }
                    if (request.adminNote.isNotBlank()) {
                        Text(
                            text = request.adminNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isDeposit) "+" else "-"}${request.amount} $currencyLabel",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = if (isDeposit) TacticalGreen else BlazeGold
                )
                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = statusEnum.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMoneyDialog(
    paymentNumber: String,
    currencyLabel: String,
    isBusy: Boolean,
    onCopyNumber: () -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (method: PaymentMethod, amount: Long, senderNumber: String, txnId: String) -> Unit
) {
    var method by remember { mutableStateOf(PaymentMethod.BKASH) }
    var amountText by remember { mutableStateOf("") }
    var txnId by remember { mutableStateOf("") }
    var senderNumber by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Text("Add Money (bKash / Nagad)", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, GlooCyan.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Send Money (${method.label})", style = MaterialTheme.typography.labelSmall, color = GlooCyan)
                            Text(
                                text = paymentNumber,
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = MonoFontFamily),
                                color = BlazeGold
                            )
                        }
                        IconButton(onClick = onCopyNumber) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Number", tint = GlooCyan)
                        }
                    }
                }

                Text("Select Payment Method", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.entries.forEach { option ->
                        FilterChip(
                            selected = method == option,
                            onClick = { method = option },
                            label = { Text(option.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("deposit_method_${option.name.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit); errorMsg = null },
                    label = { Text("Amount ($currencyLabel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("deposit_amount_input")
                )

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(50L, 100L, 200L, 500L, 1000L).forEach { preset ->
                        FilterChip(
                            selected = amountText == preset.toString(),
                            onClick = { amountText = preset.toString(); errorMsg = null },
                            label = { Text("$preset", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                OutlinedTextField(
                    value = txnId,
                    onValueChange = { txnId = it; errorMsg = null },
                    label = { Text("Transaction ID (TxnID)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("deposit_txnid_input")
                )

                OutlinedTextField(
                    value = senderNumber,
                    onValueChange = { senderNumber = it.filter { ch -> ch.isDigit() || ch == '+' } },
                    label = { Text("Your ${method.label} Number (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = DangerCrimson, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    when {
                        amt < 1L -> errorMsg = "Enter a valid amount."
                        txnId.trim().length < 4 -> errorMsg = "Enter your Transaction ID (4+ chars)."
                        else -> onSubmit(method, amt, senderNumber.trim(), txnId.trim())
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalGreen, contentColor = ObsidianBg),
                modifier = Modifier.testTag("deposit_submit_button")
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ObsidianBg)
                else Text("SUBMIT DEPOSIT", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}

@Composable
private fun WithdrawDialog(
    availableBalance: Long,
    currencyLabel: String,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (method: PaymentMethod, accountNumber: String, amount: Long) -> Unit
) {
    var method by remember { mutableStateOf(PaymentMethod.BKASH) }
    var accountNumber by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianCard,
        title = {
            Text("Withdraw Funds", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, OutlineSubtle)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Available Balance", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        Text(
                            text = "$availableBalance $currencyLabel",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = MonoFontFamily),
                            color = BlazeGold
                        )
                    }
                }

                Text("Select Receive Method", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.entries.forEach { option ->
                        FilterChip(
                            selected = method == option,
                            onClick = { method = option },
                            label = { Text(option.label, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = TextPrimary
                            ),
                            modifier = Modifier.testTag("withdraw_method_${option.name.lowercase()}")
                        )
                    }
                }

                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it.filter { ch -> ch.isDigit() || ch == '+' }; errorMsg = null },
                    label = { Text("Your ${method.label} Number") },
                    placeholder = { Text("01XXXXXXXXX") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth().testTag("withdraw_number_input")
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter(Char::isDigit); errorMsg = null },
                    label = { Text("Withdraw Amount ($currencyLabel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("withdraw_amount_input")
                )

                if (errorMsg != null) {
                    Text(errorMsg ?: "", color = DangerCrimson, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toLongOrNull() ?: 0L
                    when {
                        accountNumber.trim().length < 6 -> errorMsg = "Enter your valid ${method.label} number."
                        amt < 1L -> errorMsg = "Enter a valid amount."
                        amt > availableBalance -> errorMsg = "Insufficient wallet balance (Available: $availableBalance)."
                        else -> onSubmit(method, accountNumber.trim(), amt)
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(containerColor = BlazeGold, contentColor = ObsidianBg),
                modifier = Modifier.testTag("withdraw_submit_button")
            ) {
                if (isBusy) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ObsidianBg)
                else Text("SUBMIT WITHDRAW", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) }
        }
    )
}
