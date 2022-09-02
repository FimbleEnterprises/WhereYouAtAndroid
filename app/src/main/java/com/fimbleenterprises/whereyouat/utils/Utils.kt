package com.fimbleenterprises.whereyouat.utils

import android.animation.ObjectAnimator
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.view.View
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

}