package fr.galaxyoyo.velib;

import com.google.common.collect.Lists;

import java.util.List;

import lombok.Getter;

public class City {
    @Getter
    private int cp;

    @Getter
    private String name;

    @Getter
    private String fullname;

    @Getter
    private int code_start, code_end;

    private List<Station> stations;

    public List<Station> getStations() {
        if (stations == null)
            updateStations();
        return stations;
    }

    public void updateStations() {
       stations = Lists.newArrayList();
        for (Station st : MapsActivity.stations.values()) {
            if (st.getCode() >= code_start && st.getCode() <= code_end)
                stations.add(st);
        }
    }
}
