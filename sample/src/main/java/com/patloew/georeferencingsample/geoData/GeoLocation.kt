package com.patloew.georeferencingsample.geoData

//import com.google.android.gms.

import android.location.Location
import android.os.Environment
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import java.io.FileReader
import java.io.PrintWriter

import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.google.maps.android.SphericalUtil

/**
 * Created by maciek on 11/29/17.
 */

enum class PointType
{ LAMPION_MARKER, LAMPION_OFF, OSNOWA_MARKER, OSNOWA_GPS }

data class GeoLocation (var location: Location?,
                        val offsetX: Double, val offsetY: Double, val locationPos: Boolean, val num:Int, val refLocationNum:Int = 0,
                        val typ:PointType = PointType.LAMPION_OFF){

    constructor(offsetX: Double, offsetY: Double, refLocation:Int) : this(null, offsetX, offsetY, true, 0, refLocation){

    }

    //private var typ: PointType = PointType.LAMPION;

    //constructor(var location1: Location?, val distance1: Double, var location2: Location?, val distance2: Double){
    //}


    init {
        //positions.add(this);
    }
    //Repozytorium
    //val num = Repozytorium.

    /*
    fun isOffsetPoint(): Boolean {
        if(location == null)
            return true;
        return false;
    }*/

    fun isOffsetPoint(): Boolean {
        if(offsetX == 0.0 && offsetY == 0.0)
            return false;
        return true;
    }

    //fun setTypeOsnowa(){
//        typ = PointType.OSNOWA;
//    }

    fun getType() : PointType{
        return typ;
    }



    companion object Repozytorium{
        //public var positions: MutableList<GeoLocation> = mutableListOf();

        public var positionsUser: MutableList<GeoLocation> = mutableListOf();

        public var locationPositions: MutableMap<Int, GeoLocation> = mutableMapOf();
        public var searchPositions: MutableMap<Int, GeoLocation> = mutableMapOf();

        fun addNewLocationPoint(loc: Location, offsetX: Double, offsetY: Double) : GeoLocation{
            val nowa = GeoLocation(loc, offsetX, offsetY, true, 0);
            return addNewPointToRepo(nowa)
        }

        fun addNewPointToRepo(g: GeoLocation) : GeoLocation{

            val numer = locationPositions.size;
            val nowaZNum = g.copy(num = numer);
            locationPositions[numer]  = nowaZNum;
            positionsUser.add(nowaZNum);
            return nowaZNum

        }

        fun addNewLampionPoint(loc: Location, name: String = "default"){
             val g = addNewLocationPoint(loc, 0.0, 0.0)
        }

        fun addNewLampionPoint(relativeToNr: Int, offsetX: Double, offsetY : Double) : GeoLocation{
            val nowa = GeoLocation(null, offsetX, offsetY, true, 0, relativeToNr, PointType.LAMPION_OFF);
            return addNewPointToRepo(nowa)
        }




        fun addNewOsnowaPointWithValidPosFromGPS(loc: Location, relativeToNr: Int, offsetX: Double, offsetY : Double) : GeoLocation{
            val nowa = GeoLocation(loc, offsetX, offsetY, true, 0, relativeToNr, PointType.OSNOWA_GPS);
            return addNewPointToRepo(nowa)
        }

        fun addNewOsnowaPointWithValidPosFromMarker(loc: Location, relativeToNr: Int, offsetX: Double, offsetY : Double) : GeoLocation{
            val nowa = GeoLocation(loc, offsetX, offsetY, true, 0, relativeToNr, PointType.OSNOWA_MARKER);
            return addNewPointToRepo(nowa)
        }

        fun computeAngle(a : Double, b : Double, c: Double) : Double {
            val m = (c*c - a*a - b*b)/(-2.0*a*b);
            val mm = Math.acos(m) * 180/Math.PI;
            return mm;
        }

        fun addNewOsnowaPointWithValidPosFromMarker(relativeToNr1: Int, len1: Double, relativeToNr2: Int, len2: Double, odl: Double, leftUpper: Boolean = false) : Unit{

            val l1: GeoLocation? = Repozytorium.locationPositions[relativeToNr1];
            val l2: GeoLocation? = Repozytorium.locationPositions[relativeToNr2];

            val dist = l1!!.location!!.distanceTo(l2!!.location);
            val dir = l1!!.location!!.bearingTo(l2!!.location);


            println("odl=" + odl);
            val odlNaMapie = odl * dist;
            println("dist=" + dist + " odlNaMapie=" + odlNaMapie);

            val angle = computeAngle(len1, odlNaMapie, len2)
            println("angle=" + angle);


            var angleResult = dir + angle;
            if(leftUpper)
                angleResult = dir - angle;
            val l: LatLng = SphericalUtil.computeOffset(LatLng(l1!!.location!!.latitude, l1!!.location!!.longitude), len1*1000.0/(odl*1000.0), angleResult);
            println("computed location = " + l.toString());
            //val l: Location = l1.location.

            var lok = Location("")

            lok.longitude = l.longitude;
            lok.latitude = l.latitude;

            val nowa = GeoLocation(lok, 0.0, 0.0, true, 0, relativeToNr1, PointType.LAMPION_OFF);
            addNewPointToRepo(nowa)

            //val nowa = GeoLocation(loc, offsetX, offsetY, true, 0, relativeToNr, PointType.OSNOWA_MARKER);
            //return addNewPointToRepo(nowa)
        }


        fun getMutableMapFromJsonLocationPositions(g: Gson, text: String) : MutableMap<Int, GeoLocation> {


            locationPositions =  g.fromJson(text, object:TypeToken< MutableMap<Int, GeoLocation> >() {}.type);
            return locationPositions

        }

        fun getMutableMapFromJsonPositionsUser(g: Gson, text: String) : MutableList<GeoLocation> {


            positionsUser =  g.fromJson(text, object:TypeToken< MutableList<GeoLocation> >() {}.type);
            return positionsUser

        }



        fun isRepositoryEmpty() : Boolean {
            return giveAllStoredLocationPositions().isEmpty()
        }


        fun addStartPoint(loc: Location){
            val nowa = GeoLocation(loc, 0.0, 0.0, true, 0);

            locationPositions[0] = nowa;
        }

        fun addNewSearchPoint(loc: Location, offsetX: Double, offsetY: Double){
            val nowa = GeoLocation(loc, offsetX, offsetY, false, 0);
            val numer = searchPositions.size;
            val nowaZNum = nowa.copy(num = numer);
            searchPositions[numer]  = nowaZNum;
            positionsUser.add(nowaZNum);
        }

        fun giveAllStoredLocationPositions() : List<GeoLocation> {
            return locationPositions.values.toList()
        }

        fun giveAllStoredPositions() : List<GeoLocation> {
            return locationPositions.values.toList() + searchPositions.values.toList()
        }

        fun writePositionsToFile(){
            //val internalFolder = File(Environment.getDataDirectory() + "/repozytorium")
            //PrintWriter("repozytorium.txt").use{
              //  it.append
            val o = Environment.getExternalStorageDirectory().absolutePath + "/repozytorium.txt"
            val writer = PrintWriter(o)
            writer.append("oko")
            writer.close();

                //for(d in positions)
                  //  it.append("d=" + d.toString())



        }

        fun readFromFile() : String {
            val o = Environment.getExternalStorageDirectory().absolutePath + "/repozytorium.txt"
            val reader = FileReader(o)
            val result = reader.readText()
            reader.close();
            return result;

        }

    }

}

