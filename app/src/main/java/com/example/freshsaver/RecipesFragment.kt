package com.example.freshsaver

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class RecipesFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var recipeAdapter: RecipeAdapter
    private val recipeList = mutableListOf<Recipe>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_recipes, container, false)
        recyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recipeAdapter = RecipeAdapter(recipeList) { recipe ->
            val intent = Intent(context, RecipeDetailActivity::class.java).apply {
                putExtra("recipeTitle", recipe.title)
                putExtra("recipeImageUrl", recipe.imageUrl)
                putExtra("recipeDescription", recipe.text)
                putStringArrayListExtra("productTypeIds", ArrayList(recipe.productTypeIds))
            }
            startActivity(intent)
        }
        recyclerView.adapter = recipeAdapter

        fetchRecipes()

        return view
    }

    private fun fetchRecipes() {
        DB.getInstance().getRecipesOrdered()
            .addOnSuccessListener { result ->
                recipeList.clear()
                recipeList.addAll(result)
                recipeAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }

    private class RecipeAdapter(
        private val recipes: List<Recipe>,
        private val onClick: (Recipe) -> Unit
    ) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

        class RecipeViewHolder(view: View, val onClick: (Recipe) -> Unit) : RecyclerView.ViewHolder(view) {
            private val recipeTitle: TextView = view.findViewById(R.id.recipe_title)
            private val recipeImage: ImageView = view.findViewById(R.id.recipe_image)
            private var currentRecipe: Recipe? = null

            init {
                view.setOnClickListener {
                    currentRecipe?.let { onClick(it) }
                }
            }

            fun bind(recipe: Recipe) {
                currentRecipe = recipe
                recipeTitle.text = recipe.title
                if (recipe.imageUrl != null) {
                    Picasso.get().load(recipe.imageUrl).into(recipeImage)
                } else {
                    recipeImage.setImageResource(R.drawable.ic_placeholder)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_recipe, parent, false)
            return RecipeViewHolder(view, onClick)
        }

        override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
            holder.bind(recipes[position])
        }

        override fun getItemCount() = recipes.size
    }
}
