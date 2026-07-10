package com.example.database

import androidx.room.*
import com.example.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getActiveUserFlow(): Flow<User?>

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getActiveUser(): User?

    @Query("SELECT * FROM users WHERE role = 'Farmer'")
    fun getAllFarmersFlow(): Flow<List<User>>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAll()

    @Query("DELETE FROM users")
    suspend fun clearUsers()
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY id DESC")
    fun getProductsByCategory(category: String): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE farmerId = :farmerId ORDER BY id DESC")
    fun getFarmerProducts(farmerId: Int): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)

    @Delete
    suspend fun deleteProduct(product: Product)

    @Query("UPDATE products SET isFavorite = :isFavorite WHERE id = :productId")
    suspend fun updateFavorite(productId: Int, isFavorite: Boolean)

    @Query("SELECT * FROM products WHERE isFavorite = 1")
    fun getFavoriteProducts(): Flow<List<Product>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllProducts(products: List<Product>)
}

@Dao
interface CartDao {
    @Query("SELECT * FROM cart")
    fun getCartItems(): Flow<List<CartItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(item: CartItem)

    @Query("UPDATE cart SET quantity = :quantity WHERE id = :id")
    suspend fun updateQuantity(id: Int, quantity: Double)

    @Query("DELETE FROM cart WHERE id = :id")
    suspend fun deleteCartItem(id: Int)

    @Query("DELETE FROM cart")
    suspend fun clearCart()
}

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY timestamp DESC")
    fun getAllOrders(): Flow<List<Order>>

    @Query("SELECT * FROM orders WHERE buyerId = :buyerId ORDER BY timestamp DESC")
    fun getOrdersForBuyer(buyerId: Int): Flow<List<Order>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItem>)

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    fun getOrderItems(orderId: String): Flow<List<OrderItem>>

    @Query("UPDATE orders SET status = :status WHERE orderId = :orderId")
    suspend fun updateOrderStatus(orderId: String, status: String)

    @Query("UPDATE orders SET address = :address WHERE orderId = :orderId")
    suspend fun updateOrderAddress(orderId: String, address: String)

    @Query("UPDATE orders SET availabilityDate = :availabilityDate WHERE orderId = :orderId")
    suspend fun updateOrderAvailabilityDate(orderId: String, availabilityDate: String)
}

@Dao
interface DiseaseDao {
    @Query("SELECT * FROM diseases ORDER BY timestamp DESC")
    fun getDiseaseReports(): Flow<List<DiseaseReport>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: DiseaseReport)
}

@Dao
interface RecommendationDao {
    @Query("SELECT * FROM fertilizer_recommendations ORDER BY timestamp DESC")
    fun getRecommendations(): Flow<List<FertilizerRecommendation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecommendation(rec: FertilizerRecommendation)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getNotifications(): Flow<List<Notification>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notif: Notification)

    @Query("UPDATE notifications SET isRead = 1")
    suspend fun markAllAsRead()
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chatbot_messages ORDER BY timestamp ASC")
    fun getChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMessage)

    @Query("DELETE FROM chatbot_messages")
    suspend fun clearChat()
}

@Dao
interface ForumDao {
    @Query("SELECT * FROM forum_posts ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<ForumPost>>

    @Query("SELECT * FROM forum_posts WHERE category = :category ORDER BY timestamp DESC")
    fun getPostsByCategory(category: String): Flow<List<ForumPost>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: ForumPost)

    @Delete
    suspend fun deletePost(post: ForumPost)

    @Query("SELECT * FROM forum_replies WHERE postId = :postId ORDER BY timestamp ASC")
    fun getRepliesForPost(postId: Int): Flow<List<ForumReply>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReply(reply: ForumReply)

    @Delete
    suspend fun deleteReply(reply: ForumReply)

    @Query("DELETE FROM forum_replies WHERE postId = :postId")
    suspend fun deleteRepliesByPostId(postId: Int)
}

@Dao
interface CropFieldDao {
    @Query("SELECT * FROM crop_fields WHERE farmerId = :farmerId")
    fun getFieldsForFarmer(farmerId: Int): Flow<List<CropField>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertField(field: CropField)

    @Delete
    suspend fun deleteField(field: CropField)

    @Query("DELETE FROM crop_fields WHERE farmerId = :farmerId")
    suspend fun clearFieldsForFarmer(farmerId: Int)
}

@Database(
    entities = [
        User::class, Product::class, CartItem::class, Order::class,
        OrderItem::class, DiseaseReport::class, FertilizerRecommendation::class,
        WeatherHistory::class, Notification::class, ChatMessage::class,
        ForumPost::class, ForumReply::class, CropField::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun productDao(): ProductDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao
    abstract fun diseaseDao(): DiseaseDao
    abstract fun recommendationDao(): RecommendationDao
    abstract fun notificationDao(): NotificationDao
    abstract fun chatDao(): ChatDao
    abstract fun forumDao(): ForumDao
    abstract fun cropFieldDao(): CropFieldDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_agriculture_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
