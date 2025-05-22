package fr.android.projectandroid;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import fr.android.projectandroid.fragments.HomeFragment;
import fr.android.projectandroid.fragments.MapFragment;
import fr.android.projectandroid.fragments.SettingsFragment;
import fr.android.projectandroid.fragments.TakePhotoFragment;
import fr.android.projectandroid.utils.LocaleHelper;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawer;

    @Override
    protected void attachBaseContext(Context newBase) {
        Log.d("MainActivity_Locale", "attachBaseContext called. Initial base context locale: " +
                newBase.getResources().getConfiguration().getLocales().get(0).toLanguageTag());
        // For older APIs: Log.d(TAG, "attachBaseContext called. Initial base context locale: " + newBase.getResources().getConfiguration().locale.toLanguageTag());


        Context contextWithLocale = LocaleHelper.INSTANCE.onAttach(newBase);

        // Log the locale *after* LocaleHelper.onAttach has processed it
        if (contextWithLocale != null) {
            Configuration config = contextWithLocale.getResources().getConfiguration();
            String appliedLocaleTag;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                appliedLocaleTag = config.getLocales().get(0).toLanguageTag();
            } else {
                //noinspection deprecation
                appliedLocaleTag = config.locale.toLanguageTag();
            }
            Log.d("MainActivity_Locale", "attachBaseContext: Locale being applied by LocaleHelper.onAttach: " + appliedLocaleTag);
        } else {
            Log.e("MainActivity_Locale", "attachBaseContext: LocaleHelper.onAttach returned null context!");
        }

        super.attachBaseContext(contextWithLocale);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.nav_open, R.string.nav_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == R.id.nav_home) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeFragment())
                    .commit();
        } else if (item.getItemId() == R.id.nav_settings) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new SettingsFragment())
                    .commit();
        }
        else if (item.getItemId() == R.id.nav_map) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MapFragment())
                    .commit();
        }
        else if (item.getItemId() == R.id.nav_take_photo) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new TakePhotoFragment())
                    .commit();
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}