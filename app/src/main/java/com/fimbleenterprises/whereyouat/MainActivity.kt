package com.fimbleenterprises.whereyouat

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.MenuProvider
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.fimbleenterprises.whereyouat.databinding.ActivityMainBinding
import com.fimbleenterprises.whereyouat.model.ServiceState
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModel
import com.fimbleenterprises.whereyouat.presentation.viewmodel.MainViewModelFactory
import com.fimbleenterprises.whereyouat.utils.Constants
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    @Inject
    lateinit var viewModelFactory: MainViewModelFactory

    private lateinit var binding: ActivityMainBinding
    lateinit var mainViewModel: MainViewModel
    lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mainViewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navHostFragment.navController

        //WhereYouAt.AppPreferences.uploadMyLocWaitIntervalSeconds = 5L

        MobileAds.initialize(this) {
            it.adapterStatusMap.forEach {adapterEntry ->
                Log.i(TAG, " !!!!!!! -= onCreate:${adapterEntry.key} / ${adapterEntry.value} =- !!!!!!!")
            }
        }

        // Set our memberid if it isn't already set.
        if (WhereYouAt.AppPreferences.memberid == 0L) {
            WhereYouAt.AppPreferences.memberid = System.currentTimeMillis()
            Log.w(TAG, "onCreate: MEMBERID SET TO ${WhereYouAt.AppPreferences.memberid}!!")
        }

        // Send a text message with the current trip code.
        mainViewModel.shareCodeAction.observe(this) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.type = "text/plain"
            intent.putExtra(Intent.EXTRA_TEXT, it.message)
            intent.putExtra(MainViewModel.TRIPCODE_INTENT_EXTRA_TAG, it.tripcode)
            startActivity(Intent.createChooser(intent, "Share WhereYouAt Code"))
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()

        binding.signInButton.setOnClickListener {
            Toast.makeText(this, "Logging In", Toast.LENGTH_SHORT).show()
            signInGoogle()
        }

        // Add menu items without overriding methods in the Activity
        addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                // Add menu items here
                menuInflater.inflate(R.menu.main_menu, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                when (item.itemId) {
                    R.id.action_signin -> {
                        signInGoogle()
                    }
                    R.id.action_signout -> {
                        mGoogleSignInClient.signOut().addOnCompleteListener {
                            Toast.makeText(this@MainActivity, "Signed out of Google.", Toast.LENGTH_SHORT).show()
                            if (mainViewModel.serviceState.value?.state == ServiceState.SERVICE_STATE_RUNNING) {
                                mainViewModel.requestServiceStop()
                            }
                        }
                    }
                    R.id.action_privacy_policy -> {
                        val browserIntent =
                            Intent(Intent.ACTION_VIEW, Uri.parse(Constants.PRIVACY_URL))
                        startActivity(browserIntent)
                    }
                    /*R.id.action_settings -> {
                        findNavController(R.id.nav_host_fragment).navigate(R.id.settingsFragment)
                    }*/
                }
                return false
            }
        })

    }

    fun signInGoogle() {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, Req_Code)
    }

    private fun getServerUrl() {

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        sendBroadcast(Intent(BACK_PRESSED))
        return super.onKeyDown(keyCode, event)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStart() {
        super.onStart()
        // Check if we have foreground permissions
        if (!foregroundPermissionApproved()) {
            requestForegroundPermissions()
        }
        if (GoogleSignIn.getLastSignedInAccount(this) == null) {
            signInGoogle()
        }
    }

    // onActivityResult() function : this is where
    // we provide the task and data for the Google Account
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Req_Code) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        }
    }

    // this is where we update the UI after Google signin takes place
    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                WhereYouAt.AppPreferences.email = account.email.toString()
                WhereYouAt.AppPreferences.googleid = account.id
                WhereYouAt.AppPreferences.token = account.idToken
                WhereYouAt.AppPreferences.name = account.displayName
                WhereYouAt.AppPreferences.avatarUrl = account.photoUrl.toString()
                /*val intent = Intent(this, DashboardActivity::class.java)
                startActivity(intent)
                finish()*/
            }
        }
    }

    private fun handleResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account: GoogleSignInAccount? = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                updateUI(account)
            }
        } catch (e: ApiException) {
            /*Toast.makeText(this, "Ya gotta sign in, bitch!", Toast.LENGTH_SHORT).show()
            signInGoogle()*/
        }
    }

    // TODO: Step 1.0, Review Permissions: Method checks if permissions approved.
    private fun foregroundPermissionApproved(): Boolean {
        return PackageManager.PERMISSION_GRANTED == ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    // TODO: Step 1.0, Review Permissions: Method requests permissions.
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestForegroundPermissions() {
        val provideRationale = foregroundPermissionApproved()

        // If the user denied a previous request, but didn't check "Don't ask again", provide
        // additional rationale.
        if (provideRationale) {
            Snackbar.make(
                binding.root,
                R.string.permission_rationale,
                Snackbar.LENGTH_LONG
            )
                .setAction(R.string.okay) {
                    // Request permission
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
                    )
                }
                .show()
        } else {
            Log.d(TAG, "Request foreground only permission")
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    // TODO: Step 1.0, Review Permissions: Handles permission result.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE -> when {
                grantResults.isEmpty() ->
                    // If user interaction was interrupted, the permission request
                    // is cancelled and you receive empty arrays.
                    Log.d(TAG, "User interaction was cancelled.")
                grantResults[0] == PackageManager.PERMISSION_GRANTED ->
                    // Permission was granted.
                    Log.i(TAG, "-=MainActivity:onRequestPermissionsResult  =-")
                // foregroundOnlyLocationService?.subscribeToLocationUpdates()
                else -> {
                    Snackbar.make(
                        binding.root,
                        R.string.permission_denied_explanation,
                        Snackbar.LENGTH_LONG
                    )
                        .setAction(R.string.settings) {
                            // Build intent that displays the App settings screen.
                            val intent = Intent()
                            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            val uri = Uri.fromParts(
                                "package",
                                BuildConfig.APPLICATION_ID,
                                null
                            )
                            intent.data = uri
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                        }
                        .show()
                }
            }
        }
    }

    init { Log.i(TAG, "Initialized:MainActivity") }
    companion object {
        private const val TAG = "FIMTOWN|MainActivity"
        private const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 34
        private const val BACK_PRESSED = "BACK_PRESSED"
        val Req_Code: Int = 123
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        // Updates button states if new while in use location is added to SharedPreferences.
        // if (key == SharedPreferenceUtil.KEY_FOREGROUND_ENABLED) { }
    }

}
