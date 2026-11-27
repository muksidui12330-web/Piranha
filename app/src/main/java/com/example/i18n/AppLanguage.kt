package com.example.i18n

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AppLanguage(val code: String, val toggleLabel: String) {
    EN("en", "বাংলায় দেখুন"),
    BN("bn", "Switch to English");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            if (code == BN.code) BN else EN
    }
}

private val Context.languageDataStore by preferencesDataStore(name = "kirana_language_prefs")

class LanguagePreferences(private val context: Context) {
    private val langKey = stringPreferencesKey("selected_language")

    val languageFlow: Flow<AppLanguage> = context.languageDataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[langKey])
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.languageDataStore.edit { prefs ->
            prefs[langKey] = language.code
        }
    }
}

data class LocalizedTexts(
    val appTitle: String,
    val switchLangButton: String,
    val signInTitle: String,
    val signInSubtitle: String,
    val signInWithGoogle: String,
    val signOut: String,
    val completeProfileTitle: String,
    val completeProfileSubtitle: String,
    val fullName: String,
    val mobileNumber: String,
    val fullAddress: String,
    val useLiveLocation: String,
    val locationSaved: String,
    val saveContinue: String,
    val shopLocationTapMaps: String,
    val openingHours: String,
    val searchPlaceholder: String,
    val allCategories: String,
    val outOfStock: String,
    val similarAlternatives: String,
    val add: String,
    val viewCart: String,
    val cartTitle: String,
    val itemsTotal: String,
    val deliveryCharge: String,
    val grandTotal: String,
    val minOrderWarning: (Int) -> String,
    val paymentMethodTitle: String,
    val payCash: String,
    val payUpi: String,
    val cashBlockedMsg: String,
    val shopUpiId: String,
    val copyUpiId: String,
    val uploadScreenshot: String,
    val screenshotSelected: String,
    val upiTxnNumber: String,
    val placeOrder: String,
    val myOrders: String,
    val shopTab: String,
    val deliveryTimerLabel: String,
    val cancelOrder: String,
    val cancelWarning: String,
    val confirmCancel: String,
    val keepOrder: String,
    val customerRatingLabel: String,
    val packerQueueTitle: String,
    val markReady: String,
    val noPackingOrders: String,
    val adminPanelTitle: String,
    val tabOrders: String,
    val tabItems: String,
    val tabCustomers: String,
    val tabReports: String,
    val tabSettings: String,
    val confirmPaymentBtn: String,
    val confirmPaymentWarning: String,
    val startPacking: String,
    val outForDeliveryBtn: String,
    val markDeliveredBtn: String,
    val cashReceivedBtn: String,
    val refusedCashBtn: String,
    val newOrderAlertTitle: String,
    val silenceAlertBtn: String,
    val openInMaps: String
)

