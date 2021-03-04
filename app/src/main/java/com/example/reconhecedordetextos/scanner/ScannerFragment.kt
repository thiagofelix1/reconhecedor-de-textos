package com.example.reconhecedordetextos.scanner

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
//import com.example.reconhecedordetextos.ScannerFragmentDirections
import com.example.reconhecedordetextos.databinding.FragmentScannerBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class ScannerFragment:Fragment() {

    private var _binding:FragmentScannerBinding? = null
    val binding get() = _binding!!

    private var currentPhotoPath:String? = null

    private var REQUEST_TAKE_PHOTO = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentScannerBinding.inflate(layoutInflater , container , false)

        binding.BtnTakePhoto.setOnClickListener {
            dispatchTakePictureIntent()
        }

        binding.BtnTakePhoto.setOnClickListener {

            binding.progressBar.visibility = View.VISIBLE

            dispatchTakePictureIntent()
        }

        return binding.root
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = activity?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
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
            takePictureIntent.resolveActivity(activity?.packageManager!!)?.also {
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
                        activity?.applicationContext!!,
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

                binding.txtProgress.visibility = View.VISIBLE

                val image: InputImage

                try {
                    image = InputImage.fromFilePath(context?.applicationContext!! , Uri.fromFile(File(currentPhotoPath!!)))

                    val recognizer = TextRecognition.getClient()

                    recognizer.process(image)
                        .addOnSuccessListener {visionText->
                            val resultText = visionText.text

                           findNavController().navigate(ScannerFragmentDirections.actionScannerFragmentToResultFragment(resultText, currentPhotoPath ))

                            Log.i("resultTextVision" , resultText)
                        }
                        .addOnFailureListener{
                            findNavController().navigate(ScannerFragmentDirections.actionScannerFragmentToResultFragment(null, currentPhotoPath ))
                            Log.i("resultTextVision" , it.message!!)
                        }

                }catch (e:IOException){
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}