package com.harrylabs.shreeshyamstore.ui.screens

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.harrylabs.shreeshyamstore.R
import com.harrylabs.shreeshyamstore.data.Category
import com.harrylabs.shreeshyamstore.data.Product
import com.harrylabs.shreeshyamstore.utils.CurrencyUtils
import com.harrylabs.shreeshyamstore.utils.DateTimeUtils
import com.harrylabs.shreeshyamstore.viewmodel.Screen
import com.harrylabs.shreeshyamstore.viewmodel.ShopViewModel
import com.harrylabs.shreeshyamstore.ui.theme.*

// ==========================================
// 1. PRODUCTS MASTER LIST SCREEN
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductsScreen(viewModel: ShopViewModel) {
    val products by viewModel.products.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }

    val filteredProducts = remember(products, searchQuery, selectedCategoryId) {
        products.filter { prod ->
            val matchesCategory = selectedCategoryId == null || prod.categoryId == selectedCategoryId
            val matchesSearch = prod.name.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.products_inventory_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showCategoryManagerDialog = true }) {
                        Icon(Icons.Default.Category, contentDescription = stringResource(R.string.content_description_manage_categories))
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.content_description_add_product), tint = Color.White)
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
                placeholder = { Text(stringResource(R.string.products_search_placeholder), color = TextMutedGray) },
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

            // Category Selector Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { selectedCategoryId = null },
                        label = { Text(stringResource(R.string.category_all), fontWeight = FontWeight.Bold) }
                    )
                }
                items(categories) { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = { selectedCategoryId = cat.id },
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
                            Text(stringResource(R.string.products_empty), color = TextMediumGray, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                } else {
                    items(filteredProducts) { prod ->
                        val catName = categories.find { it.id == prod.categoryId }?.name
                            ?: stringResource(R.string.category_miscellaneous)

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (prod.isActive) Color.White else BrownContainer
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
                                            SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.product_inactive), fontWeight = FontWeight.Bold) })
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.product_category_format, catName),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMediumGray
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.product_mrp_format, CurrencyUtils.formatRupees(prod.mrp)),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMediumGray
                                        )
                                        Text(
                                            text = stringResource(R.string.product_sp_format, CurrencyUtils.formatRupees(prod.getEffectivePrice())),
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
                                                stringResource(R.string.stock_out),
                                                color = ErrorRed,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        } else if (prod.currentStock <= prod.lowStockAlertQty) {
                                            Text(
                                                stringResource(R.string.stock_low_format, prod.currentStock),
                                                color = WarningOrange,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        } else {
                                            Text(
                                                stringResource(R.string.stock_units_format, prod.currentStock),
                                                color = SuccessGreen,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    } else {
                                        Text(text = stringResource(R.string.stock_untracked_no_limit), color = TextMutedGray, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Adjustment button
                                    if (prod.trackStock) {
                                        IconButton(onClick = { viewModel.navigateTo(Screen.StockAdjustment(prod.id)) }) {
                                            Icon(Icons.Default.EditCalendar, contentDescription = stringResource(R.string.content_description_adjust_stock), tint = Color.DarkGray)
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.navigateTo(Screen.AddEditProduct(prod.id)) },
                                        modifier = Modifier.testTag("edit_product_${prod.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.content_description_edit_product), tint = MaterialTheme.colorScheme.primary)
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
                            stringResource(R.string.categories_title),
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
                                placeholder = { Text(stringResource(R.string.category_add_placeholder)) },
                                modifier = Modifier.weight(1f).testTag("category_add_input")
                            )
                            Button(onClick = {
                                if (newCatName.trim().isNotEmpty()) {
                                    viewModel.addCategory(newCatName)
                                    newCatName = ""
                                }
                            }) {
                                Text(stringResource(R.string.action_add))
                            }
                        }

                        Divider()

                        // Editing listing dialog line
                        if (renamingCat != null) {
                            Text(stringResource(R.string.category_rename_title), fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                                    Text(stringResource(R.string.action_rename))
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
                            Text(stringResource(R.string.action_done))
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

    val title = stringResource(if (productId == null) R.string.product_add_title else R.string.product_edit_title)

    // Validation flags
    var nameError by remember { mutableStateOf(false) }
    var mrpError by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        if (productId != null) {
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
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
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
                label = { Text(stringResource(R.string.product_name_label)) },
                isError = nameError,
                supportingText = { if (nameError) Text(stringResource(R.string.product_name_error), color = Color.Red) },
                modifier = Modifier.fillMaxWidth().testTag("product_name_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Category Selection Spinner Box
            var catDropdownExpanded by remember { mutableStateOf(false) }
            val selectedCategoryName = remember(categoryId, categories) {
                categories.find { it.id == categoryId }?.name
            }
            val fallbackCategoryName = stringResource(R.string.category_miscellaneous)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { catDropdownExpanded = true }
            ) {
                OutlinedTextField(
                    value = selectedCategoryName ?: fallbackCategoryName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.product_category_label), fontWeight = FontWeight.Bold) },
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
                label = { Text(stringResource(R.string.product_mrp_label)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = mrpError,
                supportingText = { if (mrpError) Text(stringResource(R.string.product_mrp_error), color = Color.Red) },
                modifier = Modifier.fillMaxWidth().testTag("product_mrp_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Selling Price (SP)
            OutlinedTextField(
                value = sellingPrice,
                onValueChange = { sellingPrice = it },
                label = { Text(stringResource(R.string.selling_price_label)) },
                placeholder = { Text(stringResource(R.string.example_price_format, mrp.ifEmpty { "10" })) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag("product_sp_input"),
                shape = RoundedCornerShape(10.dp)
            )

            // Purchase Price
            OutlinedTextField(
                value = purchasePrice,
                onValueChange = { purchasePrice = it },
                label = { Text(stringResource(R.string.purchase_price_label)) },
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
                    Text(stringResource(R.string.track_stock_label), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(stringResource(R.string.track_stock_description), fontSize = 11.sp, color = Color.Gray)
                }
                Switch(checked = trackStock, onCheckedChange = { trackStock = it })
            }

            if (trackStock) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Current Stock count input
                    OutlinedTextField(
                        value = currentStock,
                        onValueChange = { currentStock = it },
                        label = { Text(stringResource(R.string.starting_stock_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("product_stock_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Low Stock AlertsQty
                    OutlinedTextField(
                        value = lowStockQty,
                        onValueChange = { lowStockQty = it },
                        label = { Text(stringResource(R.string.low_alert_label)) },
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
                    Text(stringResource(R.string.product_active_label), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(stringResource(R.string.product_active_description), fontSize = 11.sp, color = Color.Gray)
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
                        Toast.makeText(context, context.getString(R.string.product_saved_toast, name.trim()), Toast.LENGTH_SHORT).show()
                        viewModel.navigateTo(Screen.Products)
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_product_button")
            ) {
                Text(stringResource(R.string.save_product), fontSize = 18.sp, fontWeight = FontWeight.Bold)
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
    val openingValidationToast = stringResource(R.string.opening_validation_toast)

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
                title = { Text(stringResource(R.string.opening_stock_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Home) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
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
                stringResource(R.string.choose_category_label),
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
                            stringResource(
                                R.string.opening_fast_add_category_format,
                                categories.find { it.id == catId }?.name
                                    ?: stringResource(R.string.category_miscellaneous)
                            ),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            placeholder = { Text(stringResource(R.string.opening_item_name_placeholder)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("opening_stock_item_name"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = mrp,
                                onValueChange = { mrp = it },
                                placeholder = { Text(stringResource(R.string.opening_mrp_placeholder)) },
                                label = { Text(stringResource(R.string.product_mrp_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("opening_stock_item_mrp"),
                                shape = RoundedCornerShape(10.dp)
                            )

                            OutlinedTextField(
                                value = sp,
                                onValueChange = { sp = it },
                                placeholder = { Text(stringResource(R.string.opening_selling_price_placeholder)) },
                                label = { Text(stringResource(R.string.opening_sp_label)) },
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
                                placeholder = { Text(stringResource(R.string.quantity_short)) },
                                label = { Text(stringResource(R.string.opening_stock_quantity_label)) },
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
                                Text(stringResource(R.string.count_stock_label), fontSize = 11.sp, color = Color.Gray)
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
                                    Toast.makeText(context, openingValidationToast, Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(context, context.getString(R.string.opening_added_toast, name.trim()), Toast.LENGTH_SHORT).show()
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
                                Text(stringResource(R.string.add_product_next))
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
                    Text(stringResource(R.string.recently_created_items), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Text(stringResource(R.string.total_count_format, productsInSelectedCategory.size), fontSize = 12.sp, color = Color.Gray)
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
                                stringResource(R.string.category_empty_message),
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
                                            stringResource(R.string.price_format, CurrencyUtils.formatRupees(itemInfo.getEffectivePrice())),
                                            fontSize = 12.sp, color = Color.DarkGray
                                        )
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (itemInfo.trackStock) {
                                            Text(stringResource(R.string.stock_units_format, itemInfo.currentStock), fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                                                Icon(Icons.Default.AddCircleOutline, stringResource(R.string.content_description_plus_one_stock), tint = Color.Gray)
                                            }
                                        } else {
                                            Text(stringResource(R.string.stock_uncounted), fontSize = 11.sp, color = Color.Gray)
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
    val validStockToast = stringResource(R.string.valid_stock_required_toast)
    val adjustmentSavedToast = stringResource(R.string.adjustment_saved_toast)

    val reasons = listOf(
        "Opening stock entry" to stringResource(R.string.stock_reason_opening),
        "Purchase added" to stringResource(R.string.stock_reason_purchase),
        "Manual correction" to stringResource(R.string.stock_reason_manual),
        "Damaged/expired" to stringResource(R.string.stock_reason_damaged),
        "Stock count correction" to stringResource(R.string.stock_reason_count),
        "Other" to stringResource(R.string.stock_reason_other)
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
                title = { Text(stringResource(R.string.stock_adjustment_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(Screen.Products) }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.content_description_back))
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
                        Text(text = stringResource(R.string.product_label_format, prod.name), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.current_stock_amount_format, prod.currentStock),
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
                        Text(stringResource(R.string.actual_stock_count_label), fontSize = 14.sp, fontWeight = FontWeight.Bold)

                        OutlinedTextField(
                            value = countedStock,
                            onValueChange = { countedStock = it },
                            label = { Text(stringResource(R.string.physical_stock_counted_label)) },
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
                                Text(
                                    stringResource(
                                        R.string.reason_format,
                                        reasons.find { it.first == selectedReason }?.second
                                            ?: stringResource(R.string.stock_reason_other)
                                    )
                                )
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                reasons.forEach { (storedReason, displayReason) ->
                                    DropdownMenuItem(
                                        text = { Text(displayReason) },
                                        onClick = {
                                            selectedReason = storedReason
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
                                    Toast.makeText(context, validStockToast, Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.adjustStock(
                                        productId = productId,
                                        actualStockCounted = countVal,
                                        reason = selectedReason
                                    )
                                    // Refresh details UI
                                    product = prod.copy(currentStock = countVal)
                                    Toast.makeText(context, adjustmentSavedToast, Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("adjustment_save_button")
                        ) {
                            Text(stringResource(R.string.save_correction))
                        }
                    }
                }

                // History adjustments lists logger
                Text(stringResource(R.string.stock_log_history), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)

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
                                        text = localizedStockReason(record.reason),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.DarkGray
                                    )
                                    val prefix = if (record.difference >= 0) "+" else ""
                                    Text(
                                        text = stringResource(R.string.stock_difference_pieces_format, prefix, record.difference),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (record.difference >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        stringResource(R.string.stock_changed_format, record.oldStock, record.newStock),
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

@Composable
private fun localizedStockReason(reason: String): String {
    return when (reason) {
        "Opening stock entry" -> stringResource(R.string.stock_reason_opening)
        "Purchase added" -> stringResource(R.string.stock_reason_purchase)
        "Manual correction" -> stringResource(R.string.stock_reason_manual)
        "Damaged/expired" -> stringResource(R.string.stock_reason_damaged)
        "Stock count correction" -> stringResource(R.string.stock_reason_count)
        else -> stringResource(R.string.stock_reason_other)
    }
}
