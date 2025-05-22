package fr.android.projectandroid;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
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
import java.text.SimpleDateFormat;
import java.util.Date;

import fr.android.projectandroid.utils.LocaleHelper;

public class TakePhotoActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

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
                uploadMetadataToPostgres(currentPhotoPath, description, 1, null);
            } else {
                Toast.makeText(this, "Aucune photo à envoyer", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Erreur création fichier image", Toast.LENGTH_SHORT).show();
                return;
            }

            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        "fr.android.projectandroid.fileprovider", // DOIT correspondre à AndroidManifest + res/xml/file_paths.xml
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "Aucune appli photo disponible", Toast.LENGTH_SHORT).show();
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "JPEG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(fileName, ".jpg", storageDir);
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
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Permission caméra refusée", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void uploadMetadataToPostgres(String filePath, String description, int userId, Integer locationId) {
        new Thread(() -> {
            try {
                String url = "jdbc:postgresql://ep-fancy-bush-a5gt24z3-pooler.us-east-2.aws.neon.tech/neondb?sslmode=require";
                String user = "neondb_owner";
                String password = "npg_2xD5ByRbUwgt";

                Class.forName("org.postgresql.Driver");
                try (java.sql.Connection conn = java.sql.DriverManager.getConnection(url, user, password)) {
                    String sql = "INSERT INTO photos (file_path, description, user_id, location_id) VALUES (?, ?, ?, ?)";
                    try (java.sql.PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, filePath);
                        stmt.setString(2, description);
                        stmt.setInt(3, userId);
                        if (locationId != null) {
                            stmt.setInt(4, locationId);
                        } else {
                            stmt.setNull(4, java.sql.Types.INTEGER);
                        }

                        int rows = stmt.executeUpdate();

                        runOnUiThread(() -> {
                            if (rows > 0) {
                                Toast.makeText(this, "Métadonnées envoyées à Neon ✅", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Aucune ligne insérée ❌", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Erreur : " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

}
