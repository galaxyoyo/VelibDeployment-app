package fr.galaxyoyo.velib;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class Avancement extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avancement);

        AdView adView = findViewById(R.id.adView);
        adView.loadAd(new AdRequest.Builder().build());

        if (MapsActivity.cities == null || MapsActivity.stations.isEmpty()) {
            startActivity(new Intent(Avancement.this, MapsActivity.class));
            return;
        }

        final DrawerLayout mDrawerLayout = findViewById(R.id.drawer);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        mDrawerLayout.closeDrawers();

                        if (item.getItemId() == R.id.nav_map)
                            startActivity(new Intent(Avancement.this, MapsActivity.class));
                        else if (item.getItemId() == R.id.nav_global_statistics)
                            startActivity(new Intent(Avancement.this, GlobalStatistics.class));
                        else if (item.getItemId() == R.id.nav_station_statistics)
                            startActivity(new Intent(Avancement.this, StationStatistics.class));
                        else if (item.getItemId() == R.id.nav_help)
                            startActivity(new Intent(Avancement.this, HelpActivity.class));
                        else if (item.getItemId() == R.id.nav_manage)
                            startActivity(new Intent(Avancement.this, SettingsActivity.class));

                        return true;
                    }
                });
        navigationView.getMenu().getItem(3).setChecked(true);

        ListView communes = findViewById(R.id.communes);
        List<Map<String, String>> data = Lists.newArrayList();
        final List<City> cities = Lists.newArrayList();

        for (City city : MapsActivity.cities) {
            if (!city.getStations().isEmpty())
                cities.add(city);
        }

        Collections.sort(cities, new Comparator<City>() {
            @Override
            public int compare(City o1, City o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getFullname(), o2.getFullname());
            }
        });

        for (City city : cities) {
            int opened = 0, alimented = 0, work = 0, close = 0, maintenance = 0, deleted = 0, total = city.getStations().size();
            for (Station st : city.getStations()) {
                if (st.getActivation_date() != null && st.getState() == Station.State.OPERATIVE)
                    ++opened;
                if (st.getState() == Station.State.WORK_IN_PROGRESS)
                    ++work;
                else if (st.getState() == Station.State.CLOSE)
                    ++close;
                else if (st.getState() == Station.State.MAINTENANCE)
                    ++maintenance;
                else if (st.getState() == Station.State.DELETED)
                    ++deleted;
                if (st.isElectricity())
                    ++alimented;
            }
            if (total == 0)
                continue;
            total -= deleted;
            Map<String, String> map = Maps.newHashMap();
            map.put("commune", city.getFullname());
            String st = opened + "/" + total + " stations ouvertes (" + (int) ((float) 100.0 * opened / (float) total) + " %) dont " + alimented + " alimentées";
            if (work > 0)
                st += "; " + work + " en travaux";
            if (close > 0)
                st += "; " + close + " fermée" + (close > 1 ? "s" : "");
            if (maintenance > 0)
                st += "; " + maintenance + " en maintenance";
            if (deleted > 0)
                st += "; " + deleted + " supprimée" + (deleted > 1 ? "s" : "");
            map.put("infos", st);
            data.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2, new String[] {"commune", "infos"}, new int[] {android.R.id.text1, android.R.id.text2});
        communes.setAdapter(adapter);
        communes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                City city = cities.get(position);
                assert city != null;

                Intent intent = new Intent(Avancement.this, CityActivity.class);
                intent.putExtra("cp", city.getCp());
                startActivity(intent);
            }
        });
    }

}
