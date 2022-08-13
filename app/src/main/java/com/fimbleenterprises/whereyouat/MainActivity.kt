package com.fimbleenterprises.whereyouat

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModelProvider
import coil.ImageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import com.fimbleenterprises.whereyouat.databinding.ActivityMainBinding
import com.fimbleenterprises.whereyouat.utils.Resource
import com.fimbleenterprises.whereyouat.viewmodel.MainViewModel
import com.fimbleenterprises.whereyouat.viewmodel.MainViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: MainViewModelFactory
    lateinit var mainViewModel: MainViewModel
    private lateinit var _binding: ActivityMainBinding
    private lateinit var disposable: Disposable
    private var imageUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(_binding.root)

        mainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        fetchTripUpdates("bjha")
        startObservingLiveData()

        _binding.imgRefresh.setOnClickListener {
            _binding.progressBar.visibility = View.VISIBLE
            fetchTripUpdates("bjha")
        }
        _binding.imgDownload.setOnClickListener {
            _binding.progressBar.visibility = View.VISIBLE
            mainViewModel.createTrip(System.currentTimeMillis())
        }
    }

    private fun fetchTripUpdates(tripcode: String) {
        mainViewModel.getMemberLocations(tripcode)
    }

    private fun startObservingLiveData() {

        mainViewModel.createTripApiResponse.observe(this) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let {
                        Toast.makeText(applicationContext,
                            "Found ${it.genericValue}!",
                            Toast.LENGTH_SHORT).show()
                    }
                    _binding.progressBar.visibility = View.GONE
                }

                is Resource.Error -> {
                    _binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is Resource.Loading -> {
                    _binding.progressBar.visibility = View.VISIBLE
                }
            }
        }

        mainViewModel.memberLocationsApiResponse.observe(this) { response ->
            when (response) {
                is Resource.Success -> {
                    response.data?.let {
                        Toast.makeText(applicationContext,
                            "Found ${it.memberLocations.size} members!",
                            Toast.LENGTH_SHORT).show()
                    }
                    _binding.progressBar.visibility = View.GONE
                }

                is Resource.Error -> {
                    _binding.progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        response.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is Resource.Loading -> {
                    _binding.progressBar.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroy() {
        disposable.dispose()
        super.onDestroy()
    }

    init { Log.i(TAG, "Initialized:MainActivity") }
    companion object { private const val TAG = "FIMTOWN|MainActivity" }

}
