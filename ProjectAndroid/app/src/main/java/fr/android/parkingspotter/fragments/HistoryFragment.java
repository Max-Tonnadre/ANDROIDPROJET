package fr.android.parkingspotter.fragments;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import fr.android.parkingspotter.R;
import fr.android.parkingspotter.adapters.HistoryAdapter;
import fr.android.parkingspotter.adapters.OnShowOnMapRequestListener;
import fr.android.parkingspotter.database.ParkingDatabaseHelper;
import fr.android.parkingspotter.firebase.SupabaseDatabaseManager;
import fr.android.parkingspotter.models.ParkingLocation;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import android.content.Intent;
import android.net.Uri;

public class HistoryFragment extends Fragment {
    private static final String PREF_EMAIL = "user_email";
    private static final String PREF_TOKEN = "user_token";
    private static final String SUPABASE_URL = "https://axlzigzxirgwbffwgvzz.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF4bHppZ3p4aXJnd2JmZndndnp6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDU0NDMxMTMsImV4cCI6MjA2MTAxOTExM30.xYzE9wP6quKHUZdZeivw4CfbYzM6MgqtiLX7Krf4St4";
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        // Ajout d'un TextView pour le message vide et d'un ProgressBar pour le chargement
        TextView emptyView = view.findViewById(R.id.text_empty_history);
        ProgressBar progressBar = view.findViewById(R.id.progress_history);
        emptyView.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String token = prefs.getString(PREF_TOKEN, null);
        String email = prefs.getString(PREF_EMAIL, null);
        final boolean isConnected;
        ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            isConnected = activeNetwork != null && activeNetwork.isConnected();
        } else {
            isConnected = false;
        }
        if (token != null && isTokenExpired(token)) {
            Toast.makeText(requireContext(), "Session expirée, veuillez vous reconnecter", Toast.LENGTH_LONG).show();
            // Redirige vers l'écran de connexion
            requireActivity().getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new fr.android.parkingspotter.fragments.AuthFragment())
                .commit();
            return view;
        }
        if (token != null && email != null && isConnected) {
            // Connecté ET en ligne : distant
            String userId = getUserIdFromToken(token); // même helper que dans MapFragment
            SupabaseDatabaseManager supabaseDB = new SupabaseDatabaseManager(requireContext(), SUPABASE_URL, SUPABASE_ANON_KEY, token, userId);
            progressBar.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.GONE);
            supabaseDB.getAllParkingLocationsAsync(remoteLocations -> {
                progressBar.setVisibility(View.GONE);
                if (remoteLocations == null || remoteLocations.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    HistoryAdapter adapter = new HistoryAdapter(remoteLocations, new HistoryAdapter.OnHistoryActionListener() {
                        @Override
                        public void onShowOnMap(ParkingLocation location) {
                            // Demande à l'activité d'afficher ce point sur la carte
                            if (getActivity() instanceof OnShowOnMapRequestListener) {
                                ((OnShowOnMapRequestListener) getActivity()).onShowOnMapRequest(location);
                            }
                        }
                        @Override
                        public void onShowDirections(ParkingLocation location) {
                            // Ouvre Google Maps avec la destination GPS
                            String uri = String.format("google.navigation:q=%f,%f", location.getLatitude(), location.getLongitude());
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                            intent.setPackage("com.google.android.apps.maps");
                            startActivity(intent);
                        }
                        @Override
                        public void onDelete(ParkingLocation location) {
                            if (isConnected) {
                                // Suppression distante puis synchro descendante
                                new Thread(() -> {
                                    try {
                                        URL url = new URL(SUPABASE_URL + "/rest/v1/parking_locations?id=eq." + location.getRemoteId());
                                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                                        conn.setRequestMethod("DELETE");
                                        conn.setRequestProperty("apikey", SUPABASE_ANON_KEY);
                                        conn.setRequestProperty("Authorization", "Bearer " + token);
                                        conn.setRequestProperty("Content-Type", "application/json");
                                        int responseCode = conn.getResponseCode();
                                        if (responseCode == 204) {
                                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Supprimé !", Toast.LENGTH_SHORT).show());
                                            // Supprime localement par remoteId
                                            ParkingDatabaseHelper dbHelper = new ParkingDatabaseHelper(requireContext());
                                            dbHelper.deleteParkingLocationByRemoteId(location.getRemoteId());
                                            // Rafraîchir la liste
                                            SupabaseDatabaseManager supabaseDB2 = new SupabaseDatabaseManager(requireContext(), SUPABASE_URL, SUPABASE_ANON_KEY, token, userId);
                                            fr.android.parkingspotter.sync.ParkingSyncManager.synchronize(requireContext(), supabaseDB2, dbHelper, fr.android.parkingspotter.sync.ParkingSyncManager.SyncDirection.REMOTE_TO_LOCAL, () -> requireActivity().runOnUiThread(this::refresh));
                                        } else {
                                            requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Erreur suppression distant (" + responseCode + ")", Toast.LENGTH_SHORT).show());
                                        }
                                    } catch (Exception e) {
                                        requireActivity().runOnUiThread(() -> Toast.makeText(requireContext(), "Erreur réseau", Toast.LENGTH_SHORT).show());
                                    }
                                }).start();
                            } else {
                                // Suppression locale uniquement
                                ParkingDatabaseHelper dbHelper = new ParkingDatabaseHelper(requireContext());
                                dbHelper.deleteParkingLocationByLocalId(location.getLocalId());
                                Toast.makeText(requireContext(), "Supprimé localement", Toast.LENGTH_SHORT).show();
                                // Rafraîchir la liste
                                List<ParkingLocation> refreshed = dbHelper.getAllParkingLocations();
                                recyclerView.setAdapter(new HistoryAdapter(refreshed, this));
                            }
                        }
                        private void refresh() {
                            // Relance la récupération distante pour rafraîchir la liste
                            progressBar.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                            emptyView.setVisibility(View.GONE);
                            supabaseDB.getAllParkingLocationsAsync(remoteLocations2 -> {
                                progressBar.setVisibility(View.GONE);
                                if (remoteLocations2 == null || remoteLocations2.isEmpty()) {
                                    emptyView.setVisibility(View.VISIBLE);
                                    recyclerView.setVisibility(View.GONE);
                                } else {
                                    recyclerView.setAdapter(new HistoryAdapter(remoteLocations2, this));
                                    recyclerView.setVisibility(View.VISIBLE);
                                    emptyView.setVisibility(View.GONE);
                                }
                            });
                        }
                    });
                    if (getActivity() instanceof OnShowOnMapRequestListener) {
                        adapter.setOnShowOnMapRequestListener((OnShowOnMapRequestListener) getActivity());
                    }
                    recyclerView.setAdapter(adapter);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyView.setVisibility(View.GONE);
                }
            });
        } else if (token != null && email != null && !isConnected) {
            // Connecté mais hors-ligne : local + mention
            Toast.makeText(requireContext(), "Mode hors-ligne : affichage local", Toast.LENGTH_LONG).show();
            ParkingDatabaseHelper dbHelper = new ParkingDatabaseHelper(requireContext());
            List<ParkingLocation> localLocations = dbHelper.getAllParkingLocations();
            if (localLocations == null || localLocations.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                HistoryAdapter adapter = new HistoryAdapter(localLocations, new HistoryAdapter.OnHistoryActionListener() {
                    @Override
                    public void onShowOnMap(ParkingLocation location) {
                        // Affiche le point sur la carte via l'activité si possible
                        if (getActivity() instanceof OnShowOnMapRequestListener) {
                            ((OnShowOnMapRequestListener) getActivity()).onShowOnMapRequest(location);
                        }
                    }
                    @Override
                    public void onShowDirections(ParkingLocation location) {
                        String uri = String.format("geo:%f,%f?q=%f,%f(%s)", location.getLatitude(), location.getLongitude(), location.getLatitude(), location.getLongitude(), android.net.Uri.encode(location.getAddress()));
                        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                        startActivity(intent);
                    }
                    @Override
                    public void onDelete(ParkingLocation location) {
                        // Suppression locale uniquement
                        ParkingDatabaseHelper dbHelper = new ParkingDatabaseHelper(requireContext());
                        dbHelper.deleteParkingLocationByLocalId(location.getLocalId());
                        Toast.makeText(requireContext(), "Supprimé localement", Toast.LENGTH_SHORT).show();
                        // Rafraîchir la liste
                        List<ParkingLocation> refreshed = dbHelper.getAllParkingLocations();
                        recyclerView.setAdapter(new HistoryAdapter(refreshed, this));
                    }
                });
                recyclerView.setAdapter(adapter);
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        } else {
            // Non connecté : local
            ParkingDatabaseHelper dbHelper = new ParkingDatabaseHelper(requireContext());
            List<ParkingLocation> localLocations = dbHelper.getAllParkingLocations();
            if (localLocations == null || localLocations.isEmpty()) {
                emptyView.setVisibility(View.VISIBLE);
                recyclerView.setVisibility(View.GONE);
            } else {
                HistoryAdapter adapter = new HistoryAdapter(localLocations, new HistoryAdapter.OnHistoryActionListener() {
                    @Override
                    public void onShowOnMap(ParkingLocation location) {
                        // Affiche le point sur la carte via l'activité si possible
                        if (getActivity() instanceof OnShowOnMapRequestListener) {
                            ((OnShowOnMapRequestListener) getActivity()).onShowOnMapRequest(location);
                        }
                    }
                    @Override
                    public void onShowDirections(ParkingLocation location) {
                        String uri = String.format("geo:%f,%f?q=%f,%f(%s)", location.getLatitude(), location.getLongitude(), location.getLatitude(), location.getLongitude(), android.net.Uri.encode(location.getAddress()));
                        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
                        startActivity(intent);
                    }
                    @Override
                    public void onDelete(ParkingLocation location) {
                        ParkingDatabaseHelper dbHelper = new ParkingDatabaseHelper(requireContext());
                        dbHelper.deleteParkingLocationByLocalId(location.getLocalId());
                        Toast.makeText(requireContext(), "Supprimé localement", Toast.LENGTH_SHORT).show();
                        List<ParkingLocation> refreshed = dbHelper.getAllParkingLocations();
                        recyclerView.setAdapter(new HistoryAdapter(refreshed, this));
                    }
                });
                recyclerView.setAdapter(adapter);
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        }
        return view;
    }
    // Helper pour extraire le user_id du token JWT (copie de MapFragment)
    private String getUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length > 1) {
                String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE));
                android.util.Log.d("HistoryFragment", "JWT payload: " + payload);
                JSONObject obj = new JSONObject(payload);
                return obj.optString("sub", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    // Helper pour savoir si le token est expiré
    private boolean isTokenExpired(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length > 1) {
                String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE));
                org.json.JSONObject obj = new org.json.JSONObject(payload);
                long exp = obj.optLong("exp", 0);
                long now = System.currentTimeMillis() / 1000L;
                return exp < now;
            }
        } catch (Exception e) {
            return true;
        }
        return true;
    }
}
