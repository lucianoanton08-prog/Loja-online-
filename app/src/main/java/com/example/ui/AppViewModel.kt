package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

// Unified screens representation
enum class AppScreen {
    HOME,
    DETAIL,
    CART,
    FAVORITES,
    ORDER_LIST,
    ADMIN_LOGIN,
    ADMIN_DASHBOARD,
    AI_ASSISTANT
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DataRepository(application)
    private val context: Context = application.applicationContext

    // UI state states
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _selectedProduct = MutableStateFlow<ProductEntity?>(null)
    val selectedProduct: StateFlow<ProductEntity?> = _selectedProduct.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    // Checkout user details
    val clientName = MutableStateFlow("")
    val clientLocation = MutableStateFlow("")

    // Admin state
    private val _adminIsAuthenticated = MutableStateFlow(false)
    val adminIsAuthenticated: StateFlow<Boolean> = _adminIsAuthenticated.asStateFlow()

    // Coupon state
    private val _activeCoupon = MutableStateFlow<String?>(null)
    val activeCoupon: StateFlow<String?> = _activeCoupon.asStateFlow()
    
    private val _couponDiscountPercentage = MutableStateFlow(0)
    val couponDiscountPercentage: StateFlow<Int> = _couponDiscountPercentage.asStateFlow()

