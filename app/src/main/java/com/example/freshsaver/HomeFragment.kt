package com.example.freshsaver

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private val productList = mutableListOf<Product>()
    private val categoryList = mutableListOf<Category>()
    private val productTypesByCategory = mutableListOf<ProductType>()
    private var selectedExpirationDate: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        productAdapter = ProductAdapter(productList, { product ->
            // Handle product item click
            // You can open a new window or show product details here
        }, { product ->
            showEditProductDialog(product)
        })
        recyclerView.adapter = productAdapter

        // Добавляем функциональность смахивания
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val product = productList[position]
                deleteProduct(product, position)
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

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
                Log.d("HomeFragment", "User products fetched: $productList")
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error fetching products", exception)
            }
    }

    private fun fetchCategories() {
        DB.getInstance().getCategories()
            .addOnSuccessListener { result ->
                categoryList.clear()
                categoryList.addAll(result)
                Log.d("HomeFragment", "Categories fetched: $categoryList")
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error fetching categories", exception)
            }
    }

    private fun showCategoryDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_select_category, null)
        builder.setView(dialogView)

        val gridLayout: GridLayout = dialogView.findViewById(R.id.gridLayoutCategories)

        val dialog = builder.create()

        categoryList.forEachIndexed { index, category ->
            val button = ImageButton(context).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
            }
            val layoutParams = GridLayout.LayoutParams().apply {
                width = 60.dpToPx()
                height = 60.dpToPx()
                rowSpec = GridLayout.spec(index / 3)
                columnSpec = GridLayout.spec(index % 3)
                setMargins(8, 8, 8, 8)
            }
            button.layoutParams = layoutParams

            category.imageUrl?.let {
                loadImageFromUrl(it, button)
            }

            button.setOnClickListener {
                Log.d("HomeFragment", "Category selected: ${category.title}")
                dialog.dismiss()
                showProductTypesDialog(category)
            }

            gridLayout.addView(button)
        }

        // Добавляем кнопку для создания новой категории
        val addCategoryButton = ImageButton(context).apply {
            val layoutParams = GridLayout.LayoutParams().apply {
                width = 60.dpToPx()
                height = 60.dpToPx()
                rowSpec = GridLayout.spec(categoryList.size / 3)
                columnSpec = GridLayout.spec(categoryList.size % 3)
                setMargins(8, 8, 8, 8)
            }
            this.layoutParams = layoutParams
            setImageResource(android.R.drawable.ic_input_add)
            setBackgroundColor(resources.getColor(android.R.color.transparent))
        }

        addCategoryButton.setOnClickListener {
            Log.d("HomeFragment", "Add new category button clicked")
            dialog.dismiss()
            showAddCategoryDialog()
        }

        gridLayout.addView(addCategoryButton)

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        dialog.show()
    }

    private fun showProductTypesDialog(category: Category) {
        Log.d("HomeFragment", "Showing product types for category: ${category.title}")
        DB.getInstance().getProductTypesByCategory(category.id)
            .addOnSuccessListener { result ->
                productTypesByCategory.clear()
                productTypesByCategory.addAll(result)
                Log.d("HomeFragment", "Product types fetched: $productTypesByCategory")

                val builder = AlertDialog.Builder(requireContext())
                val inflater = requireActivity().layoutInflater
                val dialogView = inflater.inflate(R.layout.dialog_select_product_type, null)
                builder.setView(dialogView)

                val gridLayout: GridLayout = dialogView.findViewById(R.id.gridLayoutProductTypes)

                val dialog = builder.create()

                productTypesByCategory.forEachIndexed { index, productType ->
                    val button = ImageButton(context).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }
                    val layoutParams = GridLayout.LayoutParams().apply {
                        width = 60.dpToPx()
                        height = 60.dpToPx()
                        rowSpec = GridLayout.spec(index / 3)
                        columnSpec = GridLayout.spec(index % 3)
                        setMargins(8, 8, 8, 8)
                    }
                    button.layoutParams = layoutParams

                    productType.imageUrl?.let {
                        loadImageFromUrl(it, button)
                    }

                    button.setOnClickListener {
                        Log.d("HomeFragment", "Product type selected: ${productType.title}")
                        dialog.dismiss()
                        showAddProductDialog(productType)
                    }

                    gridLayout.addView(button)
                }

                // Добавляем кнопку для создания нового типа продукта
                val addProductTypeButton = ImageButton(context).apply {
                    val layoutParams = GridLayout.LayoutParams().apply {
                        width = 60.dpToPx()
                        height = 60.dpToPx()
                        rowSpec = GridLayout.spec(productTypesByCategory.size / 3)
                        columnSpec = GridLayout.spec(productTypesByCategory.size % 3)
                        setMargins(8, 8, 8, 8)
                    }
                    this.layoutParams = layoutParams
                    setImageResource(android.R.drawable.ic_input_add)
                    setBackgroundColor(resources.getColor(android.R.color.transparent))
                }

                addProductTypeButton.setOnClickListener {
                    Log.d("HomeFragment", "Add new product type button clicked")
                    dialog.dismiss()
                    showAddProductTypeDialog(category.id)
                }

                gridLayout.addView(addProductTypeButton)

                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                dialog.show()
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error fetching product types", exception)
            }
    }

    private fun showAddProductDialog(productType: ProductType) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add Product: ${productType.title}")

        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_product, null)
        builder.setView(dialogView)

        val buttonSelectDate: Button = dialogView.findViewById(R.id.buttonSelectDate)
        val priceEditText: EditText = dialogView.findViewById(R.id.editTextPrice)
        val titleEditText: EditText = dialogView.findViewById(R.id.editTextTitle)

        buttonSelectDate.setOnClickListener {
            showDatePicker { date ->
                selectedExpirationDate = date
                buttonSelectDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date))
            }
        }

        builder.setPositiveButton("Add") { dialog, _ ->
            val price = priceEditText.text.toString().toDoubleOrNull() ?: 0.0
            val title = titleEditText.text.toString()

            val currentTime = System.currentTimeMillis()

            val newProduct = NewProduct(
                productTypeId = productType.id,
                purchaseDate = currentTime,
                expirationDate = selectedExpirationDate,
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

    private fun showEditProductDialog(product: Product) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Product: ${product.title}")

        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_edit_product, null)
        builder.setView(dialogView)

        val buttonEditDate: Button = dialogView.findViewById(R.id.buttonEditDate)
        val priceEditText: EditText = dialogView.findViewById(R.id.editTextEditPrice)
        val titleEditText: EditText = dialogView.findViewById(R.id.editTextEditTitle)
        val buttonSaveProduct: Button = dialogView.findViewById(R.id.buttonSaveProduct)

        buttonEditDate.setOnClickListener {
            showDatePicker { date ->
                selectedExpirationDate = date
                buttonEditDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(date))
            }
        }

        // Заполнение полей текущими значениями продукта
        priceEditText.setText(product.cost?.toString() ?: "")
        titleEditText.setText(product.title ?: "")
        selectedExpirationDate = product.expirationDate
        buttonEditDate.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(selectedExpirationDate))

        val dialog = builder.create()

        buttonSaveProduct.setOnClickListener {
            val price = priceEditText.text.toString().toDoubleOrNull() ?: 0.0
            val title = titleEditText.text.toString()

            val updatedProduct = product.copy(
                expirationDate = selectedExpirationDate,
                title = title,
                cost = price,
                userId = product.userId // Убедитесь, что userId передается правильно
            )

            DB.getInstance().setUserProduct(updatedProduct)
                .addOnSuccessListener {
                    Toast.makeText(context, "Product updated successfully", Toast.LENGTH_SHORT).show()
                    Log.d("HomeFragment", "Product updated successfully: $updatedProduct")
                    fetchUserProducts() // Обновление списка продуктов
                    dialog.dismiss() // Закрытие диалога после сохранения
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to update product", Toast.LENGTH_SHORT).show()
                    Log.e("HomeFragment", "Failed to update product", it)
                }
        }

        dialog.show()
    }

    private fun deleteProduct(product: Product, position: Int) {
        DB.getInstance().deleteUserProduct(product.id)
            .addOnSuccessListener {
                Toast.makeText(context, "Product deleted successfully", Toast.LENGTH_SHORT).show()
                productList.removeAt(position)
                productAdapter.notifyItemRemoved(position)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to delete product", Toast.LENGTH_SHORT).show()
                productAdapter.notifyItemChanged(position) // Восстановить элемент в списке в случае ошибки
            }
    }

    private fun showDatePicker(onDateSet: (Long) -> Unit) {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                onDateSet(calendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun showAddCategoryDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add New Category")

        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_category, null)
        builder.setView(dialogView)

        val titleEditText: EditText = dialogView.findViewById(R.id.editTextCategoryTitle)
        val imageUrlEditText: EditText = dialogView.findViewById(R.id.editTextCategoryImageUrl)

        builder.setPositiveButton("Add", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val title = titleEditText.text.toString().trim()
                val imageUrl = imageUrlEditText.text.toString().trim()

                if (title.isEmpty()) {
                    titleEditText.error = "Title is required"
                    return@setOnClickListener
                }

                if (imageUrl.isEmpty()) {
                    imageUrlEditText.error = "Image URL is required"
                    return@setOnClickListener
                }

                val newCategory = NewCategory(
                    title = title,
                    imageUrl = imageUrl
                )

                DB.getInstance().createCategory(newCategory)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Category added successfully", Toast.LENGTH_SHORT).show()
                        fetchCategories() // Обновление списка категорий
                        dialog.dismiss() // Закрытие диалога после сохранения
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to add category", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        dialog.show()
    }

    private fun showAddProductTypeDialog(categoryId: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add New Product Type")

        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_product_type, null)
        builder.setView(dialogView)

        val titleEditText: EditText = dialogView.findViewById(R.id.editTextProductTypeTitle)
        val imageUrlEditText: EditText = dialogView.findViewById(R.id.editTextProductTypeImageUrl)
        val timeToExpireEditText: EditText = dialogView.findViewById(R.id.editTextProductTypeTimeToExpire)

        builder.setPositiveButton("Add", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val title = titleEditText.text.toString().trim()
                val imageUrl = imageUrlEditText.text.toString().trim()
                val timeToExpire = timeToExpireEditText.text.toString().toIntOrNull()

                if (title.isEmpty()) {
                    titleEditText.error = "Title is required"
                    return@setOnClickListener
                }

                if (imageUrl.isEmpty()) {
                    imageUrlEditText.error = "Image URL is required"
                    return@setOnClickListener
                }

                if (timeToExpire == null) {
                    timeToExpireEditText.error = "Time to expire is required"
                    return@setOnClickListener
                }

                val newProductType = NewProductType(
                    categoryId = categoryId,
                    title = title,
                    imageUrl = imageUrl,
                    timeToExpire = timeToExpire
                )

                DB.getInstance().createProductType(newProductType)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Product type added successfully", Toast.LENGTH_SHORT).show()
                        fetchProductTypesAndUpdateDialog(categoryId, dialog) // Обновление списка типов продуктов
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to add product type", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        dialog.show()
    }

    private fun fetchProductTypesAndUpdateDialog(categoryId: String, dialog: AlertDialog) {
        DB.getInstance().getProductTypesByCategory(categoryId)
            .addOnSuccessListener { result ->
                productTypesByCategory.clear()
                productTypesByCategory.addAll(result)
                Log.d("HomeFragment", "Product types updated: $productTypesByCategory")

                dialog.findViewById<GridLayout>(R.id.gridLayoutProductTypes)?.let { gridLayout ->
                    gridLayout.removeAllViews()

                    productTypesByCategory.forEachIndexed { index, productType ->
                        val button = ImageButton(context).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        val layoutParams = GridLayout.LayoutParams().apply {
                            width = 60.dpToPx()
                            height = 60.dpToPx()
                            rowSpec = GridLayout.spec(index / 3)
                            columnSpec = GridLayout.spec(index % 3)
                            setMargins(8, 8, 8, 8)
                        }
                        button.layoutParams = layoutParams

                        productType.imageUrl?.let {
                            loadImageFromUrl(it, button)
                        }

                        button.setOnClickListener {
                            dialog.dismiss()
                            showAddProductDialog(productType)
                        }

                        gridLayout.addView(button)
                    }

                    // Добавляем кнопку для создания нового типа продукта
                    val addProductTypeButton = ImageButton(context).apply {
                        val layoutParams = GridLayout.LayoutParams().apply {
                            width = 60.dpToPx()
                            height = 60.dpToPx()
                            rowSpec = GridLayout.spec(productTypesByCategory.size / 3)
                            columnSpec = GridLayout.spec(productTypesByCategory.size % 3)
                            setMargins(8, 8, 8, 8)
                        }
                        this.layoutParams = layoutParams
                        setImageResource(android.R.drawable.ic_input_add)
                        setBackgroundColor(resources.getColor(android.R.color.transparent))
                    }

                    addProductTypeButton.setOnClickListener {
                        dialog.dismiss()
                        showAddProductTypeDialog(categoryId)
                    }

                    gridLayout.addView(addProductTypeButton)
                } ?: Log.e("HomeFragment", "GridLayout for product types is null")
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error fetching product types", exception)
            }
    }

    private fun loadImageFromUrl(url: String, imageView: ImageView) {
        // Load image from URL using Picasso
        Picasso.get().load(url).fit().centerCrop().into(imageView)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}
