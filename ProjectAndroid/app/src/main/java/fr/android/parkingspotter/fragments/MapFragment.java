package fr.android.parkingspotter.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import fr.android.parkingspotter.R;
import fr.android.parkingspotter.database.ParkingDatabaseHelper;
import fr.android.parkingspotter.firebase.SupabaseDatabaseManager;
import fr.android.parkingspotter.models.ParkingLocation;
import fr.android.parkingspotter.sync.ParkingSyncManager;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.preference.PreferenceManager;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int REQUEST_IMAGE_CAPTURE = 2;
    private static final int REQUEST_CAMERA_PERMISSION = 3;
    private static final String PREF_EMAIL = "user_email";
    private static final String PREF_TOKEN = "user_token";
    private static final String SUPABASE_URL = "https://axlzigzxirgwbffwgvzz.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF4bHppZ3p4aXJnd2JmZndndnp6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDU0NDMxMTMsImV4cCI6MjA2MTAxOTExM30.xYzE9wP6quKHUZdZeivw4CfbYzM6MgqtiLX7Krf4St4";
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private Location lastLocation;
    private TextView addressTextView;
    private String currentPhotoPath;
    private ParkingDatabaseHelper dbHelper;
    private String pendingAddress;
    private double pendingLat;
    private double pendingLng;
    private boolean hasFocusedOnRequest = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        Button btnParkHere = view.findViewById(R.id.btn_park_here);
        addressTextView = view.findViewById(R.id.text_address);
        dbHelper = new ParkingDatabaseHelper(requireContext());
        btnParkHere.setOnClickListener(v -> {
            if (mMap != null) {
                mMap.clear();
                LatLng center = mMap.getCameraPosition().target;
                mMap.addMarker(new MarkerOptions().position(center).title(getString(R.string.menu_map)));
                Location markerLocation = new Location("");
                markerLocation.setLatitude(center.latitude);
                markerLocation.setLongitude(center.longitude);
                // Géocode l'adresse du point marqué
                Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
                String address = "";
                try {
                    List<Address> addresses = geocoder.getFromLocation(center.latitude, center.longitude, 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        address = addresses.get(0).getAddressLine(0);
                    }
                } catch (Exception e) {
                    address = "Adresse inconnue";
                }
                addressTextView.setText(address); // <-- MAJ de l'adresse affichée
                pendingAddress = address;
                pendingLat = center.latitude;
                pendingLng = center.longitude;
                // Affiche la popup pour proposer la photo
                new AlertDialog.Builder(requireContext())
                        .setTitle("Photo du parking")
                        .setMessage("Voulez-vous prendre une photo de votre place ?")
                        .setPositiveButton("Oui", (dialog, which) -> {
                            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                                dispatchTakePictureIntent();
                            } else {
                                requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
                            }
                        })
                        .setNegativeButton("Non", (dialog, which) -> {
                            saveParkingLocation(null);
                        })
                        .show();
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction().replace(R.id.map, mapFragment).commit();
        }
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        hasFocusedOnRequest = false;
        enableMyLocation();

        // Vérifie si l'activité demande un focus temporaire
        if (getActivity() instanceof fr.android.parkingspotter.MainActivity) {
            fr.android.parkingspotter.MainActivity act = (fr.android.parkingspotter.MainActivity) getActivity();
            com.google.android.gms.maps.model.LatLng focus = act.consumePendingMapFocus();
            String focusAddress = null;
            if (focus != null) {
                hasFocusedOnRequest = true;
                mMap.clear();
                mMap.addMarker(new MarkerOptions().position(focus).title("Point sélectionné").icon(com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_AZURE)));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(focus, 16f));
                // Recherche l'adresse dans la base locale pour ce point
                ParkingDatabaseHelper dbHelper = new ParkingDatabaseHelper(requireContext());
                List<ParkingLocation> all = dbHelper.getAllParkingLocations();
                for (ParkingLocation loc : all) {
                    if (Math.abs(loc.getLatitude() - focus.latitude) < 1e-6 && Math.abs(loc.getLongitude() - focus.longitude) < 1e-6) {
                        focusAddress = loc.getAddress();
                        break;
                    }
                }
                if (focusAddress != null && !focusAddress.isEmpty() && !focusAddress.equals("Adresse inconnue")) {
                    addressTextView.setText(focusAddress);
                } else {
                    addressTextView.setText(String.format(Locale.getDefault(), "%f, %f", focus.latitude, focus.longitude));
                }
            } else {
                // Affiche le dernier point enregistré
                ParkingDatabaseHelper dbHelper = new ParkingDatabaseHelper(requireContext());
                List<ParkingLocation> all = dbHelper.getAllParkingLocations();
                if (!all.isEmpty()) {
                    ParkingLocation last = all.get(0); // le plus récent (car trié DESC)
                    LatLng pos = new LatLng(last.getLatitude(), last.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(pos).title("Dernier parking"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 16f));
                }
            }
        }

        mMap.setOnMapClickListener(latLng -> {
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        });
    }

    private void updateAddress(Location location) {
        Log.d("MapFragment", "updateAddress() called");
        if (location == null) {
            addressTextView.setText("Aucune position GPS disponible");
            Log.w("MapFragment", "Location is null, cannot geocode.");
            return;
        }
        Log.d("MapFragment", "Location utilisée: " + location.getLatitude() + ", " + location.getLongitude());
        Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            Log.d("MapFragment", "Geocoder result: " + addresses);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    sb.append(address.getAddressLine(i));
                    if (i != address.getMaxAddressLineIndex()) sb.append(", ");
                }
                addressTextView.setText(sb.toString());
                Log.i("MapFragment", "Adresse trouvée: " + sb);
            } else {
                addressTextView.setText("Aucune adresse trouvée");
                Log.w("MapFragment", "No address found for location: " + location.getLatitude() + ", " + location.getLongitude());
            }
        } catch (IOException e) {
            addressTextView.setText("Erreur géocodage");
            Log.e("MapFragment", "Geocoder IOException", e);
        }
        Log.d("MapFragment", "TextView final: " + addressTextView.getText());
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(requireActivity(), location -> {
                if (location != null && !hasFocusedOnRequest) {
                    lastLocation = location;
                    LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f));
                    updateAddress(location);
                } else if (location == null) {
                    addressTextView.setText("Aucune position GPS disponible");
                }
            });
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            }
        } else if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Log.w("MapFragment", "Permission caméra refusée");
            }
        }
    }

    private void dispatchTakePictureIntent() {
        Log.d("MapFragment", "dispatchTakePictureIntent() called");
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
                Log.d("MapFragment", "Fichier photo créé : " + photoFile.getAbsolutePath());
            } catch (Exception ex) {
                Log.e("MapFragment", "Erreur création fichier photo", ex);
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(), requireContext().getPackageName() + ".fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                Log.d("MapFragment", "Lancement de l'intent caméra avec URI : " + photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            } else {
                Log.e("MapFragment", "photoFile est null, impossible de lancer la caméra");
            }
        } else {
            Log.e("MapFragment", "Aucune app caméra disponible pour ACTION_IMAGE_CAPTURE");
        }
    }

    private File createImageFile() throws Exception {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MapFragment", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (resultCode == requireActivity().RESULT_OK) {
                Log.i("MapFragment", "Photo prise et sauvegardée : " + currentPhotoPath);
                saveParkingLocation(currentPhotoPath);
            } else {
                Log.w("MapFragment", "Prise de photo annulée ou échouée, resultCode=" + resultCode);
                saveParkingLocation(null);
            }
        }
    }

    private void saveParkingLocation(String photoPath) {
        String datetime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date());
        ParkingLocation location = new ParkingLocation(pendingLat, pendingLng, pendingAddress, datetime, photoPath);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String token = prefs.getString(PREF_TOKEN, null);
        String email = prefs.getString(PREF_EMAIL, null);
        if (token != null && email != null) {
            // Utilisateur connecté : upload photo et parking dans Supabase
            String userId = getUserIdFromToken(token);
            SupabaseDatabaseManager supabaseDB = new SupabaseDatabaseManager(requireContext(), SUPABASE_URL, SUPABASE_ANON_KEY, token, userId);
            String photoUrl = null;
            if (photoPath != null) {
                File photoFile = new File(photoPath);
                photoUrl = supabaseDB.uploadPhoto(photoFile);
            }
            supabaseDB.addParkingLocation(location, photoUrl);
            // Synchronisation descendante après ajout
            ParkingDatabaseHelper dbHelper = new ParkingDatabaseHelper(requireContext());
            ParkingSyncManager.synchronize(requireContext(), supabaseDB, dbHelper, ParkingSyncManager.SyncDirection.REMOTE_TO_LOCAL, null);
        } else {
            // Utilisateur non connecté : base locale
            dbHelper.addParkingLocation(location);
        }
        Log.i("MapFragment", "Parking enregistré : " + pendingAddress + " (photo : " + photoPath + ")");
    }

    // Helper pour extraire le user_id du token JWT (à compléter avec une vraie lib JWT si besoin)
    private String getUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length > 1) {
                String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE));
                org.json.JSONObject obj = new org.json.JSONObject(payload);
                return obj.optString("sub", null);
            }
        } catch (Exception e) {
            Log.e("MapFragment", "Erreur décodage JWT", e);
        }
        return null;
    }
}
