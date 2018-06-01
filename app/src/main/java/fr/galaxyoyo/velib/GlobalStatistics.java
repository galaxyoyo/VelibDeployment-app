package fr.galaxyoyo.velib;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.common.collect.Lists;
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

import java.net.URL;
import java.nio.charset.Charset;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class GlobalStatistics extends AppCompatActivity {

    private XYMultipleSeriesRenderer stations_renderer = new XYMultipleSeriesRenderer();
    private XYMultipleSeriesRenderer bikes_renderer = new XYMultipleSeriesRenderer();
    private XYMultipleSeriesRenderer journeys_renderer = new XYMultipleSeriesRenderer();
    private XYMultipleSeriesDataset stations_dataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesDataset bikes_dataset = new XYMultipleSeriesDataset();
    private XYMultipleSeriesDataset journeys_dataset = new XYMultipleSeriesDataset();

    @SuppressLint({"SetTextI18n", "SimpleDateFormat"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_global_statistics);

        AdView adView = findViewById(R.id.adView);
        adView.loadAd(new AdRequest.Builder().build());

        if (MapsActivity.cities == null || MapsActivity.stations.isEmpty()) {
            startActivity(new Intent(GlobalStatistics.this, MapsActivity.class));
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
                            startActivity(new Intent(GlobalStatistics.this, MapsActivity.class));
                        else if (item.getItemId() == R.id.nav_station_statistics)
                            startActivity(new Intent(GlobalStatistics.this, StationStatistics.class));
                        else if (item.getItemId() == R.id.nav_avancement)
                            startActivity(new Intent(GlobalStatistics.this, Avancement.class));
                        else if (item.getItemId() == R.id.nav_help)
                            startActivity(new Intent(GlobalStatistics.this, HelpActivity.class));
                        else if (item.getItemId() == R.id.nav_manage)
                            startActivity(new Intent(GlobalStatistics.this, SettingsActivity.class));

                        return true;
                    }
                });
        navigationView.getMenu().getItem(1).setChecked(true);

        int stations_opened = 0, working_stations = 0, working_already_opened = 0, closed_stations = 0, maintenance = 0,
                working_functionnal_stations = 0, functionnal_notworking_stations = 0, electrified = 0, deleted = 0, planned = 0, total = 0;
        int less_an_hour = 0, less_three_hours = 0, less_twelve_hours = 0, less_a_day = 0, more_a_day = 0;
        int bikes = 0, ebikes = 0, bikes_overflow = 0, ebikes_overflow = 0, free_docks = 0, total_docks = 0;
        for (Station st : MapsActivity.stations.values()) {
            if (st.getActivation_date() != null) {
                if (st.getState() == Station.State.OPERATIVE)
                    ++stations_opened;
                else if (st.getState() == Station.State.CLOSE)
                    ++closed_stations;
                else if (st.getState() == Station.State.MAINTENANCE)
                    ++maintenance;
                else if (st.getState() == Station.State.WORK_IN_PROGRESS)
                    ++working_already_opened;
                bikes += st.getNb_bike();
                ebikes += st.getNb_ebike();
                bikes_overflow += st.getNb_bike_overflow();
                ebikes_overflow += st.getNb_ebike_overflow();
                free_docks += st.getNb_free_edock() + st.getNb_free_dock();
                total_docks += st.getNb_dock() + st.getNb_edock();

                if (st.getNb_free_edock() == 0 && st.getState() == Station.State.OPERATIVE)
                    ++functionnal_notworking_stations;

                long last_retrait_time = System.currentTimeMillis() - st.getLast_movement().getTime();
                if (last_retrait_time > 24 * 3600 * 1000)
                    ++more_a_day;
                else if (last_retrait_time > 12 * 3600 * 1000)
                    ++less_a_day;
                else if (last_retrait_time > 3 * 3600 * 1000)
                    ++less_twelve_hours;
                else if (last_retrait_time > 3600 * 1000)
                    ++less_three_hours;
                else
                    ++less_an_hour;
            }
            else if (st.getState() == Station.State.WORK_IN_PROGRESS)
                ++working_stations;
            else if (st.getState() == Station.State.CLOSE)
                ++closed_stations;

            if (st.getState() == Station.State.WORK_IN_PROGRESS && st.getNb_free_edock() > 0)
                ++working_functionnal_stations;

            if (st.getState() == Station.State.DELETED)
                ++deleted;
            else if (st.getState() == Station.State.UNKNOWN)
                ++planned;
            else
                ++total;

            if (st.isElectricity())
                ++electrified;
        }

        ProgressBar progressBar = findViewById(R.id.progress);
        progressBar.setMax(1400);
        progressBar.setProgress(stations_opened);

        Calendar today = new GregorianCalendar();
        today.setTimeInMillis(System.currentTimeMillis());
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        TextView stats = findViewById(R.id.stats);
        String text = "";
        text += stations_opened + " stations ouvertes sur 1400 au " + new SimpleDateFormat("dd MMM yyyy 'à' HH:mm").format(new Date()) + "\n";
        text += working_stations + " stations sont en travaux" + (working_already_opened > 0 ? " (plus " + working_already_opened + " déjà activée" + (working_already_opened > 1 ? "s" : "") + " précédemment)" : "") + "\n";
        text += electrified + " stations sont alimentées (dernière mise à jour : mi mai)\n";
        text += closed_stations + " station" + (closed_stations > 1 ? "s" : "") + " fermée" + (closed_stations > 1 ? "s" : "") + "\n";
        if (maintenance > 0)
            text += maintenance + " station" + (maintenance > 1 ? "s sont" : " est") + " en maintenance" + "\n";
        text += planned + " anciennes stations sont en attente de travaux\n";
        text += deleted + " anciennes stations sont supprimées\n";
        text += total + " stations sont référencées au total\n";
        text += less_an_hour + " stations affichent un mouvement depuis la dernière heure\n";
        text += less_three_hours + " stations affichent un mouvement depuis les trois dernières heures\n";
        text += less_twelve_hours + " stations affichent un mouvement depuis les douze dernières heures\n";
        text += less_a_day + " stations affichent un mouvement depuis les dernières 24 heures\n";
        text += more_a_day + " stations n'affichent aucun mouvement depuis 24 heures\n";
        if (working_functionnal_stations > 0)
            text += working_functionnal_stations + (working_functionnal_stations > 1 ? " stations sont" : " station est") + " en travaux avec des bornes disponibles\n";
        if (functionnal_notworking_stations > 0)
            text += functionnal_notworking_stations + (functionnal_notworking_stations > 1 ? " stations fonctionnent officiellement mais n'ont" : " station fonctionne officiellement mais n'a")
                    + " pas de borne disponible\n";
        text += "\n";
        text += (bikes + bikes_overflow) + " vélos mécaniques (soit " + bikes + " en bornettes et " + bikes_overflow + " en overflow)\n";
        text += (ebikes + ebikes_overflow) + " vélos électriques (soit " + ebikes + " en bornettes et " + ebikes_overflow + " en overflow)\n";
        // TODO Fix this on API update
        total_docks = free_docks + bikes + ebikes;
        text += free_docks + " emplacements libres (sur un total de " + total_docks + ", soit " + ((float) (int) (1000.0 * free_docks / (float) total_docks) / 10.0) + " %)\n";
        stats.setText(text);

        ListView weekOpenning = findViewById(R.id.week_openning);
        final List<Station> opened = Lists.newArrayList();
        for (Station station : MapsActivity.stations.values()) {
            if (station.getActivation_date() == null)
                continue;

            Calendar cal = new GregorianCalendar();
            cal.setTime(station.getActivation_date());
            if (cal.get(Calendar.WEEK_OF_YEAR) == today.get(Calendar.WEEK_OF_YEAR))
                opened.add(station);
        }
        Collections.sort(opened, new Comparator<Station>() {
            @Override
            public int compare(Station o1, Station o2) {
                int i = o1.getActivation_date().compareTo(o2.getActivation_date());
                if (i == 0)
                    i = Integer.compare(o1.getCode(), o2.getCode());
                return i;
            }
        });

        List<Map<String, String>> data = Lists.newArrayList();
        for (Station station : opened) {
            Map<String, String> map = new HashMap<>(2);
            map.put("station", station.toString());
            map.put("date", new SimpleDateFormat("EEEE d MMMM yyyy").format(station.getActivation_date()));
            data.add(map);
        }
        SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2, new String[] {"station", "date"}, new int[] {android.R.id.text1, android.R.id.text2});
        weekOpenning.setAdapter(adapter);
        weekOpenning.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Station st = opened.get(position);
                assert st != null;

                Intent intent = new Intent(GlobalStatistics.this, StationStatistics.class);
                intent.putExtra("station", st.getCode());
                startActivity(intent);
            }
        });
        weekOpenning.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, opened.size() * 179));

        for (XYMultipleSeriesRenderer renderer : new XYMultipleSeriesRenderer[]{stations_renderer, bikes_renderer, journeys_renderer}) {
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

        stations_renderer.setChartTitle("Stations ouvertes");
        stations_renderer.setXTitle("Jour");
        stations_renderer.setYTitle("Nombre de stations");
        stations_renderer.setXAxisMin(1514761200000L);
        stations_renderer.setXAxisMax(System.currentTimeMillis() / 86400000 * 86400000);
        stations_renderer.setPanLimits(new double[]{stations_renderer.getXAxisMin(), stations_renderer.getXAxisMax(), 0, 1400});

        bikes_renderer.setChartTitle("Disponibilité vélos");
        bikes_renderer.setXTitle("Heure");
        bikes_renderer.setYTitle("Nombre de vélos");
        bikes_renderer.setXAxisMin(System.currentTimeMillis() / 60000 * 60000 - 86400000L);
        bikes_renderer.setXAxisMax(System.currentTimeMillis() / 60000 * 60000);
        bikes_renderer.setPanEnabled(true);

        journeys_renderer.setChartTitle("Trajets quotidiens");
        journeys_renderer.setXTitle("Jour");
        journeys_renderer.setYTitle("Nombre de dépôts/retraits");

        XYSeriesRenderer stationsRenderer = new XYSeriesRenderer();
        stationsRenderer.setColor(Color.RED);
        stationsRenderer.setPointStyle(PointStyle.DIAMOND);
        stationsRenderer.setDisplayChartValues(false);
        stationsRenderer.setLineWidth(3);
        stationsRenderer.setChartValuesTextSize(15);
        stationsRenderer.setGradientEnabled(true);
        stations_renderer.addSeriesRenderer(stationsRenderer);
        XYSeriesRenderer thisDay = new XYSeriesRenderer();
        thisDay.setPointStyle(PointStyle.DIAMOND);
        thisDay.setDisplayChartValues(false);
        thisDay.setLineWidth(3);
        thisDay.setChartValuesTextSize(15);
        thisDay.setColor(Color.MAGENTA);
        thisDay.setGradientEnabled(true);
        stations_renderer.addSeriesRenderer(thisDay);

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
        bikes_renderer.addSeriesRenderer(mecaRenderer);
        bikes_renderer.addSeriesRenderer(mecaTotRenderer);
        bikes_renderer.addSeriesRenderer(elecRenderer);
        bikes_renderer.addSeriesRenderer(elecTotRenderer);

        XYSeriesRenderer depotsRenderer = new XYSeriesRenderer();
        depotsRenderer.setColor(Color.BLUE);
        depotsRenderer.setPointStyle(PointStyle.DIAMOND);
        depotsRenderer.setDisplayChartValues(false);
        depotsRenderer.setLineWidth(3);
        depotsRenderer.setChartValuesTextSize(15);
        depotsRenderer.setGradientEnabled(true);
        journeys_renderer.addSeriesRenderer(depotsRenderer);
        XYSeriesRenderer retraitsRenderer = new XYSeriesRenderer();
        retraitsRenderer.setPointStyle(PointStyle.DIAMOND);
        retraitsRenderer.setDisplayChartValues(false);
        retraitsRenderer.setLineWidth(3);
        retraitsRenderer.setChartValuesTextSize(15);
        retraitsRenderer.setColor(Color.RED);
        retraitsRenderer.setGradientEnabled(true);
        journeys_renderer.addSeriesRenderer(retraitsRenderer);

        TimeChart stations_chart = new TimeChart(stations_dataset, stations_renderer);
        stations_chart.setDateFormat("dd/MM");

        TimeChart bikes_chart = new TimeChart(bikes_dataset, bikes_renderer);
        bikes_chart.setDateFormat("dd/MM HH:mm");

        TimeChart journeys_chart = new TimeChart(journeys_dataset, journeys_renderer);
        journeys_chart.setDateFormat("dd/MM");

        GraphicalView stations_view = new GraphicalView(getApplicationContext(), stations_chart);
        ((LinearLayout) findViewById(R.id.global_linear_layout)).addView(stations_view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 1000));

        GraphicalView bikes_view = new GraphicalView(getApplicationContext(), bikes_chart);
        ((LinearLayout) findViewById(R.id.global_linear_layout)).addView(bikes_view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 1000));

        GraphicalView journeys_view = new GraphicalView(getApplicationContext(), journeys_chart);
        ((LinearLayout) findViewById(R.id.global_linear_layout)).addView(journeys_view, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 1000));

        ((LinearLayout) findViewById(R.id.global_linear_layout)).removeView(adView);
        ((LinearLayout) findViewById(R.id.global_linear_layout)).addView(adView);

        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, 2018);
        cal.set(Calendar.MONTH, Calendar.JANUARY);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        final Calendar yesterday = new GregorianCalendar();
        yesterday.setTime(today.getTime());
        yesterday.add(Calendar.DAY_OF_YEAR, -1);

        TimeSeries station_series = new TimeSeries("Stations ouvertes");
        stations_dataset.addSeries(station_series);

        TimeSeries thisday_series = new TimeSeries("Ce jour");
        stations_dataset.addSeries(thisday_series);

        while (cal.getTimeInMillis() <= today.getTimeInMillis()) {
            int count = 0;
            int thisday = 0;
            for (Station station : MapsActivity.stations.values()) {
                if (station.getActivation_date() != null && station.getActivation_date().before(cal.getTime()))
                    ++count;
                else if (station.getActivation_date() != null && station.getActivation_date().equals(cal.getTime())) {
                    ++count;
                    ++thisday;
                }
            }

            station_series.add(cal.getTime(), count);
            thisday_series.add(cal.getTime(), thisday);
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        final TimeSeries meca = new TimeSeries("Vélos mécaniques");
        bikes_dataset.addSeries(meca);
        final TimeSeries mecaTot = new TimeSeries("Vélos mécaniques (dont overflow)");
        bikes_dataset.addSeries(mecaTot);

        final TimeSeries elec = new TimeSeries("Vélos électriques");
        bikes_dataset.addSeries(elec);
        final TimeSeries elecTot = new TimeSeries("Vélos électriques (dont overflow)");
        bikes_dataset.addSeries(elecTot);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String json = IOUtils.toString(new URL("http://galaxyoyo.com/velib/global_infos.php?after=" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(yesterday.getTime()) + "&period=1000"), Charset.defaultCharset());
                    List<Map<String, String>> list = MapsActivity.gson.fromJson(json, new TypeToken<ArrayList<HashMap<String, String>>>() {
                    }.getType());

                    for (int i = 0; i < list.size(); i++) {
                        Map<String, String> info = list.get(i);

                        @SuppressLint("SimpleDateFormat") Date record_time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(info.get("record_time"));

                        meca.add(record_time.getTime(), Double.parseDouble(info.get("nb_bike")));
                        mecaTot.add(record_time.getTime(), Double.parseDouble(info.get("nb_bike")) + Double.parseDouble(info.get("nb_bike_overflow")));
                        elec.add(record_time.getTime(), Double.parseDouble(info.get("nb_ebike")));
                        elecTot.add(record_time.getTime(), Double.parseDouble(info.get("nb_ebike")) + Double.parseDouble(info.get("nb_ebike_overflow")));
                    }

                    bikes_renderer.setYAxisMax(Math.max(mecaTot.getMaxY(), elecTot.getMaxY()) + 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        final TimeSeries depots = new TimeSeries("Dépôts");
        journeys_dataset.addSeries(depots);
        final TimeSeries retraits = new TimeSeries("Retraits");
        journeys_dataset.addSeries(retraits);

        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String json = IOUtils.toString(new URL("http://galaxyoyo.com/velib/journeys_global.php"), Charset.defaultCharset());
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

        GridLayout per_week_opening = findViewById(R.id.per_week_opening);

        for (int i = 1; i <= today.get(Calendar.WEEK_OF_YEAR); ++i) {
            int count = 0;
            for (Station station : MapsActivity.stations.values()) {
                if (station.getActivation_date() == null)
                    continue;

                cal = new GregorianCalendar();
                cal.setTime(station.getActivation_date());
                if (cal.get(Calendar.WEEK_OF_YEAR) == i)
                    ++count;
            }

            cal = new GregorianCalendar();
            cal.set(Calendar.YEAR, 2018);
            cal.set(Calendar.WEEK_OF_YEAR, i);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            TextView week = new TextView(this);
            week.setText("Semaine " + i + "\t\t");
            per_week_opening.addView(week);

            TextView number = new TextView(this);
            number.setText(count + " station" + (count >= 2 ? "s" : ""));
            number.setGravity(Gravity.END);
            per_week_opening.addView(number);
        }
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
