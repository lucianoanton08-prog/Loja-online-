package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // --- Products ---
    @Query("SELECT * FROM products ORDER BY id DESC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY id DESC")
    fun getProductsByCategory(category: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%'")
    fun searchProducts(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Int): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity): Long

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)

    @Query("DELETE FROM products WHERE id = :productId")
    suspend fun deleteProductById(productId: Int)

    // --- Cart ---
    @Query("SELECT * FROM cart_items ORDER BY addedAt DESC")
    fun getCartItems(): Flow<List<CartEntity>>

    @Query("""
        SELECT c.productId, c.quantity, c.addedAt, 
               p.name, p.category, p.price, p.oldPrice, p.discountPercentage, p.imageUrl, p.deliveryFee
        FROM cart_items c 
        INNER JOIN products p ON c.productId = p.id
        ORDER BY c.addedAt DESC
    """)
    fun getCartItemsWithProduct(): Flow<List<CartItemWithProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCartItem(cart: CartEntity)

    @Update
    suspend fun updateCartItem(cart: CartEntity)

    @Delete
    suspend fun deleteCartItem(cart: CartEntity)

    @Query("DELETE FROM cart_items WHERE productId = :productId")
    suspend fun deleteCartItemByProductId(productId: Int)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()

    // --- Favorites ---
    @Query("SELECT * FROM favorites ORDER BY addedAt DESC")
    fun getFavorites(): Flow<List<FavoriteEntity>>

    @Query("""
        SELECT f.productId, 
               p.name, p.category, p.price, p.oldPrice, p.discountPercentage, p.imageUrl, p.deliveryFee, p.description, p.availability, p.rating, p.reviewsCount, p.salesCount
        FROM favorites f
        INNER JOIN products p ON f.productId = p.id
        ORDER BY f.addedAt DESC
    """)
    fun getFavoriteItemsWithProduct(): Flow<List<FavoriteItemWithProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE productId = :productId")
    suspend fun deleteFavoriteByProductId(productId: Int)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE productId = :productId)")
    suspend fun isFavorite(productId: Int): Boolean

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE productId = :productId)")
    fun isFavoriteFlow(productId: Int): Flow<Boolean>

    // --- Orders ---
    @Query("SELECT * FROM orders ORDER BY orderDate DESC")
    fun getAllOrders(): Flow<List<OrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity): Long
}

// Helper POJOs for unified reactive updates
data class CartItemWithProduct(
    val productId: Int,
    val quantity: Int,
    val addedAt: Long,
    val name: String,
    val category: String,
    val price: Double,
    val oldPrice: Double?,
    val discountPercentage: Int,
    val imageUrl: String,
    val deliveryFee: Double
)

data class FavoriteItemWithProduct(
    val productId: Int,
    val name: String,
    val category: String,
    val price: Double,
    val oldPrice: Double?,
    val discountPercentage: Int,
    val imageUrl: String,
    val deliveryFee: Double,
    val description: String,
    val availability: String,
    val rating: Float,
    val reviewsCount: Int,
    val salesCount: Int
)
