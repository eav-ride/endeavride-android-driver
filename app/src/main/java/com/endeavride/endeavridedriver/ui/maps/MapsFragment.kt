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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.endeavride.endeavridedriver.R
import com.endeavride.endeavridedriver.databinding.FragmentMapsBinding
import com.endeavride.endeavridedriver.shared.NetworkUtils
import com.endeavride.endeavridedriver.shared.Utils
import com.endeavride.endeavridedriver.shared.Utils.isPermissionGranted
import com.endeavride.endeavridedriver.shared.Utils.requestPermission
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

    private var dest: LatLng? = null
    private var customer: LatLng? = null
    private var needDirection = false
    private var offset = 0
    private var rid: String? = null

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
//        map.setOnMapLongClickListener {
//            map.clear()
//            placeMarkerOnMap(it)
//        }
        enableMyLocation()

//        viewModel.checkIfCurrentRideAvailable()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapsBinding.inflate(inflater, container, false)
//        progressBar = binding.progressBar
//        binding.toolbar.inflateMenu(R.menu.search_place_menu)
//        setHasOptionsMenu(true)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this, MapsViewModelFactory()).get(MapsViewModel::class.java)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
        binding.acceptButton.isClickable = false

//        initRecyclerView()
//        viewModel.events.observe(viewLifecycleOwner, Observer { event ->
//            when(event) {
//                is PlacesSearchEventLoading -> {
//                    progressBar.isIndeterminate = true
//                }
//                is PlacesSearchEventError -> {
//                    progressBar.isIndeterminate = false
//                }
//                is PlacesSearchEventFound -> {
//                    progressBar.isIndeterminate = false
//                    adapter.setPredictions(event.places)
//                }
//            }
//            Log.d("Map", "#K_$event")
//        })

        viewModel.mapDirectionResult.observe(viewLifecycleOwner,
            Observer { path ->
                for (i in 0 until path.size) {
                    map.addPolyline(PolylineOptions().addAll(path[i]).color(Color.RED))
                }
            })

        viewModel.currentRide.observe(viewLifecycleOwner,
            Observer { ride ->
                println("#K_current ride $ride")
                if (ride == null) {
                    // add mark and send request when app closed if currently requesting task
                    return@Observer
                }
                ride ?: return@Observer
                val dest = Utils.decodeRideDirection(ride.direction)
                val customer = Utils.decodeRideSource(ride.direction)
                if (dest != null && customer != null) {
                    placeMarkerOnMap(dest, "destination")
                    placeMarkerOnMap(customer, "customer")
                    this.dest = dest
                    this.customer = customer
                    this.rid = ride.rid
                    this.offset += 1
                    binding.acceptButton.isClickable = true
                    requestDirection()
                }
            })

        binding.requestButton.setOnClickListener {
            map.clear()
//            dest = null
            viewModel.requestAvailableRideTask(offset, rid)
        }

        binding.acceptButton.setOnClickListener {
            Log.d("Debug", "#K_send driver request with points $lastLocation and $dest")
//            dest?.let { it1 -> NetworkUtils.user?.userId?.let { it2 ->
//                lastLocation?.let { it3 -> LatLng(it3.latitude, it3.longitude) }?.let { it4 ->
//                    viewModel.createRide(
//                        it4, it1,
//                        it2
//                    )
//                }
//            } }
//            Toast.makeText(requireContext(), "send driver request with points $lastLocation and $dest", Toast.LENGTH_SHORT).show()
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
//                placeMarkerOnMap(LatLng(lastLocation.latitude, lastLocation.longitude))

                if (needDirection) {
                    needDirection = false
                    requestDirection()
                }
            }
        }
        createLocationRequest()
    }

    private fun requestDirection() {
        if (lastLocation == null) {
            needDirection = true
            return
        }
        dest?.let { it1 -> lastLocation?.let { LatLng(it.latitude, it.longitude) }?.let {
            customer?.let { it2 ->
                viewModel.getDirection(
                    it, it1, it2
                )
            }
        } }
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

                if (needDirection) {
                    needDirection = false
                    requestDirection()
                }
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