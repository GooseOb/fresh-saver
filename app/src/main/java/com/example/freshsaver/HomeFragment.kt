package com.example.freshsaver

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.squareup.picasso.Picasso
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private val productList = mutableListOf<Product>()
    private val categoryList = mutableListOf<Category>()
    private val productTypesByCategory = mutableListOf<ProductType>()
    private var selectedExpirationDate: Long = 0L

    private val POST_NOTIFICATIONS_REQUEST_CODE = 1001
    private val SCHEDULE_EXACT_ALARM_REQUEST_CODE = 1002
    private val PREFS_NAME = "FreshSaverPrefs"
    private val KEY_NOTIF_PERMISSION_REQUESTED = "notifPermissionRequested"
    private val KEY_EXACT_ALARM_PERMISSION_REQUESTED = "exactAlarmPermissionRequested"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        productAdapter = ProductAdapter(productList) { product ->
            showEditProductDialog(product)
        }
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_NOTIF_PERMISSION_REQUESTED, false)) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), POST_NOTIFICATIONS_REQUEST_CODE)
                    prefs.edit().putBoolean(KEY_NOTIF_PERMISSION_REQUESTED, true).apply()
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!prefs.getBoolean(KEY_EXACT_ALARM_PERMISSION_REQUESTED, false)) {
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.SCHEDULE_EXACT_ALARM) != PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent().apply {
                        action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    }
                    startActivityForResult(intent, SCHEDULE_EXACT_ALARM_REQUEST_CODE)
                    prefs.edit().putBoolean(KEY_EXACT_ALARM_PERMISSION_REQUESTED, true).apply()
                }
            }
        }

        return view
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == POST_NOTIFICATIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with notification scheduling
                fetchUserProducts()
            } else {
                Toast.makeText(context, "Notification permission is required to receive expiration alerts", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchUserProducts() {
        DB.getInstance().getUserProducts()
            .addOnSuccessListener { result ->
                productList.clear()
                productList.addAll(result)
                productAdapter.notifyDataSetChanged()
                result.forEach { product ->
                    scheduleExpirationNotifications(product)
                }
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

            button.setOnLongClickListener {
                showEditCategoryDialog(category)
                true
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

                    button.setOnLongClickListener {
                        showEditProductTypeDialog(productType)
                        true
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

                val newProductType = NewProductType(
                    categoryId = categoryId,
                    title = title,
                    imageUrl = imageUrl
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

                        button.setOnLongClickListener {
                            showEditProductTypeDialog(productType)
                            true
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

    private fun showAddProductDialog(productType: ProductType) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Add New Product")

        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_product, null)
        builder.setView(dialogView)

        val titleEditText: EditText = dialogView.findViewById(R.id.editTextTitle)
        val priceEditText: EditText = dialogView.findViewById(R.id.editTextPrice)
        val buttonSelectDate: Button = dialogView.findViewById(R.id.buttonSelectDate)

        val calendar = Calendar.getInstance()
        updateButtonDateText(buttonSelectDate, calendar)

        buttonSelectDate.setOnClickListener {
            showDatePickerDialog(calendar) { selectedDate ->
                calendar.timeInMillis = selectedDate.timeInMillis
                updateButtonDateText(buttonSelectDate, calendar)
            }
        }

        builder.setPositiveButton("Add", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        val dialog = builder.create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setOnClickListener {
                val title = titleEditText.text.toString().trim()
                val price = priceEditText.text.toString().toDoubleOrNull()
                val expirationDate = calendar.timeInMillis

                if (title.isEmpty()) {
                    titleEditText.error = "Title is required"
                    return@setOnClickListener
                }

                if (price == null) {
                    priceEditText.error = "Price is required"
                    return@setOnClickListener
                }

                val newProduct = NewProduct(
                    productTypeId = productType.id,
                    purchaseDate = System.currentTimeMillis(),
                    expirationDate = expirationDate,
                    title = title,
                    cost = price
                )

                DB.getInstance().addUserProduct(newProduct)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Product added successfully", Toast.LENGTH_SHORT).show()
                        fetchUserProducts() // Обновление списка продуктов
                        dialog.dismiss() // Закрытие диалога после сохранения
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to add product", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        dialog.show()
    }

    private fun loadImageFromUrl(url: String, imageView: ImageView) {
        // Load image from URL using Picasso
        Picasso.get().load(url).fit().centerCrop().into(imageView)
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun scheduleNotification(product: Product, daysLeft: Int) {
        val currentTime = System.currentTimeMillis()
        val notificationTime = product.expirationDate - TimeUnit.DAYS.toMillis(daysLeft.toLong())

        // Проверяем, что время для уведомления еще не прошло
        if (notificationTime > currentTime) {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("productName", product.title)
                putExtra("daysLeft", daysLeft)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                product.id.hashCode() + daysLeft,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.setExact(AlarmManager.RTC_WAKEUP, notificationTime, pendingIntent)
        }
    }

    private fun cancelExistingNotifications(product: Product) {
        val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

        for (i in 0..3) {
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                product.id.hashCode() + i,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager?.cancel(pendingIntent)
        }
    }

    private fun scheduleExpirationNotifications(product: Product) {
        cancelExistingNotifications(product) // Отмена существующих уведомлений

        val currentTime = System.currentTimeMillis()
        val daysLeft = calculateDaysLeft(currentTime, product.expirationDate)

        for (i in 0..daysLeft) {
            if (daysLeft - i >= 0) {
                scheduleNotification(product, daysLeft - i)
            }
        }
    }

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
        val daysLeft = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

        return if (daysLeft >= 0) daysLeft else 0
    }

    private fun showEditProductDialog(product: Product) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Product")

        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_edit_product, null)
        builder.setView(dialogView)

        val titleEditText: EditText = dialogView.findViewById(R.id.editTextEditTitle)
        val priceEditText: EditText = dialogView.findViewById(R.id.editTextEditPrice)
        val buttonSelectDate: Button = dialogView.findViewById(R.id.buttonEditDate)

        titleEditText.setText(product.title)
        priceEditText.setText(product.cost?.toString())

        val calendar = Calendar.getInstance().apply {
            timeInMillis = product.expirationDate
        }
        updateButtonDateText(buttonSelectDate, calendar)

        buttonSelectDate.setOnClickListener {
            showDatePickerDialog(calendar) { selectedDate ->
                calendar.timeInMillis = selectedDate.timeInMillis
                updateButtonDateText(buttonSelectDate, calendar)
            }
        }

        builder.setPositiveButton("Save") { _, _ ->
            val updatedTitle = titleEditText.text.toString().trim()
            val updatedPrice = priceEditText.text.toString().toDoubleOrNull()
            val updatedExpirationDate = calendar.timeInMillis

            val updatedProduct = product.copy(
                title = updatedTitle,
                cost = updatedPrice,
                expirationDate = updatedExpirationDate
            )

            DB.getInstance().setUserProduct(updatedProduct)
                .addOnSuccessListener {
                    Toast.makeText(context, "Product updated successfully", Toast.LENGTH_SHORT).show()
                    fetchUserProducts() // Обновление списка продуктов
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to update product", Toast.LENGTH_SHORT).show()
                }
        }

        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }

        builder.create().show()
    }

    private fun showEditCategoryDialog(category: Category) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Category")

        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_category, null)
        builder.setView(dialogView)

        val titleEditText: EditText = dialogView.findViewById(R.id.editTextCategoryTitle)
        val imageUrlEditText: EditText = dialogView.findViewById(R.id.editTextCategoryImageUrl)

        titleEditText.setText(category.title)
        imageUrlEditText.setText(category.imageUrl)

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.setNeutralButton("Delete") { _, _ ->
            deleteCategory(category)
        }

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

                val updatedCategory = category.copy(
                    title = title,
                    imageUrl = imageUrl
                )

                DB.getInstance().setCategory(updatedCategory)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Category updated successfully", Toast.LENGTH_SHORT).show()
                        fetchCategories() // Обновление списка категорий
                        dialog.dismiss() // Закрытие диалога после сохранения
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update category", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        dialog.show()
    }

    private fun showEditProductTypeDialog(productType: ProductType) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Edit Product Type")

        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_product_type, null)
        builder.setView(dialogView)

        val titleEditText: EditText = dialogView.findViewById(R.id.editTextProductTypeTitle)
        val imageUrlEditText: EditText = dialogView.findViewById(R.id.editTextProductTypeImageUrl)

        titleEditText.setText(productType.title)
        imageUrlEditText.setText(productType.imageUrl)

        builder.setPositiveButton("Save", null)
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.setNeutralButton("Delete") { _, _ ->
            deleteProductType(productType)
        }

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

                val updatedProductType = productType.copy(
                    title = title,
                    imageUrl = imageUrl
                )

                DB.getInstance().setProductType(updatedProductType)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Product type updated successfully", Toast.LENGTH_SHORT).show()
                        fetchCategories() // Обновление списка категорий
                        dialog.dismiss() // Закрытие диалога после сохранения
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to update product type", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        dialog.show()
    }

    private fun updateButtonDateText(button: Button, calendar: Calendar) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        button.text = "Expiration Date: ${dateFormat.format(calendar.time)}"
    }

    private fun showDatePickerDialog(calendar: Calendar, onDateSelected: (Calendar) -> Unit) {
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                onDateSelected(calendar)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
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
            }
    }

    private fun deleteCategory(category: Category) {
        val db = DB.getInstance()
        db.getProductTypesByCategory(category.id)
            .addOnSuccessListener { productTypes ->
                val deletionTasks = productTypes.map { productType ->
                    db.getProductsByType(productType.id)
                        .continueWithTask { task ->
                            val productDeletionTasks = task.result?.map { product ->
                                db.deleteUserProduct(product.id)
                            } ?: emptyList()
                            Tasks.whenAll(productDeletionTasks)
                        }
                        .continueWithTask {
                            db.deleteProductType(productType.id)
                        }
                }

                Tasks.whenAll(deletionTasks)
                    .continueWithTask {
                        db.deleteCategory(category.id)
                    }
                    .addOnSuccessListener {
                        Toast.makeText(context, "Category and its products deleted successfully", Toast.LENGTH_SHORT).show()
                        fetchCategories()
                        fetchUserProducts()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to delete category and its products", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to fetch product types for category", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteProductType(productType: ProductType) {
        val db = DB.getInstance()
        db.getProductsByType(productType.id)
            .addOnSuccessListener { products ->
                val productDeletionTasks = products.map { product ->
                    db.deleteUserProduct(product.id)
                }

                Tasks.whenAll(productDeletionTasks)
                    .continueWithTask {
                        db.deleteProductType(productType.id)
                    }
                    .addOnSuccessListener {
                        Toast.makeText(context, "Product type and its products deleted successfully", Toast.LENGTH_SHORT).show()
                        fetchCategories()
                        fetchUserProducts()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to delete product type and its products", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to fetch products for product type", Toast.LENGTH_SHORT).show()
            }
    }
}
