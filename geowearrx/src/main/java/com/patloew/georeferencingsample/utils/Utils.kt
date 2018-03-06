package com.patloew.georeferencingsample.utils


import java.text.DecimalFormat



//import CharSequence

fun convertTo360deg(v: Double) : Double
{
    if(v < 0)
        return 360.0 +v

    return v
}

fun testFormat(integral: Int, fractional: Int, v: Double) : String {



    val s: String = "0".repeat(integral);
    val s2: String = "#".repeat(fractional);

    val df = DecimalFormat(s+"." + s2)
    return df.format(v)
    //"%.2f".format(value)
}

