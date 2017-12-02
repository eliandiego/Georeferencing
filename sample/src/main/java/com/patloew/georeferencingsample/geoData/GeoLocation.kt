package com.patloew.georeferencingsample.geoData

import android.location.Location
import android.os.Environment
import java.io.FileReader
import java.io.PrintWriter


/**
 * Created by maciek on 11/29/17.
 */


data class GeoLocation (val location: Location,
                        val offsetX: Double, val offsetY: Double, val locationPos: Boolean, val num:Int){



    init {
        positions.add(this);
    }
    //Repozytorium
    //val num = Repozytorium.

    constructor(location:Location) : this(location, 0.0, 0.0, true, 0){
        //Repozytorium.positions
        val l = Location("");

    }

    companion object Repozytorium{
        public var positions: MutableList<GeoLocation> = mutableListOf();

        public var locationPositions: MutableMap<Int, GeoLocation> = mutableMapOf();
        public var searchPositions: MutableMap<Int, GeoLocation> = mutableMapOf();

        fun addNewLocationPoint(loc: Location, offsetX: Double, offsetY: Double){
            val nowa = GeoLocation(loc, offsetX, offsetY, true, 0);
            val numer = locationPositions.size;
            val nowaZNum = nowa.copy(num = numer);
            locationPositions[numer]  = nowaZNum;
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

