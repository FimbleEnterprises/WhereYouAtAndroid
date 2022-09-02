package com.fimbleenterprises.whereyouat.presentation.ui

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.fimbleenterprises.whereyouat.R

/**
 * Basic dialog fragment to prompt the user to give us money and happiness for all parties!  I
 * mean, it would be dumb because this app is kinda dumb but a fool and his money are easily
 * parted!
 */
class BgPermRationaleDialogFragment
    constructor(
        private val decisionListener: DecisionListener
    ) : DialogFragment() {

    interface DecisionListener {
        fun affirmative()
        fun negative()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.background_permission_rationale_message))
            .setPositiveButton(getString(R.string.yes)) { _,_ ->
                decisionListener.affirmative()
                Log.i(TAG,"-= PURCHASE CONFIRMED! =-")
                this.dismiss()
            }
            .setNegativeButton(getString(R.string.no)) {_,_ ->
                Toast.makeText(context, getString(R.string.no_thankyou), Toast.LENGTH_SHORT).show()
                decisionListener.negative()
                Log.i(TAG, "-= PURCHASE DECLINED =-")
                this.dismiss()
            }
            .create()

    init { Log.i(TAG, "Initialized:BackgroundPermissionRationaleDialogFragment") }
    companion object { private const val TAG = "FIMTOWN|BackgroundPermissionRationaleDialogFragment" }

}