package fr.galaxyoyo.velib;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnCameraMoveStartedListener {
    public static Map<Integer, Station> stations = Maps.newHashMap();
    public static List<City> cities = Lists.newArrayList();
    public static Gson gson;
    private GoogleMap mMap;
    private List<Marker> markers = Lists.newArrayList();
    public static long lastUpdate = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        lastUpdate = 0L;

        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        MobileAds.initialize(this, "ca-app-pub-1691839407946394~3470243208");
        AdView adView = findViewById(R.id.adView);
        adView.loadAd(new AdRequest.Builder().build());

        final DrawerLayout mDrawerLayout = findViewById(R.id.drawer);

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        mDrawerLayout.closeDrawers();

                        if (item.getItemId() == R.id.nav_global_statistics)
                            startActivity(new Intent(MapsActivity.this, GlobalStatistics.class));
                        else if (item.getItemId() == R.id.nav_station_statistics)
                            startActivity(new Intent(MapsActivity.this, StationStatistics.class));
                        else if (item.getItemId() == R.id.nav_avancement)
                            startActivity(new Intent(MapsActivity.this, Avancement.class));
                        else if (item.getItemId() == R.id.nav_help)
                            startActivity(new Intent(MapsActivity.this, HelpActivity.class));
                        else if (item.getItemId() == R.id.nav_manage)
                            startActivity(new Intent(MapsActivity.this, SettingsActivity.class));
                        else if (item.getItemId() == R.id.nav_twitter)
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/VelibDeployment")));

                        return true;
                    }
                });
        navigationView.getMenu().getItem(0).setChecked(true);


        gson = new GsonBuilder().setPrettyPrinting().registerTypeAdapter(Date.class, new TypeAdapter<Date>() {
            @Override
            public void write(JsonWriter out, Date value) throws IOException {
                out.value(Long.toString(value.getTime()));
            }

            @SuppressLint("SimpleDateFormat")
            @Override
            public Date read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                } else {
                    try {
                        String str = in.nextString();
                        if (str.split(" ").length == 2)
                            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(str);
                        else
                            return new SimpleDateFormat("yyyy-MM-dd").parse(str);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).create();

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!cities.isEmpty())
                        return;
                    List<City> list = gson.fromJson(IOUtils.toString(new URL("http://galaxyoyo.com/velib/communes.php"), Charset.defaultCharset()), new TypeToken<ArrayList<City>>(){}.getType());
                    cities.addAll(list);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                try {
                    final String infos = IOUtils.toString(new URL("http://galaxyoyo.com/velib/information.txt"), Charset.defaultCharset());
                    if (!infos.isEmpty()) {
                        new Handler(getApplicationContext().getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
                                builder.setTitle("Informations");
                                builder.setMessage(infos);
                                builder.setPositiveButton("Ok", null);
                                builder.create().show();
                            }
                        });
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        lastUpdate = 0L;
        onCameraMoveStarted(0);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        LatLng paris = new LatLng(48.86, 2.34);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(paris, 15.0F));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            new RuntimeException("Missing location permission").printStackTrace();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, PackageManager.PERMISSION_GRANTED);
        } else
            mMap.setMyLocationEnabled(true);

        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);

        mMap.getUiSettings().setAllGesturesEnabled(true);

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @SuppressLint("SetTextI18n")
            @Override
            public View getInfoContents(final Marker marker) {
                //noinspection SuspiciousMethodCalls
                final Station station = stations.get(marker.getTag());

                LinearLayout layout = new LinearLayout(getApplicationContext());
                layout.setOrientation(LinearLayout.VERTICAL);

                TextView title = new TextView(getApplicationContext());
                title.setText(station.getCode() + " - " + station.getName());
                title.setTypeface(Typeface.DEFAULT_BOLD);
                layout.addView(title);

                TextView textView = new TextView(getApplicationContext());
                textView.setText(marker.getSnippet());
                layout.addView(textView);

                Button button = new Button(getApplicationContext());
                button.setText(R.string.more_infos);
                layout.addView(button);

                return layout;
            }
        });

        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @SuppressLint("SimpleDateFormat")
            @Override
            public void onInfoWindowClick(final Marker marker) {
                Intent intent = new Intent(MapsActivity.this, StationStatistics.class);
                intent.putExtra("station", (int) marker.getTag());
                startActivity(intent);
            }
        });

        for (Map.Entry<Integer, Station> entry : stations.entrySet()) {
            Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(entry.getValue().getLatitude(), entry.getValue().getLongitude())));
            marker.setTag(entry.getKey());
            markers.add(marker);
        }

        onCameraMoveStarted(1);

        mMap.setOnCameraMoveStartedListener(this);
    }

    @Override
    public void onCameraMoveStarted(int ignored) {
        LinearLayout layout = findViewById(R.id.map_layout);
        layout.clearFocus();

        if (System.currentTimeMillis() - lastUpdate < 30000L)
            return;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        lastUpdate = System.currentTimeMillis();

        final Handler mainHandler = new Handler(getApplicationContext().getMainLooper());

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String json = IOUtils.toString(
                            new URL("http://galaxyoyo.com/velib/stations.json.php"), "UTF-8");
                    final List<Station> st = gson.fromJson(json, new TypeToken<List<Station>>() {
                    }.getType());
                    json = IOUtils.toString(new URL("http://galaxyoyo.com/velib/bikes.json.php"), "UTF-8");
                    final List<Map<String, Integer>> bikes = gson.fromJson(json, new TypeToken<ArrayList<HashMap<String, Integer>>>() {
                    }.getType());

                    mainHandler.post(new Runnable() {
                        @SuppressLint("SimpleDateFormat")
                        @Override
                        public void run() {
                            if (mMap == null)
                                return;

                            for (Station station : st) {
                                if (!stations.containsKey(station.getCode())) {
                                    Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(station.getLatitude(), station.getLongitude())));
                                    marker.setTag(station.getCode());
                                    markers.add(marker);
                                }
                                stations.put(station.getCode(), station);
                            }

                            for (Map<String, Integer> map : bikes) {
                                Station station = stations.get(map.get("code"));

                                station.setNb_bike(map.get("nb_bike"));
                                station.setNb_ebike(map.get("nb_ebike"));
                                station.setNb_free_dock(map.get("nb_free_dock"));
                                station.setNb_free_edock(map.get("nb_free_edock"));
                                station.setNb_bike_overflow(map.get("nb_bike_overflow"));
                                station.setNb_ebike_overflow(map.get("nb_ebike_overflow"));
                            }

                            for (final Marker marker : markers) {
                                marker.setVisible(true);

                                //noinspection SuspiciousMethodCalls
                                final Station st = stations.get(marker.getTag());

                                marker.setPosition(new LatLng(st.getLatitude(), st.getLongitude()));
                                marker.setTitle(st.getCode() + " – " + st.getName());
                                if (!st.isElectricity())
                                    marker.setAlpha(0.6F);
                                else
                                    marker.setAlpha(1.0F);
                                int color = R.drawable.green;
                                switch (st.getState()) {
                                    case OPERATIVE:
                                        color = R.drawable.green;
                                        if (st.isOverflow_activation())
                                            color = R.drawable.green_dot;
                                        break;
                                    case WORK_IN_PROGRESS:
                                        if (!prefs.getBoolean("working_stations", true))
                                            marker.setVisible(false);
                                        color = R.drawable.pink;
                                        if (st.isOverflow_activation())
                                            color = R.drawable.pink_dot;
                                        break;
                                    case MAINTENANCE:
                                        if (!prefs.getBoolean("closed_stations", true))
                                            marker.setVisible(false);
                                        color = R.drawable.purple;
                                        if (st.isOverflow_activation())
                                            color = R.drawable.purple_dot;
                                        break;
                                    case CLOSE:
                                        if (!prefs.getBoolean("closed_stations", true))
                                            marker.setVisible(false);
                                        color = R.drawable.purple;
                                        if (st.isOverflow_activation())
                                            color = R.drawable.purple_dot;
                                        break;
                                    case UNKNOWN:
                                        if (!prefs.getBoolean("unknown_stations", false))
                                            marker.setVisible(false);
                                        marker.setAlpha(0.2F);
                                        color = R.drawable.pink;
                                        break;
                                    case DELETED:
                                        if (!prefs.getBoolean("deleted_stations", false))
                                            marker.setVisible(false);
                                        marker.setAlpha(1.0F);
                                        color = R.drawable.purple;
                                }
                                if (st.getState() != Station.State.OPERATIVE && st.getState() != Station.State.CLOSE && st.getState() != Station.State.MAINTENANCE && st.getNb_edock() > 0) {
                                    color = R.drawable.lightblue;
                                    if (st.isOverflow_activation())
                                        color = R.drawable.lightblue_dot;
                                    marker.setVisible(true);
                                }
                                if (st.getState() == Station.State.OPERATIVE && st.getNb_bike() + st.getNb_ebike() + st.getNb_free_dock() + st.getNb_free_edock() == 0) {
                                    color = R.drawable.grey;
                                    marker.setAlpha(0.8F);
                                    if (!prefs.getBoolean("almost_working_stations", true))
                                        marker.setVisible(false);
                                }
                                else if (st.getState() == Station.State.OPERATIVE) {
                                    long last_retrait_time = System.currentTimeMillis() - st.getLast_movement().getTime();
                                    int lmp = Integer.parseInt(prefs.getString("last_movement", "42"));
                                    if (last_retrait_time > 24 * 3600 * 1000) {
                                        if (lmp < 42)
                                            marker.setVisible(false);
                                        color = R.drawable.blue;
                                        if (st.isOverflow_activation())
                                            color = R.drawable.blue_dot;
                                    }
                                    else if (last_retrait_time > 12 * 3600 * 1000) {
                                        if (lmp < 24)
                                            marker.setVisible(false);
                                        color = R.drawable.red;
                                        if (st.isOverflow_activation())
                                            color = R.drawable.red_dot;
                                    }
                                    else if (last_retrait_time > 3 * 3600 * 1000) {
                                        if (lmp < 12)
                                            marker.setVisible(false);
                                        color = R.drawable.orange;
                                        if (st.isOverflow_activation())
                                            color = R.drawable.orange_dot;
                                    }
                                    else if (last_retrait_time > 3600 * 1000) {
                                        if (lmp < 3)
                                            marker.setVisible(false);
                                        color = R.drawable.yellow;
                                        if (st.isOverflow_activation())
                                            color = R.drawable.yellow_dot;
                                    }
                                }
                                marker.setIcon(BitmapDescriptorFactory.fromResource(color));

                                if (!st.isElectricity() && prefs.getBoolean("electrified_stations", false))
                                    marker.setVisible(false);

                                String snippet = "";
                                if (st.getState() == Station.State.OPERATIVE || st.getNb_free_edock() > 0) {
                                    snippet += "Vélos mécaniques : " + st.getNb_bike() + " (plus " + st.getNb_bike_overflow() + " en overflow)" + "\n";
                                    snippet += "Vélos électriques : " + st.getNb_ebike() + " (plus " + st.getNb_ebike_overflow() + " en overflow)" + "\n";
                                    snippet += "Places libres : " + st.getNb_free_edock() + " alimentées, " + st.getNb_free_dock() + " non alimentées" + "\n";
                                    snippet += "Places totales : " + st.getNb_edock() + " alimentées, " + st.getNb_dock() + " non alimentées" + "\n";
                                }
                                if (st.getState() == Station.State.WORK_IN_PROGRESS) {
                                    snippet += "Officiellement en travaux\n";
                                    snippet += "Mise en service prévue : " + new SimpleDateFormat("dd/MM/yyyy").format(st.getPlanned_date()) + "\n";
                                }
                                if (st.getState() == Station.State.UNKNOWN) {
                                    snippet += "Pas encore officiellement en travaux\n";
                                    snippet += "Pas de date de mise en service à donner\n";
                                }
                                if (st.getState() == Station.State.OPERATIVE || st.getNb_free_edock() > 0) {
                                    snippet += "Park+ : " + (st.isOverflow() ? "Possible (max " + st.getMax_bike_overflow() + " vélos)" : "Impossible") + "\n";
                                    snippet += "\n";
                                    snippet += "Dernier mouvement : " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(st.getLast_movement()) + "\n";
                                    snippet += "Dernier retrait : " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(st.getLast_retrait());
                                }
                                marker.setSnippet(snippet);
                            }

                            final AutoCompleteTextView textView = findViewById(R.id.search_bar);
                            Station[] st_array = stations.values().toArray(new Station[0]);
                            Arrays.sort(st_array, new Comparator<Station>() {
                                @Override
                                public int compare(Station o1, Station o2) {
                                    return Integer.compare(o1.getCode(), o2.getCode());
                                }
                            });
                            final ArrayAdapter<Station> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.support_simple_spinner_dropdown_item, st_array);
                            textView.setAdapter(adapter);
                            textView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    Station station = adapter.getItem(position);
                                    assert station != null;
                                    textView.setText("");
                                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(station.getLatitude(), station.getLongitude()), 15.0F));
                                    for (Marker marker : markers) {
                                        if (marker.getTag() == (Integer) station.getCode()) {
                                            marker.showInfoWindow();
                                            break;
                                        }
                                    }
                                }
                            });
                        }
                    });
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }
}
