package fr.android.projectandroid.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView; // Import AdapterView
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
// Assuming AppPreferences.kt is in a package accessible from here, e.g., fr.android.projectandroid.utils
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import fr.android.projectandroid.R;
import fr.android.projectandroid.utils.AppPreferences; // Adjust if AppPreferences is elsewhere
import fr.android.projectandroid.utils.LocaleHelper;

public class SettingsFragment extends Fragment {

    private Spinner languagesDropdown;
    private boolean isInitialLanguageSpinnerSetup = true;
    private Spinner themesDropdown;
    private TextView themesItemsTextView;
    private boolean isInitialThemeSpinnerSetup = true;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        languagesDropdown = view.findViewById(R.id.languages_dropdown);
        themesDropdown = view.findViewById(R.id.themes_dropdown);
        themesItemsTextView = view.findViewById(R.id.themes_items);

        // --- Configuration du Spinner de Langues ---
        ArrayAdapter<CharSequence> languageAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.languages, android.R.layout.simple_spinner_item);

        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languagesDropdown.setAdapter(languageAdapter);

        isInitialLanguageSpinnerSetup = true;
        // Set the spinner to the previously saved language
        AppPreferences.INSTANCE.setSpinnerToSavedLanguage(requireContext(), languagesDropdown);

        languagesDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
                if (isInitialLanguageSpinnerSetup) {
                    Log.d("SettingsFragment", "Initial spinner setup. Not recreating activity.");
                    isInitialLanguageSpinnerSetup = false;
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

        // --- Configuration du Spinner de Thèmes ---
        ArrayAdapter<CharSequence> themeAdapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.themes, android.R.layout.simple_spinner_item); // Assurez-vous d'avoir R.array.themes_array
        themeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        themesDropdown.setAdapter(themeAdapter);

        // Réinitialiser le drapeau avant de définir la sélection pour éviter le recreate() initial
        isInitialThemeSpinnerSetup = true;
        // Utilisez votre méthode AppPreferences pour définir le thème sauvegardé sur le spinner
        AppPreferences.INSTANCE.setSpinnerToSavedTheme(requireContext(), themesDropdown);


        themesDropdown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedThemeName = parent.getItemAtPosition(position).toString();

                if (isInitialThemeSpinnerSetup) {
                    isInitialThemeSpinnerSetup = false;
                    Log.d("SettingsFragment", "Initial theme spinner setup. Not changing theme delegate.");
                    return;
                }

                int themeModeToSet;
                switch (position) {
                    case 0:
                        themeModeToSet = AppCompatDelegate.MODE_NIGHT_NO;
                        break;
                    case 1:
                        themeModeToSet = AppCompatDelegate.MODE_NIGHT_YES;
                        break;
                    case 2:
                    default:
                        themeModeToSet = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                        break;
                }

                AppPreferences.INSTANCE.saveThemeMode(requireContext(), themeModeToSet);

                int currentNightMode = AppCompatDelegate.getDefaultNightMode();
                if (currentNightMode != themeModeToSet) {
                    AppCompatDelegate.setDefaultNightMode(themeModeToSet);
                    Log.d("SettingsFragment", "Theme selected: " + selectedThemeName + ". Recreating activity.");
                    requireActivity().recreate(); // Décommentez pour un effet immédiat
                } else {
                    Log.d("SettingsFragment", "Theme selected: " + selectedThemeName + ". No change needed.");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Interface callback
            }
        });

        return view;
    }
}