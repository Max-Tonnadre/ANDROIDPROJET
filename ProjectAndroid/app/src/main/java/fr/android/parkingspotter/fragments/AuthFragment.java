package fr.android.parkingspotter.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.preference.PreferenceManager;
import fr.android.parkingspotter.R;
import fr.android.parkingspotter.firebase.SupabaseDatabaseManager;
import fr.android.parkingspotter.database.ParkingDatabaseHelper;
import fr.android.parkingspotter.models.ParkingLocation;
import fr.android.parkingspotter.utils.SupabaseAuthManager;
import fr.android.parkingspotter.sync.ParkingSyncManager;
import org.json.JSONObject;
import java.io.File;

public class AuthFragment extends Fragment {
    private static final String SUPABASE_URL = "https://axlzigzxirgwbffwgvzz.supabase.co";
    private static final String SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImF4bHppZ3p4aXJnd2JmZndndnp6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDU0NDMxMTMsImV4cCI6MjA2MTAxOTExM30.xYzE9wP6quKHUZdZeivw4CfbYzM6MgqtiLX7Krf4St4";
    private static final String PREF_EMAIL = "user_email";
    private static final String PREF_TOKEN = "user_token";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_auth, container, false);
        EditText editEmail = view.findViewById(R.id.edit_email);
        EditText editPassword = view.findViewById(R.id.edit_password);
        Button btnLogin = view.findViewById(R.id.btn_login);
        Button btnRegister = view.findViewById(R.id.btn_register);
        Button btnLogout = view.findViewById(R.id.btn_logout); // à ajouter dans le layout
        TextView textUser = view.findViewById(R.id.text_user); // à ajouter dans le layout
        TextView textError = view.findViewById(R.id.text_error);
        LinearLayout formLayout = view.findViewById(R.id.layout_form); // à entourer le formulaire dans le layout
        SupabaseAuthManager supabaseAuth = new SupabaseAuthManager(SUPABASE_URL, SUPABASE_ANON_KEY);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String savedEmail = prefs.getString(PREF_EMAIL, null);
        String savedToken = prefs.getString(PREF_TOKEN, null);
        if (savedEmail != null && savedToken != null) {
            // Utilisateur connecté
            formLayout.setVisibility(View.GONE);
            textUser.setVisibility(View.VISIBLE);
            btnLogout.setVisibility(View.VISIBLE);
            textUser.setText("Connecté en tant que : " + savedEmail);
        } else {
            // Pas connecté
            formLayout.setVisibility(View.VISIBLE);
            textUser.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
        }
        btnLogout.setOnClickListener(v -> {
            prefs.edit().remove(PREF_EMAIL).remove(PREF_TOKEN).apply();
            formLayout.setVisibility(View.VISIBLE);
            textUser.setVisibility(View.GONE);
            btnLogout.setVisibility(View.GONE);
        });
        btnLogin.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                textError.setText(R.string.error_empty_fields);
            } else {
                textError.setText("");
                supabaseAuth.signIn(email, password, new SupabaseAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(JSONObject user) {
                        // Récupère le token et l'email
                        String token = user.optString("access_token", null);
                        if (token != null) {
                            prefs.edit().putString(PREF_EMAIL, email).putString(PREF_TOKEN, token).apply();
                            formLayout.setVisibility(View.GONE);
                            textUser.setVisibility(View.VISIBLE);
                            btnLogout.setVisibility(View.VISIBLE);
                            textUser.setText("Connecté en tant que : " + email);
                            textError.setText("");
                            // Synchronisation descendante après connexion
                            String userId = getUserIdFromToken(token);
                            SupabaseDatabaseManager supabaseDB = new SupabaseDatabaseManager(requireContext(), SUPABASE_URL, SUPABASE_ANON_KEY, token, userId);
                            ParkingDatabaseHelper dbHelper = new ParkingDatabaseHelper(requireContext());
                            ParkingSyncManager.synchronize(requireContext(), supabaseDB, dbHelper, ParkingSyncManager.SyncDirection.REMOTE_TO_LOCAL, null);
                        } else {
                            textError.setText("Connexion réussie, mais pas de token trouvé.");
                        }
                    }
                    @Override
                    public void onError(String errorMsg) {
                        try {
                            JSONObject err = new JSONObject(errorMsg);
                            String code = err.optString("error_code");
                            if ("invalid_credentials".equals(code)) {
                                textError.setText("Email ou mot de passe incorrect.");
                            } else {
                                textError.setText("Erreur: " + err.optString("msg", errorMsg));
                            }
                        } catch (Exception e) {
                            textError.setText("Erreur: " + errorMsg);
                        }
                    }
                });
            }
        });
        btnRegister.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();
            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
                textError.setText(R.string.error_empty_fields);
            } else if (password.length() < 6) {
                textError.setText("Le mot de passe doit faire au moins 6 caractères.");
            } else {
                textError.setText("");
                supabaseAuth.signUp(email, password, new SupabaseAuthManager.AuthCallback() {
                    @Override
                    public void onSuccess(JSONObject user) {
                        textError.setText("Compte créé ! Vous pouevz vous connecter.");
                        // Synchronisation locale -> distante après création de compte
                        String userId = getUserIdFromToken(user.optString("access_token", null));
                        SupabaseDatabaseManager supabaseDB = new SupabaseDatabaseManager(requireContext(), SUPABASE_URL, SUPABASE_ANON_KEY, user.optString("access_token", null), userId);
                        ParkingDatabaseHelper dbHelper = new ParkingDatabaseHelper(requireContext());
                        ParkingSyncManager.synchronize(requireContext(), supabaseDB, dbHelper, ParkingSyncManager.SyncDirection.LOCAL_TO_REMOTE, () -> {
                            requireActivity().runOnUiThread(() -> android.widget.Toast.makeText(requireContext(), "Points synchronisés", android.widget.Toast.LENGTH_SHORT).show());
                        });
                    }
                    @Override
                    public void onError(String errorMsg) {
                        try {
                            JSONObject err = new JSONObject(errorMsg);
                            String code = err.optString("error_code");
                            if ("user_already_exists".equals(code)) {
                                textError.setText("Un compte existe déjà avec cet email.");
                            } else if ("weak_pas_sword".equals(code)) {
                                textError.setText("Le mot de passe doit faire au moins 6 caractères.");
                            } else {
                                textError.setText("Erreur: " + err.optString("msg", errorMsg));
                            }
                        } catch (Exception e) {
                            textError.setText("Erreur: " + errorMsg);
                        }
                    }
                });
            }
        });
        return view;
    }

    // Helper pour extraire le user_id du token JWT (copié de MapFragment)
    private String getUserIdFromToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length > 1) {
                String payload = new String(android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE));
                org.json.JSONObject obj = new org.json.JSONObject(payload);
                return obj.optString("sub", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
