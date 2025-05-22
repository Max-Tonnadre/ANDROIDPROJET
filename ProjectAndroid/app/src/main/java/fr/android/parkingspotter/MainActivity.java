package fr.android.parkingspotter;

import android.content.Context;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import com.google.android.material.navigation.NavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import fr.android.parkingspotter.fragments.MapFragment;
import fr.android.parkingspotter.fragments.HistoryFragment;
import fr.android.parkingspotter.fragments.SettingsFragment;
import fr.android.parkingspotter.fragments.AuthFragment;
import fr.android.parkingspotter.utils.LocaleHelper;
import android.view.MenuItem;
import fr.android.parkingspotter.models.ParkingLocation;
import com.google.android.gms.maps.model.LatLng;
import fr.android.parkingspotter.adapters.OnShowOnMapRequestListener;

public class MainActivity extends AppCompatActivity implements OnShowOnMapRequestListener {
    private LatLng pendingMapFocus = null;
    @Override
    public void onShowOnMapRequest(ParkingLocation location) {
        // Change l'onglet pour MapFragment et passe la position à centrer
        pendingMapFocus = new LatLng(location.getLatitude(), location.getLongitude());
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, new fr.android.parkingspotter.fragments.MapFragment())
            .commit();
    }
    public LatLng consumePendingMapFocus() {
        LatLng tmp = pendingMapFocus;
        pendingMapFocus = null;
        return tmp;
    }
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navView = findViewById(R.id.nav_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        // Afficher MapFragment par défaut
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new MapFragment())
                .commit();
        }

        navView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                Fragment selectedFragment = null;
                int id = item.getItemId();
                if (id == R.id.nav_map) {
                    selectedFragment = new MapFragment();
                } else if (id == R.id.nav_history) {
                    selectedFragment = new HistoryFragment();
                } else if (id == R.id.nav_settings) {
                    selectedFragment = new SettingsFragment();
                } else if (id == R.id.nav_auth) {
                    selectedFragment = new AuthFragment();
                }
                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                    drawerLayout.closeDrawers();
                    return true;
                }
                return false;
            }
        });
    }
}