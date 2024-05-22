package com.example.freshsaver

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

data class Category(
    val id: String,
    val userId: String = "global",
    val title: String,
    val imageUrl: String? = null
)

data class ProductType(
    val id: String,
    val categoryId: String,
    val userId: String = "global",
    val title: String,
    val imageUrl: String? = null,
    val timeToExpire: Int? = null
)

data class Product(
    val id: String,
    val productTypeId: String,
    val purchaseDate: Long,
    val expirationDate: Long,
    val title: String? = null,
    val cost: Double? = null
)

data class NewProduct(
    val productTypeId: String,
    val purchaseDate: Long,
    val expirationDate: Long? = null,
    val title: String? = null,
    val cost: Double? = null
)

class DB {
    fun getCategories(): Task<List<Category>> {
        return db.collection("categories")
            .whereIn("user_id", getUserIds())
            .get()
            .continueWith { task ->
                if (task.isSuccessful) {
                    task.result?.documents?.map { document ->
                        Category(
                            id = document.id,
                            userId = document.getString("user_id") ?: "",
                            title = document.getString("title") ?: "",
                            imageUrl = document.getString("image_url")
                        )
                    } ?: emptyList()
                } else {
                    throw task.exception ?: Exception("Unknown error occurred")
                }
            }
    }

    fun getProductTypesByCategory(categoryId: String): Task<List<ProductType>> {
        return db.collection("product_types")
            .whereIn("user_id", getUserIds())
            .whereEqualTo("category_id", categoryId)
            .get()
            .continueWith { task ->
                if (task.isSuccessful) {
                    task.result?.documents?.map { document ->
                        ProductType(
                            id = document.id,
                            categoryId = document.getString("category_id") ?: "",
                            userId = document.getString("user_id") ?: "",
                            title = document.getString("title") ?: "",
                            imageUrl = document.getString("image_url"),
                            timeToExpire = document.getLong("time_to_expire")?.toInt()
                        )
                    } ?: emptyList()
                } else {
                    throw task.exception ?: Exception("Unknown error occurred")
                }
            }
    }

    fun getUserProducts(): Task<List<Product>> {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        return db.collection("products")
            .whereEqualTo("user_id", uid)
            .get()
            .continueWith { task ->
                if (task.isSuccessful) {
                    task.result?.documents?.map { document ->
                        Product(
                            id = document.id,
                            productTypeId = document.getString("product_type_id") ?: "",
                            purchaseDate = document.getLong("purchase_date") ?: 0,
                            expirationDate = document.getLong("expiration_date") ?: 0,
                            title = document.getString("title"),
                            cost = document.getDouble("cost")
                        )
                    } ?: emptyList()
                } else {
                    throw task.exception ?: Exception("Unknown error occurred")
                }
            }
    }

    fun addUserProduct(product: NewProduct): Task<DocumentReference> {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val productData = hashMapOf(
            "product_type_id" to product.productTypeId,
            "user_id" to userId,
            "purchase_date" to product.purchaseDate,
            "title" to product.title,
            "cost" to product.cost
        )

        if (product.expirationDate != null) {
            productData["expiration_date"] = product.expirationDate
            return db.collection("products")
                .add(productData)
        } else {
            return db.collection("product_types")
                .document(product.productTypeId)
                .get()
                .continueWithTask { task ->
                if (task.isSuccessful) {
                    productData["expiration_date"] = task.result?.getLong("time_to_expire")
                        ?.let { product.purchaseDate + it * 60 * 1000 /* Convert minutes to milliseconds */ }
                        ?: throw IllegalStateException("Expiration date not found")
                    db.collection("products")
                        .add(productData)
                } else {
                    throw task.exception ?: IllegalStateException("Failed to fetch product type")
                }
            }
        }
    }

    companion object {
        @Volatile
        private var instance: DB? = null

        fun getInstance(): DB {
            return instance ?: synchronized(this) {
                instance ?: DB().also { instance = it }
            }
        }
    }

    private val db = FirebaseFirestore.getInstance()

    private fun getUserIds(): MutableList<String> {
        val userIds = mutableListOf("global")
        FirebaseAuth.getInstance().currentUser?.uid?.let {
            userIds.add(it)
        }
        return userIds
    }
}