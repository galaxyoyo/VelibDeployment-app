package fr.galaxyoyo.velib;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.gson.reflect.TypeToken;

import org.achartengine.GraphicalView;
import org.achartengine.chart.PointStyle;
import org.achartengine.chart.TimeChart;
import org.achartengine.model.TimeSeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.BasicStroke;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class StationStatistics extends AppCompatActivity {

    private XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
    private XYMultipleSeriesRenderer journeys_renderer = new XYMultipleSeriesRenderer();
    private XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesDataset journeys = new XYMultipleSeriesDataset();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_statistics);

        AdView adView = findViewById(R.id.adView);
        adView.loadAd(new AdRequest.Builder().build());

        if (MapsActivity.cities == null || MapsActivity.stations.isEmpty()) {
            startActivity(new Intent(StationStatistics.this, MapsActivity.class));
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
                            startActivity(new Intent(StationStatistics.this, MapsActivity.class));
                        else if (item.getItemId() == R.id.nav_global_statistics)
                            startActivity(new Intent(StationStatistics.this, GlobalStatistics.class));
                        else if (item.getItemId() == R.id.nav_help)
                            startActivity(new Intent(StationStatistics.this, HelpActivity.class));
                        else if (item.getItemId() == R.id.nav_avancement)
                            startActivity(new Intent(StationStatistics.this, Avancement.class));
                        else if (item.getItemId() == R.id.nav_manage)
                            startActivity(new Intent(StationStatistics.this, SettingsActivity.class));

                        return true;
                    }
                });
        navigationView.getMenu().getItem(2).setChecked(true);

        final AutoCompleteTextView textView = findViewById(R.id.station_search);
        Station[] st_array = MapsActivity.stations.values().toArray(new Station[0]);
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

                selectStation(station);
            }
        });

        for (XYMultipleSeriesRenderer renderer : new XYMultipleSeriesRenderer[]{renderer, journeys_renderer}) {
            renderer.setAxisTitleTextSize(16);
            renderer.setChartTitleTextSize(20);
            renderer.setLabelsTextSize(15);
            renderer.setLegendTextSize(20);
            renderer.setPointSize(5f);
            renderer.setMargins(new int[]{30, 40, 20, 20});
            renderer.setMarginsColor(Color.WHITE);
            renderer.setXLabels(10);
            renderer.setYLabels(10);
            renderer.setYAxisMin(0);
            //renderer.setAxesColor(context.getResources().getColor(R.color.grey));
            renderer.setLabelsColor(Color.BLACK);
            renderer.setApplyBackgroundColor(true);
            //renderer.setGridColor(context.getResources().getColor(R.color.grey_light));
            renderer.setShowGrid(true);
            renderer.setShowLegend(true);
            renderer.setShowLabels(true);
            renderer.setZoomEnabled(true, false);
            renderer.setPanEnabled(true, false);
        }


        renderer.setChartTitle("Disponibilité vélos");
        renderer.setXTitle("Heure");
        renderer.setYTitle("Nombre de vélos");
        renderer.setXAxisMin(System.currentTimeMillis() / 60000 * 60000 - 86400000L);
        renderer.setXAxisMax(System.currentTimeMillis() / 60000 * 60000);

        journeys_renderer.setChartTitle("Trajets quotidiens");
        journeys_renderer.setXTitle("Date");
        journeys_renderer.setYTitle("Dépôts/retraits");

        XYSeriesRenderer mecaRenderer = new XYSeriesRenderer(), mecaTotRenderer = new XYSeriesRenderer();
        XYSeriesRenderer elecRenderer = new XYSeriesRenderer(), elecTotRenderer = new XYSeriesRenderer();
        mecaRenderer.setColor(Color.GREEN);
        mecaRenderer.setPointStyle(PointStyle.DIAMOND);
        mecaRenderer.setDisplayChartValues(false);
        mecaRenderer.setLineWidth(3);
        mecaRenderer.setChartValuesTextSize(15);
        mecaRenderer.setGradientEnabled(true);
        mecaTotRenderer.setColor(Color.GREEN);
        mecaTotRenderer.setPointStyle(PointStyle.DIAMOND);
        mecaTotRenderer.setDisplayChartValues(false);
        mecaTotRenderer.setLineWidth(2);
        mecaTotRenderer.setChartValuesTextSize(15);
        mecaTotRenderer.setGradientEnabled(true);
        mecaTotRenderer.setStroke(BasicStroke.DASHED);
        elecRenderer.setColor(Color.CYAN);
        elecRenderer.setPointStyle(PointStyle.DIAMOND);
        elecRenderer.setDisplayChartValues(false);
        elecRenderer.setLineWidth(3);
        elecRenderer.setChartValuesTextSize(15);
        elecRenderer.setGradientEnabled(true);
        elecTotRenderer.setColor(Color.CYAN);
        elecTotRenderer.setPointStyle(PointStyle.DIAMOND);
        elecTotRenderer.setDisplayChartValues(false);
        elecTotRenderer.setLineWidth(2);
        elecTotRenderer.setChartValuesTextSize(15);
        elecTotRenderer.setGradientEnabled(true);
        elecTotRenderer.setStroke(BasicStroke.DASHED);
        renderer.addSeriesRenderer(mecaRenderer);
        renderer.addSeriesRenderer(mecaTotRenderer);
        renderer.addSeriesRenderer(elecRenderer);
        renderer.addSeriesRenderer(elecTotRenderer);

        XYSeriesRenderer depotsRenderer = new XYSeriesRenderer();
        XYSeriesRenderer retraitsRenderer = new XYSeriesRenderer();
        depotsRenderer.setColor(Color.BLUE);
        depotsRenderer.setPointStyle(PointStyle.DIAMOND);
        depotsRenderer.setDisplayChartValues(false);
        depotsRenderer.setLineWidth(3);
        depotsRenderer.setChartValuesTextSize(15);
        depotsRenderer.setGradientEnabled(true);
        retraitsRenderer.setColor(Color.RED);
        retraitsRenderer.setPointStyle(PointStyle.DIAMOND);
        retraitsRenderer.setDisplayChartValues(false);
        retraitsRenderer.setLineWidth(3);
        retraitsRenderer.setChartValuesTextSize(15);
        retraitsRenderer.setGradientEnabled(true);
        journeys_renderer.addSeriesRenderer(depotsRenderer);
        journeys_renderer.addSeriesRenderer(retraitsRenderer);

        TimeChart chart = new TimeChart(dataset, renderer);
        chart.setDateFormat("dd/MM HH:mm");

        TimeChart journeys_chart = new TimeChart(journeys, journeys_renderer);
        chart.setDateFormat("dd/MM");

        GraphicalView view = new GraphicalView(getApplicationContext(), chart);
        ((LinearLayout) findViewById(R.id.stations_linear_layout)).addView(view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1000));
        GraphicalView journeys_view = new GraphicalView(getApplicationContext(), journeys_chart);
        ((LinearLayout) findViewById(R.id.stations_linear_layout)).addView(journeys_view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1000));

        ((LinearLayout) findViewById(R.id.stations_linear_layout)).removeView(adView);
        ((LinearLayout) findViewById(R.id.stations_linear_layout)).addView(adView);

        if (getIntent().hasExtra("station"))
            selectStation(MapsActivity.stations.get(getIntent().getIntExtra("station", 0)));
    }

    @SuppressLint({"SimpleDateFormat", "SetTextI18n"})
    private void selectStation(final Station station) {
        String snippet = "";
        snippet += "Adresse : " + station.getAddress() + "\n";
        if (station.getState() == Station.State.OPERATIVE || station.getNb_free_edock() > 0) {
            snippet += "Vélos mécaniques : " + station.getNb_bike() + " (dont " + station.getNb_bike_overflow() + " en overflow)" + "\n";
            snippet += "Vélos électriques : " + station.getNb_ebike() + " (dont " + station.getNb_ebike_overflow() + " en overflow)" + "\n";
            snippet += "Places libres : " + station.getNb_free_edock() + " alimentées, " + station.getNb_free_dock() + " non alimentées" + "\n";
            snippet += "Places totales : " + station.getNb_edock() + " alimentées, " + station.getNb_dock() + " non alimentées" + "\n";
        }
        snippet += "Mise en service prévue : " + new SimpleDateFormat("dd/MM/yyyy").format(station.getPlanned_date()) + "\n";
        if (station.getState() == Station.State.WORK_IN_PROGRESS)
            snippet += "Officiellement en travaux\n";
        else if (station.getState() == Station.State.DELETED)
            snippet += "Cette station a été supprimée\n";
        else if (station.getState() == Station.State.UNKNOWN)
            snippet += "Cette station n'est pas encore officiellement référencée\n";
        if (station.getState() == Station.State.CLOSE)
            snippet += "Cette station est fermée\n";
        if (station.getState() == Station.State.OPERATIVE || station.getNb_free_edock() > 0) {
            if (station.getActivation_date() != null)
                snippet += "Activée le : " + new SimpleDateFormat("dd/MM/yyyy").format(station.getActivation_date()) + "\n";
            snippet += station.isCredit_card() ? "Accepte les cartes de crédit\n" : "N'accepte pas les cartes de crédit\n";
            snippet += station.isKiosk_state() ? "Le totem fonctionne (pincettes)\n" : "Le totem ne fonctionne pas (pincettes)\n";
            snippet += "Park+ : " + (station.isOverflow() ? "Possible (max " + station.getMax_bike_overflow() + " vélos)" : "Impossible") + "\n";
            snippet += "\n";
            snippet += "Dernier mouvement : " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(station.getLast_movement()) + "\n";
            snippet += "Dernier retrait : " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(station.getLast_retrait()) + "\n";
            snippet += "Alimentée : " + (station.isElectricity() ? "oui" : "non");
        }

        TextView title = findViewById(R.id.station_title);
        title.setText(station.getCode() + " - " + station.getName());
        title.setTypeface(Typeface.DEFAULT_BOLD);

        TextView view = findViewById(R.id.station_info);
        view.setText(snippet);

        Button electrify = findViewById(R.id.electrify);
        electrify.setVisibility(station.getState() == Station.State.OPERATIVE ? View.VISIBLE : View.INVISIBLE);
        electrify.setText(!station.isElectricity() ? getString(R.string.electrified) : getString(R.string.non_electrified));
        electrify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                station.setElectricity(!station.isElectricity());

                Executors.newSingleThreadExecutor().execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            IOUtils.toByteArray(new URL("http://galaxyoyo.com/velib/electricity.php?state=" + (station.isElectricity() ? 1 : 0) + "&code=" + station.getCode()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

                selectStation(station);
            }
        });

        dataset.clear();

        final TimeSeries meca = new TimeSeries("Vélos mécaniques");
        dataset.addSeries(meca);
        final TimeSeries mecaTot = new TimeSeries("Vélos mécaniques (dont overflow)");
        dataset.addSeries(mecaTot);

        final TimeSeries elec = new TimeSeries("Vélos électriques");
        dataset.addSeries(elec);
        final TimeSeries elecTot = new TimeSeries("Vélos électriques (dont overflow)");
        dataset.addSeries(elecTot);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String json = IOUtils.toString(new URL("http://galaxyoyo.com/velib/station_infos.php?code=" + station.getCode() + "&after=2018-05-10 00:00:00"), Charset.defaultCharset());
                    List<Map<String, String>> list = MapsActivity.gson.fromJson(json, new TypeToken<ArrayList<HashMap<String, String>>>() {}.getType());

                    for (int i = 0; i < list.size(); i++) {
                        Map<String, String> info = list.get(i);
                        Map<String, String> next = i == list.size() - 1 ? null : list.get(i + 1);
                        Map<String, String> previous = i == 0 ? null : list.get(i - 1);

                        Date record_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(info.get("record_time"));
                        Date next_record_time = next == null ? new Date(System.currentTimeMillis() / 60000 * 60000) : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(next.get("record_time"));

                        if (previous == null || Double.parseDouble(info.get("nb_bike")) != Double.parseDouble(previous.get("nb_bike"))
                                || Double.parseDouble(info.get("nb_bike_overflow")) != Double.parseDouble(previous.get("nb_bike_overflow"))) {
                            meca.add(record_time.getTime(), Double.parseDouble(info.get("nb_bike")));
                            mecaTot.add(record_time.getTime(), Double.parseDouble(info.get("nb_bike")) + Double.parseDouble(info.get("nb_bike_overflow")));
                        }
                        if (previous == null || Double.parseDouble(info.get("nb_ebike")) != Double.parseDouble(previous.get("nb_ebike"))
                                || Double.parseDouble(info.get("nb_ebike_overflow")) != Double.parseDouble(previous.get("nb_ebike_overflow"))) {
                            elec.add(record_time.getTime(), Double.parseDouble(info.get("nb_ebike")));
                            elecTot.add(record_time.getTime(), Double.parseDouble(info.get("nb_ebike")) + Double.parseDouble(info.get("nb_ebike_overflow")));
                        }

                        if (next_record_time.getTime() - record_time.getTime() > 60000L) {
                            if (next == null || Double.parseDouble(info.get("nb_bike")) != Double.parseDouble(next.get("nb_bike"))) {
                                meca.add(next_record_time.getTime() - 60000L, Double.parseDouble(info.get("nb_bike")));
                                mecaTot.add(next_record_time.getTime() - 60000L, Double.parseDouble(info.get("nb_bike")) + Double.parseDouble(info.get("nb_bike_overflow")));
                            }
                            if (next == null || Double.parseDouble(info.get("nb_ebike")) != Double.parseDouble(next.get("nb_ebike"))) {
                                elec.add(next_record_time.getTime() - 60000L, Double.parseDouble(info.get("nb_ebike")));
                                elecTot.add(next_record_time.getTime() - 60000L, Double.parseDouble(info.get("nb_ebike")) + Double.parseDouble(info.get("nb_ebike_overflow")));
                            }
                        }

                        renderer.setYAxisMax(Math.max(mecaTot.getMaxY(), elecTot.getMaxY()) + 1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        final TimeSeries depots = new TimeSeries("Vélos déposés");
        journeys.addSeries(depots);
        final TimeSeries retraits = new TimeSeries("Vélos retirés");
        journeys.addSeries(retraits);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String json = IOUtils.toString(new URL("http://galaxyoyo.com/velib/journeys.php?code=" + station.getCode()), Charset.defaultCharset());
                    List<Map<String, String>> list = MapsActivity.gson.fromJson(json, new TypeToken<ArrayList<HashMap<String, String>>>() {
                    }.getType());

                    for (int i = 0; i < list.size(); i++) {
                        Map<String, String> info = list.get(i);

                        @SuppressLint("SimpleDateFormat") Date date = new SimpleDateFormat("yyyy-MM-dd").parse(info.get("date"));

                        depots.add(date.getTime(), Double.parseDouble(info.get("depots")));
                        retraits.add(date.getTime(), Double.parseDouble(info.get("retraits")));
                    }

                    journeys_renderer.setYAxisMax(Math.max(depots.getMaxY(), retraits.getMaxY()) + 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.drawer_view, menu);
        return true;
    }
}
