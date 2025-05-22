package fr.android.projectandroid.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView; // Import AdapterView
import android.widget.ArrayAdapter;
import android.widget.Spinner;
// Assuming AppPreferences.kt is in a package accessible from here, e.g., fr.android.projectandroid.utils
import androidx.fragment.app.Fragment;

import fr.android.projectandroid.R;
import fr.android.projectandroid.utils.AppPreferences; // Adjust if AppPreferences is elsewhere
import fr.android.projectandroid.utils.LocaleHelper;

public class SettingsFragment extends Fragment {

    private Spinner dropdownMenu;
    private boolean isInitialSpinnerSetup = true; // Instance variable

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        dropdownMenu = view.findViewById(R.id.languages_dropdown); // Make sure this ID matches your Spinner in XML

        // Create an ArrayAdapter using the string array and a default spinner layout
        // Assuming R.array.dropdown_items exists in your strings.xml or arrays.xml
        // <string-array name="dropdown_items">
        // <item>English</item>
        // <item>Spanish</item>
        // ...
        // </string-array>
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.languages, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        dropdownMenu.setAdapter(adapter);

        // Set the spinner to the previously saved language
        AppPreferences.INSTANCE.setSpinnerToSavedLanguage(requireContext(), dropdownMenu); // Access Kotlin object via INSTANCE

        dropdownMenu.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguageFullName = parent.getItemAtPosition(position).toString();
                String languageCodeToSet; // e.g., "en", "fr", "es"

                Log.d("SettingsFragment", "Selected language full name: " + selectedLanguageFullName);
                // --- You need a mapping from full language name to language code ---
                // This is a simple example; you might want a more robust way if you have many languages
                switch (selectedLanguageFullName) {
                    case "English":
                        languageCodeToSet = "en";
                        break;
                    case "Francais":
                        languageCodeToSet = "fr";
                        break;
                    case "Espanol":
                        languageCodeToSet = "es";
                        break;
                    case "Italiano":
                        languageCodeToSet = "it";
                        break;
                    case "Deustch":
                        languageCodeToSet = "de";
                        break;
                    default:
                        languageCodeToSet = "en";
                        break;
                }

                // Save the preference (you are already doing this)
                AppPreferences.INSTANCE.saveSelectedLanguage(requireContext(), languageCodeToSet); // Save the code

                // Apply the locale change
                LocaleHelper.INSTANCE.setLocale(requireActivity(), languageCodeToSet); // Use requireActivity() for context

                // --- IMPORTANT: You usually need to recreate the Activity for changes to take full effect ---
                // This will reload all UI elements with the new language's strings.
                if (isInitialSpinnerSetup) {
                    Log.d("SettingsFragment", "Initial spinner setup. Not recreating activity.");
                    isInitialSpinnerSetup = false;
                } else {
                    Log.d("SettingsFragment", "Selected language: " + parent.getItemAtPosition(position));
                    requireActivity().recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        return view;
    }
}