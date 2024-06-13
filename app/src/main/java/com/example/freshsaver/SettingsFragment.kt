package com.example.freshsaver

import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import android.widget.Button
import java.util.Locale
import android.content.Intent
import com.google.firebase.auth.FirebaseAuth
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import android.widget.ImageView
import android.net.Uri
import com.google.firebase.auth.UserProfileChangeRequest


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var auth: FirebaseAuth
    private lateinit var emailTextView: TextView
    private lateinit var loginTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var changeProfilePicButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        auth = FirebaseAuth.getInstance()

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        emailTextView = view.findViewById(R.id.textView_email)
        loginTextView = view.findViewById(R.id.textView_login)
        profileImageView = view.findViewById(R.id.profileImageView)
        changeProfilePicButton = view.findViewById(R.id.button_pic)

        val user = auth.currentUser
        user?.let {
            emailTextView.text = it.email
            loginTextView.text = it.displayName
            val photoUrl = it.photoUrl
            if (photoUrl != null) {
                Glide.with(this)
                    .load(photoUrl)
                    .transform(CircleCrop())
                    .into(profileImageView)
            }
            else {
                val defaultPhotoUrl = "https://firebasestorage.googleapis.com/v0/b/fresh-saver.appspot.com/o/profile_pictures%2Fdefault_pic.png?alt=media&token=2095c234-7b1a-475c-8d97-408833af34aa"
                Glide.with(this)
                    .load(defaultPhotoUrl)
                    .transform(CircleCrop())
                    .into(profileImageView)
            }
        }

        changeProfilePicButton.setOnClickListener {
            showProfilePictureDialog()
        }

        val button: Button = view.findViewById(R.id.button_lang)
        button.setOnClickListener {
            showLanguageDialog()
        }
        val colorModeButton: Button = view.findViewById(R.id.button_mode)
        colorModeButton.setOnClickListener {
            showColorModeDialog()
        }

        val logoutButton: Button = view.findViewById(R.id.button_logout)
        logoutButton.setOnClickListener {
            auth.signOut()

            val intent = Intent(activity, AuthActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            activity?.finish()
        }

        return view
    }

    private fun showProfilePictureDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Choose a Profile Picture")

        val dialogLayout = layoutInflater.inflate(R.layout.dialog_profile_pictures, null)
        builder.setView(dialogLayout)

        val defaultPic1 = dialogLayout.findViewById<ImageView>(R.id.defaultPic1)
        val defaultPic2 = dialogLayout.findViewById<ImageView>(R.id.defaultPic2)
        val defaultPic3 = dialogLayout.findViewById<ImageView>(R.id.defaultPic3)
        val defaultPic4 = dialogLayout.findViewById<ImageView>(R.id.defaultPic4)

        val defaultPicsUrls = listOf(
            "https://firebasestorage.googleapis.com/v0/b/fresh-saver.appspot.com/o/profile_pictures%2F3.jpg?alt=media&token=9dde2ddf-c3b6-4740-91bd-07030a9e6614",
            "https://firebasestorage.googleapis.com/v0/b/fresh-saver.appspot.com/o/profile_pictures%2F1.jpg?alt=media&token=a2c041e0-5aed-43c2-9fe0-6738c5574418",
            "https://firebasestorage.googleapis.com/v0/b/fresh-saver.appspot.com/o/profile_pictures%2F5.jpg?alt=media&token=7cea2848-1996-452a-9c39-4582d3199fc2",
            "https://firebasestorage.googleapis.com/v0/b/fresh-saver.appspot.com/o/profile_pictures%2F10.jpg?alt=media&token=91f39595-4d7d-4c79-8a58-f2440cb839c7",
        )

        val imageViews = listOf(defaultPic1, defaultPic2, defaultPic3, defaultPic4)

        for (i in imageViews.indices) {
            Glide.with(this)
                .load(defaultPicsUrls[i])
                .transform(CircleCrop())
                .into(imageViews[i])

            imageViews[i].setOnClickListener {
                updateProfilePicture(defaultPicsUrls[i])
            }
        }

        builder.show()
    }

    private fun updateProfilePicture(url: String) {
        val user = auth.currentUser

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setPhotoUri(Uri.parse(url))
            .build()

        user?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Glide.with(this)
                    .load(url)
                    .transform(CircleCrop())
                    .into(profileImageView)
            }
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Russian")                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
        val builder = AlertDialog.Builder(requireContext())                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
        builder.setTitle("Choose a language")                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
            .setItems(languages) { dialog, which ->                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
                when (which) {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
                    0 -> setLocale("en")                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
                    1 -> setLocale("ru")                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
                }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
            }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
            .create()                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          //                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    //
            .show()                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
    }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //

    private fun setLocale(languageCode: String) {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
        val locale = Locale(languageCode)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
        Locale.setDefault(locale)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
        val config = Configuration()                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
        config.setLocale(locale)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
        requireContext().resources.updateConfiguration(config, requireContext().resources.displayMetrics)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //

        activity?.recreate()                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
    }

    private fun showColorModeDialog() {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
        /*                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  si*/
        val colorModes = arrayOf("Light", "Dark")
        val builder = AlertDialog.Builder(requireContext())                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
        builder.setTitle("Choose a color mode")                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
            .setItems(colorModes) { dialog, which ->                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
                when (which) {                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
                    0 -> setColorMode(AppCompatDelegate.MODE_NIGHT_NO)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
                    1 -> setColorMode(AppCompatDelegate.MODE_NIGHT_YES)                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
                }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
            }                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                       //
            /*                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  deus*/
            .create()
            .show()
    }

    private fun setColorMode(mode: Int) {
        AppCompatDelegate.setDefaultNightMode(mode)
        activity?.recreate()
    }
    /*                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  nobiscum*/
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SettingsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SettingsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}