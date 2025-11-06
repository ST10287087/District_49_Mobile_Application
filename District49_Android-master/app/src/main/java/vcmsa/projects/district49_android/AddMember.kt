package vcmsa.projects.district49_android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import vcmsa.projects.district49_android.databinding.AddMemberBinding

class AddMember : AppCompatActivity() {
    private lateinit var binding: AddMemberBinding
    private var pickedUri: Uri? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            pickedUri = uri
            binding.ivPreview.setImageURI(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddMemberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPick.setOnClickListener { pickImage.launch("image/*") }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etName.error = "Name required"
                return@setOnClickListener
            }
            val data = Intent()
                .putExtra("name", name)
                .putExtra("photoUri", pickedUri?.toString())
            setResult(Activity.RESULT_OK, data)
            finish()
        }

        wireOptionalNavBar()
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
