package fr.android.projectandroid.utils

// File: AppPreferences.kt (e.g., in a 'utils' or 'data' package)
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.widget.Spinner

object AppPreferences {
    private const val PREFS_NAME =
        "fr.android.projectandroid.settings_prefs" // Choose a unique name
    private const val KEY_SELECTED_LANGUAGE = "selected_language"
    private const val DEFAULT_LANGUAGE = "en" // Set a default language

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSelectedLanguage(context: Context, languageCode: String) {
        val editor = getSharedPreferences(context).edit()
        Log.d("AppPreferences", "Saving language: $languageCode")
        editor.putString(KEY_SELECTED_LANGUAGE, languageCode)
        editor.apply() // Use apply() for asynchronous saving
    }

    fun getSelectedLanguage(context: Context): String {
        Log.d("AppPreferences", "Getting selected language " + KEY_SELECTED_LANGUAGE)
        return getSharedPreferences(context).getString(KEY_SELECTED_LANGUAGE, DEFAULT_LANGUAGE)
            ?: DEFAULT_LANGUAGE
    }

    // Optional: Function to set the spinner to the saved language
    fun setSpinnerToSavedLanguage(context: Context, spinner: Spinner) {
        val savedLanguage = getSelectedLanguage(context)
        val languageFullName: String = when (savedLanguage) {
            "en" -> "English" // These must match exactly what's in R.array.languages
            "fr" -> "Francais"
            "es" -> "Espanol"
            "it" -> "Italiano"
            "de" -> "Deustch" // Assuming this is German, often "Deutsch"
            else -> "English" // Default full name
        }
        Log.i("AppPreferences", "Setting spinner to saved full name: $languageFullName")

        val adapter = spinner.adapter
        if (adapter != null) {
            var found = false
            for (i in 0 until adapter.count) {
                // Direct comparison since both are expected to be full names
                // Using ignoreCase = true is good practice for user-facing strings
                if (adapter.getItem(i).toString().equals(languageFullName, ignoreCase = true)) {
                    spinner.setSelection(i)
                    Log.d("AppPreferences", "Spinner item found and selected at position $i for '$savedLanguage'")
                    found = true
                    break // Exit once found
                }
            }
            if (!found) {
                Log.w("AppPreferences", "Saved language full name '$savedLanguage' not found in spinner adapter. Ensure R.array.languages items match.")
            }
        } else {
            Log.e("AppPreferences", "Spinner adapter is null in setSpinnerToSavedLanguage!")
        }
    }
}