package com.kishan.billorapos.feature.shop.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kishan.billorapos.core.domain.Shop
import com.kishan.billorapos.core.designsystem.*
import com.kishan.billorapos.feature.shop.domain.ShopRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ShopProfilesState(val shops: List<Shop> = emptyList(), val busy: Boolean = false, val error: String? = null)
sealed interface ShopProfilesAction { data class Switch(val id: String) : ShopProfilesAction; object Reload : ShopProfilesAction }
class ShopProfilesViewModel(private val repository: ShopRepository) : ViewModel() {
    private val mutable = MutableStateFlow(ShopProfilesState())
    val state = mutable.asStateFlow()
    private var observation: Job? = null
    init { onAction(ShopProfilesAction.Reload) }
    fun onAction(action: ShopProfilesAction) {
        when (action) {
            ShopProfilesAction.Reload -> {
                observation?.cancel()
                observation = viewModelScope.launch {
                    try {
                        mutable.update { it.copy(error = null) }
                        repository.getShop()
                        repository.profiles().collect { rows -> mutable.update { it.copy(shops = rows) } }
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { mutable.update { it.copy(error = "Unable to load shops") } }
                }
            }
            is ShopProfilesAction.Switch -> {
                if (state.value.busy) return
                mutable.update { it.copy(busy = true) }
                viewModelScope.launch {
                    try { repository.selectShop(action.id) }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { mutable.update { it.copy(error = "Unable to switch shop") } }
                    finally { mutable.update { it.copy(busy = false) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopProfilesScreen(viewModel: ShopProfilesViewModel, onBack: () -> Unit, onAdd: () -> Unit, onEdit: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text("Shop Profiles") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            state.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error); TextButton(onClick = { viewModel.onAction(ShopProfilesAction.Reload) }) { Text("Retry") } } }
            items(state.shops, key = { it.id }) { shop ->
                BilloraCard(Modifier.fillMaxWidth()) {
                    Text(shop.name.ifBlank { "Unconfigured shop" }, style = MaterialTheme.typography.titleMedium)
                    Text(shop.addressLine1)
                    if (shop.isActive) {
                        GradientStatusChip("Active", BilloraGradients.Primary)
                        TextButton(onClick = onEdit) { Text("Edit profile") }
                    } else TextButton(enabled = !state.busy, onClick = { viewModel.onAction(ShopProfilesAction.Switch(shop.id)) }) { Text("Switch to this shop") }
                }
            }
            item { PrimaryButton(onPressed = onAdd, label = "+ Add Shop") }
        }
    }
}
