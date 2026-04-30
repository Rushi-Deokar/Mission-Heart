package com.example.missionheart

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel

class CartViewModel : ViewModel() {
    private val _cartItems = mutableStateListOf<CartItem>()
    val cartItems: List<CartItem> get() = _cartItems

    fun addMedicine(medicine: Medicine) {
        val existing = _cartItems.find { it.id == medicine.id && it.type == CartItemType.MEDICINE }
        if (existing != null) {
            val index = _cartItems.indexOf(existing)
            _cartItems[index] = existing.copy(quantity = existing.quantity + 1)
        } else {
            _cartItems.add(CartItem(medicine.id, medicine.name, medicine.price, 1, CartItemType.MEDICINE))
        }
    }

    fun addLabTest(labTest: LabTest) {
        val existing = _cartItems.find { it.id == labTest.id && it.type == CartItemType.LAB_TEST }
        if (existing == null) {
            _cartItems.add(CartItem(labTest.id, labTest.testName, labTest.price, 1, CartItemType.LAB_TEST))
        }
    }

    fun removeItem(item: CartItem) {
        _cartItems.remove(item)
    }

    fun updateQuantity(item: CartItem, delta: Int) {
        val index = _cartItems.indexOf(item)
        if (index != -1) {
            val newQuantity = (item.quantity + delta).coerceAtLeast(1)
            _cartItems[index] = item.copy(quantity = newQuantity)
        }
    }

    val totalAmount: Double
        get() = _cartItems.sumOf { it.price * it.quantity }
}

data class CartItem(
    val id: String,
    val name: String,
    val price: Double,
    val quantity: Int,
    val type: CartItemType
)

enum class CartItemType {
    MEDICINE, LAB_TEST
}
