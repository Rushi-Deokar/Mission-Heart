package com.example.missionheart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.missionheart.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(navController: NavController, cartViewModel: CartViewModel) {
    val cartItems = cartViewModel.cartItems
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Cart", fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground)
            )
        },
        containerColor = AppBackground,
        bottomBar = {
            if (cartItems.isNotEmpty()) {
                CheckoutBottomBar(cartViewModel.totalAmount) {
                    Toast.makeText(context, "Redirecting to Payment...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    ) { paddingValues ->
        if (cartItems.isEmpty()) {
            EmptyCartView { navController.popBackStack() }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(cartItems) { item ->
                    CartItemRow(
                        item = item,
                        onIncrement = { cartViewModel.updateQuantity(item, 1) },
                        onDecrement = { cartViewModel.updateQuantity(item, -1) },
                        onRemove = { cartViewModel.removeItem(item) }
                    )
                }

                item { BillSummary(cartViewModel.totalAmount) }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(
                    if (item.type == CartItemType.MEDICINE) "Medicine" else "Lab Test",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("₹${item.price.toInt()}", fontWeight = FontWeight.Bold, color = BrandBlue)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (item.type == CartItemType.MEDICINE) {
                    IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Remove, null, tint = BrandBlue)
                    }
                    Text("${item.quantity}", modifier = Modifier.padding(horizontal = 8.dp), fontWeight = FontWeight.Bold)
                    IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Add, null, tint = BrandBlue)
                    }
                } else {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, null, tint = ErrorRed)
                    }
                }
            }
        }
    }
}

@Composable
fun BillSummary(total: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Bill Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Item Total", color = TextSecondary)
                Text("₹${total.toInt()}", color = TextPrimary)
            }
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Delivery Charges", color = TextSecondary)
                Text("FREE", color = SuccessGreen, fontWeight = FontWeight.Bold)
            }
            Divider(Modifier.padding(vertical = 12.dp), color = InputFieldBg)
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text("Total Amount", fontWeight = FontWeight.Bold)
                Text("₹${total.toInt()}", fontWeight = FontWeight.Bold, color = BrandBlue, fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun CheckoutBottomBar(total: Double, onCheckout: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = SurfaceWhite
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Total Amount", fontSize = 12.sp, color = TextSecondary)
                Text("₹${total.toInt()}", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = BrandBlue)
            }
            Button(
                onClick = onCheckout,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                modifier = Modifier.height(50.dp).width(160.dp)
            ) {
                Text("Checkout", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun EmptyCartView(onBrowse: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.ShoppingCart, null, tint = InputFieldBg, modifier = Modifier.size(100.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Your Cart is Empty", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text("Add something to make me happy!", color = TextSecondary)
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onBrowse, colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)) {
                Text("Browse Medicines")
            }
        }
    }
}
