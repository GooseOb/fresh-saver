package com.example.freshsaver

import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

data class Category(
    val id: String,
    val userId: String = "global",
    var title: String,
    var imageUrl: String? = null
)

data class NewCategory(
    var title: String,
    var imageUrl: String? = null
)

data class ProductType(
    val id: String,
    val userId: String = "global",
    var categoryId: String,
    var title: String,
    var imageUrl: String? = null,
    var timeToExpire: Int? = null
)

data class NewProductType(
    var categoryId: String,
    var title: String,
    var imageUrl: String? = null,
    var timeToExpire: Int? = null
)

data class Product(
    val id: String,
    var productTypeId: String,
    var purchaseDate: Long,
    var expirationDate: Long,
    var title: String? = null,
    var cost: Double? = null
)

data class NewProduct(
    var productTypeId: String,
    var purchaseDate: Long,
    var expirationDate: Long? = null,
    var title: String? = null,
    var cost: Double? = null
)

class DB {
    fun getCategories(): Task<List<Category>> {
        return categories
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
        return productTypes
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

    fun getProductsByType(productTypeId: String): Task<List<Product>> {
        return db.collection("products")
            .whereEqualTo("product_type_id", productTypeId)
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
        val userId = auth.currentUser?.uid
        val productData = hashMapOf(
            "product_type_id" to product.productTypeId,
            "user_id" to userId,
            "purchase_date" to product.purchaseDate,
            "title" to product.title,
            "cost" to product.cost
        )

        if (product.expirationDate != null) {
            productData["expiration_date"] = product.expirationDate
            return products.add(productData)
        } else {
            return productTypes
                .document(product.productTypeId)
                .get()
                .continueWithTask { task ->
                    if (task.isSuccessful) {
                        productData["expiration_date"] = task.result?.getLong("time_to_expire")
                            ?.let { product.purchaseDate + it * 60 * 1000 /* Convert minutes to milliseconds */ }
                            ?: throw IllegalStateException("Expiration date not found")
                        products.add(productData)
                    } else {
                        throw task.exception ?: IllegalStateException("Failed to fetch product type")
                    }
                }
        }
    }

    fun createCategory(category: NewCategory): Task<DocumentReference> {
        return categories.add(hashMapOf(
            "title" to category.title,
            "user_id" to auth.currentUser?.uid,
            "image_url" to category.imageUrl
        ))
    }

    fun createProductType(productType: NewProductType): Task<DocumentReference> {
        return productTypes.add(hashMapOf(
            "category_id" to productType.categoryId,
            "user_id" to auth.currentUser?.uid,
            "title" to productType.title,
            "image_url" to productType.imageUrl,
            "time_to_expire" to productType.timeToExpire
        ))
    }

    fun deleteUserProduct(id: String): Task<Void> {
        return products.document(id).delete()
    }

    fun deleteProductType(id: String): Task<Void> {
        return productTypes.document(id).delete()
    }

    fun deleteCategory(id: String): Task<Void> {
        return categories.document(id).delete()
    }

    fun setUserProduct(product: Product): Task<Void> {
        return products.document(product.id).set(mapOf(
            "product_type_id" to product.productTypeId,
            "purchase_date" to product.purchaseDate,
            "expiration_date" to product.expirationDate,
            "title" to product.title,
            "cost" to product.cost
        ))
    }

    fun setProductType(productType: ProductType): Task<Void> {
        return productTypes.document(productType.id).set(mapOf(
            "user_id" to productType.userId,
            "category_id" to productType.categoryId,
            "title" to productType.title,
            "image_url" to productType.imageUrl,
            "time_to_expire" to productType.timeToExpire
        ))
    }

    fun setCategory(category: Category): Task<Void> {
        return categories.document(category.id).set(mapOf(
            "user_id" to category.userId,
            "title" to category.title,
            "image_url" to category.imageUrl
        ))
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
    private val auth = FirebaseAuth.getInstance()

    private val products = db.collection("products")
    private val productTypes = db.collection("product_types")
    private val categories = db.collection("categories")

    private fun getUserIds(): MutableList<String> {
        val userIds = mutableListOf("global")
        auth.currentUser?.uid?.let {
            userIds.add(it)
        }
        return userIds
    }
}
