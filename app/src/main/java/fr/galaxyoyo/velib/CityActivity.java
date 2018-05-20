package fr.galaxyoyo.velib;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.common.collect.Lists;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CityActivity extends AppCompatActivity {
    private City city;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_city);

        if (MapsActivity.cities == null || MapsActivity.stations.isEmpty()) {
            startActivity(new Intent(CityActivity.this, MapsActivity.class));
            return;
        }

        int cp = getIntent().getIntExtra("cp", 1000);
        for (City c : MapsActivity.cities) {
            if (c.getCp() == cp) {
                city = c;
                break;
            }
        }

        ListView list = findViewById(R.id.city_stations);
        final List<Station> stations = Lists.newArrayList(city.getStations());
        Collections.sort(stations, new Comparator<Station>() {
            @Override
            public int compare(Station o1, Station o2) {
                return Integer.compare(o1.getCode(), o2.getCode());
            }
        });

        List<Map<String, String>> data = Lists.newArrayList();
        for (Station station : stations) {
            Map<String, String> map = new HashMap<>(2);
            map.put("station", station.toString());
            switch (station.getState()) {
                case OPERATIVE:
                    if (station.getActivation_date() != null)
                        map.put("date", new SimpleDateFormat("EEEE d MMMM yyyy").format(station.getActivation_date()) + (station.isElectricity() ? ", alimentée" : ""));
                    else
                        map.put("date", "Officiellement ouverte mais n'a pas de borne dispo");
                    break;
                case WORK_IN_PROGRESS:
                    map.put("date", "En travaux");
                    break;
                case MAINTENANCE:
                    map.put("date", "En maintenance");
                    break;
                case CLOSE:
                    map.put("date", "Fermée");
                    break;
                case UNKNOWN:
                    map.put("date", "Station Decaux pas encore en travaux");
                    break;
                case DELETED:
                    map.put("date", "Supprimée");
                    break;
            }
            data.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2, new String[] {"station", "date"}, new int[] {android.R.id.text1, android.R.id.text2});
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Station st = stations.get(position);
                assert st != null;

                Intent intent = new Intent(CityActivity.this, StationStatistics.class);
                intent.putExtra("station", st.getCode());
                startActivity(intent);
            }
        });
        list.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, stations.size() * 179));
    }
}
