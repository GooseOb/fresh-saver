package com.example.freshsaver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private val productList = mutableListOf<Product>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        productAdapter = ProductAdapter(productList) { product ->
            // Обработка клика по элементу списка
            // Можно открыть новое окно или показать детали продукта
        }
        recyclerView.adapter = productAdapter

        fetchUserProducts()

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
                // Обработка ошибок
            }
    }
}
