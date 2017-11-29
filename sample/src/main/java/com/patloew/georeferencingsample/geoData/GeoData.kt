package com.patloew.georeferencingsample.geoData

import android.location.Location
import android.os.Environment
import java.io.FileReader
import java.io.PrintWriter


/**
 * Created by maciek on 11/29/17.
 */


data class GeoData(val location: Location, val lat: Double, val lon: Double){


    constructor(location:Location) : this(location, 0.0, 0.0){

    val l = Location("");

    }

    companion object Repozytorium{
        var positions: MutableList<GeoData> = mutableListOf();

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

