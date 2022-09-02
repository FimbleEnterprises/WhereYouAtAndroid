package com.fimbleenterprises.whereyouat.presentation.ui

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.R
import com.fimbleenterprises.whereyouat.WhereYouAt.AppPreferences
import com.fimbleenterprises.whereyouat.databinding.FragmentStartBinding
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.whereyouat.utils.Resource
import com.fimbleenterprises.whereyouat.utils.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_start.*
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
        viewmodel = (activity as MainActivity).mainViewModel

        /* Listen for keyboard show/hide so we can re-focus the container when the keyboard goes to sleepy.
        The "container" is the outer-most element of the XML design resource and if it is not focused
        we cannot get onKey events from the onKeyListener that we have attached to it.  */
        /*val decorView  = activity?.window?.decorView ?: return
        ViewCompat.setOnApplyWindowInsetsListener(decorView) { _, insets ->
            val showingKeyboard = insets.isVisible(WindowInsetsCompat.Type.ime())
            if(showingKeyboard){
                Log.i(TAG, "-=onViewCreated:KEYBOARD IS SHOWING =-")
            } else {
                Log.i(TAG, "-=onViewCreated:KEYBOARD IS NOT SHOWING =-")
                // Re-focus the container so we can detect back presses
                binding.container.isFocusableInTouchMode = true
                binding.container.requestFocus()
            }
            insets
        }*/

    } // onViewCreated

    // Button onClickListeners are implemented here.
    override fun onStart() {
        super.onStart()

        // Create button doubles as a cancel button - check the service status
        // to decide what action to perform (create or cancel)
        binding.btnCreate.setOnClickListener {
            // Service running, we cancel it.
            if (viewmodel.serviceStatus.value?.isStarting == true
                || viewmodel.serviceStatus.value?.isRunning == true) {
                viewmodel.stopService()
            } else {
                // Create a trip and go
                //startObservingCreateTripLiveData()
                viewmodel.createTrip(System.currentTimeMillis())
                viewmodel.tripcodeCreated.observe(viewLifecycleOwner) {
                    when (it) {
                        true -> {
                            // Navigate to the map
                            findNavController().navigate(
                                R.id.action_startFragment_to_mapFragment
                            )
                        }
                    }
                }
                isProcessing(true)
            }
        }

        // Entered code will be validated and if passes, a trip will begin (navigate to map frag)
        binding.btnJoin.setOnClickListener {
            // Check the state of the button
            if (!tripCodeEntryExpanded) {
                // Gracefully show the enter trip code edit text
                expandTripcodeInput(true)
                // Try to show the keyboard (doesn't always work - device dependant?)
                val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(binding.edittextTripCode, InputMethodManager.SHOW_FORCED)
            } else {
                val proposedTripCode = binding.edittextTripCode.text.toString()
                isProcessing(true)
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
                    isProcessing(false)
                    return@setOnClickListener
                }

                validateAndGoToTrip(proposedTripCode)

            } // tripcode entered by user
        } // button click

        // Will attempt to join the last successfully joined trip.
        binding.btnResume.setOnClickListener {
            if (AppPreferences.lastTripCode != null) {
                validateAndGoToTrip(AppPreferences.lastTripCode!!)
            } else {
                Toast.makeText(context, "No trip to resume.", Toast.LENGTH_SHORT).show()
            }
        }

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
    private fun validateAndGoToTrip(proposedTripCode: String) {
        isProcessing(true)
        // Validate code server-side and go to map (trip now active) if validation passes
        CoroutineScope(IO).launch {
            viewmodel.validateCode(proposedTripCode).collect {
                when (it) {
                    is Resource.Success -> {
                        if (it.data?.wasSuccessful == true) {
                            if (it.data.genericValue.toBoolean()) {
                                // Trip code is valid according to server - we proceed.
                                withContext(Main) {

                                    // Set the trip code (we'll null this out if something goes wrong below)
                                    AppPreferences.tripCode = proposedTripCode

                                    // Set the ServiceStatus isStarting == true
                                    viewmodel.requestTripStart()

                                    // Navigate to the map
                                    findNavController().navigate(
                                        R.id.action_startFragment_to_mapFragment
                                    )

                                    // Stop progressbars and enable buttons.
                                    isProcessing(false)
                                }
                            } else { // Proper response, no errors but trip code is not valid

                                // Null out trip code (it probably already is but still)
                                AppPreferences.tripCode = null

                                // Show toast and be done.
                                withContext(Main) {
                                    isProcessing(false)
                                    Toast.makeText(context,
                                        "Trip code is no longer active.",
                                        Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else { // I/O problem with API
                            withContext(Main) {

                                // Show toast and be done.
                                isProcessing(false)
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
        anim1.duration = 350
        anim2.duration = 350

        anim1.start()
        anim2.start()
    }

    /**
     * Enables/Disables controls while processing.
     */
    private fun isProcessing(processing: Boolean) {
        if (processing) { binding.progressBar.visibility = View.VISIBLE } else
            { binding.progressBar.visibility = View.GONE }
        binding.btnJoin.isEnabled = !processing
        binding.btnCreate.isEnabled = !processing
        binding.btnResume.isEnabled = !processing
        edittext_trip_code.isEnabled = !processing
    }

    /**
     * Observes the ServiceStatus member stored in the viewmodel for insight on current trip (or
     * lack thereof).
     */
    private fun observeServiceStatus() = CoroutineScope(Main).launch {
        binding.btnResume.text = getString(R.string.resume_trip_button_text, AppPreferences.lastTripCode ?: "")
        viewmodel.serviceStatus.observe(viewLifecycleOwner) {
            when (it.isRunning) {
                true -> {
                    // Probably don't need this here as we are going to navigate the user to the
                    // map screen immediately anyway but I'm gonna leave it here anyway.
                    binding.btnCreate.text = getString(R.string.leave_trip_button)

                    findNavController().navigate(
                        R.id.action_startFragment_to_mapFragment
                    )
                }
                false -> {
                    binding.btnCreate.text = getString(R.string.create_button)
                }
            }
            /*when (it.isStarting) {
                true -> {
                    // Navigate to the map
                    findNavController().navigate(
                        R.id.action_startFragment_to_mapFragment
                    )
                }
                else -> {}
            }*/
        }
    }
}























