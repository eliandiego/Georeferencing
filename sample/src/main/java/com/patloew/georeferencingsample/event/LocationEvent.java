package com.patloew.georeferencingsample.event;

import android.location.Location;

import java.util.Date;

/**
 * Created by gunhansancar on 06/04/16.
 */
public class LocationEvent {
    private Location location;
    private int id;

    public LocationEvent(Location l) {
        this.id = 3;
        this.location =  l;
    }

    public Location getLocation() {
        return location;
    }

    public int getId() {
        return id;
    }
}
