package com.endeavride.endeavridedriver.ui.maps

import android.Manifest
import android.content.IntentSender
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentContainerView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.endeavride.endeavridedriver.R
import com.endeavride.endeavridedriver.databinding.FragmentMapsBinding
import com.endeavride.endeavridedriver.shared.NetworkUtils
import com.endeavride.endeavridedriver.shared.Utils
import com.endeavride.endeavridedriver.shared.Utils.isPermissionGranted
import com.endeavride.endeavridedriver.shared.Utils.requestPermission
import com.endeavride.endeavridedriver.ui.data.model.Ride
import com.endeavride.endeavridedriver.ui.ui.login.LoginViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class MapsFragment : Fragment(), GoogleMap.OnMarkerClickListener,
    ActivityCompat.OnRequestPermissionsResultCallback {

    companion object {
        /**
         * Request code for location permission request.
         *
         * @see .onRequestPermissionsResult
         */
        const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val REQUEST_CHECK_SETTINGS = 2
        private const val PLACE_PICKER_REQUEST = 3

        private const val TAG = "MapsFragment"
    }

    enum class OrderStatus(val value: Int)
    {
        DEFAULT(-1),
        UNASSIGNED(0),
        ASSIGNING(1),
        PICKING(2),
        ARRIVED_USER_LOCATION(3),
        STARTED(4),
        FINISHED(5),
        CANCELED(6);

        companion object {
            private val VALUES = values()
            fun from(value: Int) = VALUES.firstOrNull { it.value == value }
        }
    }

    enum class OrderType(val value: Int) {
        RIDE_SERVICE(0),
        HOME_SERVICE(1);

        companion object {
            private val VALUES = OrderType.values()
            fun from(value: Int) = VALUES.firstOrNull { it.value == value }
        }
    }

    //    private lateinit var progressBar: ProgressBar
//    private val adapter = PlacePredictionAdapter()
    private lateinit var viewModel: MapsViewModel
    private lateinit var loginViewModel: LoginViewModel

    private var permissionDenied = false
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var lastLocation: Location? = null

    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    private var _binding: FragmentMapsBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var requestButton: Button
    private lateinit var acceptButton: Button
    private lateinit var actionButton: Button
    private lateinit var textView: TextView
    private lateinit var buttonContainer: View

    private var dest: LatLng? = null
    private var customer: LatLng? = null
    private var needDirection = false
    private var offset = 0
    private var rid: String? = null

    private var status: OrderStatus = OrderStatus.DEFAULT
    private var type: OrderType = OrderType.RIDE_SERVICE
    private var isAutoPollingEnabled = false
    private var isPostingDriveRecord = false
    private var isAcceptingNewTask = true

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        map = googleMap
        enableMyLocation()

        viewModel.checkIfCurrentRideAvailable()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
        requestButton = binding.requestButton
        acceptButton = binding.acceptButton
        actionButton = binding.actionButton
        textView = binding.textView
        buttonContainer = binding.view
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, MapsViewModelFactory()).get(MapsViewModel::class.java)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        reloadData()

        viewModel.mapDirectionResult.observe(viewLifecycleOwner,
            Observer { path ->
                for (i in 0 until path.size) {
                    map.addPolyline(PolylineOptions().addAll(path[i]).color(Color.RED))
                }
            })

        viewModel.driveRecordResult.observe(viewLifecycleOwner, Observer { error ->
            error?.let { Log.e("Error", "Post drive record error: ${error.message}") }
            if (isPostingDriveRecord) {
                lastLocation?.let { viewModel.postDriveRecord(0, it) }
            }
        })

        viewModel.currentRide.observe(viewLifecycleOwner,
            Observer { ride ->
                println("#K_current ride $ride")
                if (!isAcceptingNewTask) {
                    isAcceptingNewTask = true
                }
                if (ride == null) {
                    // add mark and send request when app closed if currently requesting task
                    this.dest = null
                    this.customer = null
                    this.rid = null
                    setStatus(ride)
                    return@Observer
                }
                dest = Utils.decodeLocationString(ride.destination)
                customer = if (ride.user_location == null) {
                    null
                } else {
                    Utils.decodeLocationString(ride.user_location)
                }
                rid = ride.rid
                setStatus(ride)
            })

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                lastLocation.let {
                    if (isPostingDriveRecord && it != null) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(it.latitude, it.longitude), 12f))
                    }
                }
            }
        }
        createLocationRequest()
    }

    private fun setStatus(ride: Ride?) {
        val rideStatus = ride?.status ?: -1
        status = OrderStatus.from(rideStatus) ?: OrderStatus.DEFAULT

        reloadData()
    }

    private fun reloadData() {
        Log.d(TAG, "Reloading data with status: $status")
        if (status == OrderStatus.ASSIGNING) {
//            if (!isAcceptingNewTask) {
//                viewModel.requestAvailableRideTask(0, offset, rid, false)
//                return
//            }
            requestButton.text = "Change Task"
            acceptButton.text = "Accept Task"
            textView.text = if (type == OrderType.HOME_SERVICE) {
                "Home service task"
            } else {
                "Ride service task"
            }
            actionButton.isVisible = false
            requestButton.isVisible = true
            acceptButton.isVisible = true

            requestButton.setOnClickListener {
                map.clear()
                isAutoPollingEnabled = true
                offset += 1
                viewModel.requestAvailableRideTask(0, offset, rid)

                dest = null
                customer = null
                rid = null
            }

            acceptButton.setOnClickListener {
                if (rid == null) {
                    Toast.makeText(requireContext(), "No available Ride request to accept!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                startLocationUpdates()
                rid?.let { it1 -> viewModel.acceptRideRequest(it1, type.value) }
            }

            requestDirectionIfNeeded()
        } else if (status == OrderStatus.PICKING) {
            textView.text = "Start driving to user"
            actionButton.text = "Arrived at User Place"
            actionButton.isVisible = true
            requestButton.isVisible = false
            acceptButton.isVisible = false

            // start updating GPS to server
            if (!isPostingDriveRecord) {
                Log.d(TAG, "Start Posting GPS Records")
                isPostingDriveRecord = true
                try {
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

                    if (lastLocation == null) {
                        Log.e(TAG, "No last location found!")
                    }
                    lastLocation?.let { viewModel.postDriveRecord(0, it) }
                } catch (unlikely: SecurityException) {
                    Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
                }
            }

            actionButton.setOnClickListener {
                isPostingDriveRecord = false
                val removeTask = fusedLocationClient.removeLocationUpdates(locationCallback)
                removeTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Location Callback removed.")
                    } else {
                        Log.d(TAG, "Failed to remove Location Callback.")
                    }
                }
                //send arrived at user location request
                rid?.let { it1 -> viewModel.updateRideRequest(it1, OrderStatus.ARRIVED_USER_LOCATION.value) }
            }
            requestDirectionIfNeeded()
        } else if (status == OrderStatus.ARRIVED_USER_LOCATION) {
            textView.text = "Waiting user to get on ride"
            actionButton.text = "User Onboarded Start Driving"
            actionButton.isVisible = true
            requestButton.isVisible = false
            acceptButton.isVisible = false

            actionButton.setOnClickListener {
                //send start drive user to destination request
                rid?.let { it1 -> viewModel.updateRideRequest(it1, OrderStatus.STARTED.value) }
            }
            requestDirectionIfNeeded()
        } else if (status == OrderStatus.STARTED) {
            requestButton.text = "Start Driving User to Destination"
            requestButton.isClickable = false
            acceptButton.text = "Arrived at Destination"
            acceptButton.isEnabled = true
            textView.text = "Start driving to destination..."
            actionButton.text = "Arrived at Destination"
            actionButton.isVisible = true
            requestButton.isVisible = false
            acceptButton.isVisible = false

            // start updating GPS to server
            if (!isPostingDriveRecord) {
                Log.d(TAG, "Start Posting GPS Records")
                isPostingDriveRecord = true
                try {
                    fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

                    if (lastLocation == null) {
                        Log.e(TAG, "No last location found!")
                    }
                    lastLocation?.let { viewModel.postDriveRecord(0, it) }
                } catch (unlikely: SecurityException) {
                    Log.e(TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
                }
            }

            actionButton.setOnClickListener {
                isPostingDriveRecord = false
                val removeTask = fusedLocationClient.removeLocationUpdates(locationCallback)
                removeTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Location Callback removed.")
                    } else {
                        Log.d(TAG, "Failed to remove Location Callback.")
                    }
                }
                //send finish task request
                rid?.let { it1 -> viewModel.updateRideRequest(it1, OrderStatus.FINISHED.value) }
            }
            requestDirectionIfNeeded()
        } else if (status == OrderStatus.FINISHED) {
            Toast.makeText(requireContext(), "Good Job!!", Toast.LENGTH_SHORT).show()
            map.clear()
            resetAttributes()
            defaultStatusActions()
        } else if (status == OrderStatus.CANCELED) {
            Toast.makeText(requireContext(), "User has canceled the request!", Toast.LENGTH_SHORT).show()
            map.clear()
            defaultStatusActions()
        } else {
            // DEFAULT
            defaultStatusActions()
        }
    }

    private fun resetAttributes() {
        isAutoPollingEnabled = false
        needDirection = true
        offset = 0
        rid = null
        dest = null
        customer = null
    }

    private fun defaultStatusActions() {
        isPostingDriveRecord = false
        needDirection = true
        rid = null
        dest = null
        customer = null
        if (isAutoPollingEnabled) {
            requestButton.text = "Requesting..."
            acceptButton.text = "Stop"
            textView.text = "Requesting..."
            actionButton.isVisible = false
            requestButton.isVisible = true
            acceptButton.isVisible = true

            requestButton.setOnClickListener {
                map.clear()
                isAutoPollingEnabled = true
                viewModel.requestAvailableRideTask(0, offset, rid)
            }

            acceptButton.setOnClickListener {
                map.clear()
                resetAttributes()
                isAutoPollingEnabled = false
                isAcceptingNewTask = false
                reloadData()
            }

            viewModel.requestAvailableRideTask(3000, offset, rid)
        } else {
            actionButton.text = "Request Tasks"
            textView.text = "Click 'Request Task' to start"
            actionButton.isVisible = true
            requestButton.isVisible = false
            acceptButton.isVisible = false
            actionButton.isEnabled = isAcceptingNewTask

            actionButton.setOnClickListener {
                map.clear()
                isAutoPollingEnabled = true
                viewModel.requestAvailableRideTask(0, offset, rid)
            }
        }
    }

    private fun requestDirection() {
        if (lastLocation == null) {
            needDirection = true
            return
        }
        dest?.let { it1 -> lastLocation?.let { LatLng(it.latitude, it.longitude) }?.let {
            needDirection = false
            placeMarkerOnMap(it1, "destination")
            if (customer != null) {
                placeMarkerOnMap(customer!!, "customer")
            }
            viewModel.getDirection(
                it, it1, customer
            )
        } }
    }

    private fun requestDirectionIfNeeded() {
        if (dest != null && needDirection) {
            requestDirection()
        }
    }

    override fun onResume() {
        super.onResume()
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
//            showMissingPermissionError()
            permissionDenied = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        println("#K_app map fragment on destroy view")
    }

    override fun onStop() {
        super.onStop()
        println("#K_app map fragment on stop")
    }

    override fun onDestroy() {
        println("#K_app map fragment on destroy")
        super.onDestroy()
    }

    // MARK: search place by address
