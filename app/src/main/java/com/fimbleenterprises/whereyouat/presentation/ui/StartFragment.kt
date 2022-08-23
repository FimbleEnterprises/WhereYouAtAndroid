package com.fimbleenterprises.whereyouat.presentation.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.R
import com.fimbleenterprises.whereyouat.WhereYouAt
import com.fimbleenterprises.whereyouat.data.usecases.GetServiceStatusUseCase
import com.fimbleenterprises.whereyouat.data.usecases.SaveServiceStatusUseCase
import com.fimbleenterprises.whereyouat.databinding.FragmentStartBinding
import com.fimbleenterprises.whereyouat.model.ServiceStatus
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.whereyouat.utils.Resource
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class StartFragment : Fragment() {

    private lateinit var binding: FragmentStartBinding
    private lateinit var viewmodel: MainViewModel
/*    @Inject
    lateinit var saveServiceStatusUseCase: SaveServiceStatusUseCase*/
    @Inject
    lateinit var getServiceStatusUseCase: GetServiceStatusUseCase

    init { Log.i(TAG, "Initialized:StartFragment") }
    companion object { private const val TAG = "FIMTOWN|StartFragment" }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_start, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentStartBinding.bind(view)
        viewmodel = (activity as MainActivity).mainViewModel
        startObservingServiceStatus()
    }

    override fun onStart() {
        super.onStart()

        binding.btnCreate.setOnClickListener {
            CoroutineScope(Main).launch {
                val status = getServiceStatusUseCase.execute()
                if (status.isStarting || status.isRunning) {
                    // Send a cancel intent to the service.
                    viewmodel.stopService()
                } else {
                    startObservingCreateTripLiveData()
                    viewmodel.createTrip(System.currentTimeMillis())
                    withContext(Main) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }
            }
        }

        binding.btnJoin.setOnClickListener {
            binding.progressBar.visibility = View.VISIBLE
            CoroutineScope(IO).launch {
                val status = getServiceStatusUseCase.execute()
                if (status.isStarting || status.isRunning) {
                    // Already running or starting
                    withContext(Main) {
                        findNavController().navigate(
                            R.id.mapFragment
                        )
                        findNavController().clearBackStack(R.id.mapFragment)
                    }
                } else {
                    // Not running - we start it.
                    viewmodel.setServiceStatus(
                        ServiceStatus(isStarting = true, isRunning = false, "HN0QU")
                    )
                    withContext(Main) {
                        WhereYouAt.AppPreferences.tripcode = "HN0QU"
                        findNavController().navigate(
                            R.id.mapFragment
                        )
                        findNavController().clearBackStack(R.id.mapFragment)
                    }
                }
            }
        }

    }

    override fun onStop() {
        super.onStop()
    }

    override fun onResume() {
        manageButtonStates()
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

    }

    private fun startObservingCreateTripLiveData() {

        viewmodel.createTripApiResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let {
                        Toast.makeText(context,
                            "Found ${it.genericValue}!",
                            Toast.LENGTH_SHORT).show()
                    }

                    CoroutineScope(Main).launch {
                        viewmodel.setServiceStatus(
                            ServiceStatus(
                                1,
                                false,
                                System.currentTimeMillis(),
                                true,
                                response.data?.genericValue
                            )
                        )
                        // Navigate to the map
                        findNavController().navigate(
                            R.id.mapFragment
                        )
                        findNavController().clearBackStack(R.id.mapFragment)
                    }
                    binding.progressBar.visibility = View.GONE
                }
                is Resource.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        context,
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                is Resource.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun startObservingServiceStatus() {
        viewmodel.serviceStatus.observe(viewLifecycleOwner) {
            manageButtonStates()
        }
    }

    private fun manageButtonStates() {
        CoroutineScope(IO).launch {
            val serviceStatus = getServiceStatusUseCase.execute()
            when (serviceStatus.isRunning) {
                true -> {
                    withContext(Main) {
                        binding.btnCreate.text = getString(R.string.leave_trip_button)
                        binding.btnJoin.text = getString(R.string.resume_trip_button_text, viewmodel.serviceStatus.value?.tripcode)
                    }
                }
                false -> {
                    withContext(Main) {
                        binding.btnCreate.text = getString(R.string.create_button)
                        binding.btnJoin.text = getString(R.string.join_existing_button)
                    }
                }
            }
        }
    }

}























