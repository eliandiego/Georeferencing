package com.patloew.georeferencingsample.geoData

import android.location.Location

/**
 * Created by maciek on 12/26/17.
 */
typealias MapaNrLokacja =  Map<Int, Location>

fun ListToMapGeolocations(l : List<GeoLocation>) : Map<Int, GeoLocation>{



    var result = HashMap<Int, GeoLocation>();

    for( ll in l){
        result[ll.num] = ll;
    }

    return result;

}

public fun computeDistances(l: List<GeoLocation>) : String {

    var result: String = ""

    val mapa = ListToMapGeolocations(l)

    for( ll in l){

        val base : GeoLocation?  = mapa[0]

        result += computeDistance(ll, base) + "\n"

    }

    return result
}

fun computeDistance(l1 : GeoLocation, l2: GeoLocation?) : String {

    val ll1 : Location? = l1.location;
    val ll2 : Location? = l2?.location;
    assert( ll1 != null && ll2 != null);
    return ll1?.distanceTo(ll2).toString()


}