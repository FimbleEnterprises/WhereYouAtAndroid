package com.fimbleenterprises.whereyouat.presentation.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.R
import com.fimbleenterprises.whereyouat.WhereYouAt.AppPreferences
import com.fimbleenterprises.whereyouat.databinding.FragmentStartBinding
import com.fimbleenterprises.whereyouat.model.ApiEvent
import com.fimbleenterprises.whereyouat.model.ServiceState
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.whereyouat.utils.Resource
import com.fimbleenterprises.whereyouat.utils.Utils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@AndroidEntryPoint
class StartFragment : Fragment(), View.OnKeyListener {

    init { Log.i(TAG, "Initialized:StartFragment") }
    companion object { private const val TAG = "FIMTOWN|StartFragment" }

    /**
     * Used to track whether or not the tripcode entry is presented to the user.
     */
    private var tripCodeEntryExpanded = false
    private lateinit var binding: FragmentStartBinding
    private lateinit var viewmodel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStartBinding.bind(view)
        isProcessing(processing = true, showProgress = false)
        viewmodel = (activity as MainActivity).mainViewModel
    } // onViewCreated

    // Button onClickListeners are implemented here.
    override fun onStart() {
        super.onStart()

        // materialToolbar.title = "Create, join or resume"

        // Create button doubles as a cancel button - check the service status
        // to decide what action to perform (create or cancel)
        binding.btnCreate.setOnClickListener {

            // Check that user is signed in
            if (GoogleSignIn.getLastSignedInAccount(requireContext()) == null) {
                Toast.makeText(context, getString(R.string.please_sign_in), Toast.LENGTH_SHORT).show()
                (activity as MainActivity).signInGoogle()
                return@setOnClickListener
            }

            when (viewmodel.serviceState.value?.state) {
                // Service running, we cancel it.
                ServiceState.SERVICE_STATE_RUNNING -> {
                    viewmodel.requestServiceStop()
                }
                // Create a trip and go
                ServiceState.SERVICE_STATE_IDLE -> {
                    isProcessing(true)
                    viewmodel.createTrip(System.currentTimeMillis())
                }
            }
        }

        // Entered code will be validated and if passes will be navigated to map frag
        binding.btnJoin.setOnClickListener {

            // Check that user is signed in
            if (GoogleSignIn.getLastSignedInAccount(requireContext()) == null) {
                Toast.makeText(context, getString(R.string.please_sign_in), Toast.LENGTH_SHORT).show()
                (activity as MainActivity).signInGoogle()
                return@setOnClickListener
            }

            // Check the state of the button
            if (!tripCodeEntryExpanded) {
                // Gracefully show the enter trip code edit text
                expandTripcodeInput(true)
                // Try to show the keyboard (doesn't always work - device dependant?)
                val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.edittextTripCode, InputMethodManager.SHOW_FORCED)
                AppPreferences.lastTripCode?.let {
                    binding.edittextTripCode.setText(it)
                }
            } else {
                val proposedTripCode = binding.edittextTripCode.text.toString()
                // isProcessing(true)
                // We have a tripcode from the user, we need to validate it against the server.
                // Valid meaning:
                //      CLIENTSIDE:
                //          the correct amount of characters (5)
                //          not null
                //      SERVER-SIDE:
                //          Exists in the trips table
                //          Has been written to by anyone within the last two hours

                // validate code client-side
                if (!viewmodel.tripcodeIsValidClientside(proposedTripCode)) {
                    Toast.makeText(context, getString(R.string.toast_code_not_valid), Toast.LENGTH_SHORT).show()
                    // isProcessing(false)
                    return@setOnClickListener
                }

                validateThenJoinTrip(proposedTripCode)

            } // tripcode entered by user
        } // button click

