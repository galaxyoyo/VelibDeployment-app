package fr.galaxyoyo.velib;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        AdView adView = findViewById(R.id.adView);
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (MapsActivity.ADS) {
            MobileAds.initialize(this, "ca-app-pub-1691839407946394~3470243208");
            adView.loadAd(new AdRequest.Builder().build());
        }
        else {
            ((LinearLayout) findViewById(R.id.help_linear_layout)).removeView(adView);
            navigationView.getMenu().removeItem(R.id.remove_ads);
        }

        final DrawerLayout mDrawerLayout = findViewById(R.id.drawer);

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        mDrawerLayout.closeDrawers();

                        if (item.getItemId() == R.id.nav_map)
                            startActivity(new Intent(HelpActivity.this, MapsActivity.class));
                        else if (item.getItemId() == R.id.nav_global_statistics)
                            startActivity(new Intent(HelpActivity.this, GlobalStatistics.class));
                        else if (item.getItemId() == R.id.nav_station_statistics)
                            startActivity(new Intent(HelpActivity.this, StationStatistics.class));
                        else if (item.getItemId() == R.id.nav_avancement)
                            startActivity(new Intent(HelpActivity.this, Avancement.class));
                        else if (item.getItemId() == R.id.nav_manage)
                            startActivity(new Intent(HelpActivity.this, SettingsActivity.class));

                        return true;
                    }
                });
        navigationView.getMenu().getItem(4).setChecked(true);
    }
}
