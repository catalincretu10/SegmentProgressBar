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
    private var isAnimating = false
    private var currentAnimator: ValueAnimator? = null
    private lateinit var gestureDetector: GestureDetector
    private var currentPosition: Int = -1
    private var isFirstFragmentLaunched: Boolean = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_story)
        setupViews()
        setupListeners()


        //         Pornește prima animație
//        if (!isFirstFragmentLaunched) {
//            isFirstFragmentLaunched = true
//            if (!isAnimating)
//                viewPager.post {
//                    animateProgressBar(0, 2000L)
//                }
//        }
        viewPager.post {
            animateProgressBar(0, 2000L)
        }
    }

    private fun setupViews() {
        viewPager = findViewById(R.id.viewPager)
        progressBarContainer = findViewById(R.id.progressBarContainer)
        storyAdapter = StoryAdapter(this)
        setupGestureDetector()

        val fragments = listOf(
            StoryFragment(),
            SecondStoryFragment(),
            FragmentThree(),
            StoryFragment(),
            SecondStoryFragment()
        )
        fragments.forEachIndexed { index, _ ->
            addProgressBarSegment(index, fragments.size)
        }
        storyAdapter.setFragments(fragments)
        viewPager.adapter = storyAdapter
    }

    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            private val SWIPE_THRESHOLD = 100
            private val SWIPE_VELOCITY_THRESHOLD = 100

            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                try {
                    val diffX = e2.x.minus(e1!!.x)
                    val diffY = e2.y.minus(e1.y)
                    if (abs(diffX) > abs(diffY)) {
                        if (abs(diffX) > SWIPE_THRESHOLD && abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                            cancelAnimation()
                            if (diffX < 0)
                                completeProgressBar(currentPosition)
                            else
                                resetProgressBar(currentPosition)
                            return true
                        }
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
                return false
            }

            override fun onSingleTapConfirmed(motionEvent: MotionEvent): Boolean {
                val screenWidth = resources.displayMetrics.widthPixels

                cancelAnimation()
                if (motionEvent.x < screenWidth / 2) {

                    // Tap Left
                    if (viewPager.currentItem > 0) {
                        resetProgressBar(currentPosition)
                        viewPager.currentItem = viewPager.currentItem - 1
                        return true
                        //Tap Right
                    } else if (viewPager.currentItem == 0) {
                        resetProgressBar(currentPosition)
                        simulateFakeSwipeLeft() // we need this function to trigger onPageScrollStateChanged function
                        //we prefer this approach to avoid launching from here another animation which can interfere with
                        //another one due lack of synchronization
                        return true
                    }
                } else {

                    if (viewPager.currentItem < (viewPager.adapter?.itemCount?.minus(1) ?: 0)) {
                        completeProgressBar(currentPosition)
                        viewPager.currentItem = viewPager.currentItem + 1
                        return true
                    } else if (viewPager.currentItem == (viewPager.adapter?.itemCount?.minus(1)
                            ?: 0)
                    ) {
                        completeProgressBar(currentPosition)
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupListeners() {
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (!isAnimating) {
                    animateProgressBar(position, 2000L)
                }
                currentPosition = position
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    if (!isAnimating) {
                        animateProgressBar(viewPager.currentItem, 2000L)
                    }
                }
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
        if (currentPosition == storyAdapter.itemCount - 1)
            finish()
    }


    private fun resetProgressBar(position: Int) {
        val progressBarView = progressBarViews[position]
        val params = progressBarView.layoutParams as ViewGroup.LayoutParams
        params.width = 0
        progressBarView.layoutParams = params
        progressBarView.requestLayout()
    }

    private fun simulateFakeSwipeLeft() {
        if (viewPager.beginFakeDrag()) {
            viewPager.fakeDragBy(1f)
            viewPager.endFakeDrag()
        }
    }

    private fun cancelAnimation() {
        if (currentAnimator?.isRunning == true) {
            currentAnimator?.removeAllListeners()
            currentAnimator?.cancel()
            currentAnimator = null
            isAnimating = false
        }
    }


    private fun addProgressBarSegment(index: Int, totalSegments: Int) {
        val segmentContainer = FrameLayout(this)

        val greyBackgroundView = View(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#667085"))
        }

        val whiteForegroundView = View(this).apply {
            layoutParams =
                FrameLayout.LayoutParams(0, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                    gravity = Gravity.START
                }
            setBackgroundColor(Color.parseColor("#D0D5DD"))
            pivotX = 0f
        }

        segmentContainer.addView(greyBackgroundView)
        segmentContainer.addView(whiteForegroundView)

        val params = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT).apply {
            weight = 1f / totalSegments
            marginEnd =
                if (index < totalSegments - 1) 4.dpToPx(this@StoryActivity) else 0 // Space between segments
        }
        progressBarContainer.addView(segmentContainer, params)
        progressBarViews.add(whiteForegroundView)
    }

    private fun animateProgressBar(position: Int, duration: Long) {
        isAnimating = true
        Log.d("StoryActivity", "Animating progress bar for position: $position")


        val progressBarView = progressBarViews[position]
        currentAnimator =
            ValueAnimator.ofInt(0, progressBarContainer.width / progressBarViews.size).apply {
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
                        currentAnimator = null
                        //here we not call currentAnimator.cancel(), because we want as status segment to be displayed as completed
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
            finish()
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
        val path = MediaStore.Images.Media.insertImage(
            contentResolver, bitmap,
            "Title", null
        )
        val uri = Uri.parse(path)

        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "image/png"
        }

        startActivity(Intent.createChooser(shareIntent, "Share Image"))
    }
}




