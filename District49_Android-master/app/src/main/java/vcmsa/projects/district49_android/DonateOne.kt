package vcmsa.projects.district49_android

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*

class DonateOne : AppCompatActivity() {

    private lateinit var adminRepository: AdminRepository
    private lateinit var chart: PieChart
    private lateinit var tvDonutPercent: TextView
    private lateinit var tvRaisedPrefix: TextView
    private lateinit var tvGoalValue: TextView
    private lateinit var progressGoal: android.widget.ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.donate1)

        adminRepository = AdminRepository()

        initializeViews()
        loadDonationData()
        wireNavBar()


        findViewById<MaterialButton>(R.id.btn_donate_now)?.setOnClickListener {
            startActivity(Intent(this, DonateTwo::class.java))
        }

        findViewById<MaterialButton>(R.id.btn_donations_we_accept)?.setOnClickListener {
            startActivity(Intent(this, DonationsWeAccept::class.java))
        }
    }

    private fun wireNavBar() {
        // Back button - goes back to previous screen
        findViewById<ImageButton?>(R.id.nav_btn_arrow_back)?.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }


        findViewById<ImageButton?>(R.id.nav_btn_plus_circle)?.setOnClickListener {
            runCatching {
                startActivity(Intent(this, MenuActivity::class.java))
            }.onFailure {
                Toast.makeText(this, "Menu not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Notifications - goes to NotificationSettingsActivity like homepage
        findViewById<ImageButton?>(R.id.nav_btn_notifications)?.setOnClickListener {
            runCatching {
                startActivity(Intent(this, NotificationSettingsActivity::class.java))
            }.onFailure {
                Toast.makeText(this, "Notifications not available", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<ImageButton?>(R.id.nav_btn_profile)?.setOnClickListener {
            runCatching {
                startActivity(Intent(this, ProfileActivity::class.java))
            }.onFailure {
                Toast.makeText(this, "Profile not available", Toast.LENGTH_SHORT).show()
            }
        }

        // Home - goes back to Homepage like homepage
        findViewById<ImageButton?>(R.id.nav_btn_home)?.setOnClickListener {
            runCatching {
                // Clear back stack and go to homepage
                val intent = Intent(this, Homepage::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }.onFailure {
                Toast.makeText(this, "Home not available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeViews() {
        val imgDonateBg = findViewById<ImageView>(R.id.img_donate_bg)
        val options = BitmapFactory.Options().apply { inSampleSize = 4 }
        val bitmap = BitmapFactory.decodeResource(
            resources,
            R.drawable.donateone_background_one_writing,
            options
        )
        imgDonateBg.setImageBitmap(bitmap)

        chart = findViewById(R.id.donut_chart)
        tvDonutPercent = findViewById(R.id.tv_donut_percent)
        tvRaisedPrefix = findViewById(R.id.tv_raised_prefix)
        tvGoalValue = findViewById(R.id.tv_goal_value)
        progressGoal = findViewById(R.id.progress_goal)
    }

    private fun loadDonationData() {
        lifecycleScope.launch {
            try {
                val donationResult = adminRepository.getCurrentDonationGoal()
                if (donationResult.isSuccess) {
                    val donationGoal = donationResult.getOrNull()!!
                    updateDonationDisplay(donationGoal)
                } else {
                    updateDonationDisplay(
                        vcmsa.projects.district49_android.models.DonationGoal(
                            id = "default",
                            goalAmount = 120000,
                            raisedAmount = 50000
                        )
                    )
                }
            } catch (_: Exception) {
                updateDonationDisplay(
                    vcmsa.projects.district49_android.models.DonationGoal(
                        id = "default",
                        goalAmount = 120000,
                        raisedAmount = 50000
                    )
                )
            }
        }
    }

    private fun updateDonationDisplay(donationGoal: vcmsa.projects.district49_android.models.DonationGoal) {
        val percentage = if (donationGoal.goalAmount > 0) {
            ((donationGoal.raisedAmount.toFloat() / donationGoal.goalAmount.toFloat()) * 100f).coerceIn(0f, 100f)
        } else {
            0f
        }

        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "ZA")).apply {
            maximumFractionDigits = 0
        }

        val raisedFormatted = formatter.format(donationGoal.raisedAmount).replace("ZAR", "R")
        val goalFormatted = formatter.format(donationGoal.goalAmount).replace("ZAR", "R")

        setupDonutChart(chart, percentage)

        tvDonutPercent.text = "${percentage.toInt()}%"
        tvRaisedPrefix.text = raisedFormatted  // Just the amount (no "OF" text)
        tvGoalValue.text = goalFormatted
        progressGoal.progress = percentage.toInt()
    }

    private fun setupDonutChart(chart: PieChart, percent: Float) {
        val safePercent = percent.coerceIn(0f, 100f)
        val entries = listOf(
            PieEntry(safePercent, ""),
            PieEntry(100f - safePercent, "")
        )

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.parseColor("#FEAB43"), Color.parseColor("#E6E6E6"))
            setDrawValues(false)
            sliceSpace = 2f
        }

        chart.apply {
            data = PieData(dataSet)
            setUsePercentValues(false)
            setDrawEntryLabels(false)
            legend.isEnabled = false
            description = Description().apply { text = "" }
            isDrawHoleEnabled = true
            holeRadius = 70f
            transparentCircleRadius = 75f
            setHoleColor(Color.TRANSPARENT)
            setDrawCenterText(false)
            setTouchEnabled(false)
            invalidate()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDonationData()
    }
}