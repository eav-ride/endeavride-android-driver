package com.endeavride.endeavridedriver.shared

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.endeavride.endeavridedriver.R
import com.endeavride.endeavridedriver.ui.ui.login.LoginFragment
import com.google.android.gms.maps.model.LatLng

internal object Utils {
    fun decodeLocationString(location: String):LatLng? {
        val point = location.split(",")
        if (point.size != 2) return null
        return LatLng(point[0].toDouble(), point[1].toDouble())
    }
    /**
     * Requests the fine location permission. If a rationale with an additional explanation should
     * be shown to the user, displays a dialog that triggers the request.
     */
    @JvmStatic
    fun requestPermission(
        activity: FragmentActivity, requestId: Int,
        permission: String, finishActivity: Boolean
    ) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            // Display a dialog with rationale.
            RationaleDialog.newInstance(requestId, finishActivity)
                .show(activity.supportFragmentManager, "dialog")
        } else {
            // Location permission has not been granted yet, request it.
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permission),
                requestId
            )
        }
    }

    /**
     * Checks if the result contains a [PackageManager.PERMISSION_GRANTED] result for a
     * permission from a runtime permissions request.
     *
     * @see androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback
     */
    @JvmStatic
    fun isPermissionGranted(
        grantPermissions: Array<String>, grantResults: IntArray,
        permission: String
    ): Boolean {
        for (i in grantPermissions.indices) {
            if (permission == grantPermissions[i]) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED
            }
        }
        return false
    }

    /**
     * A dialog that explains the use of the location permission and requests the necessary
     * permission.
     *
     *
     * The activity should implement
     * [androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback]
     * to handle permit or denial of this permission request.
     */
    class RationaleDialog : DialogFragment() {
        private var finishActivity = false
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val requestCode =
                arguments?.getInt(ARGUMENT_PERMISSION_REQUEST_CODE) ?: 0
            finishActivity =
                arguments?.getBoolean(ARGUMENT_FINISH_ACTIVITY) ?: false
            return AlertDialog.Builder(activity)
                .setMessage(R.string.permission_rationale_location)
                .setPositiveButton(android.R.string.ok) { dialog, which -> // After click on Ok, request the permission.
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        requestCode
                    )
                    // Do not finish the Activity while requesting permission.
                    finishActivity = false
                }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        }

        override fun onDismiss(dialog: DialogInterface) {
            super.onDismiss(dialog)
            if (finishActivity) {
                Toast.makeText(
                    activity,
                    R.string.permission_required_toast,
                    Toast.LENGTH_SHORT
                ).show()
                activity?.finish()
            }
        }

        companion object {
            private const val ARGUMENT_PERMISSION_REQUEST_CODE = "requestCode"
            private const val ARGUMENT_FINISH_ACTIVITY = "finish"

            /**
             * Creates a new instance of a dialog displaying the rationale for the use of the location
             * permission.
             *
             *
             * The permission is requested after clicking 'ok'.
             *
             * @param requestCode    Id of the request that is used to request the permission. It is
             * returned to the
             * [androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback].
             * @param finishActivity Whether the calling Activity should be finished if the dialog is
             * cancelled.
             */
            fun newInstance(requestCode: Int, finishActivity: Boolean): RationaleDialog {
                val arguments = Bundle().apply {
                    putInt(ARGUMENT_PERMISSION_REQUEST_CODE, requestCode)
                    putBoolean(ARGUMENT_FINISH_ACTIVITY, finishActivity)
                }
                return RationaleDialog().apply {
                    this.arguments = arguments
                }
            }
        }
    }

    /**
     * Provides access to SharedPreferences for location to Activities and Services.
     */

    const val KEY_FOREGROUND_ENABLED = "tracking_foreground_location"

    /**
     * Returns true if requesting location updates, otherwise returns false.
     *
     * @param context The [Context].
     */
    fun getStringPref(context: Context, key: String): String? =
        context.getSharedPreferences(
            key, Context.MODE_PRIVATE)
            .getString(KEY_FOREGROUND_ENABLED, null)

    /**
     * Stores the location updates state in SharedPreferences.
     * @param requestingLocationUpdates The location updates state.
     */
    fun saveStringPref(context: Context, value: String, key: String) =
        context.getSharedPreferences(
            key,
            Context.MODE_PRIVATE).edit {
            putString(KEY_FOREGROUND_ENABLED, value).commit()
        }
}
