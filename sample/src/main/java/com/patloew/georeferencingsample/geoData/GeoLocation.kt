package com.patloew.georeferencingsample.geoData

//import com.google.android.gms.

import android.location.Location
import android.os.Environment
import java.io.FileReader
import java.io.PrintWriter


/**
 * Created by maciek on 11/29/17.
 */

enum class PointType
{ LAMPION_MARKER, LAMPION_OFF, OSNOWA_MARKER, OSNOWA_GPS }

data class GeoLocation (val location: Location?,
                        val offsetX: Double, val offsetY: Double, val locationPos: Boolean, val num:Int, val refLocationNum:Int = 0,
                        val typ:PointType = PointType.LAMPION_OFF){

    constructor(offsetX: Double, offsetY: Double, refLocation:Int) : this(null, offsetX, offsetY, true, 0, refLocation){

    }

    //private var typ: PointType = PointType.LAMPION;




    init {
        positions.add(this);
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

    constructor(location: Location) : this(location, 0.0, 0.0, true, 0){
        //Repozytorium.positions
        val l = Location("");

    }

    companion object Repozytorium{
        public var positions: MutableList<GeoLocation> = mutableListOf();

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

        fun addNewLampionPoint(relativeToNr: Int, offsetX: Double, offsetY : Double){

        }


        fun addNewOsnowaPoint(loc: Location){
            val g = addNewLocationPoint(loc, 0.0, 0.0)
            //g.setTypeOsnowa();
        }

        fun addNewOsnowaPointWithValidPosFromGPS(loc: Location, relativeToNr: Int, offsetX: Double, offsetY : Double) : GeoLocation{
            val nowa = GeoLocation(loc, offsetX, offsetY, true, 0, relativeToNr, PointType.OSNOWA_GPS);
            return addNewPointToRepo(nowa)
        }

        fun addNewOsnowaPointWithValidPosFromMarker(loc: Location, relativeToNr: Int, offsetX: Double, offsetY : Double) : GeoLocation{
            val nowa = GeoLocation(loc, offsetX, offsetY, true, 0, relativeToNr, PointType.OSNOWA_MARKER);
            return addNewPointToRepo(nowa)
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

