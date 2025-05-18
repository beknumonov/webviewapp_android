package com.beknumonov.webviewapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.afollestad.materialdialogs.MaterialDialog

class UiHelper {
    private val CAMERA_STORAGE_REQUEST_CODE = 611
    private val ONLY_CAMERA_REQUEST_CODE = 612
    private val ONLY_STORAGE_REQUEST_CODE = 613

    fun showImagePickerDialog(
        callingClassContext: Context,
        imagePickerLister: IImagePickerLister
    ) {
        MaterialDialog.Builder(callingClassContext)
            .items(R.array.imagePicker).title(R.string.app_name)
            .canceledOnTouchOutside(true)
            .itemsCallback { dialog, itemView, position, text ->
                if (position == 0) {
                    imagePickerLister.onOptionSelected(ImagePickerEnum.FROM_GALLERY)
                } else if (position == 1) imagePickerLister.onOptionSelected(
                    ImagePickerEnum.FROM_CAMERA
                )
                dialog.dismiss()
            }.show()
    }

    fun toast(context: Context?, content: String?) {
        Toast.makeText(context, content, Toast.LENGTH_LONG).show()
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    fun checkSelfPermissions(activity: Activity): Boolean {
        if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && activity.checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                CAMERA_STORAGE_REQUEST_CODE
            )
            return false
        } else if (activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                ONLY_CAMERA_REQUEST_CODE
            )
            return false
        } else if (activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                ONLY_STORAGE_REQUEST_CODE
            )
            return false
        }
        return true
    }
}
