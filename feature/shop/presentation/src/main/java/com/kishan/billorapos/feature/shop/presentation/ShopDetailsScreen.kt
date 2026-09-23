package com.kishan.billorapos.feature.shop.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kishan.billorapos.core.designsystem.InputLabel
import com.kishan.billorapos.core.designsystem.PrimaryButton
import com.kishan.billorapos.core.designsystem.PrimaryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopDetailsScreen(
    viewModel: ShopViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    var name by remember { mutableStateOf("") }
    var address1 by remember { mutableStateOf("") }
    var address2 by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var footerText by remember { mutableStateOf("") }

    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(state.shop) {
        val s = state.shop
        if (s != null && !isInitialized) {
            name = s.name
            address1 = s.addressLine1
            address2 = s.addressLine2
            phone = s.phoneNumber
            upiId = s.upiId
            footerText = s.footerText
            isInitialized = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ShopEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is ShopEvent.SaveSuccess -> {
                    onNavigateBack()
                }
            }
        }
    }

    var nameError by remember { mutableStateOf(false) }
    var address1Error by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Shop Details", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        if (state.isLoading && !isInitialized) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryColor)
            }
        } else {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text(text = "GENERAL INFORMATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryColor.copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "These details will appear on your digital and printed receipts.", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(24.dp))

                    InputLabel(text = "Shop Name")
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it; nameError = false },
                        placeholder = { Text("e.g. QuickMart Superstore", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = nameError,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryColor, unfocusedBorderColor = Color.LightGray, containerColor = Color.White),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    InputLabel(text = "Address Line 1")
                    OutlinedTextField(
                        value = address1,
                        onValueChange = { address1 = it; address1Error = false },
                        placeholder = { Text("Samrajpet, Mecheri", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = address1Error,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryColor, unfocusedBorderColor = Color.LightGray, containerColor = Color.White),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    InputLabel(text = "Address Line 2 (Optional)")
                    OutlinedTextField(
                        value = address2,
                        onValueChange = { address2 = it },
                        placeholder = { Text("Salem - 636453", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryColor, unfocusedBorderColor = Color.LightGray, containerColor = Color.White),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    InputLabel(text = "Phone Number")
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it; phoneError = false },
                        placeholder = { Text("+91 7010674588", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        isError = phoneError,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryColor, unfocusedBorderColor = Color.LightGray, containerColor = Color.White),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    InputLabel(text = "UPI ID")
                    OutlinedTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        placeholder = { Text("dineshsowndar@oksbi", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryColor, unfocusedBorderColor = Color.LightGray, containerColor = Color.White),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(15.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        InputLabel(text = "Receipt Footer Text", modifier = Modifier.weight(1f))
                        Text(text = "Max 150 chars", color = Color.LightGray, fontSize = 11.sp)
                    }
                    OutlinedTextField(
                        value = footerText,
                        onValueChange = { if (it.length <= 60) footerText = it },
                        placeholder = { Text("Thank you, Visit again!!!", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryColor, unfocusedBorderColor = Color.LightGray, containerColor = Color.White),
                        maxLines = 2,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                    )

                    Spacer(modifier = Modifier.height(120.dp))
                }

                Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
                    PrimaryButton(
                        onPressed = {
                            var hasError = false
                            if (name.isBlank()) { nameError = true; hasError = true }
                            if (address1.isBlank()) { address1Error = true; hasError = true }
                            if (phone.isBlank()) { phoneError = true; hasError = true }

                            if (!hasError) {
                                viewModel.onAction(ShopAction.SaveShop(name, address1, address2, phone, upiId, footerText))
                            }
                        },
                        label = "Save Details"
                    )
                }
            }
        }
    }
}