//    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
//        super.onCreateOptionsMenu(menu, inflater)
//        Log.d("Map", "#K_onCreateOptionsMenu")
//        val searchView = menu.findItem(R.id.search).actionView as SearchView
//        searchView.apply {
//            queryHint = getString(R.string.search_a_place)
//            isIconifiedByDefault = false
//            isFocusable = true
//            isIconified = false
//            requestFocusFromTouch()
//            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//                override fun onQueryTextSubmit(query: String): Boolean {
//                    return false
//                }
//
//                override fun onQueryTextChange(newText: String): Boolean {
//                    viewModel.onSearchQueryChanged(newText)
//                    return true
//                }
//            })
//        }
//    }

//    private fun initRecyclerView() {
//        val linearLayoutManager = LinearLayoutManager(this)
//        findViewById<RecyclerView>(R.id.recycler_view).apply {
//            layoutManager = linearLayoutManager
////            adapter = this@PlacesSearchDemoActivity.adapter
//            addItemDecoration(
//                DividerItemDecoration(
//                    this@PlacesSearchDemoActivity,
//                    linearLayoutManager.orientation
//                )
//            )
//        }
//    }

    /**
     * Enables the My Location layer if the fine location permission has been granted.
     */
    private fun enableMyLocation() {
        if (!::map.isInitialized) return
        if (context == null) {
            return
        }
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing. Show rationale and request permission
            requestPermission(requireActivity(), LOCATION_PERMISSION_REQUEST_CODE,
                Manifest.permission.ACCESS_FINE_LOCATION, true
            )

            Log.d("TAG", "#K_permission granting")
            return
        }
        Log.d("TAG", "#K_permission granted")
        map.isMyLocationEnabled = true
        map.mapType = GoogleMap.MAP_TYPE_NORMAL

        fusedLocationClient.lastLocation.addOnSuccessListener(requireActivity()) { location ->
            // Got last known location. In some rare situations this can be null.
            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
//                placeMarkerOnMap(currentLatLng)
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))
//                map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f))

