package com.example.data

import com.example.model.CatalogItem
import com.example.model.ItemCategory
import com.example.model.ShopConfig

object StarterCatalog {
    const val PRIMARY_ADMIN_EMAIL = "muksidui12330@gmail.com"
    const val MAX_ADMIN_SLOTS = 3

    val defaultShopConfig = ShopConfig(
        shopName = "Gramin Kirana",
        bannerText = "Delivery in 30 minutes",
        bannerTextBn = "৩০ মিনিটে ডেলিভারি",
        locationText = "Bazar Tiniali, Nagaon, Assam 782001",
        latitude = 26.3464,
        longitude = 92.6840,
        openingHours = "7:00 AM - 9:00 PM",
        deliveryCharge = 20,
        minimumOrderAmount = 200,
        deliveryMinutes = 30,
        deliveryTimeText = "Delivery in 30 minutes",
        deliveryTimeTextBn = "৩০ মিনিটে ডেলিভারি",
        upiId = "graminkirana@upi"
    )

    val items: List<CatalogItem> = listOf(
        CatalogItem(
            id = "starter_joha_rice",
            nameEn = "Assam Joha Rice",
            nameBn = "আসাম জোহা চাল",
            category = ItemCategory.GRAINS.name,
            price = 95,
            unit = "1 kg",
            available = true,
            imageKey = "img_rice_grains",
            similarItemIds = listOf("starter_miniket_rice")
        ),
        CatalogItem(
            id = "starter_miniket_rice",
            nameEn = "Miniket Rice",
            nameBn = "মিনিকেট চাল",
            category = ItemCategory.GRAINS.name,
            price = 68,
            unit = "1 kg",
            available = true,
            imageKey = "img_rice_grains",
            similarItemIds = listOf("starter_joha_rice")
        ),
        CatalogItem(
            id = "starter_mustard_oil_premium",
            nameEn = "Kachi Ghani Mustard Oil",
            nameBn = "খাঁটি সরিষার তেল",
            category = ItemCategory.OILS_SPICES.name,
            price = 165,
            unit = "1 L",
            available = false,
            imageKey = "img_mustard_oil",
            similarItemIds = listOf("starter_mustard_oil_pouch")
        ),
        CatalogItem(
            id = "starter_mustard_oil_pouch",
            nameEn = "Pure Mustard Oil Pouch",
            nameBn = "সরিষার তেল পাউচ",
            category = ItemCategory.OILS_SPICES.name,
            price = 148,
            unit = "1 L",
            available = true,
            imageKey = "img_mustard_oil",
            similarItemIds = listOf("starter_mustard_oil_premium")
        ),
        CatalogItem(
            id = "starter_masoor_dal",
            nameEn = "Red Masoor Dal",
            nameBn = "মসুর ডাল",
            category = ItemCategory.GRAINS.name,
            price = 120,
            unit = "1 kg",
            available = true,
            imageKey = "img_masoor_dal",
            similarItemIds = listOf("starter_joha_rice")
        ),
        CatalogItem(
            id = "starter_turmeric_powder",
            nameEn = "Haldi Turmeric Powder",
            nameBn = "হলুদ গুঁড়ো",
            category = ItemCategory.OILS_SPICES.name,
            price = 55,
            unit = "200 g",
            available = true,
            imageKey = "img_turmeric_spice",
            similarItemIds = listOf("starter_masoor_dal")
        ),
        CatalogItem(
            id = "starter_assam_tea",
            nameEn = "Assam CTC Leaf Tea",
            nameBn = "আসাম চা পাতা",
            category = ItemCategory.DAIRY_TEA.name,
            price = 110,
            unit = "250 g",
            available = true,
            imageKey = "img_assam_tea",
            similarItemIds = listOf("starter_sugar")
        ),
        CatalogItem(
            id = "starter_fresh_milk",
            nameEn = "Fresh Cow Milk",
            nameBn = "তাজা গরুর দুধ",
            category = ItemCategory.DAIRY_TEA.name,
            price = 64,
            unit = "1 L",
            available = true,
            imageKey = "img_fresh_milk",
            similarItemIds = listOf("starter_assam_tea")
        ),
        CatalogItem(
            id = "starter_sugar",
            nameEn = "White Sugar",
            nameBn = "চিনি",
            category = ItemCategory.SNACKS_ESSENTIALS.name,
            price = 48,
            unit = "1 kg",
            available = true,
            imageKey = "img_sugar_bowl",
            similarItemIds = listOf("starter_assam_tea")
        )
    )
}
