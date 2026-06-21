package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.CartItemWithProduct
import com.example.ui.theme.MyApplicationTheme
import java.text.NumberFormat
import java.util.Locale
import com.example.data.FavoriteItemWithProduct
import com.example.data.OrderEntity
import com.example.data.ProductEntity
import com.example.ui.theme.ChopGreen
import com.example.ui.theme.ChopDarkGreen
import com.example.ui.theme.ChopOrange
import com.example.ui.theme.ChopBlack
import com.example.ui.theme.ChopWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppUi(viewModel: AppViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

    MyApplicationTheme(darkTheme = isDark) {
        Scaffold(
            bottomBar = {
                // Persistent bottom navigation menu for professional multi-screen ergonomics
                ChopBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.setScreen(it) },
                    cartCount = viewModel.cartItems.collectAsState().value.sumOf { it.quantity },
                    favoritesCount = viewModel.favorites.collectAsState().value.size
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) with fadeOut(animationSpec = tween(220))
                    },
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        AppScreen.HOME -> HomeScreen(viewModel)
                        AppScreen.DETAIL -> ProductDetailScreen(viewModel)
                        AppScreen.CART -> CartScreen(viewModel)
                        AppScreen.FAVORITES -> FavoritesScreen(viewModel)
                        AppScreen.ORDER_LIST -> OrdersScreen(viewModel)
                        AppScreen.ADMIN_LOGIN -> AdminLoginScreen(viewModel)
                        AppScreen.ADMIN_DASHBOARD -> AdminDashboardScreen(viewModel)
                        AppScreen.AI_ASSISTANT -> AiAssistantScreen(viewModel)
                    }
                }
            }
        }
    }
}

// --- Bottom Navigation Composable ---
@Composable
fun ChopBottomBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    cartCount: Int,
    favoritesCount: Int
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        modifier = Modifier.testTag("main_navigation_bar")
    ) {
        val items = listOf(
            NavigationItem("Início", Icons.Default.Home, AppScreen.HOME),
            NavigationItem("Favoritos", Icons.Default.Favorite, AppScreen.FAVORITES, badgeCount = favoritesCount),
            NavigationItem("Carrinho", Icons.Default.ShoppingCart, AppScreen.CART, badgeCount = cartCount),
            NavigationItem("IA Chat", Icons.Default.Star, AppScreen.AI_ASSISTANT),
            NavigationItem("Admin", Icons.Default.Person, AppScreen.ADMIN_LOGIN)
        )

        items.forEach { item ->
            val selected = when (item.screen) {
                AppScreen.HOME -> currentScreen == AppScreen.HOME || currentScreen == AppScreen.DETAIL
                AppScreen.ADMIN_LOGIN -> currentScreen == AppScreen.ADMIN_LOGIN || currentScreen == AppScreen.ADMIN_DASHBOARD || currentScreen == AppScreen.ORDER_LIST
                else -> currentScreen == item.screen
            }

            NavigationBarItem(
                selected = selected,
                onClick = { onNavigate(item.screen) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (item.badgeCount > 0) {
                                Badge(
                                    containerColor = ChopOrange,
                                    contentColor = Color.White
                                ) {
                                    Text(item.badgeCount.toString(), fontSize = 10.sp)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = if (selected) ChopGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) ChopGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = ChopGreen.copy(alpha = 0.15f)
                ),
                modifier = Modifier.testTag("nav_item_${item.label.lowercase()}")
            )
        }
    }
}

data class NavigationItem(
    val label: String,
    val icon: ImageVector,
    val screen: AppScreen,
    val badgeCount: Int = 0
)

