package com.patloew.georeferencingsample.UtilMS

import java.util.*

/**
 * Created by maciek on 12/1/17.
 */

class UtilsMS {

    companion object  {

        fun randomDoublePos(): Double {
            val r : Random = Random();
            val rangeMin = -20.0;
            val rangeMax = 20.0;
            val randomValue = rangeMin + (rangeMax - rangeMin) * r.nextDouble();
            return randomValue;

        }

    }

}