package fr.android.parkingspotter.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import fr.android.parkingspotter.R;
import fr.android.parkingspotter.utils.LocaleHelper;

public class SettingsFragment extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        RadioGroup radioGroup = view.findViewById(R.id.radio_group_language);
        RadioButton radioFr = view.findViewById(R.id.radio_fr);
        RadioButton radioEn = view.findViewById(R.id.radio_en);

        // Sélectionne la langue courante
        String lang = LocaleHelper.onAttach(requireContext()).getResources().getConfiguration().locale.getLanguage();
        if (lang.equals("fr")) {
            radioFr.setChecked(true);
        } else {
            radioEn.setChecked(true);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newLang = checkedId == R.id.radio_fr ? "fr" : "en";
            Context context = LocaleHelper.setLocale(requireContext(), newLang);
            requireActivity().recreate();
        });
        return view;
    }
}
