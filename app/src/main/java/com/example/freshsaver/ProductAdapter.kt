package com.example.freshsaver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class ProductAdapter(private val productList: List<Product>, private val onItemClick: (Product) -> Unit) :
    RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View, val onItemClick: (Product) -> Unit) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.product_name)
        val descriptionTextView: TextView = itemView.findViewById(R.id.product_description)
        val expirationTextView: TextView = itemView.findViewById(R.id.product_expiration)

        fun bind(product: Product) {
            nameTextView.text = product.title ?: "No Title"
            descriptionTextView.text = "Cost: ${product.cost ?: "Unknown"}"
            val daysLeft = calculateDaysLeft(product.expirationDate)
            expirationTextView.text = "Days Left: $daysLeft"
            itemView.setOnClickListener { onItemClick(product) }
        }

        private fun calculateDaysLeft(expirationDate: Long): Long {
            val currentTime = System.currentTimeMillis()
            val timeDifference = expirationDate - currentTime
            val daysLeft = TimeUnit.MILLISECONDS.toDays(timeDifference)
            return if (daysLeft < 0) 0 else daysLeft
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(itemView, onItemClick)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount() = productList.size
}