    // AI Chat Messaging State
    data class ChatMessage(val text: String, val isUser: Boolean)
    private val _aiChatMessages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Olá! Sou o Assistente Inteligente da CHOP.KZ. Posso sugerir produtos, ajudá-lo a escolher compras ou gerar relatórios em tempo real de Luanda. Como posso ajudar?", false)
    ))
    val aiChatMessages: StateFlow<List<ChatMessage>> = _aiChatMessages.asStateFlow()

    private val _aiLoading = MutableStateFlow(false)
    val aiLoading: StateFlow<Boolean> = _aiLoading.asStateFlow()

    private val _aiReportContent = MutableStateFlow<String?>(null)
    val aiReportContent: StateFlow<String?> = _aiReportContent.asStateFlow()

    // Reactive streams from Room
    val products: StateFlow<List<ProductEntity>> = combine(
        repository.allProducts,
        _selectedCategory,
        _searchQuery
    ) { all, category, query ->
        var list = all
        if (category != "Todos") {
            list = list.filter { it.category == category }
        }
        if (query.isNotEmpty()) {
            list = list.filter { 
                it.name.contains(query, ignoreCase = true) || 
                it.category.contains(query, ignoreCase = true) ||
                it.description.contains(query, ignoreCase = true)
            }
        }
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartItems: StateFlow<List<CartItemWithProduct>> = repository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<FavoriteItemWithProduct>> = repository.favoriteItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val orders: StateFlow<List<OrderEntity>> = repository.allOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Auto-computed cart totals
    val subtotalState: StateFlow<Double> = cartItems.map { list ->
        list.sumOf { it.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val deliveryFeeState: StateFlow<Double> = cartItems.map { list ->
        if (list.isEmpty()) 0.0 else 2500.0 // Standard in Luanda as requested
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val discountState: StateFlow<Double> = combine(subtotalState, _couponDiscountPercentage) { subtotal, discountPercent ->
        subtotal * (discountPercent.toDouble() / 100.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val finalTotalState: StateFlow<Double> = combine(subtotalState, deliveryFeeState, discountState) { subtotal, delivery, discount ->
        maxOf(0.0, (subtotal + delivery) - discount)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    init {
        // Initialize Room DB with local mockup-premium data on first run
        viewModelScope.launch {
            repository.seedDatabase()
        }
    }

    // --- Actions ---
    fun setScreen(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectProduct(product: ProductEntity) {
        _selectedProduct.value = product
        setScreen(AppScreen.DETAIL)
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    // --- Cart Actions ---
    fun addToCart(productId: Int) {
        viewModelScope.launch {
            repository.addToCart(productId)
            Toast.makeText(context, "Adicionado ao carrinho com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateCartQuantity(productId: Int, newQty: Int) {
        viewModelScope.launch {
            repository.updateCartQuantity(productId, newQty)
        }
    }

    fun removeFromCart(productId: Int) {
        viewModelScope.launch {
            repository.removeFromCart(productId)
            Toast.makeText(context, "Item removido do carrinho", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearCart() {
        viewModelScope.launch {
            repository.clearCart()
            Toast.makeText(context, "Carrinho limpo", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Favorite Actions ---
    fun toggleFavorite(productId: Int) {
        viewModelScope.launch {
            repository.toggleFavorite(productId)
        }
    }

    fun isFavorite(productId: Int): Flow<Boolean> {
        return repository.isFavoriteFlow(productId)
    }

    // --- Panel Admin Actions ---
    fun checkPasscode(passcode: String): Boolean {
        return if (passcode == "Toyota1212hd") {
            _adminIsAuthenticated.value = true
            setScreen(AppScreen.ADMIN_DASHBOARD)
            true
        } else {
            false
        }
    }

    fun adminLogout() {
        _adminIsAuthenticated.value = false
        setScreen(AppScreen.HOME)
    }

    fun addProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.insertProduct(product)
            Toast.makeText(context, "Produto adicionado com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    fun editProduct(product: ProductEntity) {
        viewModelScope.launch {
            repository.updateProduct(product)
            Toast.makeText(context, "Produto atualizado com sucesso!", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteProduct(productId: Int) {
        viewModelScope.launch {
            repository.deleteProductById(productId)
            Toast.makeText(context, "Produto removido!", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Discount Coupons ---
    fun applyCoupon(code: String): Boolean {
        val uppercaseCode = code.trim().uppercase()
        val discount = when (uppercaseCode) {
            "CHOP15" -> 15
            "ANGOLA10" -> 10
            "TEMU20" -> 20
            "CLIENTEPRO" -> 25
            else -> 0
        }
        return if (discount > 0) {
            _activeCoupon.value = uppercaseCode
            _couponDiscountPercentage.value = discount
            Toast.makeText(context, "Cupão $uppercaseCode aplicado com sucesso!", Toast.LENGTH_SHORT).show()
            true
        } else {
            Toast.makeText(context, "Cupão inválido ou expirado", Toast.LENGTH_SHORT).show()
            false
        }
    }

    fun removeCoupon() {
        _activeCoupon.value = null
        _couponDiscountPercentage.value = 0
    }

    // --- WhatsApp integration ---
    fun checkoutWithWhatsApp(clientNameVal: String, clientLocationVal: String) {
        viewModelScope.launch {
            val items = cartItems.value
            if (items.isEmpty()) {
                Toast.makeText(context, "O seu carrinho está vazio!", Toast.LENGTH_SHORT).show()
                return@launch
            }

            if (clientNameVal.trim().isEmpty() || clientLocationVal.trim().isEmpty()) {
                Toast.makeText(context, "Por favor preencha o seu nome e localização!", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Cache values
            clientName.value = clientNameVal
            clientLocation.value = clientLocationVal

            val subtotal = subtotalState.value
            val deliveryFee = deliveryFeeState.value
            val total = finalTotalState.value
            val couponCode = activeCoupon.value
            val discountAmt = discountState.value

            // Format items list
            val itemsLines = items.joinToString("\n") { item ->
                "• ${item.name} (${item.quantity}x) - ${formatMoney(item.price * item.quantity)}"
            }

            // Compose WhatsApp message template
            val templateMessage = """
                Olá Equipa CHOP.KZ.
                Desejo fazer a seguinte encomenda:
                
                *LISTA DOS PRODUTOS:*
                $itemsLines
                
                *VALORES:*
                Subtotal: ${formatMoney(subtotal)}
                ${if (discountAmt > 0) "Desconto (Cupão $couponCode): -${formatMoney(discountAmt)}\n" else ""}Taxa de entrega: ${formatMoney(deliveryFee)}
                *Total Final:* ${formatMoney(total)}
                
                *DADOS DO CLIENTE:*
                Nome do cliente: ${clientNameVal.trim()}
                Localização: ${clientLocationVal.trim()}
                
                Obrigado.
            """.trimIndent()

            // Save order inside database for reports
            repository.placeOrder(clientNameVal, clientLocationVal)

            // Trigger WhatsApp Intent
            val phoneNumber = "+244956849231"
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(templateMessage)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                // If Whatsapp is not installed, open web link
                val webUri = Uri.parse("https://wa.me/$phoneNumber?text=${Uri.encode(templateMessage)}")
                val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(webIntent)
                } catch (e2: Exception) {
                    Toast.makeText(context, "Não foi possível abrir o WhatsApp. Por favor, envie uma mensagem para o número $phoneNumber", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- AI Assistant Dialog ---
    fun sendAiMessage(userText: String) {
        if (userText.trim().isEmpty()) return
        
        // Add user message
        _aiChatMessages.value = _aiChatMessages.value + ChatMessage(userText, true)
        _aiLoading.value = true

        viewModelScope.launch {
            val systemInstruction = """
                Tu és a Inteligência Artificial especializada da CHOP.KZ, a maior plataforma premium de compras online de Angola.
                Inspirado em recursos avançados de busca, sugestão de produtos relacionados e relatórios de vendas.
                Responde em português de Angola com simpatia profissional e confiança.
                Sempre aconselha produtos que fazem sentido, calcula o custo de entrega de 2.500 Kz para Luanda, Angola.
                Podes orientar sobre cupões disponíveis: CHOP15 (15%), ANGOLA10 (10%), TEMU20 (20%) e CLIENTEPRO (25%).
            """.trimIndent()

            val response = GeminiServiceClient.generateResponse(userText, systemInstruction)
            
            _aiChatMessages.value = _aiChatMessages.value + ChatMessage(response, false)
            _aiLoading.value = false
        }
    }

    fun askAiProductRecommendation(productName: String) {
        setScreen(AppScreen.AI_ASSISTANT)
        sendAiMessage("Podes sugerir produtos relacionados ou complementares para o produto: $productName?")
    }

    // --- AI Smart Analytics Report ---
    fun generateAiSalesReport() {
        _aiLoading.value = true
        _aiReportContent.value = null

        viewModelScope.launch {
            val currentProducts = products.value
            val currentOrders = orders.value

            val productsSummary = currentProducts.joinToString("\n") { 
                "- ${it.name} (${it.category}): ${formatMoney(it.price)} | Vendas: ${it.salesCount} | Nota: ${it.rating}"
            }

            val ordersSummary = if (currentOrders.isEmpty()) {
                "Nenhuma encomenda real registada no dispositivo ainda (Modo de simulação estratégica ativo)."
            } else {
                currentOrders.joinToString("\n") {
                    "- Emolumento de ${it.clientName} em ${it.location}: Total ${formatMoney(it.total)} | Itens: ${it.itemsSummary}"
                }
            }

            val queryPrompt = """
                Gere um Relatório Geral de Comércio Eletrónico Inteligente para a marca CHOP.KZ.
                
                Aqui estão os produtos disponíveis no catálogo atual:
                $productsSummary
                
                E aqui estão as encomendas registadas recentemente no banco de dados local:
                $ordersSummary
                
                Por favor, faça uma análise de inteligência artificial de vendas:
                1. Quais são as categorias que mais convertem em Angola?
                2. Calcule os totais automaticamente e projete vendas. 
                3. Sugira produtos para renovação (mais procurados) e sugestões de re-stocking.
                4. Mostre tendências inteligentes baseadas no comportamento do consumidor de Luanda.
                Formate com design visual nobre e emojis. Muito profissional.
            """.trimIndent()

            val response = GeminiServiceClient.generateResponse(queryPrompt)
            _aiReportContent.value = response
            _aiLoading.value = false
        }
    }

    // --- Money Formatter Helper ---
    fun formatMoney(amount: Double): String {
        val numberFormat = NumberFormat.getNumberInstance(Locale("pt", "AO"))
        return "${numberFormat.format(amount.toLong())} Kz"
    }
}
