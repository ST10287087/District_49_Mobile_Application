package vcmsa.projects.district49_android

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class DonateOne : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.donate1)

        val imgDonateBg = findViewById<ImageView>(R.id.img_donate_bg)

        // Load large image with downsampling
        val options = BitmapFactory.Options().apply {
            inSampleSize = 4 // reduce image size to 1/4 in each dimension
        }
        val bitmap = BitmapFactory.decodeResource(resources, R.drawable.donateone_background_one_writing, options)
        imgDonateBg.setImageBitmap(bitmap)
    }
}
