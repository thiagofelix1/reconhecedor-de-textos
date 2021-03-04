package com.example.reconhecedordetextos

import com.google.mlkit.nl.translate.TranslateLanguage

class LanguageCodeFactory {

    companion object{
        fun languageCodeIdentify(language:String):String{
            val languageMap = mapOf(
                    "Inglês" to TranslateLanguage.ENGLISH,
                    "Português" to TranslateLanguage.PORTUGUESE,
                    "Espanhol" to TranslateLanguage.SPANISH,
                    "Francês" to TranslateLanguage.FRENCH,
                    "Italiano" to TranslateLanguage.ITALIAN,
                    "Chinês" to TranslateLanguage.CHINESE,
                    "Japonês" to TranslateLanguage.JAPANESE
            )

            return languageMap[language].toString()
        }
    }

}