object AppStrings {
    fun get(lang: AppLanguage): LocalizedTexts = when (lang) {
        AppLanguage.EN -> LocalizedTexts(
            appTitle = "Gramin Kirana",
            switchLangButton = "বাংলা",
            signInTitle = "Welcome to Gramin Kirana",
            signInSubtitle = "Fresh village groceries delivered to your doorstep in 30 minutes.",
            signInWithGoogle = "Sign in with Google",
            signOut = "Sign Out",
            completeProfileTitle = "Your Delivery Details",
            completeProfileSubtitle = "Fill this once so our delivery scooter finds your house easily.",
            fullName = "Full Name",
            mobileNumber = "Mobile Number",
            fullAddress = "Full House Address & Landmark",
            useLiveLocation = "Use my live location",
            locationSaved = "GPS Location Saved",
            saveContinue = "Save & Start Shopping",
            shopLocationTapMaps = "Tap to open in Google Maps",
            openingHours = "Open",
            searchPlaceholder = "Search rice, oil, চাল, তেল...",
            allCategories = "All",
            outOfStock = "Out of stock",
            similarAlternatives = "Similar available items:",
            add = "ADD",
            viewCart = "View Cart",
            cartTitle = "Your Basket",
            itemsTotal = "Items Total",
            deliveryCharge = "Delivery Charge",
            grandTotal = "Grand Total",
            minOrderWarning = { min -> "Minimum order is Rs $min. Add Rs more items to order." },
            paymentMethodTitle = "Choose Payment",
            payCash = "Cash on Delivery",
            payUpi = "UPI (GPay / PhonePe)",
            cashBlockedMsg = "Cash is disabled (1-star rating or admin lock). Please pay via UPI.",
            shopUpiId = "Shop UPI ID",
            copyUpiId = "Copy ID",
            uploadScreenshot = "Upload Payment Screenshot",
            screenshotSelected = "Screenshot Attached",
            upiTxnNumber = "UPI Transaction Number / UTR",
            placeOrder = "Place Order",
            myOrders = "Orders",
            shopTab = "Shop",
            deliveryTimerLabel = "Delivery Timer",
            cancelOrder = "Cancel Order",
            cancelWarning = "Cancelling reduces your rating by 1 star. At 1 star, Cash on Delivery is disabled.",
            confirmCancel = "Yes, Cancel",
            keepOrder = "Keep Order",
            customerRatingLabel = "Your Rating",
            packerQueueTitle = "Packing List",
            markReady = "Mark Ready",
            noPackingOrders = "No orders waiting to be packed right now.",
            adminPanelTitle = "Admin Dashboard",
            tabOrders = "Orders",
            tabItems = "Items",
            tabCustomers = "Customers",
            tabReports = "Reports",
            tabSettings = "Settings",
            confirmPaymentBtn = "Confirm Payment",
            confirmPaymentWarning = "Check money received in your own phone/bank first. Never confirm only from screenshot.",
            startPacking = "Send to Packer",
            outForDeliveryBtn = "Out for Delivery",
            markDeliveredBtn = "Mark Delivered",
            cashReceivedBtn = "Cash Received",
            refusedCashBtn = "Refused Cash (-1★)",
            newOrderAlertTitle = "New Order Alert Active!",
            silenceAlertBtn = "Acknowledge & Silence",
            openInMaps = "Open House in Maps"
        )

        AppLanguage.BN -> LocalizedTexts(
            appTitle = "গ্রামীণ কিরানা",
            switchLangButton = "English",
            signInTitle = "গ্রামীণ কিরানা-তে স্বাগতম",
            signInSubtitle = "গ্রামের তাজা মুদি সামগ্রী মাত্র ৩০ মিনিটে আপনার দরজায় পৌঁছে যাবে।",
            signInWithGoogle = "গুগল দিয়ে সাইন ইন করুন",
            signOut = "সাইন আউট",
            completeProfileTitle = "আপনার ঠিকানা ও তথ্য",
            completeProfileSubtitle = "একবার পূরণ করুন যাতে ডেলিভারি স্কুটার সহজে আপনার বাড়ি খুঁজে পায়।",
            fullName = "আপনার পুরো নাম",
            mobileNumber = "মোবাইল নম্বর",
            fullAddress = "সম্পূর্ণ বাড়ির ঠিকানা ও ল্যান্ডমার্ক",
            useLiveLocation = "আমার লাইভ লোকেশন ব্যবহার করুন",
            locationSaved = "জিপিএস লোকেশন সংরক্ষিত হয়েছে",
            saveContinue = "সেভ করুন ও কেনাকাটা শুরু করুন",
            shopLocationTapMaps = "গুগল ম্যাপে দেখতে ট্যাপ করুন",
            openingHours = "খোলা থাকে",
            searchPlaceholder = "চাল, তেল, চা বা ইংরেজিতে খুঁজুন...",
            allCategories = "সব",
            outOfStock = "স্টক শেষ",
            similarAlternatives = "বিকল্প উপলব্ধ জিনিস:",
            add = "যোগ করুন",
            viewCart = "ঝুড়ি দেখুন",
            cartTitle = "আপনার বাজারের ঝুড়ি",
            itemsTotal = "জিনিসের মোট দাম",
            deliveryCharge = "ডেলিভারি চার্জ",
            grandTotal = "সর্বমোট বিল",
            minOrderWarning = { min -> "সর্বনিম্ন অর্ডার Rs $min। অর্ডার করতে আরও জিনিস যোগ করুন।" },
            paymentMethodTitle = "পেমেন্ট পদ্ধতি বেছে নিন",
            payCash = "ক্যাশ অন ডেলিভারি",
            payUpi = "ইউপিআই (GPay / PhonePe)",
            cashBlockedMsg = "নগদ পেমেন্ট বন্ধ আছে (১-স্টার রেটিং বা অ্যাডমিন লক)। অনুগ্রহ করে UPI দিয়ে পেমেন্ট করুন।",
            shopUpiId = "দোকানের UPI আইডি",
            copyUpiId = "আইডি কপি",
            uploadScreenshot = "পেমেন্ট স্ক্রিনশট দিন",
            screenshotSelected = "স্ক্রিনশট যুক্ত হয়েছে",
            upiTxnNumber = "UPI ট্রানজ্যাকশন নম্বর",
            placeOrder = "অর্ডার কনফার্ম করুন",
            myOrders = "অর্ডার",
            shopTab = "দোকান",
            deliveryTimerLabel = "ডেলিভারি সময় বাকি",
            cancelOrder = "অর্ডার বাতিল করুন",
            cancelWarning = "অর্ডার বাতিল করলে আপনার রেটিং ১ স্টার কমে যাবে। ১ স্টার হলে ক্যাশ বন্ধ হয়ে যাবে।",
            confirmCancel = "হ্যাঁ, বাতিল করুন",
            keepOrder = "অর্ডার রাখুন",
            customerRatingLabel = "আপনার রেটিং",
            packerQueueTitle = "প্যাকিং তালিকা",
            markReady = "প্রস্তুত (Ready)",
            noPackingOrders = "এই মুহূর্তে প্যাক করার জন্য কোনো অর্ডার নেই।",
            adminPanelTitle = "অ্যাডমিন প্যানেল",
            tabOrders = "অর্ডার",
            tabItems = "পণ্য",
            tabCustomers = "গ্রাহক",
            tabReports = "রিপোর্ট",
            tabSettings = "সেটিংস",
            confirmPaymentBtn = "পেমেন্ট নিশ্চিত করুন",
            confirmPaymentWarning = "আগে নিজের ব্যাংক বা ফোনে টাকা এসেছে কিনা দেখুন। শুধু স্ক্রিনশট দেখে নিশ্চিত করবেন না।",
            startPacking = "প্যাকারকে পাঠান",
            outForDeliveryBtn = "ডেলিভারিতে বেরিয়েছে",
            markDeliveredBtn = "ডেলিভারি সম্পন্ন",
            cashReceivedBtn = "নগদ টাকা পাওয়া গেছে",
            refusedCashBtn = "টাকা দিতে অস্বীকার (-১★)",
            newOrderAlertTitle = "নতুন অর্ডার এসেছে!",
            silenceAlertBtn = "অর্ডার দেখুন ও অ্যালার্ম বন্ধ করুন",
            openInMaps = "ম্যাপে বাড়ি দেখুন"
        )
    }
}