/*        // Will attempt to join the last successfully joined trip.
        binding.btnResume.setOnClickListener {

            // Check that user is signed in
            if (GoogleSignIn.getLastSignedInAccount(requireContext()) == null) {
                Toast.makeText(context, getString(R.string.please_sign_in), Toast.LENGTH_SHORT).show()
                (activity as MainActivity).signInGoogle()
                return@setOnClickListener
            }

            if (AppPreferences.lastTripCode != null) {
                validateThenJoinTrip(AppPreferences.lastTripCode!!)
            } else {
                Toast.makeText(context, "No trip to resume.", Toast.LENGTH_SHORT).show()
            }
        }*/

        // Attach an onkeylistener to the outer-most view in the fragment (to look for back presses)
        binding.container.setOnKeyListener(this)

        // Focus the container so it can detect onKey events
        binding.container.requestFocus()

        /* Listen for keyboard show/hide so we can re-focus the container when the keyboard goes to
        sleepy. The "container" is just an arbitrary view (the outer-most element in this case) of
        the XML resource file.  We assign an onKeyListener to it so we can listen for back presses
        so we can then hide the enter trip code animation on back press instead of just closing
        the activity.  The onKeyListener only captures events when it is focused so that is what we
        aim to do here.

        There is no magic bullet for detecting when the keyboard is visible but this hack is my
        favorite way.  There is a way using setWindowInsetsAnimationCallback and the decor view
        which works great but it has a side effect of changing the task bar color (and it ain't subtle).  */
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            //r will be populated with the coordinates of your view that area still visible.
            binding.root.getWindowVisibleDisplayFrame(r)
            val heightDiff: Int = binding.root.rootView.height - (r.bottom - r.top)
            if (heightDiff > 500) { // if more than 500 pixels, its probably a keyboard...
                Log.i(TAG, "-=onStart: =-")
            } else {
                binding.container.isFocusableInTouchMode = true
                binding.container.requestFocus()
            }
        }
    }

    /** Performs server-side validation of the trip code and then navigates to the map if it's good.*/
    private fun validateThenJoinTrip(proposedTripCode: String) {
        isProcessing(true)
        // Validate code server-side and go to map (trip now active) if validation passes
        CoroutineScope(IO).launch {
            viewmodel.validateCode(proposedTripCode).collect {
                when (it) {
                    is Resource.Success -> {
                        if (it.data?.wasSuccessful == true) {
                            // We know that the API will use the GenericValue property to pass
                            // a boolean indicating validity.
                            if (it.data.genericValue?.toString().toBoolean()) {
                                // Trip code is valid according to server - we proceed.
                                withContext(Main) {
                                    // Set the trip code (we'll null this out if something goes wrong below)
                                    AppPreferences.tripCode = proposedTripCode
                                    // Set the ServiceStatus isStarting == true
                                    viewmodel.requestTripStart()
                                }
                            } else { // Proper response, no errors, but trip code is not valid
                                // Null out trip code (it probably already is but still)
                                AppPreferences.tripCode = null
                                viewmodel.setServiceIdle()
                                // Show toast and be done.
                                withContext(Main) {
                                    Toast.makeText(
                                        context,
                                        "Trip code is no longer active.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    if (proposedTripCode == AppPreferences.lastTripCode) {
                                        AppPreferences.lastTripCode = null
                                        binding.edittextTripCode.setText("")
                                    }
                                    expandTripcodeInput(false)
                                }
                            }
                        } else { // I/O problem with API
                            withContext(Main) {

                                // Null out trip code (it probably already is but still)
                                AppPreferences.tripCode = null

                                viewmodel.setServiceIdle()

                                // Show toast and be done.
                                Toast.makeText(context,
                                    "Something went wrong.",
                                    Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    else -> { // I/O problem with API
                        withContext(Main) {
                            // Show toast and be done.
                            isProcessing(false)
                            Toast.makeText(context,
                                "Something went wrong.",
                                Toast.LENGTH_SHORT).show()
                            isProcessing(false)
                        }
                    }
                } // API response
            } // use case
        } // Coroutine scope
    }

    /**
     * Looking for back press so we can collapse the trip code edittext column before leaving app.
     */
    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        val shouldCollapse = tripCodeEntryExpanded
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            // If the edittext is showing we hide it
            if (tripCodeEntryExpanded) {
                expandTripcodeInput(false)
            }
        }
        return shouldCollapse
    }

    override fun onResume() {
        observeServiceStatus()
        observeApiEvents()
        super.onResume()
    }

    /**
     * Gracefully shows/hides the enter tripcode edittext column using animation and tracks its state.
     */
    private fun expandTripcodeInput(expand: Boolean) {

        // Update frag-wide flag tracking the state.
        tripCodeEntryExpanded = expand

        // Arbitrary column sizes; may need tweaking (i.e. intelligence) for different screen sizes.
        val inputCellWeight: Float
        val buttonCellWeight: Float

        if (expand) { // will effectively un-hide the tripcode edittext
            inputCellWeight = .65f
            buttonCellWeight = .35f
        } else { // will effectively hide the tripcode edittext
            inputCellWeight = 0f
            buttonCellWeight = 1f
        }

        // Animate each column of the table containing the edit text and button.  One gets
        // smaller and the other bigger (respectively).
        val animationWrapper1 = Utils.ViewWeightAnimationWrapper(binding.cellTripcode)
        val animationWrapper2 = Utils.ViewWeightAnimationWrapper(binding.cellJoinbutton)

        val anim1 = ObjectAnimator.ofFloat(
            animationWrapper1,
            "weight",
            animationWrapper1.weight,
            inputCellWeight
        )
        val anim2 = ObjectAnimator.ofFloat(
            animationWrapper2,
            "weight",
            animationWrapper2.weight,
            buttonCellWeight
        )

        // Arbitrary durations
        anim1.duration = 300
        anim2.duration = 350

        anim1.start()
        anim2.start()
    }

    /**
     * Enables/Disables controls while processing.
     */
    private fun isProcessing(processing: Boolean, showProgress: Boolean = true) {

        if (processing && showProgress) {
            // Utils.crossFadeAnimation(binding.progressBar, binding.controls, 350)
            binding.progressBar.visibility = View.VISIBLE
        } else {
            // Utils.crossFadeAnimation(binding.controls, binding.progressBar, 350)
            binding.progressBar.visibility = View.GONE
        }
        binding.btnJoin.isEnabled = !processing
        binding.btnCreate.isEnabled = !processing
        // binding.btnResume.isEnabled = !processing
        binding.edittextTripCode.isEnabled = !processing
    }

    private fun observeApiEvents() {
        viewmodel.apiEvent.observe(viewLifecycleOwner) {
            when (it?.event) {
                ApiEvent.Event.REQUEST_FAILED -> {
                    Log.w(TAG, "observeApiEvents: $it")
                    Toast.makeText(context, "Failed to create trip.", Toast.LENGTH_SHORT).show()
                    isProcessing(false)
                } else -> {
                    Log.i(TAG, "-=observeApiEvents:Value:$it =-")
                }
            }
        }
    }

    /**
     * Observes the ServiceStatus member stored in the viewmodel for insight on current trip (or
     * lack thereof).
     */
    private fun observeServiceStatus() = CoroutineScope(Main).launch {
        // binding.btnResume.text = getString(R.string.resume_trip_button_text, AppPreferences.lastTripCode ?: "")
        viewmodel.serviceState.observe(viewLifecycleOwner) {
            when (it.state) {
                // This state means that the service is already running, a trip is active.  It is
                // not super easy to get here with this state - requiring the user to have specifically
                // swiped away the activity and then come back but not super rare either.
                ServiceState.SERVICE_STATE_RUNNING -> {
                    // Probably don't need this here as we are going to navigate the user to the
                    // map screen immediately anyway but I'm gonna leave it here anyway.
                    // binding.btnCreate.text = getString(R.string.leave_trip_button)
                    Handler(Looper.getMainLooper()).postDelayed({
                        isProcessing(true)
                        // Navigate to the map
                        findNavController().popBackStack()
                        val navBuilder = NavOptions.Builder()
                        navBuilder.setEnterAnim(R.anim.slide_in_left).setExitAnim(R.anim.slide_out_left)
                            .setPopEnterAnim(android.R.anim.fade_in).setPopExitAnim(android.R.anim.fade_out)
                        findNavController().navigate(
                            R.id.mapFragment, null, navBuilder.build()
                        )
                    }, 500)
                }
                // This will be the state if the VM has made an API call and validated the
                // created /existing trip.  We navigate to the map and the map frag will
                // start the service and we'll be off!
                ServiceState.SERVICE_STATE_STARTING -> {
                    // binding.btnCreate.text = getString(R.string.setting_up)
                    // binding.btnCreate.setTextAnimation(getString(R.string.setting_up), 200)
                    isProcessing(true)
                    Handler(Looper.getMainLooper()).postDelayed({
                        // Navigate to the map
                        findNavController().popBackStack()
                        val navBuilder = NavOptions.Builder()
                        navBuilder.setEnterAnim(R.anim.slide_in_left).setExitAnim(R.anim.slide_out_left)
                            .setPopEnterAnim(android.R.anim.fade_in).setPopExitAnim(android.R.anim.fade_out)
                        findNavController().navigate(
                            R.id.mapFragment, null, navBuilder.build()
                        )
                    }, 1500)
                }
                // This is a good state for this frag - ready to join/create a trip.
                ServiceState.SERVICE_STATE_STOPPED -> {
                    binding.btnCreate.text = getString(R.string.create_button)
                    isProcessing(false)
                }
                ServiceState.SERVICE_STATE_STOPPING -> {
                    binding.btnCreate.text = getString(R.string.stopping)
                    isProcessing(true)
                }
                else -> {
                    binding.btnCreate.text = getString(R.string.create_button)
                    // binding.btnResume.isEnabled = AppPreferences.lastTripCode != null
                    isProcessing(false)
                }
            }
        }
    }
}























