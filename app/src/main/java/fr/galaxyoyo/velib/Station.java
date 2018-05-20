package fr.galaxyoyo.velib;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

public class Station
{
    @Getter
    @Setter
    private int code;

    @Getter
    @Setter
    private String name;

    @Getter
    @Setter
    private State state;

    @Getter
    @Setter
    private boolean type;

    @Getter
    @Setter
    private Date planned_date, activation_date;

    @Getter
    @Setter
    private double latitude;

    @Getter
    @Setter
    private double longitude;

    @Getter
    @Setter
    private double altitude;

    @Getter
    @Setter
    private String address;

    @Getter
    @Setter
    private boolean credit_card;

    @Getter
    @Setter
    private boolean kiosk_state, overflow, overflow_activation;

    @Getter
    @Setter
    private int max_bike_overflow, density_level;

    @Getter
    @Setter
    private Date last_movement, last_retrait;

    @Getter
    @Setter
    private boolean electricity;

    @Getter
    @Setter
    private int nb_bike, nb_ebike, nb_free_dock, nb_free_edock;

    @Getter
    @Setter
    private int nb_dock, nb_edock, nb_bike_overflow, nb_ebike_overflow;

    public enum State
    {
        OPERATIVE, WORK_IN_PROGRESS, MAINTENANCE, CLOSE, UNKNOWN, DELETED
    }

    @Override
    public String toString()
    {
        return code + " " + name;
    }
}
