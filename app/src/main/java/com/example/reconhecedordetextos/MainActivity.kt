package com.example.reconhecedordetextos

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.example.reconhecedordetextos.databinding.ActivityMainBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import pub.devrel.easypermissions.EasyPermissions
import pub.devrel.easypermissions.PermissionRequest
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding:ActivityMainBinding

    private lateinit var currentPhotoPath:String
    private val REQUEST_TAKE_PHOTO = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val navController = this.findNavController(R.id.myNavHostFragment)

        NavigationUI.setupActionBarWithNavController(this, navController)

        askCameraPermission()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.myNavHostFragment)
        return navController.navigateUp()
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager!!)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                            applicationContext!!,
                            "com.android.fileprovider",
                            it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_TAKE_PHOTO ){
            if (resultCode == Activity.RESULT_OK){
                val image:InputImage
                try {
                    image = InputImage.fromFilePath(this , Uri.fromFile(File(currentPhotoPath)))

                    val recognizer = TextRecognition.getClient()

                    val result = recognizer.process(image)
                            .addOnSuccessListener {visionText->
                                val resultText = visionText.text

                                Log.i("resultTextVision" , resultText)
                            }
                            .addOnFailureListener{
                                Log.i("resultTextVision" , it.message!!)
                            }

                }catch (e:IOException){
                    e.printStackTrace()
                }
            }
        }
    }
    override fun onRequestPermissionsResult (
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        /* Encaminhando resultados para EasyPermissions API */
        EasyPermissions.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults,
                this )
    }

    private fun askCameraPermission(){
        EasyPermissions.requestPermissions(
                PermissionRequest.Builder( this, 1, Manifest.permission.CAMERA )
                        .setRationale( "A permissão de uso de câmera é necessária para que o aplicativo funcione." )
                        .setPositiveButtonText( "Ok" )
                        .setNegativeButtonText( "Cancelar" )
                        .build() )

        EasyPermissions.requestPermissions(
                PermissionRequest.Builder( this, 2, Manifest.permission.WRITE_EXTERNAL_STORAGE )
                        .setRationale( "A permissão de uso de memória é necessária para que o aplicativo funcione." )
                        .setPositiveButtonText( "Ok" )
                        .setNegativeButtonText( "Cancelar" )
                        .build() )
    }
    fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap? {
        return try {
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_NORMAL -> return bitmap
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
                    matrix.setRotate(180f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    matrix.setRotate(90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    matrix.setRotate(-90f)
                    matrix.postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
                else -> return bitmap
            }
            try {
                val bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                bmRotated
            } catch (e: OutOfMemoryError) {
                e.printStackTrace()
                null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
        return bitmap
    }
}