//                requestDirectionIfNeeded()
            }
        }
    }

    private fun placeMarkerOnMap(location: LatLng, title: String?) {
        val markerOptions = MarkerOptions().position(location)

        val titleStr = getAddress(location)  // add these two lines
        markerOptions.title(title ?: titleStr)

        map.addMarker(markerOptions)

        dest = location
    }

    override fun onMarkerClick(p0: Marker): Boolean {
        TODO("Not yet implemented")
    }

    private fun getAddress(latLng: LatLng): String {
        // 1
        if (context == null) {
            return ""
        }
        val geocoder = Geocoder(requireContext())
        val addresses: List<Address>?
        val address: Address?
        var addressText = ""

        try {
            // 2
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            // 3
            if (null != addresses && !addresses.isEmpty()) {
                address = addresses[0]
                for (i in 0 until address.maxAddressLineIndex) {
                    addressText += if (i == 0) address.getAddressLine(i) else "\n" + address.getAddressLine(i)
                }
            }
        } catch (e: IOException) {
            Log.e("MapsActivity", e.localizedMessage)
        }

        return addressText
    }

    private fun startLocationUpdates() {
        if (context == null) {
            return
        }
        if (ActivityCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    private fun createLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(requireActivity())
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            if (e is ResolvableApiException) {
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    e.startResolutionForResult(requireActivity(),
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    // [START maps_check_location_permission_result]
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return
        }
        if (isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation()
        } else {
            // Permission was denied. Display an error message
            // [START_EXCLUDE]
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true
            // [END_EXCLUDE]
        }
    }

    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
//    private fun showMissingPermissionError() {
//        newInstance(true).show(supportFragmentManager, "dialog")
//    }
}