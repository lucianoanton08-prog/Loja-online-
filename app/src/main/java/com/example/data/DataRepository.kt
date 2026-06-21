package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DataRepository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.appDao()

    // Flow listings
    val allProducts: Flow<List<ProductEntity>> = dao.getAllProducts()
    val cartItems: Flow<List<CartItemWithProduct>> = dao.getCartItemsWithProduct()
    val favoriteItems: Flow<List<FavoriteItemWithProduct>> = dao.getFavoriteItemsWithProduct()
    val allOrders: Flow<List<OrderEntity>> = dao.getAllOrders()

    fun getProductsByCategory(category: String): Flow<List<ProductEntity>> = dao.getProductsByCategory(category)
    fun searchProducts(query: String): Flow<List<ProductEntity>> = dao.searchProducts(query)
    fun isFavoriteFlow(productId: Int): Flow<Boolean> = dao.isFavoriteFlow(productId)

    suspend fun getProductById(id: Int): ProductEntity? = withContext(Dispatchers.IO) {
        dao.getProductById(id)
    }

    // --- Product management (used by panel admin) ---
    suspend fun insertProduct(product: ProductEntity): Long = withContext(Dispatchers.IO) {
        dao.insertProduct(product)
    }

    suspend fun updateProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        dao.updateProduct(product)
    }

    suspend fun deleteProduct(product: ProductEntity) = withContext(Dispatchers.IO) {
        dao.deleteProduct(product)
    }

    suspend fun deleteProductById(productId: Int) = withContext(Dispatchers.IO) {
        dao.deleteProductById(productId)
    }

    // --- Cart operations ---
    suspend fun addToCart(productId: Int, quantity: Int = 1) = withContext(Dispatchers.IO) {
        val existing = dao.getCartItems().first().find { it.productId == productId }
        if (existing != null) {
            dao.insertCartItem(existing.copy(quantity = existing.quantity + quantity))
        } else {
            dao.insertCartItem(CartEntity(productId = productId, quantity = quantity))
        }
    }

    suspend fun updateCartQuantity(productId: Int, newQuantity: Int) = withContext(Dispatchers.IO) {
        if (newQuantity <= 0) {
            dao.deleteCartItemByProductId(productId)
        } else {
            val existing = dao.getCartItems().first().find { it.productId == productId }
            if (existing != null) {
                dao.insertCartItem(existing.copy(quantity = newQuantity))
            } else {
                dao.insertCartItem(CartEntity(productId = productId, quantity = newQuantity))
            }
        }
    }

    suspend fun removeFromCart(productId: Int) = withContext(Dispatchers.IO) {
        dao.deleteCartItemByProductId(productId)
    }

    suspend fun clearCart() = withContext(Dispatchers.IO) {
        dao.clearCart()
    }

    // --- Favorite operations ---
    suspend fun toggleFavorite(productId: Int) = withContext(Dispatchers.IO) {
        val exists = dao.getFavorites().first().any { it.productId == productId }
        if (exists) {
            dao.deleteFavoriteByProductId(productId)
        } else {
            dao.insertFavorite(FavoriteEntity(productId = productId))
        }
    }

    suspend fun isFavorite(productId: Int): Boolean = withContext(Dispatchers.IO) {
        dao.isFavorite(productId)
    }

    // --- Order placement ---
    suspend fun placeOrder(clientName: String, location: String): OrderEntity? = withContext(Dispatchers.IO) {
        val cartList = dao.getCartItemsWithProduct().first()
        if (cartList.isEmpty()) return@withContext null

        val subtotal = cartList.sumOf { it.price * it.quantity }
        val deliveryFee = 2500.0 // Fixed for Luanda as requested
        val total = subtotal + deliveryFee

        val itemsSummary = cartList.joinToString(", ") { "${it.name} (x${it.quantity})" }

        val order = OrderEntity(
            clientName = clientName,
            location = location,
            itemsSummary = itemsSummary,
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            total = total,
            status = "Pendente"
        )

        val autoId = dao.insertOrder(order)
        dao.clearCart() // Clear cart after ordering!
        order.copy(id = autoId.toInt())
    }

    // --- Database pre-population seeding ---
    suspend fun seedDatabase() = withContext(Dispatchers.IO) {
        val products = dao.getAllProducts().first()
        if (products.isEmpty()) {
            val seedItems = listOf(
                ProductEntity(
                    name = "iPhone 15 Pro Max",
                    category = "📱 Telemóveis",
                    price = 1250000.0,
                    oldPrice = 1450000.0,
                    discountPercentage = 13,
                    imageUrl = "img_iphone",
                    description = "Última tecnologia Apple com camera de 48MP e acabamento em liga de titânio premium. Desempenho profissional.",
                    availability = "Em Stock",
                    rating = 4.9f,
                    reviewsCount = 142,
                    salesCount = 850,
                    isFeatured = true,
                    isBestSeller = true
                ),
                ProductEntity(
                    name = "Samsung Galaxy S24 Ultra",
                    category = "📱 Telemóveis",
                    price = 1100000.0,
                    oldPrice = 1280000.0,
                    discountPercentage = 14,
                    imageUrl = "img_samsung",
                    description = "O poder da inteligência artificial Galaxy AI, camera zoom 100x e S-Pen inclusa para produtividade avançada.",
                    availability = "Em Stock",
                    rating = 4.8f,
                    reviewsCount = 98,
                    salesCount = 590,
                    isNew = true
                ),
                ProductEntity(
                    name = "MacBook Pro 16\" M3 Max",
                    category = "💻 Computadores",
                    price = 3400000.0,
                    oldPrice = 3800000.0,
                    discountPercentage = 10,
                    imageUrl = "img_macbook",
                    description = "O notebook mais avançado do mundo para criadores de conteúdo e profissionais exigentes. Ecrã Liquid Retina XDR.",
                    availability = "Em Stock",
                    rating = 5.0f,
                    reviewsCount = 37,
                    salesCount = 120,
                    isFeatured = true
                ),
                ProductEntity(
                    name = "Portátil Gaming HP Victus",
                    category = "💻 Computadores",
                    price = 950000.0,
                    oldPrice = 1150000.0,
                    discountPercentage = 17,
                    imageUrl = "img_hp_victus",
                    description = "Processador Intel Core i7 de 13ª geração com placa NVIDIA RTX 4050 para jogar em alta performance em Luanda.",
                    availability = "Em Stock",
                    rating = 4.6f,
                    reviewsCount = 55,
                    salesCount = 205,
                    isBestSeller = true
                ),
                ProductEntity(
                    name = "Auscultadores Sony WH-1000XM5",
                    category = "🎧 Acessórios",
                    price = 350000.0,
                    oldPrice = 420000.0,
                    discountPercentage = 16,
                    imageUrl = "img_sony_headphones",
                    description = "Cancelamento de ruído líder de mercado com áudio de alta resolução e autonomia de 30 horas incríveis.",
                    availability = "Em Stock",
                    rating = 4.9f,
                    reviewsCount = 245,
                    salesCount = 1080,
                    isFeatured = true,
                    isBestSeller = true
                ),
                ProductEntity(
                    name = "Apple AirPods Pro 2",
                    category = "🎧 Acessórios",
                    price = 210000.0,
                    oldPrice = 260000.0,
                    discountPercentage = 19,
                    imageUrl = "img_airpods",
                    description = "Com áudio espacial personalizado e cancelamento de ruído inteligente ativo. Conforto extraordinário.",
                    availability = "Poucas Unidades",
                    rating = 4.7f,
                    reviewsCount = 412,
                    salesCount = 2500,
                    isPromoOfDay = true
                ),
                ProductEntity(
                    name = "Apple Watch Ultra 2",
                    category = "⌚ Relógios",
                    price = 790000.0,
                    oldPrice = 890000.0,
                    discountPercentage = 11,
                    imageUrl = "img_apple_watch",
                    description = "O relógio desportivo absoluto com GPS de dupla frequência de alta precisão e 36 horas de bateria em uso ativo.",
                    availability = "Em Stock",
                    rating = 4.8f,
                    reviewsCount = 64,
                    salesCount = 310,
                    isNew = true
                ),
                ProductEntity(
                    name = "Relógio Automático Rolex Submariner",
                    category = "⌚ Relógios",
                    price = 12500000.0,
                    oldPrice = null,
                    discountPercentage = 0,
                    imageUrl = "img_rolex",
                    description = "Uma lenda dos relógios de luxo. Design icónico em aço Oystersteel durável com calibre automático preciso de 70h reserva.",
                    availability = "Poucas Unidades",
                    rating = 5.0f,
                    reviewsCount = 8,
                    salesCount = 15,
                    isFeatured = true
                ),
                ProductEntity(
                    name = "PlayStation 5 Slim 1TB",
                    category = "🎮 Gaming",
                    price = 650000.0,
                    oldPrice = 750000.0,
                    discountPercentage = 13,
                    imageUrl = "img_ps5",
                    description = "A consola de última geração da Sony com SSD de altíssima velocidade e suporte a gráficos 4K vibrantes.",
                    availability = "Em Stock",
                    rating = 4.9f,
                    reviewsCount = 190,
                    salesCount = 1150,
                    isFeatured = true,
                    isBestSeller = true
                ),
                ProductEntity(
                    name = "Cadeira Gaming Ergónomica",
                    category = "🎮 Gaming",
                    price = 180000.0,
                    oldPrice = 220000.0,
                    discountPercentage = 18,
                    imageUrl = "img_gaming_chair",
                    description = "Pele sintética premium, apoio cervical e lombar reguláveis para máximo conforto durante longas sessões de trabalho ou jogo.",
                    availability = "Em Stock",
                    rating = 4.5f,
                    reviewsCount = 84,
                    salesCount = 320
                ),
                ProductEntity(
                    name = "Sapatilhas Nike Air Force 1 '07",
                    category = "👟 Calçados",
                    price = 95000.0,
                    oldPrice = 120000.0,
                    discountPercentage = 20,
                    imageUrl = "img_nike",
                    description = "O clássico do basquetebol e do streetwear com amortecimento macio Nike Air e solado de borracha aderente.",
                    availability = "Em Stock",
                    rating = 4.8f,
                    reviewsCount = 520,
                    salesCount = 3800,
                    isBestSeller = true,
                    isPromoOfDay = true
                ),
                ProductEntity(
                    name = "Sapatilhas Adidas Samba OG",
                    category = "👟 Calçados",
                    price = 85000.0,
                    oldPrice = null,
                    discountPercentage = 0,
                    imageUrl = "img_adidas",
                    description = "Nascidas para os relvados e adotadas pelas ruas de Luanda. Listras retro clássicas que nunca passam de moda.",
                    availability = "Em Stock",
                    rating = 4.7f,
                    reviewsCount = 188,
                    salesCount = 1420,
                    isNew = true
                ),
                ProductEntity(
                    name = "Perfume Dior Sauvage Eau de Parfum",
                    category = "💄 Beleza",
                    price = 140000.0,
                    oldPrice = 170000.0,
                    discountPercentage = 17,
                    imageUrl = "img_sauvage",
                    description = "Fragrância amadeirada nobre e fresca. O perfume mais vendido do mundo para o homem elegante e misterioso.",
                    availability = "Em Stock",
                    rating = 4.9f,
                    reviewsCount = 310,
                    salesCount = 1680,
                    isBestSeller = true
                ),
                ProductEntity(
                    name = "Robô Aspirador Xiaomi S10+",
                    category = "🏠 Casa",
                    price = 220000.0,
                    oldPrice = 270000.0,
                    discountPercentage = 18,
                    imageUrl = "img_robot_vacuum",
                    description = "Mapeamento inteligente a laser, aspiração forte de 4000Pa e mopa húmida rotativa dupla para manter a sua casa limpa.",
                    availability = "Em Stock",
                    rating = 4.6f,
                    reviewsCount = 42,
                    salesCount = 225
                ),
                ProductEntity(
                    name = "Smart TV LG QNED 65\"",
                    category = "🏠 Casa",
                    price = 1150000.0,
                    oldPrice = 1350000.0,
                    discountPercentage = 14,
                    imageUrl = "img_smart_tv",
                    description = "Cores ricas Quantum Dot NanoCell puras, processador avançado Gen6 com canais inteligentes de IA e som surround.",
                    availability = "Disponível por Encomenda",
                    rating = 4.7f,
                    reviewsCount = 29,
                    salesCount = 115,
                    isPromoOfDay = true
                )
            )

            for (item in seedItems) {
                dao.insertProduct(item)
            }
        }
    }
}
