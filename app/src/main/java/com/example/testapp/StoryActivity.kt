package com.example.testapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import java.io.ByteArrayOutputStream
import kotlin.math.abs

class StoryActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var storyAdapter: StoryAdapter
    private lateinit var progressBarContainer: LinearLayout
    private val progressBarViews = mutableListOf<View>()
    private var isAnimating = false // Flag pentru a controla starea animației
    private var currentAnimator: ValueAnimator? = null
    private lateinit var gestureDetector: GestureDetector
    private var previousPosition: Int = -1
    private var lastPositionOffsetPixels = 0
    private var isFirstFragmentLaunched: Boolean = false
    private var isDraggingRight = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                try {
                    val diffX = e2.x.minus(e1!!.x)
                    val diffY = e2.y.minus(e1.y)
                    if (abs(diffX) > abs(diffY)) {
                        if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            if (currentAnimator?.isRunning == true) {
                                currentAnimator?.removeAllListeners()
                                currentAnimator?.cancel()
                                currentAnimator = null
                                isAnimating = false
                                if (diffX < 0)
                                    completeProgressBar(previousPosition)
                                else
                                    resetProgressBar(previousPosition)
                            }
                            return true
                        }
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
                return false
            }
        })

        viewPager = findViewById(R.id.viewPager)

        progressBarContainer = findViewById(R.id.progressBarContainer)
        storyAdapter = StoryAdapter(this)

        val fragments = listOf(
            StoryFragment(),
            SecondStoryFragment(),
            FragmentThree(),
            StoryFragment(),
            SecondStoryFragment()
        )
        setupListeners()

        fragments.forEachIndexed { index, _ ->
            addProgressBarSegment(index, fragments.size)
        }
        storyAdapter.setFragments(fragments)
        viewPager.adapter = storyAdapter

        //         Pornește prima animație
//        if (!isFirstFragmentLaunched) {
//            isFirstFragmentLaunched = true
//            if (!isAnimating)
////                viewPager.post {
////                    animateProgressBar(0, 2000L)
////                }
//        }
    }
    private fun setupListeners() {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)

//                if (previousPosition != -1 && position < previousPosition) {
//                    resetProgressBar(previousPosition)
//                }
//
                if (!isAnimating || previousPosition == position) {
                    animateProgressBar(position, 2000L)
                }
                previousPosition = position
            }
        })

        viewPager.getChildAt(0).setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    currentAnimator?.pause()
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    currentAnimator?.resume()
                }
            }
            false
        }


        val shareBtn: Button = findViewById(R.id.share_button)
        val cancelBtn: Button = findViewById(R.id.cancel_button)
        shareBtn.setOnClickListener {
            captureScreenshot()
        }
        cancelBtn.setOnClickListener {
        }
    }

    private fun completeProgressBar(position: Int) {
        val progressBarView = progressBarViews[position]
        progressBarView.layoutParams.width = progressBarContainer.width / progressBarViews.size
        progressBarView.requestLayout()
    }


    private fun resetProgressBar(position: Int) {
        val progressBarView = progressBarViews[position]
        val params = progressBarView.layoutParams as ViewGroup.LayoutParams
        params.width = 0
        progressBarView.layoutParams = params
        progressBarView.requestLayout()
    }

    private fun addProgressBarSegment(index: Int, totalSegments: Int) {
        val segmentContainer = FrameLayout(this)

        val greyBackgroundView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
            setBackgroundColor(Color.parseColor("#667085"))
        }

        val whiteForegroundView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                gravity = Gravity.START
            }
            setBackgroundColor(Color.parseColor("#D0D5DD"))
            pivotX = 0f
        }

        segmentContainer.addView(greyBackgroundView)
        segmentContainer.addView(whiteForegroundView)

        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT).apply {
            weight = 1f / totalSegments
            marginEnd = if (index < totalSegments - 1) 4.dpToPx(this@StoryActivity) else 0 // Spațiu între segmente
        }
        progressBarContainer.addView(segmentContainer, params)
        progressBarViews.add(whiteForegroundView)
    }

    private fun animateProgressBar(position: Int, duration: Long) {
        isAnimating = true
        Log.d("StoryActivity", "Animating progress bar for position: $position")

//        if (isAnimating) {
//            currentAnimator?.apply {
//                removeAllListeners()
//                cancel()
//            }
//            currentAnimator = null
//        }

        val progressBarView = progressBarViews[position]
       currentAnimator = ValueAnimator.ofInt(0, progressBarContainer.width / progressBarViews.size).apply {
           this.duration = duration
           addUpdateListener { animator ->
               val value = animator.animatedValue as Int
               progressBarView.layoutParams.width = value
               progressBarView.requestLayout()
           }

           addListener(object : AnimatorListenerAdapter() {
               override fun onAnimationEnd(animation: Animator) {
                   super.onAnimationEnd(animation)
                   isAnimating = false
                   currentAnimator?.removeAllListeners()
//                   currentAnimator?.cancel()
                   currentAnimator = null
                   if (position < progressBarViews.size - 1) {
                       nextStory()
                   }
               }
           })
           start()
       }
    }

    private fun Int.dpToPx(context: Context): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    //
    fun nextStory() {
        val currentItem = viewPager.currentItem
        if (currentItem < storyAdapter.itemCount - 1) {
            viewPager.currentItem = currentItem + 1
        } else {
            finish() // Închide activitatea dacă toate story-urile au fost vizualizate
        }
    }

    private fun captureScreenshot() {
        val currentFragment = supportFragmentManager.findFragmentByTag("f" + viewPager.currentItem)
        currentFragment?.view?.let { view ->
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)


            // În loc să salvezi bitmap-ul, îl transmitem direct funcției de share
            shareScreenshot(bitmap)
        }
    }

    private fun shareScreenshot(bitmap: Bitmap) {
        val bytes = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(contentResolver, bitmap,
        "Title", null)
        val uri = Uri.parse(path)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
        }

        startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }
//
//
//    private fun onSwipeRight() {
//        stopCurrentAnimation()
//        if (viewPager.currentItem > 0) {
//            viewPager.currentItem = viewPager.currentItem - 1
//        }
//    }
//
//    private fun onSwipeLeft() {
//        stopCurrentAnimation()
//        if (viewPager.currentItem < storyAdapter.itemCount - 1) {
//            viewPager.currentItem = viewPager.currentItem + 1
//        }
//    }

//    private fun stopCurrentAnimation() {
//        currentAnimator?.let {
//            if (it.isRunning) {
//                it.cancel() // Oprește animația
//            }
//        }
//    }



//    private fun addProgressBarSegment(index: Int, totalSegments: Int) {
//        val progressBarView = View(this).apply {
//            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT).apply {
//                weight = 1f / totalSegments
//                marginEnd = if (index < totalSegments - 1) 4.dpToPx(this@StoryActivity) else 0
//            }
//            setBackgroundColor(Color.parseColor("#D0D5DD")) //
////            setFore(Color.parseColor("#667085"))
//        }
//        progressBarContainer.addView(progressBarView)
//        progressBarViews.add(progressBarView)
//    }

}




