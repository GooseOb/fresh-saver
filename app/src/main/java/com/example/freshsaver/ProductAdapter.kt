package com.example.freshsaver

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ProductAdapter(
    private val productList: List<Product>,
    private val onItemClick: (Product) -> Unit,
    private val onItemLongClick: (Product) -> Unit // Добавим обработчик долгого нажатия
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]
        holder.bind(product, onItemClick, onItemLongClick)
    }

    override fun getItemCount() = productList.size

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productName: TextView = itemView.findViewById(R.id.product_name)
        private val productDescription: TextView = itemView.findViewById(R.id.product_description)
        private val productExpiration: TextView = itemView.findViewById(R.id.product_expiration)

        fun bind(product: Product, onItemClick: (Product) -> Unit, onItemLongClick: (Product) -> Unit) {
            productName.text = product.title
            productDescription.text = "Cost: ${product.cost ?: "Unknown"}"
            val daysLeft = ((product.expirationDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
            productExpiration.text = "Days Left: $daysLeft"

            itemView.setOnClickListener { onItemClick(product) }
            itemView.setOnLongClickListener {
                onItemLongClick(product)
                true
            }
        }
    }
}
