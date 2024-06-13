package com.example.freshsaver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class RecipeDetailActivity : AppCompatActivity() {

    private lateinit var productRecyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private val productTypeIds = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_detail)

        val recipeTitle = intent.getStringExtra("recipeTitle") ?: ""
        val recipeImageUrl = intent.getStringExtra("recipeImageUrl")
        val recipeDescription = intent.getStringExtra("recipeDescription")
        val productTypeIdsFromIntent = intent.getStringArrayListExtra("productTypeIds") ?: arrayListOf()

        val titleTextView: TextView = findViewById(R.id.recipe_detail_title)
        val imageView: ImageView = findViewById(R.id.recipe_detail_image)
        val descriptionTextView: TextView = findViewById(R.id.recipe_detail_description)

        titleTextView.text = recipeTitle
        descriptionTextView.text = recipeDescription

        if (recipeImageUrl != null) {
            Picasso.get().load(recipeImageUrl).into(imageView)
        } else {
            imageView.setImageResource(R.drawable.ic_placeholder)
        }

        productRecyclerView = findViewById(R.id.product_recycler_view)
        productRecyclerView.layoutManager = LinearLayoutManager(this)
        productAdapter = ProductAdapter(productTypeIds)
        productRecyclerView.adapter = productAdapter

        fetchProductTypeIds(productTypeIdsFromIntent)
    }

    private fun fetchProductTypeIds(productTypeIdsFromIntent: List<String>) {
        productTypeIds.clear()
        productTypeIds.addAll(productTypeIdsFromIntent)
        productAdapter.notifyDataSetChanged()
    }

    private class ProductAdapter(private val productTypeIds: List<String>) :
        RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

        class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            private val productTypeIdTextView: TextView = view.findViewById(R.id.product_name)

            fun bind(productTypeId: String) {
                productTypeIdTextView.text = productTypeId
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_product_name, parent, false)
            return ProductViewHolder(view)
        }

        override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
            holder.bind(productTypeIds[position])
        }

        override fun getItemCount() = productTypeIds.size
    }
}
