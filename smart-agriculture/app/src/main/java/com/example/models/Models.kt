package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "users")
@JsonClass(generateAdapter = true)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val email: String,
    val phone: String,
    val role: String, // "Farmer", "Buyer", "Transport"
    val isLoggedIn: Boolean = false,
    val token: String? = null,
    val area: String = "",
    val farmImage: String = ""
)

@Entity(tableName = "products")
@JsonClass(generateAdapter = true)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "Vegetables", "Fruits", "Grains", "Seeds", "Organic Products"
    val price: Double,
    val unit: String, // "kg", "quintal", "g"
    val stock: Double,
    val imageUrl: String,
    val farmerId: Int,
    val farmerName: String,
    val description: String,
    val isFavorite: Boolean = false
)

@Entity(tableName = "cart")
@JsonClass(generateAdapter = true)
data class CartItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productId: Int,
    val productName: String,
    val price: Double,
    val unit: String,
    val imageUrl: String,
    val quantity: Double,
    val farmerId: Int
)

@Entity(tableName = "orders")
@JsonClass(generateAdapter = true)
data class Order(
    @PrimaryKey val orderId: String,
    val buyerId: Int,
    val buyerName: String,
    val buyerPhone: String,
    val address: String,
    val paymentMethod: String, // "COD", "UPI", "Card"
    val totalAmount: Double,
    val status: String, // "Pending", "Confirmed", "Packed", "Shipped", "Delivered"
    val timestamp: Long = System.currentTimeMillis(),
    val estimatedDelivery: String = "3 days from now",
    val deliveryPersonName: String = "John Doe",
    val deliveryPersonPhone: String = "+1 234-567-8900",
    val availabilityDate: String = "Anytime"
)

@Entity(tableName = "order_items")
@JsonClass(generateAdapter = true)
data class OrderItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderId: String,
    val productId: Int,
    val productName: String,
    val price: Double,
    val quantity: Double,
    val unit: String
)

@Entity(tableName = "diseases")
@JsonClass(generateAdapter = true)
data class DiseaseReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cropName: String,
    val diseaseName: String,
    val confidence: Double,
    val symptoms: String,
    val causes: String,
    val prevention: String,
    val treatment: String,
    val recommendedFertilizer: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imagePath: String? = null // local URI of leaf image
)

@Entity(tableName = "fertilizer_recommendations")
@JsonClass(generateAdapter = true)
data class FertilizerRecommendation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cropName: String,
    val soilType: String,
    val diseaseType: String,
    val weather: String,
    val farmArea: Double,
    val fertilizerName: String,
    val quantityRequired: String,
    val applicationMethod: String,
    val bestTime: String,
    val organicAlternatives: String,
    val estimatedCost: Double,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "weather_history")
@JsonClass(generateAdapter = true)
data class WeatherHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val temp: Double,
    val humidity: Int,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "notifications")
@JsonClass(generateAdapter = true)
data class Notification(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val message: String,
    val type: String, // "Order", "Weather", "Disease", "Fertilizer"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

@Entity(tableName = "chatbot_messages")
@JsonClass(generateAdapter = true)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "forum_posts")
@JsonClass(generateAdapter = true)
data class ForumPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val authorId: Int,
    val authorName: String,
    val authorRole: String,
    val category: String, // "Pest Control", "Farming Techniques", "Market Prices"
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "forum_replies")
@JsonClass(generateAdapter = true)
data class ForumReply(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val authorId: Int,
    val authorName: String,
    val authorRole: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "crop_fields")
@JsonClass(generateAdapter = true)
data class CropField(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val farmerId: Int,
    val fieldName: String,
    val cropType: String,
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val soilPh: Double,
    val temp: String,
    val humidity: String,
    val rainProb: String
)