// --- Shared Dynamic Composable for Product Image rendering to keep UI crash-free ---
@Composable
fun ProductImageRenderer(
    name: String,
    category: String,
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageResId = remember(imageUrl) {
        if (imageUrl.isNotEmpty()) {
            context.resources.getIdentifier(imageUrl, "drawable", context.packageName)
        } else 0
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        ChopGreen.copy(alpha = 0.08f),
                        ChopDarkGreen.copy(alpha = 0.15f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (imageResId != 0) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Sophisticated gradient vector fallback matching categories of CHOP.KZ
            val iconEmoji = when (category) {
                "📱 Telemóveis" -> "📱"
                "💻 Computadores" -> "💻"
                "🎧 Acessórios" -> "🎧"
                "⌚ Relógios" -> "⌚"
                "👕 Moda Masculina" -> "👕"
                "👗 Moda Feminina" -> "👗"
                "👟 Calçados" -> "👟"
                "💄 Beleza" -> "💄"
                "🏠 Casa" -> "🏠"
                "🎮 Gaming" -> "🎮"
                "📚 Livros" -> "📚"
                "🚗 Automóveis" -> "🚗"
                else -> "🎁"
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = iconEmoji,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = name.take(12),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChopDarkGreen,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// --- HOME SCREEN ---
@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val products by viewModel.products.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isDark by viewModel.isDarkTheme.collectAsState()

    val promoProducts = remember(products) { products.filter { it.isPromoOfDay } }
    val featuredProducts = remember(products) { products.filter { it.isFeatured } }
    val bestSellers = remember(products) { products.filter { it.isBestSeller } }
    val newArrivals = remember(products) { products.filter { it.isNew } }

    val categories = listOf(
        "Todos",
        "📱 Telemóveis",
        "💻 Computadores",
        "🎧 Acessórios",
        "⌚ Relógios",
        "👕 Moda Masculina",
        "👗 Moda Feminina",
        "👟 Calçados",
        "💄 Beleza",
        "🏠 Casa",
        "🎮 Gaming",
        "📚 Livros",
        "🚗 Automóveis"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Upper Header with Branding, Search Bar, and Dark Mode switch
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "CHOP.KZ",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = ChopGreen,
                            letterSpacing = 1.sp,
                            fontFamily = FontFamily.SansSerif,
                            modifier = Modifier.testTag("app_brand_logo")
                        )
                        Text(
                            text = "Comprar Online Nunca Foi Tão Fácil",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row {
                        // Quick AI Help Trigger
                        IconButton(
                            onClick = { viewModel.setScreen(AppScreen.AI_ASSISTANT) },
                            modifier = Modifier.testTag("ai_assist_top_button")
                        ) {
                            Icon(Icons.Default.Star, "IA Assistente", tint = ChopOrange)
                        }

                        // Theme switch
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("theme_toggle")
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.Share else Icons.Default.Search, // Using default search/share safely as toggles
                                contentDescription = "Alternar Tema",
                                tint = ChopGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Pesquise dezenas de produtos premium em Angola...") },
                    leadingIcon = { Icon(Icons.Default.Search, "Pesquisar", tint = ChopGreen) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, "Limpar", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(30.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChopGreen,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        focusedContainerColor = MaterialTheme.colorScheme.background,
                        unfocusedContainerColor = MaterialTheme.colorScheme.background
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("search_input_bar")
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Rotating Promotional Hero Banner (combining generated graphic with rich UI labels)
            item {
                PromoCarousel()
            }

            // Categories list (Horizontal scrolling professional pill list)
            item {
                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                    Text(
                        text = "Categorias Premium",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = selectedCategory == category
                            val contextColors = if (isSelected) {
                                ButtonDefaults.buttonColors(containerColor = ChopGreen)
                            } else {
                                ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            ElevatedButton(
                                onClick = { viewModel.setCategory(category) },
                                shape = RoundedCornerShape(20.dp),
                                colors = contextColors,
                                border = if (!isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)) else null,
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("category_pill_${category.lowercase().replace(" ", "_")}")
                            ) {
                                Text(
                                    text = category,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            if (selectedCategory != "Todos" || searchQuery.isNotEmpty()) {
                // Filtered Catalog
                item {
                    Text(
                        text = "Resultados Encontrados (${products.size})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                if (products.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("📦", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Nenhum produto encontrado.",
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray
                            )
                            Text(
                                "Aproveite para redefinir o seu filtro ou fazer outra pesquisa.",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    item {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 2000.dp) // Bound height to integrate inside outer LazyColumn
                        ) {
                            items(products) { item ->
                                ProductCard(product = item, onProductClick = { viewModel.selectProduct(item) })
                            }
                        }
                    }
                }
            } else {
                // Default Home dashboard with curated e-commerce modules

                // Promoções do Dia List (Laranja)
                if (promoProducts.isNotEmpty()) {
                    item {
                        SectionHeader("🔥 Promoções Recorrentes do Dia", ChopOrange) {
                            // Quick Action: apply special coupon automatically
                            viewModel.applyCoupon("CHOP15")
                        }
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(promoProducts) { prod ->
                                RowProductCard(product = prod, onProductClick = { viewModel.selectProduct(prod) })
                            }
                        }
                    }
                }

                // Featured Products Grid
                if (featuredProducts.isNotEmpty()) {
                    item {
                        SectionHeader("💎 Produtos em Destaque", ChopGreen)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 480.dp) // Multi-grid limit
                        ) {
                            items(featuredProducts) { prod ->
                                ProductCard(product = prod, onProductClick = { viewModel.selectProduct(prod) })
                            }
                        }
                    }
                }

                // Best Sellers
                if (bestSellers.isNotEmpty()) {
                    item {
                        SectionHeader("📈 Líderes em Angola (Mais Vendidos)", ChopDarkGreen)
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            items(bestSellers) { prod ->
                                RowProductCard(product = prod, onProductClick = { viewModel.selectProduct(prod) })
                            }
                        }
                    }
                }

                // New Arrivals Grid
                if (newArrivals.isNotEmpty()) {
                    item {
                        SectionHeader("✨ Novidades Fresquinhas", ChopOrange)
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 480.dp)
                        ) {
                            items(newArrivals) { prod ->
                                ProductCard(product = prod, onProductClick = { viewModel.selectProduct(prod) })
                            }
                        }
                    }
                }
            }

            // Trust badging at bottom
            item {
                AngolaTrustFooter(viewModel)
            }
        }
    }
}

// --- CURATED BANNER AND SECTIONS ---
@Composable
fun PromoCarousel() {
    val context = LocalContext.current
    val imageResId = remember {
        context.resources.getIdentifier("img_banner_promo_1", "drawable", context.packageName)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(155.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (imageResId != 0) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = "CHOP.KZ banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Elegant fallback background gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(ChopDarkGreen, ChopGreen)
                            )
                        )
                )
            }

            // Sleek Dark overlay veil for premium contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f))
            )

            // Content
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(16.dp)
                    .align(Alignment.CenterStart),
                verticalArrangement = Arrangement.Center
            ) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("TEMU & ALIBABA INSPIRED", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = ChopOrange) },
                    colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ChopWhite.copy(alpha = 0.9f)),
                    border = null,
                    modifier = Modifier.height(20.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "DESCONTO ATÉ 25% EM TODA LUANDA",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = "Usa o Cupão CLIENTEPRO no carrinho e economiza já!",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, accentColor: Color, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 18.dp)
                    .background(accentColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text("Preencher Cupão IP", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Grid layout Product Card
@Composable
fun ProductCard(product: ProductEntity, onProductClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onProductClick)
            .testTag("product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                ProductImageRenderer(
                    name = product.name,
                    category = product.category,
                    imageUrl = product.imageUrl,
                    modifier = Modifier.fillMaxSize()
                )

                // Extra discount badge floating
                if (product.discountPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = ChopOrange,
                                shape = RoundedCornerShape(bottomEnd = 12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "-${product.discountPercentage}% OFF",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = product.category,
                    fontSize = 10.sp,
                    color = ChopGreen,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = product.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Rating and sales count
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = ChopOrange,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = product.rating.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${product.salesCount} vendidos",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Price display module
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatMoneyLocal(product.price),
                        color = ChopGreen,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (product.oldPrice != null) {
                        Text(
                            text = formatMoneyLocal(product.oldPrice),
                            color = Color.Gray,
                            fontSize = 11.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }
        }
    }
}

// Curated Row product card representing premium lists
@Composable
fun RowProductCard(product: ProductEntity, onProductClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(210.dp)
            .clickable(onClick = onProductClick)
            .testTag("row_product_card_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(105.dp)
            ) {
                ProductImageRenderer(
                    name = product.name,
                    category = product.category,
                    imageUrl = product.imageUrl,
                    modifier = Modifier.fillMaxSize()
                )

                if (product.discountPercentage > 0) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = ChopOrange,
                                shape = RoundedCornerShape(bottomEnd = 12.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "-${product.discountPercentage}% USD",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = product.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${product.salesCount}+ adquiridos",
                    fontSize = 10.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = formatMoneyLocal(product.price),
                        color = ChopGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (product.oldPrice != null) {
                        Text(
                            text = formatMoneyLocal(product.oldPrice),
                            color = Color.Gray,
                            fontSize = 10.sp,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }
        }
    }
}

// --- PRODUCT DETAIL SCREEN ---
@Composable
fun ProductDetailScreen(viewModel: AppViewModel) {
    val product by viewModel.selectedProduct.collectAsState()
    val isFav by if (product != null) viewModel.isFavorite(product!!.id).collectAsState(false) else remember { mutableStateOf(false) }

    val productsList by viewModel.products.collectAsState()
    val relatedProducts = remember(product, productsList) {
        if (product != null) productsList.filter { it.category == product!!.category && it.id != product!!.id } else emptyList()
    }

    if (product == null) return

    Column(modifier = Modifier.fillMaxSize()) {
        // Simple elegant top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = { viewModel.setScreen(AppScreen.HOME) },
                modifier = Modifier.testTag("detail_back_button")
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = ChopBlack)
            }

            Text(
                text = "Detalhes do Produto",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            // Favorited button
            IconButton(
                onClick = { viewModel.toggleFavorite(product!!.id) },
                modifier = Modifier.testTag("detail_fav_button")
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Default.Share, // safe outlines/filled representation
                    contentDescription = "Favoritar",
                    tint = if (isFav) ChopOrange else Color.Gray
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Main Product Hero Image Rendered
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    ProductImageRenderer(
                        name = product!!.name,
                        category = product!!.category,
                        imageUrl = product!!.imageUrl,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Desconto tag oversized
                    if (product!!.discountPercentage > 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .background(ChopOrange, RoundedCornerShape(topStart = 16.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "-${product!!.discountPercentage}% Desconto",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Brand category and status indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = product!!.category.uppercase(),
                        color = ChopGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    // Stock availability label
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (product!!.availability == "Em Stock") ChopGreen.copy(alpha = 0.15f) else ChopOrange.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = product!!.availability,
                            color = if (product!!.availability == "Em Stock") ChopDarkGreen else ChopOrange,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = product!!.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Ratings Block
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Row {
                        repeat(5) { index ->
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (index < product!!.rating.toInt()) ChopOrange else Color.Gray.copy(alpha = 0.5f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${product!!.rating} (${product!!.reviewsCount} avaliações)",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "|   ${product!!.salesCount} encomendados",
                        fontSize = 12.sp,
                        color = ChopDarkGreen,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.15f))

                // Prices display card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Preço CHOP.KZ:",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                modifier = Modifier.weight(1f)
                            )
                            if (product!!.oldPrice != null) {
                                Text(
                                    text = "Antes: ${formatMoneyLocal(product!!.oldPrice!!)}",
                                    fontSize = 12.sp,
                                    textDecoration = TextDecoration.LineThrough,
                                    color = Color.Gray
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = formatMoneyLocal(product!!.price),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = ChopGreen
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Em Luanda",
                                fontSize = 12.sp,
                                color = ChopGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Delivery highlight module required by system guidelines
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(ChopGreen.copy(alpha = 0.06f), RoundedCornerShape(4.dp))
                                .padding(8.dp)
                        ) {
                            Text("🚚", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Taxa de Entrega Exclusiva",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ChopDarkGreen
                                )
                                Text(
                                    text = "Entrega ao domicílio em Luanda: 2.500 Kz",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Descrição do Produto",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = product!!.description,
                    fontSize = 14.sp,
                    lineHeight = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // AI Product Recommendation advisor prompt button
                OutlinedButton(
                    onClick = { viewModel.askAiProductRecommendation(product!!.name) },
                    border = BorderStroke(1.dp, ChopGreen),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_ai_consult_button")
                ) {
                    Icon(Icons.Default.Star, "IA", tint = ChopOrange, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Consultar IA para produtos relacionados",
                        color = ChopGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Product Related Items scroll
                if (relatedProducts.isNotEmpty()) {
                    Text(
                        text = "Membros Recomendados Relacionados",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        items(relatedProducts) { related ->
                            RowProductCard(product = related, onProductClick = { viewModel.selectProduct(related) })
                        }
                    }
                }
            }
        }

        // Action Panel pinned to bottom of the screen
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Add to Cart
                Button(
                    onClick = { viewModel.addToCart(product!!.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = ChopDarkGreen),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("add_to_cart_button")
                ) {
                    Icon(Icons.Default.ShoppingCart, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Adicionar ao Carrinho", fontWeight = FontWeight.Bold)
                }

                // Buy Now (Triggers checkout cart, directly registers and launches checkout dialog)
                Button(
                    onClick = {
                        viewModel.addToCart(product!!.id)
                        viewModel.setScreen(AppScreen.CART)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChopOrange),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("buy_now_button")
                ) {
                    Text("Comprar Agora", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- CART SCREEN (CARRINHO INTELIGENTE) ---
@Composable
fun CartScreen(viewModel: AppViewModel) {
    val items by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.subtotalState.collectAsState()
    val deliveryFee by viewModel.deliveryFeeState.collectAsState()
    val discount by viewModel.discountState.collectAsState()
    val finalTotal by viewModel.finalTotalState.collectAsState()
    val activeCoupon by viewModel.activeCoupon.collectAsState()

    var couponCodeVal by remember { mutableStateOf("") }
    var clientNameVal by remember { mutableStateOf(viewModel.clientName.value) }
    var clientLocationVal by remember { mutableStateOf(viewModel.clientLocation.value) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillNavigatingHeader()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Carrinho Inteligente de Angola",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ChopGreen
                )

                if (items.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearCart() }) {
                        Text("Limpar", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🛒", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "O seu carrinho de compras está limpo!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Explore a nossa montra para encher o carrinho com produtos milionários de alta qualidade.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { viewModel.setScreen(AppScreen.HOME) },
                    colors = ButtonDefaults.buttonColors(containerColor = ChopGreen),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Explorar Produtos Premium", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // List of cart items
                Text(
                    text = "Artigos no Pedido (${items.sumOf { it.quantity }})",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                items.forEach { cartItem ->
                    CartProductRow(item = cartItem, viewModel = viewModel)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp), color = Color.Gray.copy(alpha = 0.15f))

                // Coupons Section
                Text(
                    text = "Cupão de Desconto CHOP.KZ",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = couponCodeVal,
                        onValueChange = { couponCodeVal = it },
                        placeholder = { Text("Ex: CHOP15, TEMU20, CLIENTEPRO") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ChopGreen,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("coupon_input_code")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (viewModel.applyCoupon(couponCodeVal)) {
                                couponCodeVal = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChopGreen),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("coupon_apply_button")
                    ) {
                        Text("Aplicar", fontWeight = FontWeight.Bold)
                    }
                }

                if (activeCoupon != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, null, tint = ChopGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Cupão $activeCoupon ativo (${viewModel.couponDiscountPercentage.collectAsState().value}% desconto)",
                                color = ChopGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(onClick = { viewModel.removeCoupon() }) {
                            Text("Excluir", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Customer Data details required for order submission
                Text(
                    text = "📍 Dados de Entrega em Luanda",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ChopDarkGreen
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = clientNameVal,
                    onValueChange = { clientNameVal = it },
                    label = { Text("Nome Completo do Cliente") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChopGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("client_name_input")
                )

                OutlinedTextField(
                    value = clientLocationVal,
                    onValueChange = { clientLocationVal = it },
                    label = { Text("Localização de Luanda (Bairro/Condomínio)") },
                    placeholder = { Text("Ex: Luanda Sul, Talatona, Kilamba, Viana") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChopGreen
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .testTag("client_location_input")
                )

                // Unified calculated Totals summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Resumo da Compra",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", color = Color.Gray)
                            Text(formatMoneyLocal(subtotal), fontWeight = FontWeight.Bold)
                        }

                        if (discount > 0) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Descontos Cupão", color = ChopOrange)
                                Text("-${formatMoneyLocal(discount)}", color = ChopOrange, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Taxa de Entrega (Luanda)", color = Color.Gray)
                            Text(formatMoneyLocal(deliveryFee), fontWeight = FontWeight.Bold)
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Total Final:",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = ChopBlack
                            )
                            Text(
                                formatMoneyLocal(finalTotal),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = ChopGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Checkout button triggering Whatsapp logic
                Button(
                    onClick = { viewModel.checkoutWithWhatsApp(clientNameVal, clientLocationVal) },
                    colors = ButtonDefaults.buttonColors(containerColor = ChopGreen),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("checkout_whatsapp_button")
                ) {
                    Icon(Icons.Default.Check, null) // Check represent standard execution
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Comprar Agora pelo WhatsApp",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CartProductRow(item: CartItemWithProduct, viewModel: AppViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cart_item_row_${item.productId}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImageRenderer(
                name = item.name,
                category = item.category,
                imageUrl = item.imageUrl,
                modifier = Modifier.size(70.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = item.category,
                    fontSize = 11.sp,
                    color = ChopGreen,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = formatMoneyLocal(item.price),
                    color = ChopGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Quantity adjusters plus delete
            Column(horizontalAlignment = Alignment.End) {
                IconButton(
                    onClick = { viewModel.removeFromCart(item.productId) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, "Remover", tint = Color.Red.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(15.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.updateCartQuantity(item.productId, item.quantity - 1) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = ChopGreen)
                    }

                    Text(
                        text = item.quantity.toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = { viewModel.updateCartQuantity(item.productId, item.quantity + 1) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, null, tint = ChopGreen)
                    }
                }
            }
        }
    }
}

// --- FAVORITES SCREEN ---
@Composable
fun FavoritesScreen(viewModel: AppViewModel) {
    val items by viewModel.favorites.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Os Meus Favoritos",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ChopGreen,
                modifier = Modifier.padding(16.dp)
            )
        }

        if (items.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("🤍", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ainda não tens favoritos salvos",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Toca no ícone de favoritos de qualquer produto para mantê-lo guardado aqui.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(items) { item ->
                    // Transform to Entity format for standard ProductCard usage
                    val mappedEntity = ProductEntity(
                        id = item.productId,
                        name = item.name,
                        category = item.category,
                        price = item.price,
                        oldPrice = item.oldPrice,
                        discountPercentage = item.discountPercentage,
                        imageUrl = item.imageUrl,
                        description = item.description,
                        availability = item.availability,
                        rating = item.rating,
                        reviewsCount = item.reviewsCount,
                        salesCount = item.salesCount,
                        deliveryFee = item.deliveryFee
                    )
                    ProductCard(product = mappedEntity, onProductClick = { viewModel.selectProduct(mappedEntity) })
                }
            }
        }
    }
}

// --- ORDERS SCREEN ---
@Composable
fun OrdersScreen(viewModel: AppViewModel) {
    val orders by viewModel.orders.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.setScreen(AppScreen.ADMIN_DASHBOARD) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar", tint = ChopBlack)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Encomendas Registradas",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ChopGreen
                )
            }
        }

        if (orders.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("📋", fontSize = 56.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Nenhuma encomenda faturada ainda",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(orders) { order ->
                    OrderCardRow(order = order)
                }
            }
        }
    }
}

@Composable
fun OrderCardRow(order: OrderEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ID Pedido: #${order.id}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = ChopGreen
                )

                // Status Badge
                Box(
                    modifier = Modifier
                        .background(ChopGreen.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = order.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChopDarkGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Cliente: ${order.clientName}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Localização: ${order.location}",
                fontSize = 12.sp,
                color = Color.Gray
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.1f))

            Text(
                text = "Artigos: ${order.itemsSummary}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Incluindo entrega (2.500 Kz)",
                    fontSize = 11.sp,
                    color = Color.Gray
                )

                Text(
                    text = formatMoneyLocal(order.total),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = ChopGreen
                )
            }
        }
    }
}

// --- ADMIN LOGIN SCREEN ---
@Composable
fun AdminLoginScreen(viewModel: AppViewModel) {
    var passCode by remember { mutableStateOf("") }
    var loginError by remember { mutableStateOf(false) }

    // Auto navigate to dashboard if already authenticated
    LaunchedEffect(viewModel.adminIsAuthenticated.collectAsState().value) {
        if (viewModel.adminIsAuthenticated.value) {
            viewModel.setScreen(AppScreen.ADMIN_DASHBOARD)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏢", fontSize = 64.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Área Administrativa Central",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = ChopDarkGreen
        )
        Text(
            text = "Acesso exclusivo e sob encriptação para o proprietário CHOP.KZ",
            fontSize = 12.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Palavra-Passe de Segurança",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = passCode,
                    onValueChange = { 
                        passCode = it
                        loginError = false
                    },
                    placeholder = { Text("Insira a palavra-passe do painel...") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChopOrange
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_password_input")
                )

                if (loginError) {
                    Text(
                        text = "Palavra-passe incorreta! Tente novamente.",
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (!viewModel.checkPasscode(passCode)) {
                            loginError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ChopOrange),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_login_submit")
                ) {
                    Text("Aceder ao Painel", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- ADMIN DASHBOARD PANEL SCREEN ---
@Composable
fun AdminDashboardScreen(viewModel: AppViewModel) {
    val products by viewModel.products.collectAsState()
    val orders by viewModel.orders.collectAsState()
    val aiLoading by viewModel.aiLoading.collectAsState()
    val aiOutputReport by viewModel.aiReportContent.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editTargetProduct by remember { mutableStateOf<ProductEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Painel Administrador",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ChopGreen
                    )
                    Text(
                        text = "Controle total de produtos, vendas e relatórios IA",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Row {
                    IconButton(onClick = { viewModel.setScreen(AppScreen.ORDER_LIST) }) {
                        Icon(Icons.Default.List, "Pedidos", tint = ChopGreen)
                    }
                    IconButton(onClick = { viewModel.adminLogout() }) {
                        Icon(Icons.Default.Close, "Sair", tint = Color.Red)
                    }
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            // General Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val revenue = orders.sumOf { it.total }
                    StatCard("Ganhos Totais", viewModel.formatMoney(revenue), "💵", Modifier.weight(1f))
                    StatCard("Encomendas", orders.size.toString(), "📦", Modifier.weight(1f))
                    StatCard("Catálogo", products.size.toString(), "🛍️", Modifier.weight(1f))
                }
            }

            // AI Smart Reports Block
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = ChopGreen.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, ChopGreen.copy(alpha = 0.25f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("💡", fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Relatório Especial de IA",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    color = ChopDarkGreen
                                )
                            }

                            Button(
                                onClick = { viewModel.generateAiSalesReport() },
                                colors = ButtonDefaults.buttonColors(containerColor = ChopGreen),
                                shape = RoundedCornerShape(20.dp),
                                enabled = !aiLoading,
                                modifier = Modifier.testTag("admin_generate_report_button")
                            ) {
                                if (aiLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Text("Analisar Catálogo", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (aiOutputReport != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Text(
                                    text = aiOutputReport!!,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .verticalScroll(rememberScrollState())
                                        .heightIn(max = 240.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gerir Catálogo de Artigos",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = ChopOrange),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("admin_add_product_button")
                    ) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Criar Produto", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Product management rows
            if (products.isEmpty()) {
                item {
                    Text(
                        "Sem artigos listados. Prima 'Criar Produto' no topo.",
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                items(products) { item ->
                    AdminProductRow(
                        product = item,
                        viewModel = viewModel,
                        onEdit = { editTargetProduct = item }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }

    // Modal adding product
    if (showAddDialog) {
        ProductFormDialog(
            onSubmit = { 
                viewModel.addProduct(it)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    // Modal editing product
    if (editTargetProduct != null) {
        ProductFormDialog(
            initialProduct = editTargetProduct,
            onSubmit = { 
                viewModel.editProduct(it)
                editTargetProduct = null
            },
            onDismiss = { editTargetProduct = null }
        )
    }
}

@Composable
fun StatCard(label: String, value: String, emoji: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Black, color = ChopGreen)
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AdminProductRow(product: ProductEntity, viewModel: AppViewModel, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("admin_row_item_${product.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProductImageRenderer(
                name = product.name,
                category = product.category,
                imageUrl = product.imageUrl,
                modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = product.category,
                    fontSize = 10.sp,
                    color = ChopGreen
                )

                Text(
                    text = formatMoneyLocal(product.price),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ChopBlack
                )
            }

            // Edit/Delete triggers
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Editar", tint = ChopGreen, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { viewModel.deleteProduct(product.id) }) {
                    Icon(Icons.Default.Delete, "Deletar", tint = Color.Red, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// Dialog Form for Add/Edit matching M3 aesthetics
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormDialog(
    initialProduct: ProductEntity? = null,
    onSubmit: (ProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialProduct?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(initialProduct?.category ?: "📱 Telemóveis") }
    var priceStr by remember { mutableStateOf(initialProduct?.price?.toLong()?.toString() ?: "") }
    var oldPriceStr by remember { mutableStateOf(initialProduct?.oldPrice?.toLong()?.toString() ?: "") }
    var discountStr by remember { mutableStateOf(initialProduct?.discountPercentage?.toString() ?: "0") }
    var keyDescription by remember { mutableStateOf(initialProduct?.description ?: "") }
    var activeAvailability by remember { mutableStateOf(initialProduct?.availability ?: "Em Stock") }

    val categories = listOf(
        "📱 Telemóveis", "💻 Computadores", "🎧 Acessórios", "⌚ Relógios", 
        "👕 Moda Masculina", "👗 Moda Feminina", "👟 Calçados", "💄 Beleza", 
        "🏠 Casa", "🎮 Gaming", "📚 Livros", "🚗 Automóveis"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialProduct == null) "Criar Novo Artigo" else "Editar Artigo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = ChopDarkGreen,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Produto") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("admin_form_name")
                )

                // Category Selection list
                Text("Categoria:", fontSize = 12.sp, color = Color.Gray)
                LazyRow(
                    contentPadding = PaddingValues(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val active = selectedCategory == cat
                        FilterChip(
                            selected = active,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChopGreen.copy(alpha = 0.2f),
                                selectedLabelColor = ChopDarkGreen
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Preço Atual (Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("admin_form_price")
                )

                OutlinedTextField(
                    value = oldPriceStr,
                    onValueChange = { oldPriceStr = it },
                    label = { Text("Preço Antigo Riscado (Opcional Kz)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = discountStr,
                    onValueChange = { discountStr = it },
                    label = { Text("Desconto Percentual (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = keyDescription,
                    onValueChange = { keyDescription = it },
                    label = { Text("Descrição Detalhada do Produto") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                // Availability Selection
                Text("Disponibilidade:", fontSize = 12.sp, color = Color.Gray)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    listOf("Em Stock", "Poucas Unidades", "Esgotado").forEach { status ->
                        val active = activeAvailability == status
                        ElevatedFilterChip(
                            selected = active,
                            onClick = { activeAvailability = status },
                            label = { Text(status, fontSize = 10.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (name.isNotEmpty() && priceStr.isNotEmpty()) {
                                val p = ProductEntity(
                                    id = initialProduct?.id ?: 0,
                                    name = name,
                                    category = selectedCategory,
                                    price = priceStr.toDoubleOrNull() ?: 1000.0,
                                    oldPrice = oldPriceStr.toDoubleOrNull(),
                                    discountPercentage = discountStr.toIntOrNull() ?: 0,
                                    imageUrl = initialProduct?.imageUrl ?: "", // keep existing image ref
                                    description = keyDescription,
                                    availability = activeAvailability,
                                    rating = initialProduct?.rating ?: 4.5f,
                                    reviewsCount = initialProduct?.reviewsCount ?: 12,
                                    salesCount = initialProduct?.salesCount ?: 35,
                                    isFeatured = initialProduct?.isFeatured ?: false
                                )
                                onSubmit(p)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ChopGreen),
                        modifier = Modifier.testTag("admin_form_submit")
                    ) {
                        Text("Guardar Alterações")
                    }
                }
            }
        }
    }
}


// --- AI CONVERSATION COGNITIVE SCREEN ---
@Composable
fun AiAssistantScreen(viewModel: AppViewModel) {
    val messages by viewModel.aiChatMessages.collectAsState()
    val loading by viewModel.aiLoading.collectAsState()
    var inputMsg by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    // Auto-scroll chat down when messages arrive
    LaunchedEffect(messages.size) {
        delay(100)
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🧠", fontSize = 24.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Suporte de IA CHOP.KZ",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = ChopGreen
                    )
                    Text(
                        text = "Inteligência Artificial conectada em tempo real",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Chat Dialog Content Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            messages.forEach { msg ->
                ChatBubble(message = msg)
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (loading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp), 
                                strokeWidth = 2.dp,
                                color = ChopGreen
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "IA CHOP.KZ está a analisar...", 
                                fontSize = 12.sp, 
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        // Suggestion Box
        if (messages.size == 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val suggestions = listOf(
                    "Recomenda-me um telemóvel ótimo",
                    "Como funciona a entrega em Luanda?",
                    "Quais são os cupões disponíveis?",
                    "Como falar com a equipa?"
                )
                items(suggestions) { keyword ->
                    SuggestionChip(
                        onClick = { 
                            viewModel.sendAiMessage(keyword)
                        },
                        label = { Text(keyword, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = ChopDarkGreen) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = ChopGreen.copy(alpha = 0.08f)),
                        border = BorderStroke(1.dp, ChopGreen.copy(alpha = 0.2f))
                    )
                }
            }
        }

        // Typing input section
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputMsg,
                    onValueChange = { inputMsg = it },
                    placeholder = { Text("Pergunte à IA algo sobre e-commerce...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ChopGreen
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("ai_conversation_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputMsg.trim().isNotEmpty()) {
                            viewModel.sendAiMessage(inputMsg)
                            inputMsg = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(ChopGreen, CircleShape)
                        .testTag("ai_conversation_submit")
                ) {
                    Icon(Icons.Default.Send, "Enviar", tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: AppViewModel.ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (message.isUser) ChopGreen else MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 0.dp,
                        bottomEnd = if (message.isUser) 0.dp else 16.dp
                    )
                )
                .border(
                    width = if (message.isUser) 0.dp else 1.dp,
                    color = Color.Gray.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 0.dp,
                        bottomEnd = if (message.isUser) 0.dp else 16.dp
                    )
                )
                .padding(14.dp)
                .widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = if (message.isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (message.isUser) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}


// --- TRUST BADGING FOOTER IN KOTLIN ---
@Composable
fun AngolaTrustFooter(viewModel: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CHOP.KZ",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = ChopGreen
        )
        Text(
            text = "A Maior Plataforma de Compra Online de Angola",
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.Gray.copy(alpha = 0.12f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            TrustIndicator("📦 Entrega Rápida", "Luanda (2.500 Kz)")
            TrustIndicator("🔒 Compra Segura", "WhatsApp Direto")
            TrustIndicator("💎 Premium", "Qualidade Selada")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Custom dotted line drawBehind representation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .drawBehind {
                    drawRoundRect(
                        color = ChopGreen.copy(alpha = 0.2f),
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )
                }
                .clickable { viewModel.applyCoupon("CHOP15") },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "CUPÃO DE HOJE: CHOP15 (Pressione para resgatar)",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = ChopOrange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "© 2026 CHOP.KZ Limitada. Inspirado nas melhores funcionalidades da AliExpress, Temu e Amazon adaptado fielmente para o mercado angolano.",
            fontSize = 9.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun TrustIndicator(title: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ChopBlack)
        Text(subtitle, fontSize = 10.sp, color = Color.Gray)
    }
}

// --- Dynamic Circular/Backport helpers ---
fun formatMoneyLocal(amount: Double): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale("pt", "AO"))
    return "${numberFormat.format(amount.toLong())} Kz"
}

// Custom Extension to prevent modifier compilation issues
fun Modifier.fillNavigatingHeader(): Modifier = this.fillMaxWidth()
