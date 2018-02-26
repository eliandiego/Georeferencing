package com.patloew.georeferencingsample.mapsHelpers

import android.location.Location
import com.patloew.georeferencingsample.geoData.GeoLocation


fun getBoundingRect( points: MutableMap<Int, GeoLocation> ) : Pair<Location?, Location?> {


    var  eastSouth : Location? = null;
    var  northWest: Location? = null;

    for(o in points.values){

        if(eastSouth == null) {
            eastSouth = Location("");

            eastSouth!!.latitude=   o.location!!.latitude;
            eastSouth!!.longitude=   o.location!!.longitude;
        }else {
            if (o!!.location!!.latitude > eastSouth.latitude)
                eastSouth.latitude = o.location!!.latitude
            if (o!!.location!!.longitude < eastSouth.latitude)
                eastSouth.longitude = o.location!!.longitude
        }

        if(northWest == null) {
            northWest = Location("");
            northWest!!.latitude = o.location!!.latitude;
            northWest!!.longitude = o.location!!.longitude;
        }else {
            if (o!!.location!!.latitude < northWest.latitude)
                northWest.latitude = o.location!!.latitude
            if (o!!.location!!.longitude > northWest.latitude)
                northWest.longitude = o.location!!.longitude
        }

    }



    return Pair(eastSouth, northWest);

}