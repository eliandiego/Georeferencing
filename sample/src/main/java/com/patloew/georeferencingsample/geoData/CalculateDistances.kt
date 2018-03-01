package com.patloew.georeferencingsample.geoData

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import kotlin.math.pow
import kotlin.math.sqrt

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

public fun computeLampionPosition(l: GeoLocation,  proporcja: Double, lBase: GeoLocation?) {
    if(l.typ != PointType.LAMPION_OFF)
        return
    println("computing lampion position for pkt:" + l.num + "offsetX=" + l.offsetX)
    var a = Location("dd")
    //a.latitude = 52.2;
    //a.longitude = 21.2;
    a.latitude = lBase!!.location!!.latitude;
    a.longitude = lBase!!.location!!.longitude;

    var headingX = 90.0;
    var offX = l.offsetX;
    if(l.offsetX < 0) {
        offX = -offX;
        headingX = -90.0;
    }

    var headingY = 0.0;
    var offY = l.offsetY;
    if(offY < 0) {
        offY = -offY;
        headingY = -180.0;
    }

    val ln : LatLng = SphericalUtil.computeOffset(LatLng(a.latitude, a.longitude), offX * proporcja, headingX);
    val ln2 : LatLng = SphericalUtil.computeOffset(LatLng(ln.latitude, ln.longitude), offY * proporcja, headingY);
    //l.location = ln
    println("setting transformed lat" + ln.latitude)
    a.latitude = ln2.latitude
    a.longitude = ln2.longitude
    //computeOffset()

    l.location = a
}



public fun computeLampionDistances(l: List<GeoLocation>, odl : Double)  {

    var result: Double = 0.0;

    val mapa = ListToMapGeolocations(l)

    for( ll in l){

        if(ll.typ == PointType.LAMPION_OFF || ll.typ == PointType.LAMPION_MARKER) {

            // to niedobrze, nie chcemy przeliczac pkt juz obliczonych przy dodawaniu 2x odl. Zmienic! Liczyc to przy dodawaniu pkt
            if(ll.offsetX == 0.0 && ll.offsetY == 0.0)
                continue;

            val baseNr = ll.refLocationNum;
            val base: GeoLocation? = mapa[baseNr]

            computeLampionPosition(ll, odl, base)
        }

    }


}


public fun calibrateDistances(l: List<GeoLocation>) : Double {

    var result: Double = 0.0;

    val mapa = ListToMapGeolocations(l)
    var numOfDopasowanie : Int = 0;
    for( ll in l){

        //computeLampionPosition(ll)



        if(ll.typ == PointType.OSNOWA_MARKER || ll.typ == PointType.OSNOWA_GPS) {

            if(ll.refLocationNum == ll.num){
                println("continuing... reflocnum==num")
                continue
            }

            val baseNr = ll.refLocationNum;
            if(baseNr == -1)
                continue;
            val base: GeoLocation? = mapa[baseNr]

            val r = computeDistance(ll, base)
            if(r != 0.0){
                result += r;
                numOfDopasowanie++
            }
        }

    }
    result /= numOfDopasowanie

    println("wynik kalibracji=" + result)

    return result
}

fun computeDistance(l1 : GeoLocation, l2: GeoLocation?) : Double {

    val ll1 : Location? = l1.location;
    val ll2 : Location? = l2?.location;
    assert( ll1 != null && ll2 != null);
    var result: String = "";
    result += l1.num.toString() + "<->" + l2?.num.toString();

    val pointDistanceTerrainMeters  = ll1?.distanceTo(ll2)

    result += ":" + pointDistanceTerrainMeters.toString()
    result += "offsetX=" + l1!!.offsetX + " offsetY=" + l1.offsetY + "\n"
    val kwadrat  = l1!!.offsetY?.pow(2).plus( l1?.offsetX.pow(2));
    val pointDistanceCm=sqrt(kwadrat )
    result += "dist[cm]=" + pointDistanceCm;


    result += "dist cm/m" + pointDistanceCm/pointDistanceTerrainMeters!!
    val metersPerCm : Double = 1.0/(pointDistanceCm/pointDistanceTerrainMeters!!)
    result += "dist m/cm" + metersPerCm

    println(result)
    return metersPerCm


}