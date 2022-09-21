package com.fimbleenterprises.whereyouat.utils

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.LinearLayout


object Utils {

    fun hasInternetConnection(context: Context?): Boolean {
        if (context == null)
            return false
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager

        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    /**
     * Wrapper class for views for use with ObjectAnimator.  ObjectAnimator requires the "weight"
     * property to function and the View class does not have that property.  So we can use this as
     * a wrapper which gets/sets the weight from LinearLayout.LayoutParams.
     */
    class ViewWeightAnimationWrapper(view: View) {
        private var view: View? = null
        var weight: Float
            get() = (view?.layoutParams as LinearLayout.LayoutParams).weight
            set(weight) {
                val params = view?.layoutParams as LinearLayout.LayoutParams
                params.weight = weight
                view?.parent?.requestLayout()
            }

        init {
            if (view.layoutParams is LinearLayout.LayoutParams) {
                this.view = view
            } else {
                throw IllegalArgumentException("The view should have LinearLayout as parent")
            }
        }
    }

    fun fadeOutView(view: View) {
        val alphaAnimation = AlphaAnimation(1.0f, 0.0f)
        alphaAnimation.duration = 100
        alphaAnimation.repeatCount = 0
        alphaAnimation.repeatMode = Animation.REVERSE
        view.startAnimation(alphaAnimation)
    }

    fun fadeInView(view: View) {
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation.duration = 100
        alphaAnimation.repeatCount = 0
        alphaAnimation.repeatMode = Animation.REVERSE
        view.startAnimation(alphaAnimation)
    }

    fun crossFadeAnimation(fadeInTarget: View, fadeOutTarget: View, duration: Long) {
        val mAnimationSet = AnimatorSet()
        val fadeOut = ObjectAnimator.ofFloat(fadeOutTarget, View.ALPHA, 1f, .5f)
        fadeOut.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                fadeOutTarget.isEnabled = false
            }
            override fun onAnimationEnd(animation: Animator) {
                fadeOutTarget.visibility = View.GONE
            }

            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        fadeOut.interpolator = LinearInterpolator()
        val fadeIn = ObjectAnimator.ofFloat(fadeInTarget, View.ALPHA, .5f, 1f)
        fadeIn.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {
                fadeInTarget.visibility = View.VISIBLE
            }

            override fun onAnimationEnd(animation: Animator) {
                fadeInTarget.isEnabled = true
            }
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
        })
        fadeIn.interpolator = LinearInterpolator()
        mAnimationSet.duration = duration
        mAnimationSet.playTogether(fadeOut, fadeIn)
        mAnimationSet.start()
    }

}

fun Button.setTextAnimation(text: String, duration: Long = 200, completion: (() -> Unit)? = null) {
    fadOutAnimation(duration) {
        this.text = text
        fadInAnimation(duration) {
            completion?.let {
                it()
            }
        }
    }
}// ViewExtensions

fun Button.fadeDisable(duration: Long = 200) {
    fadOutDisable(duration) { }
}// ViewExtensions

fun Button.fadeEnable(duration: Long = 200) {
    fadInEnable(duration) { }
}// ViewExtensions

fun View.fadOutAnimation(duration: Long = 200, visibility: Int = View.INVISIBLE, completion: (() -> Unit)? = null) {
    animate()
        .alpha(0f)
        .setDuration(duration)
        .withEndAction {
            this.visibility = visibility
            completion?.let {
                it()
            }
        }
}

fun View.fadInAnimation(duration: Long = 200, completion: (() -> Unit)? = null) {
    alpha = 0f
    visibility = View.VISIBLE
    animate()
        .alpha(1f)
        .setDuration(duration)
        .withEndAction {
            completion?.let {
                it()
            }
        }
}

fun View.fadOutDisable(duration: Long = 200, completion: (() -> Unit)? = null) {
    animate()
        .alpha(.4f)
        .setDuration(duration)
        .withEndAction {
            this.isEnabled = false
            completion?.let {
                it()
            }
        }
}

fun View.fadInEnable(duration: Long = 200, completion: (() -> Unit)? = null) {
    alpha = .4f
    visibility = View.VISIBLE
    animate()
        .alpha(1f)
        .setDuration(duration)
        .withEndAction {
            this.isEnabled = true
            completion?.let {
                it()
            }
        }
}