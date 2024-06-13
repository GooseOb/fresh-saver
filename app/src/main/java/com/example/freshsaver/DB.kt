package com.example.freshsaver

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

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

data class Recipe(
    val id: String,
    var title: String,
    var imageUrl: String? = null,
    var productTypeIds: List<String>,
    var text: String,
    val userId: String = "global"
)


data class NewRecipe(
    var title: String,
    var imageUrl: String? = null,
    var productTypeIds: List<String>,
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
        return products
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

    fun getProductTypeById(id: String): Task<ProductType> {
        return productTypes.document(id).get().continueWith { task ->
            if (task.isSuccessful) {
                val document = task.result
                ProductType(
                    id = document.id,
                    userId = document.getString("user_id") ?: "",
                    categoryId = document.getString("category_id") ?: "",
                    title = document.getString("title") ?: "",
                    imageUrl = document.getString("image_url"),
                    timeToExpire = document.getLong("time_to_expire")?.toInt()
                )
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

    fun createRecipe(recipe: NewRecipe): Task<DocumentReference> {
        return recipes.add(mapOf(
            "title" to recipe.title,
            "image_url" to recipe.imageUrl,
            "product_type_ids" to recipe.productTypeIds,
            "user_id" to auth.currentUser?.uid
        ))
    }

    fun getRecipesOrdered(): Task<List<Recipe>> {
        val userProductsTask = products.get()
        val productTypesTask = productTypes.get()
        val currentTime = System.currentTimeMillis()

        return Tasks.whenAllSuccess<QuerySnapshot>(userProductsTask, productTypesTask).onSuccessTask { snapshots ->
            val productsSnapshot = snapshots[0]
            val productTypesSnapshot = snapshots[1]

            val productTypeMap = productTypesSnapshot.documents.associate { doc ->
                doc.id to doc.getString("title")
            }

            val userProductTypeIds = productsSnapshot.documents
                .filter { doc ->
                    val expirationDate = doc.getLong("expiration_date") ?: 0L
                    expirationDate > currentTime
                }
                .mapNotNull { it.getString("product_type_id") }

            recipes
                .whereIn("user_id", getUserIds())
                .get()
                .continueWith { task ->
                    val recipesSnapshot = task.result
                    val recipesList = recipesSnapshot?.documents?.map { document ->
                        Recipe(
                            id = document.id,
                            title = document.getString("title") ?: "",
                            imageUrl = document.getString("image_url"),
                            productTypeIds = document.get("product_type_ids") as? List<String> ?: emptyList(),
                            text = document.getString("text") ?: "", // Добавлено получение текста рецепта
                            userId = document.getString("user_id") ?: "global"
                        )
                    } ?: emptyList()

                    recipesList.sortedByDescending { recipe ->
                        recipe.productTypeIds.count { it in userProductTypeIds }
                    }.map{
                        it.apply {
                            productTypeIds = productTypeIds.mapNotNull { productTypeMap[it] }
                        }
                    }
                }
        }
    }


    fun deleteUserProduct(id: String): Task<Void> {
        return products.document(id).delete()
    }

    fun deleteProductType(id: String): Task<Void> {
        return productTypes.document(id).delete()
    }

    fun deleteProductsByType(productTypeId: String): Task<Void> {
        return getProductsByType(productTypeId).onSuccessTask { products ->
            val deleteTasks = products.map { product ->
                deleteUserProduct(product.id)
            }
            Tasks.whenAll(deleteTasks)
        }
    }

    fun deleteCategory(id: String): Task<Void> {
        return getProductTypesByCategory(id).onSuccessTask { productTypes ->
            val deleteTasks = productTypes.map { productType ->
                deleteProductsByType(productType.id).onSuccessTask {
                    deleteProductType(productType.id)
                }
            }
            Tasks.whenAll(deleteTasks)
        }.onSuccessTask {
            categories.document(id).delete()
        }
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
            "category_id" to productType.categoryId,
            "title" to productType.title,
            "image_url" to productType.imageUrl,
            "time_to_expire" to productType.timeToExpire,
            "user_id" to productType.userId
        ))
    }

    fun setCategory(category: Category): Task<Void> {
        return categories.document(category.id).set(mapOf(
            "title" to category.title,
            "image_url" to category.imageUrl,
            "user_id" to category.userId
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

    fun deleteRecipe(id: String): Task<Void> {
        return recipes.document(id).delete()
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val products = db.collection("products")
    private val productTypes = db.collection("product_types")
    private val categories = db.collection("categories")
    private val recipes = db.collection("recipes")

    private fun getUserIds(): MutableList<String> {
        val userIds = mutableListOf("global")
        auth.currentUser?.uid?.let {
            userIds.add(it)
        }
        return userIds
    }
}
