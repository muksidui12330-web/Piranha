package com.example.ui.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.model.CartEntry
import com.example.model.CatalogItem
import com.example.model.ItemCategory
import com.example.model.ShopConfig
import com.example.model.UiState
import com.example.model.UserProfile
import com.example.ui.components.KiranaImage
import com.example.ui.components.StarRatingRow
import com.example.ui.theme.KiranaGreen
import com.example.ui.theme.KiranaGreenDark
import com.example.ui.theme.KiranaGreenLight
import com.example.ui.theme.KiranaOutline
import com.example.ui.theme.KiranaRed
import com.example.ui.theme.KiranaRedLight
import com.example.ui.theme.KiranaSurfaceTint
import com.example.ui.theme.KiranaSurfaceWhite
import com.example.util.LocationHelper

@Composable
fun CustomerStorefrontScreen(
    shopConfig: ShopConfig,
    catalogState: UiState<List<CatalogItem>>,
    profile: UserProfile,
    searchQuery: String,
    selectedCategory: ItemCategory?,
    cartQuantities: Map<String, Int>,
    cartEntries: List<CartEntry>,
    language: AppLanguage,
    onSearchChange: (String) -> Unit,
    onCategorySelected: (ItemCategory?) -> Unit,
    onIncrement: (CatalogItem) -> Unit,
    onDecrement: (String) -> Unit,
    onOpenCart: () -> Unit,
    onEditAddress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = AppStrings.get(language)

    val allItems = (catalogState as? UiState.Success)?.data ?: emptyList()
    val itemsById = remember(allItems) { allItems.associateBy { it.id } }

    val filteredItems = remember(allItems, searchQuery, selectedCategory) {
        val q = searchQuery.trim().lowercase()
        allItems.filter { item ->
            val catMatch = selectedCategory == null || item.category == selectedCategory.name
            val qMatch = q.isEmpty() ||
                item.nameEn.lowercase().contains(q) ||
                item.nameBn.lowercase().contains(q)
            catMatch && qMatch
        }
    }
    val itemRows = remember(filteredItems) { filteredItems.chunked(2) }
    val cartCount = cartEntries.sumOf { it.quantity }
    val cartItemsTotal = cartEntries.sumOf { it.item.price * it.quantity }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KiranaSurfaceTint)
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = if (cartCount > 0) 104.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Shop Hero Photo & Info Card
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    KiranaImage(
                        imageBase64 = shopConfig.shopPhotoBase64,
                        imageKey = "img_shop_hero",
                        contentDescription = shopConfig.shopName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = KiranaSurfaceWhite),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, KiranaOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = shopConfig.shopName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                StarRatingRow(rating = profile.rating)
                            }

                            Surface(
                                color = KiranaGreenLight,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        LocationHelper.openGoogleMaps(
                                            context = context,
                                            latitude = shopConfig.latitude,
                                            longitude = shopConfig.longitude,
                                            label = shopConfig.shopName
                                        )
                                    }
                                    .testTag("shop_location_maps_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = strings.shopLocationTapMaps,
                                        tint = KiranaGreenDark
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = shopConfig.locationText,
                                            fontWeight = FontWeight.Bold,
                                            color = KiranaGreenDark,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = strings.shopLocationTapMaps,
                                            fontSize = 13.sp,
                                            color = KiranaGreenDark
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${strings.openingHours}: ${shopConfig.openingHours}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                TextButton(onClick = onEditAddress) {
                                    Text(
                                        text = if (language == AppLanguage.BN) "ঠিকানা" else "Address",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Search Bar (English & Bengali)
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchChange,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    placeholder = { Text(strings.searchPlaceholder, fontSize = 16.sp) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(KiranaSurfaceWhite, RoundedCornerShape(16.dp))
                        .testTag("search_items_input")
                )
            }

            // 3. Category Filter Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { onCategorySelected(null) },
                            label = {
                                Text(
                                    text = strings.allCategories,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KiranaGreen,
                                selectedLabelColor = KiranaSurfaceWhite
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                    items(ItemCategory.entries) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { onCategorySelected(category) },
                            label = {
                                Text(
                                    text = if (language == AppLanguage.BN) category.bnLabel else category.enLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KiranaGreen,
                                selectedLabelColor = KiranaSurfaceWhite
                            ),
                            modifier = Modifier.heightIn(min = 48.dp)
                        )
                    }
                }
            }

            // 4. Loading / Error / 2-Column Large Photo Grid
            when (catalogState) {
                is UiState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = KiranaGreen)
                        }
                    }
                }
                is UiState.Error -> {
                    item {
                        Surface(
                            color = KiranaRedLight,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = catalogState.message,
                                color = KiranaRed,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
                is UiState.Success -> {
                    if (itemRows.isEmpty()) {
                        item {
                            Text(
                                text = if (language == AppLanguage.BN) "কোনো জিনিস পাওয়া যায়নি" else "No items match your search",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        items(itemRows, key = { row -> row.joinToString("_") { it.id } }) { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { item ->
                                    val alternatives = item.similarItemIds
                                        .mapNotNull { itemsById[it] }
                                        .filter { it.available }
                                    CatalogGridCard(
                                        item = item,
                                        quantity = cartQuantities[item.id] ?: 0,
                                        alternatives = alternatives,
                                        language = language,
                                        onIncrement = { onIncrement(item) },
                                        onDecrement = { onDecrement(item.id) },
                                        onAddAlternative = onIncrement,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (rowItems.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom Cart Bar
        AnimatedVisibility(
            visible = cartCount > 0,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            Surface(
                color = KiranaGreen,
                contentColor = KiranaSurfaceWhite,
                shape = RoundedCornerShape(18.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenCart)
                    .testTag("sticky_cart_bar")
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBasket,
                            contentDescription = null,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "$cartCount ${if (language == AppLanguage.BN) "টি জিনিস" else "items"}",
                                fontSize = 14.sp,
                                color = KiranaGreenLight
                            )
                            Text(
                                text = "Rs $cartItemsTotal",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Surface(
                        color = KiranaSurfaceWhite,
                        contentColor = KiranaGreenDark,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = strings.viewCart,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogGridCard(
    item: CatalogItem,
    quantity: Int,
    alternatives: List<CatalogItem>,
    language: AppLanguage,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onAddAlternative: (CatalogItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = AppStrings.get(language)
    Card(
        colors = CardDefaults.cardColors(containerColor = KiranaSurfaceWhite),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, KiranaOutline),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.testTag("catalog_card_${item.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White)
            ) {
                KiranaImage(
                    imageBase64 = item.imageBase64,
                    imageKey = item.imageKey,
                    contentDescription = item.bilingualName(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (!item.available) {
                    Surface(
                        color = KiranaRed,
                        contentColor = KiranaSurfaceWhite,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = strings.outOfStock,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.bilingualName(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.heightIn(min = 46.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.unit,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Rs ${item.price}",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = KiranaGreenDark
            )

            Spacer(modifier = Modifier.height(8.dp))
            if (item.available) {
                if (quantity == 0) {
                    Button(
                        onClick = onIncrement,
                        colors = ButtonDefaults.buttonColors(containerColor = KiranaGreen),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("add_item_${item.id}")
                    ) {
                        Text(
                            text = strings.add,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Surface(
                        color = KiranaGreenLight,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            IconButton(
                                onClick = onDecrement,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("dec_item_${item.id}")
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = KiranaGreenDark)
                            }
                            Text(
                                text = quantity.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = KiranaGreenDark
                            )
                            IconButton(
                                onClick = onIncrement,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("inc_item_${item.id}")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = KiranaGreenDark)
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = KiranaRedLight,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = strings.outOfStock,
                            color = KiranaRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        if (alternatives.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = strings.similarAlternatives,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            alternatives.take(2).forEach { alt ->
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(
                                    color = KiranaSurfaceWhite,
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, KiranaGreen),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAddAlternative(alt) }
                                ) {
                                    Text(
                                        text = "+ ${alt.nameEn} (Rs ${alt.price})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KiranaGreenDark,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
