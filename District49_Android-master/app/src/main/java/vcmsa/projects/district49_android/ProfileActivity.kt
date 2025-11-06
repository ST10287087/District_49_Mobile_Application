package vcmsa.projects.district49_android

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import vcmsa.projects.district49_android.models.User
import vcmsa.projects.district49_android.utils.AuthManager
import java.io.ByteArrayOutputStream
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var authManager: AuthManager
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var firebaseAuth: FirebaseAuth

    // UI Components
    private lateinit var profilePicture: ImageView
    private lateinit var editTextName: EditText
    private lateinit var editTextSurname: EditText
    private lateinit var editTextEmail: EditText
    private lateinit var editTextPhone: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var btnEditProfile: Button
    private lateinit var btnSaveProfile: Button
    private lateinit var textResetPassword: TextView

    private var currentUser: User? = null
    private var isEditMode = false
    private var selectedImageUri: Uri? = null

    // Image picker launcher for gallery
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                // Load with circular crop
                Glide.with(this)
                    .load(uri)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .into(profilePicture)
            }
        }
    }

    // Camera launcher
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as? Bitmap
            imageBitmap?.let { bitmap ->
                selectedImageUri = getImageUri(bitmap)
                // Load with circular crop
                Glide.with(this)
                    .load(bitmap)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .into(profilePicture)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_page)

        initializeFirebase()
        initializeViews()
        setupClickListeners()
        loadUserProfile()
        wireOptionalNavBar()
    }

    private fun initializeFirebase() {
        authManager = AuthManager.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()
    }

    private fun initializeViews() {
        profilePicture = findViewById(R.id.profilePicture)
        editTextName = findViewById(R.id.editTextName)
        editTextSurname = findViewById(R.id.editTextSurname)
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPhone = findViewById(R.id.editTextPhone)
        editTextPassword = findViewById(R.id.editTextPassword)
        btnEditProfile = findViewById(R.id.btnEditProfile)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)
        textResetPassword = findViewById(R.id.textResetPassword)

        // Setup reset password link
        setupResetPasswordLink()
    }

    private fun setupResetPasswordLink() {
        val content = SpannableString("Reset Password")
        content.setSpan(UnderlineSpan(), 0, content.length, 0)
        textResetPassword.text = content
        textResetPassword.setTextColor(resources.getColor(android.R.color.holo_blue_dark, theme))

        textResetPassword.setOnClickListener {
            sendPasswordResetEmail()
        }
    }

    private fun sendPasswordResetEmail() {
        val user = firebaseAuth.currentUser
        user?.email?.let { email ->
            firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Password reset email sent. Please check your inbox.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Failed to send reset email: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } ?: run {
            Toast.makeText(this, "No user email found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupClickListeners() {
        // Profile picture - show image selection dialog
        profilePicture.setOnClickListener {
            if (isEditMode) {
                showImageSelectionDialog()
            } else {
                Toast.makeText(this, "Enable edit mode to change profile picture", Toast.LENGTH_SHORT).show()
            }
        }

        // Edit Profile button
        btnEditProfile.setOnClickListener {
            toggleEditMode()
        }

        // Save Profile button
        btnSaveProfile.setOnClickListener {
            saveProfileChanges()
        }

        // Reset Password link
        textResetPassword.setOnClickListener {
            sendPasswordResetEmail()
        }
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val userResult = authManager.getCurrentUserData()

                if (userResult.isSuccess) {
                    currentUser = userResult.getOrNull()!!

                    runOnUiThread {
                        populateUserFields()
                        loadProfilePicture()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@ProfileActivity, "Failed to load profile", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun populateUserFields() {
        currentUser?.let { user ->
            editTextName.setText(user.name)
            editTextSurname.setText(user.surname)
            editTextEmail.setText(user.email)
            editTextPhone.setText(user.phone)
            editTextPassword.setText("••••••••") // Masked password
        }
    }

    private fun loadProfilePicture() {
        currentUser?.let { user ->
            if (user.profilePictureUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(user.profilePictureUrl)
                    .apply(RequestOptions.bitmapTransform(CircleCrop()))
                    .placeholder(R.drawable.generic_avatar)
                    .error(R.drawable.generic_avatar)
                    .into(profilePicture)
            } else {
                profilePicture.setImageResource(R.drawable.generic_avatar)
            }
        }
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode

        editTextName.isEnabled = isEditMode
        editTextSurname.isEnabled = isEditMode
        editTextPhone.isEnabled = isEditMode

        // Email and Password remain disabled (require special handling)
        editTextEmail.isEnabled = false
        editTextPassword.isEnabled = false

        if (isEditMode) {
            btnEditProfile.visibility = View.GONE
            btnSaveProfile.visibility = View.VISIBLE
            textResetPassword.visibility = View.VISIBLE
            Toast.makeText(this, "Edit mode enabled. Click profile picture to change it.", Toast.LENGTH_SHORT).show()
        } else {
            btnEditProfile.visibility = View.VISIBLE
            btnSaveProfile.visibility = View.GONE
            textResetPassword.visibility = View.GONE
            // Reload original data if canceled
            populateUserFields()
            selectedImageUri = null
        }
    }

    private fun showImageSelectionDialog() {
        // Check if user has a profile picture
        val hasProfilePicture = currentUser?.profilePictureUrl?.isNotEmpty() == true

        val options = if (hasProfilePicture) {
            arrayOf("Take Photo", "Choose from Gallery", "Remove Photo", "Cancel")
        } else {
            arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        }

        AlertDialog.Builder(this)
            .setTitle("Select Profile Picture")
            .setItems(options) { dialog, which ->
                if (hasProfilePicture) {
                    when (which) {
                        0 -> openCamera()
                        1 -> openGallery()
                        2 -> removeProfilePicture()
                        3 -> dialog.dismiss()
                    }
                } else {
                    when (which) {
                        0 -> openCamera()
                        1 -> openGallery()
                        2 -> dialog.dismiss()
                    }
                }
            }
            .show()
    }

    private fun removeProfilePicture() {
        AlertDialog.Builder(this)
            .setTitle("Remove Profile Picture")
            .setMessage("Are you sure you want to remove your profile picture?")
            .setPositiveButton("Remove") { _, _ ->
                // Set to default avatar
                profilePicture.setImageResource(R.drawable.generic_avatar)
                selectedImageUri = null

                // Update Firestore to remove profile picture URL
                currentUser?.let { user ->
                    firestore.collection("users")
                        .document(user.uid)
                        .update("profilePictureUrl", "")
                        .addOnSuccessListener {
                            // Delete from Firebase Storage if exists
                            if (user.profilePictureUrl.isNotEmpty()) {
                                try {
                                    storage.getReferenceFromUrl(user.profilePictureUrl).delete()
                                } catch (e: Exception) {
                                    // Ignore if file doesn't exist
                                }
                            }

                            Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show()
                            loadUserProfile() // Reload to update UI
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to remove: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun saveProfileChanges() {
        val newName = editTextName.text.toString().trim()
        val newSurname = editTextSurname.text.toString().trim()
        val newPhone = editTextPhone.text.toString().trim()

        if (newName.isEmpty() || newSurname.isEmpty()) {
            Toast.makeText(this, "Name and Surname are required", Toast.LENGTH_SHORT).show()
            return
        }

        // Disable save button during upload
        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = "Saving..."

        lifecycleScope.launch {
            try {
                var profilePictureUrl = currentUser?.profilePictureUrl ?: ""

                // Upload profile picture if changed
                if (selectedImageUri != null) {
                    profilePictureUrl = uploadProfilePicture(selectedImageUri!!)
                }

                // Update user data
                val updates = hashMapOf<String, Any>(
                    "name" to newName,
                    "surname" to newSurname,
                    "phone" to newPhone,
                    "profilePictureUrl" to profilePictureUrl
                )

                currentUser?.let { user ->
                    firestore.collection("users")
                        .document(user.uid)
                        .update(updates)
                        .addOnSuccessListener {
                            runOnUiThread {
                                Toast.makeText(this@ProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                                toggleEditMode()
                                loadUserProfile() // Reload updated data
                            }
                        }
                        .addOnFailureListener { e ->
                            runOnUiThread {
                                Toast.makeText(this@ProfileActivity, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                btnSaveProfile.isEnabled = true
                                btnSaveProfile.text = "Save"
                            }
                        }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@ProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnSaveProfile.isEnabled = true
                    btnSaveProfile.text = "Save"
                }
            }
        }
    }

    private suspend fun uploadProfilePicture(imageUri: Uri): String {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val imageBytes = baos.toByteArray()

            val imageId = UUID.randomUUID().toString()
            val imageRef = storage.reference.child("profile_pictures/${currentUser?.uid}_$imageId.jpg")

            imageRef.putBytes(imageBytes).await()
            val downloadUrl = imageRef.downloadUrl.await()

            downloadUrl.toString()
        } catch (e: Exception) {
            throw Exception("Failed to upload profile picture: ${e.message}")
        }
    }

    private fun getImageUri(bitmap: Bitmap): Uri {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap, "Profile_${System.currentTimeMillis()}", null)
        return Uri.parse(path)
    }

    private fun wireOptionalNavBar() {
        // Back
        findViewById<ImageButton?>(R.id.nav_btn_arrow_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Menu
        findViewById<ImageButton?>(R.id.nav_btn_plus_circle)?.setOnClickListener {
            val intent = Intent(this, MenuActivity::class.java)
            startActivity(intent)
        }

        // Notifications
        findViewById<ImageButton?>(R.id.nav_btn_notifications)?.setOnClickListener {
            val intent = Intent(this, NotificationSettingsActivity::class.java)
            startActivity(intent)
        }
        // Profile
        findViewById<ImageButton?>(R.id.nav_btn_profile)?.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }
        // Home
        findViewById<ImageButton?>(R.id.nav_btn_home)?.setOnClickListener {
            val intent = Intent(this, Homepage::class.java)
            startActivity(intent)
        }
    }
}