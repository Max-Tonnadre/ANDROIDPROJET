package fr.android.parkingspotter.firebase;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import fr.android.parkingspotter.models.ParkingLocation;
import fr.android.parkingspotter.utils.ImageDownloader;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SupabaseDatabaseManager {
    private final String supabaseUrl;
    private final String supabaseAnonKey;
    private final String supabaseToken;
    private final String userId;
    private final Context context;
    private static final String BUCKET = "parking-photos";

    public SupabaseDatabaseManager(Context context, String supabaseUrl, String supabaseAnonKey, String supabaseToken, String userId) {
        this.context = context;
        this.supabaseUrl = supabaseUrl;
        this.supabaseAnonKey = supabaseAnonKey;
        this.supabaseToken = supabaseToken;
        this.userId = userId;
    }

    // Upload une image dans le bucket et retourne l'URL publique (asynchrone)
    public String uploadPhoto(File photoFile) {
        final String[] resultUrl = {null};
        Thread thread = new Thread(() -> {
            try {
                String fileName = System.currentTimeMillis() + ".jpg";
                String objectPath = userId + "/" + fileName;
                URL url = new URL(supabaseUrl + "/storage/v1/object/" + BUCKET + "/" + objectPath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + supabaseToken);
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Content-Type", "image/jpeg");
                conn.setDoOutput(true);
                OutputStream os = conn.getOutputStream();
                FileInputStream fis = new FileInputStream(photoFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                fis.close();
                os.close();
                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    resultUrl[0] = supabaseUrl + "/storage/v1/object/public/" + BUCKET + "/" + objectPath;
                } else {
                    Log.e("SupabaseDB", "Erreur upload photo: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("SupabaseDB", "Exception upload photo", e);
            }
        });
        thread.start();
        try { thread.join(); } catch (InterruptedException ignored) {}
        return resultUrl[0];
    }

    // Ajoute un parking dans la base distante (asynchrone)
    public void addParkingLocation(ParkingLocation location, String photoUrl) {
        new Thread(() -> {
            try {
                URL url = new URL(supabaseUrl + "/rest/v1/parking_locations");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Authorization", "Bearer " + supabaseToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=representation");
                conn.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("user_id", userId);
                body.put("latitude", location.getLatitude());
                body.put("longitude", location.getLongitude());
                body.put("address", location.getAddress());
                body.put("datetime", location.getDatetime());
                body.put("photo_url", photoUrl);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.close();
                int responseCode = conn.getResponseCode();
                if (responseCode == 201) {
                    // OK
                } else {
                    Log.e("SupabaseDB", "Erreur addParkingLocation: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("SupabaseDB", "Exception addParkingLocation", e);
            }
        }).start();
    }

    // Ajoute un parking dans la base distante avec un id imposé (pour synchro)
    public void addParkingLocationWithId(ParkingLocation location, String photoUrl, String uuid) {
        new Thread(() -> {
            try {
                URL url = new URL(supabaseUrl + "/rest/v1/parking_locations");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Authorization", "Bearer " + supabaseToken);
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Prefer", "return=representation");
                conn.setDoOutput(true);
                JSONObject body = new JSONObject();
                body.put("id", uuid); // UUID distant
                body.put("user_id", userId);
                body.put("latitude", location.getLatitude());
                body.put("longitude", location.getLongitude());
                body.put("address", location.getAddress());
                body.put("datetime", location.getDatetime());
                body.put("photo_url", photoUrl);
                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes());
                os.close();
                int responseCode = conn.getResponseCode();
                if (responseCode == 201) {
                    // OK
                } else {
                    Log.e("SupabaseDB", "Erreur addParkingLocationWithId: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("SupabaseDB", "Exception addParkingLocationWithId", e);
            }
        }).start();
    }

    public interface ParkingLocationsCallback {
        void onResult(List<ParkingLocation> locations);
    }

    public void getAllParkingLocationsAsync(ParkingLocationsCallback callback) {
        new Thread(() -> {
            List<ParkingLocation> result = new ArrayList<>();
            try {
                String urlStr = supabaseUrl + "/rest/v1/parking_locations?user_id=eq." + userId + "&select=*";
                Log.d("SupabaseDB", "GET URL: " + urlStr);
                Log.d("SupabaseDB", "userId: " + userId);
                Log.d("SupabaseDB", "Token: " + supabaseToken);
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("apikey", supabaseAnonKey);
                conn.setRequestProperty("Authorization", "Bearer " + supabaseToken);
                conn.setRequestProperty("Content-Type", "application/json");
                Log.d("SupabaseDB", "Headers: apikey=" + supabaseAnonKey + ", Authorization=Bearer ...");
                int responseCode = conn.getResponseCode();
                Log.d("SupabaseDB", "Response code: " + responseCode);
                if (responseCode == 200) {
                    String response = new String(conn.getInputStream().readAllBytes());
                    JSONArray arr = new JSONArray(response);
                    for (int i = 0; i < arr.length(); i++) {
                        JSONObject obj = arr.getJSONObject(i);
                        String remoteId = obj.optString("id");
                        double lat = obj.optDouble("latitude");
                        double lng = obj.optDouble("longitude");
                        String address = obj.optString("address");
                        String datetime = obj.optString("datetime");
                        String photoUrl = obj.optString("photo_url");
                        String localPhotoPath = null;
                        if (photoUrl != null && !photoUrl.isEmpty() && photoUrl.startsWith("http")) {
                            // Télécharge l'image distante et stocke localement
                            String fileName = remoteId + ".jpg";
                            localPhotoPath = ImageDownloader.downloadToLocal(context, photoUrl, fileName);
                        }
                        ParkingLocation loc = new ParkingLocation(
                            remoteId,
                            lat,
                            lng,
                            address,
                            datetime,
                            (localPhotoPath != null) ? localPhotoPath : photoUrl // si échec, garde l'URL
                        );
                        result.add(loc);
                    }
                } else {
                    Log.e("SupabaseDB", "Erreur getAllParkingLocations: " + responseCode);
                }
            } catch (Exception e) {
                Log.e("SupabaseDB", "Exception getAllParkingLocations", e);
            }
            new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
        }).start();
    }
}
