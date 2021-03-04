package com.example.reconhecedordetextos.result

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.example.reconhecedordetextos.LanguageCodeFactory
import com.example.reconhecedordetextos.R
//import com.example.reconhecedordetextos.ResultFragmentArgs
import com.example.reconhecedordetextos.databinding.FragmentResultBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import java.io.File
import java.io.IOException
import java.util.*

class ResultFragment:Fragment() {

    private var _binding:FragmentResultBinding? = null
    private val binding get() = _binding!!

    private lateinit var clip:ClipData
    private lateinit var clipboard:ClipboardManager

    private var contextM:Context? = null
    private lateinit var customAlertDialogView: View

    val args: ResultFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentResultBinding.inflate(layoutInflater , container ,false)

        if (args.resultText != null){

            clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

            clip = ClipData.newPlainText("simple text" , args.resultText)

            binding.txtResult.text = args.resultText
        }else{
            binding.btnCopy.isEnabled = false
            binding.txtResult.text = "Texto não identificado"
            binding.btnTranslate.isEnabled = false
        }

        if (args.photoPath != null){
            var imageBit = BitmapFactory.decodeFile(args.photoPath )

            val exif = ExifInterface(args.photoPath!!)
            val orientation: Int = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

            val bmRotated = rotateBitmap(imageBit , orientation)
            binding.imageView2.setImageBitmap(bmRotated)
        }else{
            Toast.makeText(context , "Não foi possível recuperar a foto" , Toast.LENGTH_LONG).show()
        }

        binding.btnCopy.setOnClickListener {

            clipboard.setPrimaryClip(clip)

        }

        binding.btnTranslate.setOnClickListener {

            customAlertDialogView = LayoutInflater.from(contextM)
                    .inflate(R.layout.dialog_language_options , null , false)

            openDialog()

        }

        return binding.root

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

    fun openDialog(){

        Log.i("stringLanguage" , TranslateLanguage.ENGLISH)
        val inputEnterLanguage = customAlertDialogView.findViewById<TextInputLayout>(R.id.text_enter_language)
        val inputExitLanguage = customAlertDialogView.findViewById<TextInputLayout>(R.id.text_exit_language)

        val enterList = listOf("Identificar Língua" , "Inglês" , "Português" , "Espanhol" , "Francês" , "Italiano" , "Chinês" , "Japonês")
        val exitList = listOf( "Inglês" , "Português" , "Espanhol" , "Francês" , "Italiano" , "Chinês" , "Japonês")

        val adapterEnter = ArrayAdapter(requireContext() , R.layout.list_item , enterList)
        (inputEnterLanguage.editText!! as? AutoCompleteTextView)?.setAdapter(adapterEnter)

        val adapterExit = ArrayAdapter(requireContext() , R.layout.list_item , exitList)
        (inputExitLanguage.editText!! as? AutoCompleteTextView)?.setAdapter(adapterExit)




        MaterialAlertDialogBuilder(contextM!!)
                .setView(customAlertDialogView)
            .setPositiveButton("Traduzir"){dialog, which ->
                val enterLanguage = (inputEnterLanguage.editText!! as? AutoCompleteTextView)?.text.toString()
                val exitLanguage = (inputExitLanguage.editText!! as? AutoCompleteTextView)?.text.toString()
                Log.i("teste" , "$enterLanguage ++ $exitLanguage")

                if (enterLanguage.isNotEmpty() && exitLanguage.isNotEmpty()){
                    if (inputEnterLanguage.editText!!.text.toString() == "Identificar Língua"){
                        indentifyLanguage(LanguageCodeFactory.languageCodeIdentify(exitLanguage))
                    }else{
                        val enterLanguageCode = LanguageCodeFactory.languageCodeIdentify(enterLanguage)
                        val exitLanguageCode = LanguageCodeFactory.languageCodeIdentify(exitLanguage)
                        Log.i("teste" , "$enterLanguageCode ++ $exitLanguageCode")


                        translate(enterLanguageCode , exitLanguageCode)
                    }
                }else{
                    Snackbar.make(binding.root , "Preencha a linguagem de entrada e saída",  Snackbar.LENGTH_SHORT)
                            .setBackgroundTint(resources.getColor(R.color.purple_500))
                            .show()
                }

            }
                .show()

    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        contextM = context
    }

    private fun translate(sourceLanguage:String , targetLanguage:String){
        binding.progressBar2.visibility = View.VISIBLE
        binding.btnTranslate.isEnabled = false

        val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage)
                .setTargetLanguage(targetLanguage)
                .build()
        val englishPortugueseTranslation = Translation.getClient(options)

        val conditions = DownloadConditions.Builder()
                .requireWifi()
                .build()

        englishPortugueseTranslation.downloadModelIfNeeded(conditions)
                .addOnSuccessListener {
                    englishPortugueseTranslation.translate(args.resultText!!)
                            .addOnSuccessListener {textTrad->
                                binding.progressBar2.visibility = View.GONE
                                binding.btnTranslate.isEnabled = true
                                binding.txtResult.text = textTrad
                            }
                            .addOnFailureListener {
                                Toast.makeText(context , "houve um erro" , Toast.LENGTH_LONG).show()
                            }
                }
                .addOnFailureListener {
                    Toast.makeText(context , "Desculpe, houve um erro" , Toast.LENGTH_LONG).show()
                }
    }

    private fun indentifyLanguage(exitLanguage:String){
        binding.progressBar2.visibility = View.VISIBLE
        binding.btnTranslate.isEnabled = false
        val languageIdentifier = LanguageIdentification.getClient()
        languageIdentifier.identifyLanguage(args.resultText!!)
                .addOnSuccessListener { languageCode ->
                    if (languageCode == "und") {
                        Log.i("TAG", "Can't identify language.")
                    } else {
                        Log.i("TAG", "Language: $languageCode")
                        translate(languageCode  , exitLanguage)
                    }

                }
                .addOnFailureListener {
                    // Model couldn’t be loaded or other internal error.
                    // ...

                }

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}