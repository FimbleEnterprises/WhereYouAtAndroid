package com.fimbleenterprises.whereyouat.presentation.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.fimbleenterprises.whereyouat.MainActivity
import com.fimbleenterprises.whereyouat.R
import com.fimbleenterprises.whereyouat.databinding.FragmentStartBinding
import com.fimbleenterprises.whereyouat.utils.Resource
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel

/**
 * A simple [Fragment] subclass.
 * Use the [StartFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class StartFragment : Fragment() {

    private lateinit var binding: FragmentStartBinding
    private lateinit var viewmodel: MainViewModel

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

        binding.btnCreate.setOnClickListener {
            startObservingCreateTripLiveData()
            viewmodel.createTrip(System.currentTimeMillis())
            binding.progressBar.visibility = View.VISIBLE
        }

        binding.btnJoin.setOnClickListener {
            startObservingJoinTripLiveData()
            binding.progressBar.visibility = View.VISIBLE
            viewmodel.setTripCode("IBASJ")
            fetchTripUpdates("IBASJ")
        }
    }

    private fun fetchTripUpdates(tripcode: String) {
        viewmodel.getMemberLocations(tripcode)
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
                    binding.progressBar.visibility = View.GONE

                    // Navigate to the map
                    findNavController().navigate(
                        R.id.mapFragment
                    )
                    findNavController().clearBackStack(R.id.mapFragment)
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

    private fun startObservingJoinTripLiveData() {
        viewmodel.memberLocationsApiResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let {
                        Toast.makeText(context,
                            "Found ${it.locUpdates.size} members!",
                            Toast.LENGTH_SHORT).show()
                    }
                    binding.progressBar.visibility = View.GONE

                    // Navigate to the map
                    findNavController().navigate(
                        R.id.mapFragment
                    )
                    findNavController().clearBackStack(R.id.mapFragment)
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

}