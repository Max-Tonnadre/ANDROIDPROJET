package fr.android.projectandroid.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import fr.android.projectandroid.R;
import fr.android.projectandroid.TakePhotoActivity;

public class TakePhotoFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_take_photo, container, false);

        Button takePhotoButton = view.findViewById(R.id.btnTakePhoto);
        takePhotoButton.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), TakePhotoActivity.class);
            startActivity(intent);
        });

        return view;
    }
}