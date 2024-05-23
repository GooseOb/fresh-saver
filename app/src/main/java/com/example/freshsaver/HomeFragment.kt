package com.example.freshsaver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.app.AlertDialog
import android.widget.*
import com.squareup.picasso.Picasso

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private val productList = mutableListOf<Product>()
    private val categoryList = mutableListOf<Category>()
    private val productTypesByCategory = mutableListOf<ProductType>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        productAdapter = ProductAdapter(productList) { product ->
            // Handle product item click
            // You can open a new window or show product details here
        }
        recyclerView.adapter = productAdapter

        val imageButton: ImageButton = view.findViewById(R.id.imageButton)
        imageButton.setOnClickListener {
            showCategoryDialog()
        }

        fetchUserProducts()
        fetchCategories()

        return view
    }

    private fun fetchUserProducts() {
        DB.getInstance().getUserProducts()
            .addOnSuccessListener { result ->
                productList.clear()
                productList.addAll(result)
                productAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Handle errors
            }
    }

    private fun fetchCategories() {
        DB.getInstance().getCategories()
            .addOnSuccessListener { result ->
                categoryList.clear()
                categoryList.addAll(result)
            }
            .addOnFailureListener { exception ->
                // Handle errors
            }
    }

    private fun showCategoryDialog() {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Select a Category")

        val layout = LinearLayout(context)
        layout.orientation = LinearLayout.VERTICAL

        categoryList.forEach { category ->
            val button = ImageButton(context)
            // Set image to button if imageUrl is not null
            category.imageUrl?.let {
                loadImageFromUrl(it, button)
            }
            button.setOnClickListener {
                showProductTypesDialog(category)
            }
            layout.addView(button)
        }

        builder.setView(layout)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.show()
    }

    private fun showProductTypesDialog(category: Category) {
        DB.getInstance().getProductTypesByCategory(category.id)
            .addOnSuccessListener { result ->
                productTypesByCategory.clear()
                productTypesByCategory.addAll(result)

                val builder = AlertDialog.Builder(context)
                builder.setTitle("Select a Product Type from ${category.title}")

                val layout = LinearLayout(context)
                layout.orientation = LinearLayout.VERTICAL

                productTypesByCategory.forEach { productType ->
                    val button = ImageButton(context)
                    // Set image to button if product type has image
                    productType.imageUrl?.let {
                        loadImageFromUrl(it, button)
                    }
                    button.setOnClickListener {
                        showAddProductDialog(productType)
                    }
                    layout.addView(button)
                }

                builder.setView(layout)
                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                builder.show()
            }
            .addOnFailureListener { exception ->
                // Handle errors
            }
    }

    private fun showAddProductDialog(productType: ProductType) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle("Add Product: ${productType.title}")

        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_product, null)
        builder.setView(dialogView)

        val expirationDateEditText: EditText = dialogView.findViewById(R.id.editTextExpirationDate)
        val priceEditText: EditText = dialogView.findViewById(R.id.editTextPrice)
        val titleEditText: EditText = dialogView.findViewById(R.id.editTextTitle)

        builder.setPositiveButton("Add") { dialog, _ ->
            val expirationDate = expirationDateEditText.text.toString().toInt()
            val price = priceEditText.text.toString().toDouble()
            val title = titleEditText.text.toString()

            val currentTime = System.currentTimeMillis()
            val expirationTime = currentTime + expirationDate * 24 * 60 * 60 * 1000L // Convert days to milliseconds

            val newProduct = NewProduct(
                productTypeId = productType.id,
                purchaseDate = currentTime,
                expirationDate = expirationTime,
                title = title,
                cost = price
            )

            DB.getInstance().addUserProduct(newProduct)
                .addOnSuccessListener {
                    Toast.makeText(context, "Product added successfully", Toast.LENGTH_SHORT).show()
                    fetchUserProducts() // Обновление списка продуктов
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to add product", Toast.LENGTH_SHORT).show()
                }
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        builder.show()
    }

    private fun loadImageFromUrl(url: String, imageView: ImageView) {
        // Load image from URL using Picasso
        Picasso.get().load(url).into(imageView)
    }
}
