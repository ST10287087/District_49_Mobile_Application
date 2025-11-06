package vcmsa.projects.district49_android

import android.app.Activity
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import vcmsa.projects.district49_android.utils.AuthManager
import vcmsa.projects.district49_android.utils.RoleBasedUIHelper
import java.io.ByteArrayOutputStream
import java.util.*

class GalleryActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var authManager: AuthManager
    private lateinit var prefs: SharedPreferences

    // UI Components
    private lateinit var insertImageButton: ImageButton
    private lateinit var adminAddButton: Button
    private lateinit var adminDeleteButton: Button
    private lateinit var leftArrowButton: ImageButton
    private lateinit var rightArrowButton: ImageButton
    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView

    // Gallery data
    private var galleryImages = mutableListOf<GalleryImage>()
    private var currentImageIndex = 0
    private var selectedImageUri: Uri? = null
    private var isImageSelected = false

    // SharedPreferences keys
    private companion object {
        const val PREFS_NAME = "gallery_prefs"
        const val KEY_INSTRUCTIONS_SHOWN = "instructions_shown"
    }

    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                isImageSelected = true
                // Show preview in main image frame
                insertImageButton.setImageURI(uri)
                insertImageButton.scaleType = ImageView.ScaleType.CENTER_CROP
                Toast.makeText(this, "Image selected. Click 'INSERT IMAGE' to add to gallery.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.gallery_page)

        initializeFirebase()
        initializeViews()
        setupRoleBasedUI()
        setupClickListeners()
        loadGalleryImages()

        // Check if we should show instructions (only for admins)
        checkAndShowInstructions()
    }

    private fun initializeFirebase() {
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        authManager = AuthManager.getInstance()
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
    }

    private fun initializeViews() {
        insertImageButton = findViewById(R.id.InsertimageButton)
        adminAddButton = findViewById(R.id.AdminAddbutton)
        adminDeleteButton = findViewById(R.id.AdminDeletebutton)
        leftArrowButton = findViewById(R.id.LeftArrowButton)
        rightArrowButton = findViewById(R.id.RightArrowButton)
        imageView1 = findViewById(R.id.image_view_1)
        imageView2 = findViewById(R.id.image_view_2)
    }

    private fun setupRoleBasedUI() {
        // Hide admin buttons for regular users
        val adminOnlyViews = listOf(adminAddButton, adminDeleteButton)

        RoleBasedUIHelper.setupAdminOnlyViews(this, adminOnlyViews) { isAdmin ->
            if (isAdmin) {
                Toast.makeText(this, "Admin mode: You can add/delete images", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkAndShowInstructions() {
        // Check if user is admin and hasn't seen instructions before
        lifecycleScope.launch {
            try {
                val userResult = authManager.getCurrentUserData()
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()
                    if (user?.isAdmin() == true) {
                        val instructionsShown = prefs.getBoolean(KEY_INSTRUCTIONS_SHOWN, false)
                        if (!instructionsShown) {
                            showInstructionsDialog()
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently fail - instructions are not critical
            }
        }
    }

    private fun showInstructionsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Gallery Management Instructions")
            .setMessage(
                " How to Manage Gallery Images:\n\n" +
                        "Adding Images:\n" +
                        "1. Click 'Add' button\n" +
                        "2. Select an image from your gallery\n" +
                        "3. Click the main image frame to upload\n" +
                        "4. Image will be added to the gallery\n\n" +
                        "Deleting Images:\n" +
                        "1. Navigate to the image using arrows\n" +
                        "2. Click 'Delete' button\n" +
                        "3. Confirm deletion\n\n" +
                        "Viewing Images:\n" +
                        "• Use arrow buttons to navigate\n" +
                        "• Click any image to view fullscreen\n" +
                        "• Thumbnails show next images in gallery"
            )
            .setPositiveButton("Got it!") { dialog, which ->
                // Mark instructions as shown
                prefs.edit().putBoolean(KEY_INSTRUCTIONS_SHOWN, true).apply()
            }
            .setCancelable(false)
            .show()
    }

    private fun addClickAnimation(button: ImageButton, onClick: () -> Unit = {}) {
        button.setOnClickListener {
            button.animate()
                .scaleX(0.93f)
                .scaleY(0.93f)
                .setDuration(70)
                .withEndAction {
                    button.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(70)
                        .withEndAction {
                            onClick()
                        }
                        .start()
                }
                .start()
        }
    }

    private fun setupClickListeners() {
        // Admin Add button - opens image picker
        adminAddButton.setOnClickListener {
            openImagePicker()
        }

        // Admin Delete button - deletes current image
        adminDeleteButton.setOnClickListener {
            deleteCurrentImage()
        }

        // Insert Image button - saves selected image to gallery
        insertImageButton.setOnClickListener {
            if (isImageSelected && selectedImageUri != null) {
                uploadImageToGallery()
            } else {
                // If no image selected, treat as gallery navigation
                if (galleryImages.isNotEmpty()) {
                    showImageFullscreen(galleryImages[currentImageIndex])
                }
            }
        }

        // Arrow navigation with animation
        addClickAnimation(leftArrowButton) {
            navigateGallery(-1)
        }

        addClickAnimation(rightArrowButton) {
            navigateGallery(1)
        }

        imageView1.setOnClickListener {
            if (currentImageIndex + 1 < galleryImages.size) {
                showImageFullscreen(galleryImages[currentImageIndex + 1])
            }
        }

        imageView2.setOnClickListener {
            if (currentImageIndex + 2 < galleryImages.size) {
                showImageFullscreen(galleryImages[currentImageIndex + 2])
            }
        }

        // Setup navbar listeners (same as your Homepage)
        setupNavbarListeners()
    }

    private fun setupNavbarListeners() {
        // Back button
        findViewById<ImageButton?>(R.id.nav_btn_arrow_back)?.setOnClickListener {
            finish()
        }

        // Notifications button
        findViewById<ImageButton?>(R.id.nav_btn_notifications)?.setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }

        // Profile button
        findViewById<ImageButton?>(R.id.nav_btn_profile)?.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Home button
        findViewById<ImageButton?>(R.id.nav_btn_home)?.setOnClickListener {
            startActivity(Intent(this, Homepage::class.java))
        }

        // Plus button
        findViewById<ImageButton?>(R.id.nav_btn_plus_circle)?.setOnClickListener {
            startActivity(Intent(this, MenuActivity::class.java))
        }
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun uploadImageToGallery() {
        selectedImageUri?.let { uri ->
            lifecycleScope.launch {
                try {
                    adminAddButton.isEnabled = false
                    adminAddButton.text = "Uploading..."

                    // Convert image to bytes
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    val baos = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                    val imageBytes = baos.toByteArray()

                    // Upload to Firebase Storage
                    val imageId = UUID.randomUUID().toString()
                    val imageRef = storage.reference.child("gallery_images/$imageId.jpg")

                    val uploadTask = imageRef.putBytes(imageBytes)
                    uploadTask.addOnSuccessListener {
                        // Get download URL
                        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            // Save to Firestore
                            val galleryImage = GalleryImage(
                                id = imageId,
                                imageUrl = downloadUri.toString(),
                                uploadedAt = System.currentTimeMillis(),
                                uploadedBy = authManager.getCurrentFirebaseUser()?.uid ?: ""
                            )

                            firestore.collection("gallery")
                                .document(imageId)
                                .set(galleryImage)
                                .addOnSuccessListener {
                                    runOnUiThread {
                                        Toast.makeText(this@GalleryActivity, "Image added to gallery!", Toast.LENGTH_SHORT).show()
                                        resetImageSelection()
                                        loadGalleryImages() // Refresh gallery
                                    }
                                }
                                .addOnFailureListener { e ->
                                    runOnUiThread {
                                        Toast.makeText(this@GalleryActivity, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
                                        resetUploadButton()
                                    }
                                }
                        }
                    }.addOnFailureListener { e ->
                        runOnUiThread {
                            Toast.makeText(this@GalleryActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            resetUploadButton()
                        }
                    }

                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this@GalleryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        resetUploadButton()
                    }
                }
            }
        }
    }

    private fun deleteCurrentImage() {
        if (galleryImages.isEmpty()) {
            Toast.makeText(this, "No image to delete", Toast.LENGTH_SHORT).show()
            return
        }

        val currentImage = galleryImages[currentImageIndex]

        // Confirm deletion
        android.app.AlertDialog.Builder(this)
            .setTitle("Delete Image")
            .setMessage("Are you sure you want to delete this image?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        // Delete from Storage
                        storage.getReferenceFromUrl(currentImage.imageUrl).delete()

                        // Delete from Firestore
                        firestore.collection("gallery")
                            .document(currentImage.id)
                            .delete()
                            .addOnSuccessListener {
                                Toast.makeText(this@GalleryActivity, "Image deleted", Toast.LENGTH_SHORT).show()
                                loadGalleryImages() // Refresh gallery
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@GalleryActivity, "Delete failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } catch (e: Exception) {
                        Toast.makeText(this@GalleryActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadGalleryImages() {
        firestore.collection("gallery")
            .orderBy("uploadedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                galleryImages.clear()
                for (document in documents) {
                    val galleryImage = document.toObject(GalleryImage::class.java)
                    galleryImages.add(galleryImage)
                }
                updateGalleryDisplay()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load gallery: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateGallery(direction: Int) {
        if (galleryImages.isEmpty()) return

        currentImageIndex += direction
        if (currentImageIndex < 0) currentImageIndex = galleryImages.size - 1
        if (currentImageIndex >= galleryImages.size) currentImageIndex = 0

        updateGalleryDisplay()
    }

    private fun updateGalleryDisplay() {
        if (galleryImages.isEmpty()) {
            imageView1.visibility = View.GONE
            imageView2.visibility = View.GONE
            if (!isImageSelected) {
                insertImageButton.setImageResource(R.drawable.selectimage_button)
            }
            return
        }

        if (!isImageSelected) {
            loadImageIntoView(galleryImages[currentImageIndex].imageUrl, insertImageButton)
        }

        // Update thumbnail 1 (next image)
        if (currentImageIndex + 1 < galleryImages.size) {
            imageView1.visibility = View.VISIBLE
            loadImageIntoView(galleryImages[currentImageIndex + 1].imageUrl, imageView1)
        } else {
            imageView1.visibility = View.GONE
        }

        // Update thumbnail 2 (image after next)
        if (currentImageIndex + 2 < galleryImages.size) {
            imageView2.visibility = View.VISIBLE
            loadImageIntoView(galleryImages[currentImageIndex + 2].imageUrl, imageView2)
        } else {
            imageView2.visibility = View.GONE
        }
    }

    private fun loadImageIntoView(imageUrl: String, imageView: ImageView) {
        Glide.with(this)
            .load(imageUrl)
            .centerCrop()
            .placeholder(R.drawable.image_view_gallery)
            .error(R.drawable.image_view_gallery)
            .into(imageView)
    }

    private fun showImageFullscreen(galleryImage: GalleryImage) {
        val intent = Intent(this, FullscreenImageActivity::class.java)
        intent.putExtra("imageUrl", galleryImage.imageUrl)
        startActivity(intent)
    }

    private fun resetImageSelection() {
        selectedImageUri = null
        isImageSelected = false
        insertImageButton.setImageResource(R.drawable.selectimage_button)
        insertImageButton.scaleType = ImageView.ScaleType.FIT_CENTER
        resetUploadButton()
    }

    private fun resetUploadButton() {
        adminAddButton.isEnabled = true
        adminAddButton.text = "Add"
    }
}