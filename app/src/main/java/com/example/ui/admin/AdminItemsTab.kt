package com.example.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CatalogItem
import com.example.model.ItemCategory
import com.example.model.UiState
import com.example.ui.components.KiranaImage
import com.example.ui.theme.KiranaGreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaOutline
import com.example.ui.theme.KiranaRed
import com.example.ui.theme.KiranaSurfaceWhite
import com.example.util.ImageCompressionUtil
import kotlinx.coroutines.launch

@Composable
fun AdminItemsTab(
    itemsState: UiState<List<CatalogItem>>,
    onSaveItem: (CatalogItem) -> Unit,
    onToggleStock: (String, Boolean) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var editingItem by remember { mutableStateOf<CatalogItem?>(null) }
    val allItems = (itemsState as? UiState.Success)?.data ?: emptyList()

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = { editingItem = CatalogItem() },
            colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .heightIn(min = 52.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Grocery Item", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        when (itemsState) {
            is UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = KiranaGreen)
                }
            }
            is UiState.Error -> {
                Text(itemsState.message, color = KiranaRed, modifier = Modifier.padding(16.dp))
            }
            is UiState.Success -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(allItems, key = { it.id }) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = KiranaSurfaceWhite),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, KiranaOutline),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KiranaImage(
                                    imageBase64 = item.imageBase64,
                                    imageKey = item.imageKey,
                                    contentDescription = item.bilingualName(),
                                    modifier = Modifier
                                        .size(68.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.bilingualName(), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Rs ${item.price} / ${item.unit}", color = KiranaGreenDark, fontWeight = FontWeight.Bold)
                                    Text(
                                        text = if (item.available) "In Stock" else "Out of stock",
                                        color = if (item.available) KiranaGreenDark else KiranaRed,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Switch(
                                        checked = item.available,
                                        onCheckedChange = { checked -> onToggleStock(item.id, checked) }
                                    )
                                    Row {
                                        IconButton(onClick = { editingItem = item }, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit item")
                                        }
                                        IconButton(onClick = { onDeleteItem(item.id) }, modifier = Modifier.size(48.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete item", tint = KiranaRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingItem?.let { current ->
        ItemEditorDialog(
            initial = current,
            allItems = allItems.filter { it.id != current.id },
            onDismiss = { editingItem = null },
            onSave = { updated ->
                onSaveItem(updated)
                editingItem = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ItemEditorDialog(
    initial: CatalogItem,
    allItems: List<CatalogItem>,
    onDismiss: () -> Unit,
    onSave: (CatalogItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var nameEn by remember { mutableStateOf(initial.nameEn) }
    var nameBn by remember { mutableStateOf(initial.nameBn) }
    var priceText by remember { mutableStateOf(if (initial.price > 0) initial.price.toString() else "") }
    var unit by remember { mutableStateOf(initial.unit.ifBlank { "1 kg" }) }
    var category by remember { mutableStateOf(ItemCategory.fromString(initial.category)) }
    var available by remember { mutableStateOf(initial.available) }
    var imageBase64 by remember { mutableStateOf(initial.imageBase64) }
    var similarIds by remember { mutableStateOf(initial.similarItemIds.toSet()) }
    var compressing by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            compressing = true
            scope.launch {
                ImageCompressionUtil.compressUriToBase64(context, uri, maxDimensionPx = 420, maxBytes = 120_000)
                    .onSuccess { b64 -> imageBase64 = b64 }
                compressing = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initial.id.isBlank()) "Add Grocery Item" else "Edit Item",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (compressing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pick Item Photo", fontWeight = FontWeight.Bold)
                    }
                }
                if (!imageBase64.isNullOrBlank()) {
                    KiranaImage(
                        imageBase64 = imageBase64,
                        imageKey = initial.imageKey,
                        contentDescription = "Preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("English Name (e.g. Rice)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameBn,
                    onValueChange = { nameBn = it },
                    label = { Text("Bengali Name (e.g. চাল)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { c -> c.isDigit() } },
                        label = { Text("Price (Rs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unit (1 kg)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Category", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ItemCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat.enLabel) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Available in Stock", fontWeight = FontWeight.Bold)
                    Switch(checked = available, onCheckedChange = { available = it })
                }

                if (allItems.isNotEmpty()) {
                    Text("Similar / Alternative Items (max 5)", fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        allItems.take(12).forEach { candidate ->
                            val selected = similarIds.contains(candidate.id)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    similarIds = if (selected) {
                                        similarIds - candidate.id
                                    } else if (similarIds.size < 5) {
                                        similarIds + candidate.id
                                    } else {
                                        similarIds
                                    }
                                },
                                label = { Text(candidate.nameEn) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceText.toIntOrNull() ?: 0
                    if (nameEn.isNotBlank() && nameBn.isNotBlank() && price > 0 && unit.isNotBlank()) {
                        onSave(
                            initial.copy(
                                nameEn = nameEn.trim(),
                                nameBn = nameBn.trim(),
                                price = price,
                                unit = unit.trim(),
                                category = category.name,
                                available = available,
                                imageBase64 = imageBase64,
                                imageKey = initial.imageKey ?: "img_rice_grains",
                                similarItemIds = similarIds.toList()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen)
            ) {
                Text("Save Item", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
