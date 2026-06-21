package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String,
    val price: Double,
    val oldPrice: Double? = null,
    val discountPercentage: Int = 0,
    val imageUrl: String, // Can be drawable resource name like "ic_phone", "img_product_...", etc. or empty
    val description: String,
    val availability: String = "Em Stock", // "Em Stock", "Esgotado", "Poucas Unidades"
    val rating: Float = 4.5f,
    val reviewsCount: Int = 12,
    val salesCount: Int = 45,
    val deliveryFee: Double = 2500.0,
    val isFeatured: Boolean = false,
    val isPromoOfDay: Boolean = false,
    val isBestSeller: Boolean = false,
    val isNew: Boolean = false
)

@Entity(tableName = "cart_items")
data class CartEntity(
    @PrimaryKey val productId: Int,
    val quantity: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val productId: Int,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val orderDate: Long = System.currentTimeMillis(),
    val clientName: String,
    val location: String,
    val itemsSummary: String, // Text summary of the items (e.g. "iPhone 15 Pro x1, Mackbook x2")
    val subtotal: Double,
    val deliveryFee: Double,
    val total: Double,
    val status: String = "Pendente" // "Pendente", "A Caminho", "Entregue"
)
