package com.patloew.georeferencingsample.event;

import com.patloew.rxwear.RxWear;

import java.util.Date;

/**
 * Created by gunhansancar on 06/04/16.
 */
public class RxWearEvent {
    private RxWear rxWear;


    public RxWearEvent(RxWear r) {

        this.rxWear= r;
    }

    public RxWear getRxWear() {
        return rxWear;
    }


}
