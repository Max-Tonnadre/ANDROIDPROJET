package fr.android.projectandroid;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;
import java.util.Date;

import fr.android.projectandroid.utils.LocaleHelper;

public class TakePhotoActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private static final String TAG = "TakePhoto";

    private String currentPhotoPath;
    private Uri photoUri;

    private EditText descriptionEditText;
    private Button takePhotoBtn, sendBtn;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.INSTANCE.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_photo);

        descriptionEditText = findViewById(R.id.editDescription);
        takePhotoBtn = findViewById(R.id.btnTakePhoto);
        sendBtn = findViewById(R.id.btnSend);

        takePhotoBtn.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
            } else {
                dispatchTakePictureIntent();
            }
        });

        sendBtn.setOnClickListener(v -> {
            String description = descriptionEditText.getText().toString();
            if (currentPhotoPath != null) {
                uploadMetadata(currentPhotoPath, description, 1, null);
            } else {
                Toast.makeText(this, "Aucune photo à envoyer", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.resolveActivity(getPackageManager());
        File photoFile;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Erreur création fichier image", Toast.LENGTH_SHORT).show();
            return;
        }

        if (photoFile != null) {
            photoUri = FileProvider.getUriForFile(this, "fr.android.projectandroid.fileprovider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_" + timeStamp, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            File imgFile = new File(currentPhotoPath);
            if (imgFile.exists() && imgFile.length() > 0) {
                Toast.makeText(this, "Photo prise avec succès 📷", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Fichier photo introuvable ou vide ❌", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            dispatchTakePictureIntent();
        } else {
            Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadMetadata(String filePath, String description, int userId, Integer locationId) {
        new Thread(() -> {
            try (Connection conn = connectToMySQL()) {
                int rows = insertPhotoMetadata(conn, filePath, description, userId, locationId);
                showUploadResult(rows);
            } catch (Exception e) {
                Log.e(TAG, "Erreur durant l'insertion MySQL", e);
                runOnUiThread(() -> Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private Connection connectToMySQL() throws Exception {
        String url = "jdbc:mysql://localhost:3306/android";
        String user = "root";
        String password = "admin";
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, user, password);
    }

    private int insertPhotoMetadata(Connection conn, String filePath, String description, int userId, Integer locationId) throws Exception {
        String sql = "INSERT INTO photos (file_path, description, user_id, location_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, filePath);
            stmt.setString(2, description);
            stmt.setInt(3, userId);
            if (locationId != null) {
                stmt.setInt(4, locationId);
            } else {
                stmt.setNull(4, java.sql.Types.INTEGER);
            }
            return stmt.executeUpdate();
        }
    }

    private void showUploadResult(int rows) {
        runOnUiThread(() -> {
            if (rows > 0) {
                Toast.makeText(this, "Métadonnées envoyées à MySQL ✅", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Aucune ligne insérée ❌", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
