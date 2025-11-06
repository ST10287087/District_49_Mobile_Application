package vcmsa.projects.district49_android

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SuccessStoriesActivity : AppCompatActivity() {

    private lateinit var storyBox: LinearLayout
    private lateinit var storyTitle: TextView
    private lateinit var storyBody: TextView

    private var startY = 0f
    private var isExpanded = false
    private val collapsedHeight = 400
    private val expandedHeight = 1600 // adjust for your screen height

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.success_stories)

        storyBox = findViewById(R.id.story_box)
        storyTitle = findViewById(R.id.story_title)
        storyBody = findViewById(R.id.story_body)

        val stories = listOf(
            Story(
                "Anonymous",
                "Good day everyone. I am 31 years old and currently living in Esikhawini township. " +
                        "I was born there but raised in Mtwalume, at my grandmother's house.\n\n" +
                        "Due to personal problems, I was placed at District 49 Children’s Home.\n\n" +
                        "I was still very young, in grade six, when I was taken there. I remember being scared and not knowing what to expect.\n\n" +
                        "But when I arrived, it truly felt like a home. There were about 46 children, along with house parents and staff. I adjusted quickly, made friends, and felt safe.\n\n" +
                        "When I was in grade 11, my social worker helped me reconnect with my real family, and I was reunited with my mother’s side.\n\n" +
                        "Life after that has been a journey. I finished matric, and today I work as a receiving clerk at Shoprite.\n\n" +
                        "District 49 raised me with unconditional love and care, and for that, I will always be thankful."
            ),
            Story(
                "Boniswa",
                "Greetings, everybody. My name is Boniswa. I'm 31 years old, originally from Port Shepstone, " +
                        "but currently I am based in Mtubatuba where I work as a qualified social worker.\n\n" +
                        "I was removed from the care of my aunt and placed in District 49. When a child is removed from their home it’s always for a reason. " +
                        "At the time, I was in grade 10. I completed my matric while living at District 49 and attending high school in Mahlongwa.\n\n" +
                        "Being in District 49 taught me independence and resilience. I realized that you have to work for everything you want in life.\n\n" +
                        "After completing my matric, I was 18 and had to leave the system. Fortunately, District 49 offered me an opportunity under the independent living program. " +
                        "They employed me first as a receptionist, and later as a child care worker, where I looked after children.\n\n" +
                        "I have learned so much at District 49, and I am forever grateful for the guidance, care, and opportunities that shaped me into the person I am today."
            )
        )

        val story = stories[0]
        storyTitle.text = story.name
        storyBody.text = story.text

        // Add pull-up animation
        storyBox.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startY = event.rawY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val endY = event.rawY
                    if (startY - endY > 100) expandStoryBox() // Pull up
                    else if (endY - startY > 100) collapseStoryBox() // Pull down
                    true
                }
                else -> false
            }
        }
    }

    private fun expandStoryBox() {
        if (isExpanded) return
        animateBoxHeight(collapsedHeight, expandedHeight)
        isExpanded = true
    }

    private fun collapseStoryBox() {
        if (!isExpanded) return
        animateBoxHeight(expandedHeight, collapsedHeight)
        isExpanded = false
    }

    private fun animateBoxHeight(from: Int, to: Int) {
        val animator = ValueAnimator.ofInt(from, to)
        animator.addUpdateListener {
            val value = it.animatedValue as Int
            val params = storyBox.layoutParams
            params.height = value
            storyBox.layoutParams = params
        }
        animator.duration = 400
        animator.start()
    }

    data class Story(val name: String, val text: String)
}
