package com.example.freshsaver

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ProductAdapter(
    private val products: List<Product>,
    private val onProductLongClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productName: TextView = itemView.findViewById(R.id.product_name)
        val productDescription: TextView = itemView.findViewById(R.id.product_description)
        val productExpiration: TextView = itemView.findViewById(R.id.product_expiration)

        init {
            itemView.setOnLongClickListener {
                onProductLongClick(products[adapterPosition])
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.productName.text = product.title
        holder.productDescription.text = "Cost: ${product.cost ?: "Unknown"}"

        val currentTime = System.currentTimeMillis()
        val expirationTime = product.expirationDate

        val daysLeft = calculateDaysLeft(currentTime, expirationTime)
        if (daysLeft > 0) {
            holder.productExpiration.text = "Days Left: $daysLeft"
            holder.productExpiration.setTextColor(when (daysLeft) {
                1, 2, 3 -> Color.parseColor("#FFA500") // Orange color
                0 -> Color.parseColor("#FF0000") // Red color
                else -> Color.BLACK // Default color
            })
        } else if (daysLeft == 0) {
            holder.productExpiration.text = "Days Left: $daysLeft"
            holder.productExpiration.setTextColor(Color.parseColor("#FF0000")) // Red color
        } else {
            holder.productExpiration.text = "Expiration Date Passed"
            holder.productExpiration.setTextColor(Color.parseColor("#FF0000")) // Red color
        }
    }

    override fun getItemCount() = products.size

    private fun calculateDaysLeft(currentTime: Long, expirationTime: Long): Int {
        val currentCalendar = Calendar.getInstance()
        currentCalendar.timeInMillis = currentTime
        currentCalendar.set(Calendar.HOUR_OF_DAY, 0)
        currentCalendar.set(Calendar.MINUTE, 0)
        currentCalendar.set(Calendar.SECOND, 0)
        currentCalendar.set(Calendar.MILLISECOND, 0)

        val expirationCalendar = Calendar.getInstance()
        expirationCalendar.timeInMillis = expirationTime
        expirationCalendar.set(Calendar.HOUR_OF_DAY, 0)
        expirationCalendar.set(Calendar.MINUTE, 0)
        expirationCalendar.set(Calendar.SECOND, 0)
        expirationCalendar.set(Calendar.MILLISECOND, 0)

        val diffInMillis = expirationCalendar.timeInMillis - currentCalendar.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
    }
}
