package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.Category
import com.example.data.Product
import com.example.utils.CurrencyUtils
import com.example.utils.DateTimeUtils
import com.example.viewmodel.Screen
import com.example.viewmodel.ShopViewModel
import com.example.ui.theme.*

// ==========================================
// 1. PRODUCTS MASTER LIST SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var filterOnlyLowStock by remember { mutableStateOf(false) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var showReorderDialog by remember { mutableStateOf(false) }

    val lowStockProducts = remember(products) {
        products.filter { it.isActive && it.trackStock && it.currentStock <= it.lowStockAlertQty }
    }

    val filteredProducts = remember(products, searchQuery, selectedCategoryId, filterOnlyLowStock) {
        products.filter { prod ->
            val matchesCategory = selectedCategoryId == null || prod.categoryId == selectedCategoryId
            val matchesSearch = prod.name.contains(searchQuery, ignoreCase = true)
            val matchesLowStock = !filterOnlyLowStock || (prod.isActive && prod.trackStock && prod.currentStock <= prod.lowStockAlertQty)
            matchesCategory && matchesSearch && matchesLowStock
        }
    }

    if (showReorderDialog) {
        LowStockReorderDialog(
            viewModel = viewModel,
            lowStockProducts = lowStockProducts,
            categories = categories,
            onDismiss = { showReorderDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("स्टॉक लिस्ट (Inventory) 📦", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Re-order List Action Icon with Badge
                    IconButton(
                        onClick = { showReorderDialog = true },
                        modifier = Modifier.testTag("open_reorder_list_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (lowStockProducts.isNotEmpty()) {
                                    Badge(
                                        containerColor = ErrorRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("${lowStockProducts.size}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.PlaylistAddCheck,
                                contentDescription = "Re-order List",
                                tint = if (lowStockProducts.isNotEmpty()) ErrorRed else SaffronPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.exportStockCsv(context) },
                        modifier = Modifier.testTag("export_stock_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Export Stock CSV", tint = SaffronPrimary)
                    }
                    IconButton(onClick = { showCategoryManagerDialog = true }) {
                        Icon(Icons.Default.Category, contentDescription = "Manage Categories")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.navigateTo(Screen.AddEditProduct(null)) },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("fab_add_product")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product", tint = Color.White)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(WarmCreamBg)
        ) {
            // Search Input Block
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("सामान का नाम खोजें (Search products)...", color = TextMutedGray) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SaffronPrimary) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("product_search_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = SaffronPrimary,
                    unfocusedBorderColor = BorderStrong,
                    focusedTextColor = TextNearBlack,
                    unfocusedTextColor = TextNearBlack
                )
            )

            // Low Stock Re-order Quick Alert Banner
            if (lowStockProducts.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ErrorRedLight),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.2.dp, ErrorRed.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { showReorderDialog = true }
                        .testTag("low_stock_reorder_banner")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                            Column {
                                Text(
                                    text = "कम स्टॉक चेतावनी (Low Stock): ${lowStockProducts.size} सामान",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = ErrorRed
                                )
                                Text(
                                    text = "थोक ऑर्डर लिस्ट तैयार करें 📋",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextNearBlack
                                )
                            }
                        }

                        Button(
                            onClick = { showReorderDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("ऑर्डर लिस्ट", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Category & Low Stock Selector Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null && !filterOnlyLowStock,
                        onClick = {
                            selectedCategoryId = null
                            filterOnlyLowStock = false
                        },
                        label = { Text("सभी (All)", fontWeight = FontWeight.Bold) }
                    )
                }
                item {
                    FilterChip(
                        selected = filterOnlyLowStock,
                        onClick = { filterOnlyLowStock = !filterOnlyLowStock },
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    Icons.Default.PriorityHigh,
                                    contentDescription = null,
                                    tint = if (filterOnlyLowStock) Color.White else ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "कम स्टॉक (${lowStockProducts.size})",
                                    fontWeight = FontWeight.Black,
                                    color = if (filterOnlyLowStock) Color.White else ErrorRed
                                )
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ErrorRed,
                            selectedLabelColor = Color.White
                        )
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id && !filterOnlyLowStock,
                        onClick = {
                            selectedCategoryId = cat.id
                            filterOnlyLowStock = false
                        },
                        label = { Text(cat.name, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            // Products list view
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (filteredProducts.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Inbox, null, modifier = Modifier.size(54.dp), tint = BorderStrong)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("कोई सामान उपलब्ध नहीं है!", color = TextMediumGray, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                } else {
                    items(filteredProducts) { prod ->
                        val catName = categories.find { it.id == prod.categoryId }?.name ?: "Miscellaneous"

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (prod.isActive) Color.White else SlateContainer
                            ),
                            border = BorderStroke(1.2.dp, BorderStrong),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = prod.name,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 17.sp,
                                            color = if (prod.isActive) TextNearBlack else TextMutedGray
                                        )
                                        if (!prod.isActive) {
                                            SuggestionChip(onClick = {}, label = { Text("Inactive", fontWeight = FontWeight.Bold) })
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Category: $catName",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMediumGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = "MRP: ${CurrencyUtils.formatRupees(prod.mrp)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMediumGray
                                        )
                                        Text(
                                            text = "SP: ${CurrencyUtils.formatRupees(prod.getEffectivePrice())}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = SaffronPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    // Stock Badging
                                    if (prod.trackStock) {
                                        if (prod.currentStock <= 0) {
                                            Text(
                                                "स्टॉक ख़त्म (Out of Stock) ❌",
                                                color = ErrorRed,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        } else if (prod.currentStock <= prod.lowStockAlertQty) {
                                            Text(
                                                "कम स्टॉक (Low Stock): ${prod.currentStock} बचा है ⚠️",
                                                color = WarningOrange,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        } else {
                                            Text(
                                                "स्टॉक: ${prod.currentStock} पीस 👍",
                                                color = SuccessGreen,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    } else {
                                        Text(text = "स्टॉक अनट्रैक्ड (No limit)", color = TextMutedGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Adjustment button
                                    if (prod.trackStock) {
                                        IconButton(onClick = { viewModel.navigateTo(Screen.StockAdjustment(prod.id)) }) {
                                            Icon(Icons.Default.EditCalendar, contentDescription = "Adjust Stock", tint = Color.DarkGray)
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.navigateTo(Screen.AddEditProduct(prod.id)) },
                                        modifier = Modifier.testTag("edit_product_${prod.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Product", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SUB INLINE CATEGORY MANAGER DIALOG ---
        if (showCategoryManagerDialog) {
            Dialog(onDismissRequest = { showCategoryManagerDialog = false }) {
                var newCatName by remember { mutableStateOf("") }
                var renamingCat by remember { mutableStateOf<Category?>(null) }
                var renameText by remember { mutableStateOf("") }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .background(Color.White)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "कैटेगरी मैनेजमेंट (Categories) 🗂️",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Create form
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = newCatName,
                                onValueChange = { newCatName = it },
                                placeholder = { Text("विविध कैटेगरी नाम (e.g. Rice)") },
                                modifier = Modifier.weight(1f).testTag("category_add_input")
                            )
                            Button(onClick = {
                                if (newCatName.trim().isNotEmpty()) {
                                    viewModel.addCategory(newCatName)
                                    newCatName = ""
                                }
                            }) {
                                Text("जोड़ें")
                            }
                        }

                        Divider()

                        // Editing listing dialog line
                        if (renamingCat != null) {
                            Text("कैटेगरी का नाम बदलें:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = renameText,
                                    onValueChange = { renameText = it },
                                    modifier = Modifier.weight(1f)
                                )
                                Button(onClick = {
                                    renamingCat?.let {
                                        viewModel.renameCategory(it, renameText)
                                        renamingCat = null
                                        renameText = ""
                                    }
                                }) {
                                    Text("बदलें")
                                }
                            }
                        }

                        // Categories List
                        Box(modifier = Modifier.heightIn(max = 200.dp).fillMaxWidth()) {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(categories) { cat ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(cat.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        if (cat.name != "Miscellaneous") {
                                            Row {
                                                IconButton(onClick = {
                                                    renamingCat = cat
                                                    renameText = cat.name
                                                }) {
                                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = { showCategoryManagerDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Done (पूर्ण)")
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 2. ADD/EDIT PRODUCT DETAIL SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductScreen(viewModel: ShopViewModel, productId: Long?) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()

    var name by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<Long>(categories.firstOrNull()?.id ?: 1L) }
    var mrp by remember { mutableStateOf("") }
    var sellingPrice by remember { mutableStateOf("") }
    var purchasePrice by remember { mutableStateOf("") }
    var trackStock by remember { mutableStateOf(true) }
    var currentStock by remember { mutableStateOf("") }
    var lowStockQty by remember { mutableStateOf("5") }
    var isActive by remember { mutableStateOf(true) }

    var title by remember { mutableStateOf("नया सामान जोड़ें (New Product)") }

    // Validation flags
    var nameError by remember { mutableStateOf(false) }
    var mrpError by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        if (productId != null) {
            title = "सामान बदलाव (Edit Product) ✏️"
            val prod = viewModel.getProduct(productId)
            if (prod != null) {
                name = prod.name
                categoryId = prod.categoryId
                mrp = prod.mrp.toString()
                sellingPrice = prod.sellingPrice?.toString() ?: ""
                purchasePrice = prod.purchasePrice?.toString() ?: ""
                trackStock = prod.trackStock
                currentStock = prod.currentStock.toString()
                lowStockQty = prod.lowStockAlertQty.toString()
                isActive = prod.isActive
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Products) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Product Name
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.trim().isNotEmpty()) nameError = false
                },
                label = { Text("Product Name * (सामान का नाम)") },
                isError = nameError,
                supportingText = { if (nameError) Text("Name cannot be empty!", color = Color.Red) },
                modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Category Selection Spinner Box
            var catDropdownExpanded by remember { mutableStateOf(false) }
            val selectedCategoryName = remember(categoryId, categories) {
                categories.find { it.id == categoryId }?.name ?: "Miscellaneous"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { catDropdownExpanded = true }
            ) {
                OutlinedTextField(
                    value = selectedCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category (कैटेगरी चुनें)", fontWeight = FontWeight.Bold) },
                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, tint = MaterialTheme.colorScheme.primary) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                // Transparent overlay to safely capture taps
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { catDropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = catDropdownExpanded,
                    onDismissRequest = { catDropdownExpanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name, fontWeight = FontWeight.Bold) },
                            onClick = {
                                categoryId = cat.id
                                catDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // MRP
            OutlinedTextField(
                value = mrp,
                onValueChange = {
                    mrp = it
                    if (it.trim().toDoubleOrNull() != null) mrpError = false
                },
                label = { Text("MRP * (अधिकतम प्रिंट रेट - ₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = mrpError,
                supportingText = { if (mrpError) Text("Provide a valid numeric MRP!", color = Color.Red) },
                modifier = Modifier.fillMaxWidth().testTag("product_mrp_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Selling Price (SP)
            OutlinedTextField(
                value = sellingPrice,
                onValueChange = { sellingPrice = it },
                label = { Text("Selling Price (बिक्री रेट - ₹, blank assumes MRP)") },
                placeholder = { Text("e.g. ${mrp.ifEmpty { "10" }}") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("product_sp_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Purchase Price
            OutlinedTextField(
                value = purchasePrice,
                onValueChange = { purchasePrice = it },
                label = { Text("Purchase Price (ख़रीद रेट - ₹ - optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Divider()

            // Track stock panel switches
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Track Stock (स्टॉक की गिनती करें)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("स्टॉक ऑटोमैटिक घटेगा बिक्री होने पर।", fontSize = 11.sp, color = Color.Gray)
                }
                Switch(checked = trackStock, onCheckedChange = { trackStock = it })
            }

            if (trackStock) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Current Stock count input
                    OutlinedTextField(
                        value = currentStock,
                        onValueChange = { currentStock = it },
                        label = { Text("Starting Stock *(शुरुआती स्टॉक)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_stock_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Low Stock AlertsQty
                    OutlinedTextField(
                        value = lowStockQty,
                        onValueChange = { lowStockQty = it },
                        label = { Text("Low Alert *(अलर्ट सीमा)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Inactive product toggle switch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text("Product Active (चालू स्थिति)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("बंद करने पर यह बिलिंग में छिप जाएगा।", fontSize = 11.sp, color = Color.Gray)
                }
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Save Product
            Button(
                onClick = {
                    val mrpValue = mrp.trim().toDoubleOrNull()
                    val spValue = sellingPrice.trim().toDoubleOrNull()
                    val purValue = purchasePrice.trim().toDoubleOrNull()
                    val stockValue = currentStock.trim().toIntOrNull() ?: 0
                    val alertValue = lowStockQty.trim().toIntOrNull() ?: 5

                    // Validations checks
                    if (name.trim().isEmpty()) {
                        nameError = true
                    }
                    if (mrpValue == null || mrpValue <= 0.0) {
                        mrpError = true
                    }

                    if (name.trim().isNotEmpty() && mrpValue != null && mrpValue > 0.0) {
                        viewModel.saveProduct(
                            id = productId ?: 0L,
                            name = name,
                            categoryId = categoryId,
                            mrp = mrpValue,
                            sellingPrice = if (spValue != null && spValue > 0.0) spValue else null,
                            purchasePrice = if (purValue != null && purValue > 0) purValue else null,
                            currentStock = if (trackStock) stockValue else 0,
                            trackStock = trackStock,
                            lowStockAlertQty = alertValue,
                            isActive = isActive
                        )
                        Toast.makeText(context, "${name.trim()} Saved successfully!", Toast.LENGTH_SHORT).show()
                        viewModel.navigateTo(Screen.Products)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_product_button")
            ) {
                Text("सुरक्षित करें (Save Product) 💾", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


// ==========================================
// 3. OPENING STOCK ENTRY (BULK ADD PANEL)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpeningStockScreen(viewModel: ShopViewModel) {
    val context = LocalContext.current
    val categories by viewModel.categories.collectAsState()
    val products by viewModel.products.collectAsState()

    var selectedCatId by remember { mutableStateOf<Long?>(null) }
    
    // Quick Add input values
    var name by remember { mutableStateOf("") }
    var mrp by remember { mutableStateOf("") }
    var sp by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("10") }
    var trackStock by remember { mutableStateOf(true) }

    LaunchedEffect(categories) {
        if (selectedCatId == null && categories.isNotEmpty()) {
            selectedCatId = categories.first().id
        }
    }

    val productsInSelectedCategory = remember(products, selectedCatId) {
        products.filter { it.categoryId == selectedCatId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("शुरुआती स्टॉक (Opening Stock) 📚", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF7F9FC))
        ) {
            // Select category header scroll
            Text(
                "कैटेगरी चुनें (Choose Category):",
                fontSize = 14.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCatId == cat.id,
                        onClick = { selectedCatId = cat.id },
                        label = { Text(cat.name) }
                    )
                }
            }

            selectedCatId?.let { catId ->
                // Fast Entry Card Layout
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Fast Add: '${categories.find { it.id == catId }?.name}' category",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text("Item Name - (e.g. Parle G 100g)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("opening_stock_item_name"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = mrp,
                                onValueChange = { mrp = it },
                                placeholder = { Text("MRP Code Price") },
                                label = { Text("MRP *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("opening_stock_item_mrp"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = sp,
                                onValueChange = { sp = it },
                                placeholder = { Text("Selling Price") },
                                label = { Text("SP (Blank=MRP)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("opening_stock_item_sp"),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = stock,
                                onValueChange = { stock = it },
                                placeholder = { Text("Qty") },
                                label = { Text("Stock Quantity *") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1.2f).testTag("opening_stock_item_qty"),
                                shape = RoundedCornerShape(10.dp),
                                enabled = trackStock
                            )

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.End
                            ) {
                                Text("Count Stock?", fontSize = 11.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Switch(checked = trackStock, onCheckedChange = { trackStock = it })
                            }
                        }

                        Button(
                            onClick = {
                                val mrpValue = mrp.trim().toDoubleOrNull()
                                val spValue = sp.trim().toDoubleOrNull()
                                val stockValue = stock.trim().toIntOrNull() ?: 0

                                if (name.trim().isEmpty() || mrpValue == null) {
                                    Toast.makeText(context, "Item Name and MRP Price required!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.saveProduct(
                                        id = 0L, // insert new
                                        name = name,
                                        categoryId = catId,
                                        mrp = mrpValue,
                                        sellingPrice = if (spValue != null && spValue > 0) spValue else null,
                                        purchasePrice = null,
                                        currentStock = if (trackStock) stockValue else 0,
                                        trackStock = trackStock,
                                        lowStockAlertQty = 5,
                                        isActive = true
                                    )
                                    Toast.makeText(context, "'${name.trim()}' added successfully!", Toast.LENGTH_SHORT).show()
                                    // Reset fields to trigger rapid sequential entries!
                                    name = ""
                                    mrp = ""
                                    sp = ""
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("opening_stock_fast_save")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Add, null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("जोड़ें (Add Product & Next)")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Shows recently added product list headers
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Recently Created Items:", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text("Total: ${productsInSelectedCategory.size}", fontSize = 12.sp, color = Color.Gray)
                }

                // Listing rows
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (productsInSelectedCategory.isEmpty()) {
                        item {
                            Text(
                                "इस कैटेगरी में कोई सामान नहीं जुड़ा है। ऊपर दर्ज करें!",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(32.dp)
                            )
                        }
                    } else {
                        items(productsInSelectedCategory.takeLast(10).reversed()) { itemInfo ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(itemInfo.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            "Price: ${CurrencyUtils.formatRupees(itemInfo.getEffectivePrice())}",
                                            fontSize = 12.sp, color = Color.DarkGray
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (itemInfo.trackStock) {
                                            Text("Stock: ${itemInfo.currentStock}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            // Edit counts indicator to change errors quickly
                                            IconButton(onClick = {
                                                viewModel.saveProduct(
                                                    id = itemInfo.id,
                                                    name = itemInfo.name,
                                                    categoryId = itemInfo.categoryId,
                                                    mrp = itemInfo.mrp,
                                                    sellingPrice = itemInfo.sellingPrice,
                                                    purchasePrice = itemInfo.purchasePrice,
                                                    currentStock = itemInfo.currentStock + 1,
                                                    trackStock = itemInfo.trackStock,
                                                    lowStockAlertQty = itemInfo.lowStockAlertQty,
                                                    isActive = itemInfo.isActive
                                                )
                                            }) {
                                                Icon(Icons.Default.AddCircleOutline, "Plus 1 to Stock", tint = Color.Gray)
                                            }
                                        } else {
                                            Text("Stock Uncounted", fontSize = 11.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 4. MANUAL STOCK ADJUSTMENTS HISTORY
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentScreen(viewModel: ShopViewModel, productId: Long) {
    val context = LocalContext.current
    var product by remember { mutableStateOf<Product?>(null) }
    var countedStock by remember { mutableStateOf("") }
    var selectedReason by remember { mutableStateOf("Manual correction") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val reasons = listOf(
        "Opening stock entry",
        "Purchase added",
        "Manual correction",
        "Damaged/expired",
        "Stock count correction",
        "Other"
    )

    val adjustmentHistory = viewModel.getAdjustmentsForProduct(productId).collectAsState(initial = emptyList())

    LaunchedEffect(productId) {
        product = viewModel.getProduct(productId)
        product?.let {
            countedStock = it.currentStock.toString()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("स्टॉक सुधारे (Adjustment) 🔧", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Products) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        product?.let { prod ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF7F9FC))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "सामान (Product): ${prod.name}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "अभी दर्ज स्टॉक Amount: ${prod.currentStock} पीस",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                    }
                }

                // Count input
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("सच्चा स्टॉक संख्या डालें (Actual stock physical count):", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = countedStock,
                            onValueChange = { countedStock = it },
                            label = { Text("Physical Stock Counted") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("adjustment_stock_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        // Reason dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { dropdownExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reason: $selectedReason")
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                reasons.forEach { reas ->
                                    DropdownMenuItem(
                                        text = { Text(reas) },
                                        onClick = {
                                            selectedReason = reas
                                            dropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val countVal = countedStock.toIntOrNull()
                                if (countVal == null || countVal < 0) {
                                    Toast.makeText(context, "Valid stock quantity is required!", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.adjustStock(
                                        productId = productId,
                                        actualStockCounted = countVal,
                                        reason = selectedReason
                                    )
                                    // Refresh details UI
                                    product = prod.copy(currentStock = countVal)
                                    Toast.makeText(context, "Adjustment Saved!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("adjustment_save_button")
                        ) {
                            Text("स्टॉक दर्ज करें (Save Correction)")
                        }
                    }
                }

                // History adjustments lists logger
                Text("स्टॉक सुधार इतिहास (Log History):", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(adjustmentHistory.value) { record ->
                        Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = record.reason,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.DarkGray
                                    )
                                    val prefix = if (record.difference >= 0) "+" else ""
                                    Text(
                                        text = "$prefix${record.difference} पीस",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (record.difference >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "स्टॉक बदला: ${record.oldStock} ➔ ${record.newStock}",
                                        fontSize = 12.sp, color = Color.Gray
                                    )
                                    Text(
                                        DateTimeUtils.formatDateTime(record.createdAt),
                                        fontSize = 11.sp, color = Color.LightGray
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

// ==========================================
// 6. LOW STOCK RE-ORDER LIST DIALOG
// ==========================================
@Composable
fun LowStockReorderDialog(
    viewModel: ShopViewModel,
    lowStockProducts: List<Product>,
    categories: List<Category>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val categoryMap = remember(categories) { categories.associate { it.id to it.name } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            border = BorderStroke(1.5.dp, ErrorRed.copy(alpha = 0.4f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .testTag("low_stock_reorder_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.ListAlt, contentDescription = null, tint = SaffronPrimary, modifier = Modifier.size(24.dp))
                            Text(
                                text = "थोक आर्डर लिस्ट (Re-order)",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextNearBlack
                            )
                        }
                        Text(
                            text = "कुल ${lowStockProducts.size} सामान कम हैं",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMediumGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Buttons Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp Share
                    Button(
                        onClick = {
                            viewModel.shareReorderListViaWhatsApp(context, lowStockProducts)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                            .testTag("share_reorder_whatsapp_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("WhatsApp ऑर्डर 📱", fontSize = 13.sp, fontWeight = FontWeight.Black)
                        }
                    }

                    // Copy Button
                    OutlinedButton(
                        onClick = {
                            viewModel.copyReorderListToClipboard(context, lowStockProducts)
                        },
                        border = BorderStroke(1.2.dp, BorderStrong),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextNearBlack),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(44.dp)
                            .testTag("copy_reorder_list_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ContentCopy, null, tint = SaffronPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("कॉपी 📋", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BorderStrong)
                Spacer(modifier = Modifier.height(8.dp))

                if (lowStockProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(54.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "बधाई हो! कोई सामान कम नहीं है 🎉",
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp,
                                color = SuccessGreen
                            )
                            Text(
                                text = "दुकान में पर्याप्त स्टॉक मौजूद है।",
                                fontSize = 13.sp,
                                color = TextMediumGray
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(lowStockProducts) { item ->
                            val catName = categoryMap[item.categoryId] ?: "General"
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = WarmCreamBg),
                                border = BorderStroke(1.dp, BorderStrong)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = TextNearBlack
                                            )
                                            Text(
                                                text = "$catName | MRP: ₹${item.mrp}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextMediumGray
                                            )
                                        }

                                        // Status Pill
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (item.currentStock <= 0) ErrorRedLight else WarningOrangeLight,
                                            border = BorderStroke(1.dp, if (item.currentStock <= 0) ErrorRed else WarningOrange)
                                        ) {
                                            Text(
                                                text = if (item.currentStock <= 0) "ख़त्म (0)" else "बचा: ${item.currentStock} पीस",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (item.currentStock <= 0) ErrorRed else WarningOrange,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Quick Restock Steppers (+5, +10, +25, +50)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "नया माल आया:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextNearBlack
                                        )

                                        listOf(5, 10, 25, 50).forEach { qty ->
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.bulkRestockProduct(item, qty)
                                                    Toast.makeText(context, "+$qty स्टॉक जोड़ा गया (${item.name})", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                border = BorderStroke(1.dp, SaffronPrimary.copy(alpha = 0.6f)),
                                                colors = ButtonDefaults.outlinedButtonColors(containerColor = SurfaceWhite),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("+$qty", fontSize = 11.sp, fontWeight = FontWeight.Black, color = SaffronDark)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = SlateContainer, contentColor = TextNearBlack),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                ) {
                    Text("बंद करें (Close)", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
