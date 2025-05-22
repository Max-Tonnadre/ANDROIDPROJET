package fr.android.parkingspotter.utils;

import android.os.Handler;
import android.os.Looper;
import org.json.JSONObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class SupabaseAuthManager {
    private final String supabaseUrl;
    private final String supabaseAnonKey;

    public SupabaseAuthManager(String supabaseUrl, String supabaseAnonKey) {
        this.supabaseUrl = supabaseUrl;
        this.supabaseAnonKey = supabaseAnonKey;
    }

    public interface AuthCallback {
        void onSuccess(JSONObject user);
        void onError(String errorMsg);
    }

    public void signUp(String email, String password, AuthCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(supabaseUrl + "/auth/v1/signup");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.close();
                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    String response = new String(conn.getInputStream().readAllBytes());
                    JSONObject json = new JSONObject(response);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(json));
                } else {
                    String error = new String(conn.getErrorStream().readAllBytes());
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(error));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }

    public void signIn(String email, String password, AuthCallback callback) {
        new Thread(() -> {
            try {
                URL url = new URL(supabaseUrl + "/auth/v1/token?grant_type=password");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.close();
                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    String response = new String(conn.getInputStream().readAllBytes());
                    JSONObject json = new JSONObject(response);
                    new Handler(Looper.getMainLooper()).post(() -> callback.onSuccess(json));
                } else {
                    String error = new String(conn.getErrorStream().readAllBytes());
                    new Handler(Looper.getMainLooper()).post(() -> callback.onError(error));
                }
            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> callback.onError(e.getMessage()));
            }
        }).start();
    }
}
