package vcmsa.projects.district49_android

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class FullscreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //fullscreen layout
        setContentView(R.layout.activity_fullscreen_image)

        val imageUrl = intent.getStringExtra("imageUrl")
        val imageView = findViewById<ImageView>(R.id.fullscreen_image_view)

        imageUrl?.let {
            // Load the image (same method as in gallery)
            loadImageIntoView(it, imageView)
        }

        // Click to close
        imageView.setOnClickListener {
            finish()
        }
    }

    private fun loadImageIntoView(imageUrl: String, imageView: ImageView) {
        Glide.with(this)
            .load(imageUrl)
            .fitCenter()
            .placeholder(R.drawable.image_view_gallery)
            .error(R.drawable.image_view_gallery)
            .into(imageView)
    }
}