package com.patloew.georeferencingsample.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.os.Bundle;


import com.google.android.gms.location.LocationRequest;
import com.patloew.georeferencingsample.event.ServiceEvent;
import com.patloew.georeferencingsample.event.SimpleEvent;
import com.patloew.rxlocation.RxLocation;
import com.patloew.rxwear.GoogleAPIConnectionException;
import com.patloew.rxwear.RxWear;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;



/**
 * Created by gunhansancar on 06/04/16.
 */
public class MyService extends Service {


    public interface WearChannelModes {

        String MESSAGE = "/message";
        String PERSISTENT = "/persistentText";
    }




    static private RxWear rxWear;

    private static final String TAG = "BOOMBOOMTESTGPS";
    private static final int LOCATION_INTERVAL = 1000;
    private static final float LOCATION_DISTANCE = 10f;

    private final CompositeDisposable disposable = new CompositeDisposable();
    private RxLocation rxLocation;
    private final LocationRequest locationRequest;
    private AtomicInteger currentMS = new AtomicInteger(0);

    private CompositeSubscription subscription = new CompositeSubscription();

    private LocationManager mLocationManager = null;

    public MyService(){
        //rxWear = new RxWear(this);


        rxLocation = null;
        locationRequest = LocationRequest.create();
        locationRequest.setInterval(3000);
        /*
        locationclient.requestLocationUpdates(locationrequest,new com.google.android.gms.location.LocationListener() {

            @Override
            public void onLocationChanged(Location location) {
                Log.i(TAG, "Last Known Location :" + location.getLatitude() + "," + location.getLongitude());
            }
        });*/

        /*
        this.rxLocation = new RxLocation(this);
        this.rxLocation.setDefaultTimeout(15, TimeUnit.SECONDS);

        this.locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000);
                */
    }



    private Timer timer;
    private AtomicInteger counter = new AtomicInteger();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTimer();
        return START_STICKY_COMPATIBILITY;
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        EventBus.getDefault().register(this);

        rxWear = new RxWear(getApplicationContext());
        rxLocation = new RxLocation(getApplicationContext());
        rxLocation.setDefaultTimeout(15, TimeUnit.SECONDS);

        startTimer();
        startRxLocationRefresh();

        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }

    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        stopTimer();
        super.onDestroy();
    }

    @Subscribe
    public void onEvent(ServiceEvent event) {
        stopTimer();
        stopSelf();
    }

    private void startTimer() {
        stopTimer();
        timer = new Timer();
        timer.scheduleAtFixedRate(new SimpleTimerTask(counter), 0, 3000);
    }

    private void stopTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private static class SimpleTimerTask extends TimerTask {

        private AtomicInteger current;

        public SimpleTimerTask(AtomicInteger current) {
            this.current = current;
        }

        @Override
        public void run() {
            EventBus.getDefault().postSticky(new SimpleEvent(current.addAndGet(1)));

            Calendar calendar = Calendar.getInstance();

            String s = "sec=" + calendar.get(Calendar.SECOND);
            //sendToWear(s);
        }
    }

    public void onAddressUpdate(Address address) {

    }

    // rxLocation
    public void onLocationUpdate(Location location) {
       EventBus.getDefault().postSticky(new SimpleEvent(currentMS.addAndGet(1)));
       String s = location.getLatitude() + "/" + location.getLongitude();
       Calendar calendar = Calendar.getInstance();

       String s2 = "sec=" + calendar.get(Calendar.SECOND);
        Log.e("MyService", "poz rxLocation=" + s + "/" + s2);

       sendToWearMessage(s, "rxLocation" + s2);
    }

    public void startRxLocationRefresh() {

        disposable.add(
                rxLocation.settings().checkAndHandleResolution(locationRequest)
                        .flatMapObservable(this::getAddressObservable)
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onAddressUpdate, throwable -> Log.e("MainPresenter", "Error fetching location/address updates", throwable))
        );
    }

    public Maybe<Address> fromLocationMS(Location location) {
        Locale l = new Locale("pl");
        Address a = new Address(l);
        a.setLatitude(2.2);
        a.setLongitude(2.3);
        Maybe<Address> m = Maybe.create(emitter -> {
            emitter.onSuccess(a);

        });

        return m;
    }

    private Observable<Address> getAddressFromLocation(Location location) {
        return fromLocationMS(location).toObservable()
                .subscribeOn(Schedulers.io());

        //return null;
    }

    private Observable<Address> getAddressObservable(boolean success) {
        if(success) {
            return rxLocation.location().updates(locationRequest)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(this::onLocationUpdate)
                    .flatMap(this::getAddressFromLocation);

        } else {
            //view.onLocationSettingsUnsuccessful();

            return rxLocation.location().lastLocation()
                    .doOnSuccess(this::onLocationUpdate)
                    .flatMapObservable(this::getAddressFromLocation);
        }
    }

    static private void sendToWear(String s){
        Log.e("MyService", "sendToWear:" + s );
        rxWear.data().putDataMap().urgent().to("/persistentText").putString("text", s).toObservable().subscribe(requestId -> //Snackbar.make(coordinatorLayout, "Sent message", Snackbar.LENGTH_LONG).show()
                {
                },
                // ,
                throwable -> {
                    Log.e("MainActivity", "Error on sending (Data) message", throwable);

                    if (throwable instanceof GoogleAPIConnectionException) {
                        //              Toast.makeText(getApplicationContext(), "Android Wear app is not installed", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Android Wear app is not installed", Snackbar.LENGTH_LONG).show();
                    } else {
                        //            Toast.makeText(getApplicationContext(), "Could not send message", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Could not send message", Snackbar.LENGTH_LONG).show();
                    }
                });

    }

    static private void sendToWearMessage(String s, String s2){
        Log.e("MyService", "sendToWearMessage:" + s + "," + s2);
        rxWear.message().sendDataMapToAllRemoteNodes("/message")
                .putString("title", s)
                .putString("message", s2)
                .toObservable().subscribe(requestId -> //Snackbar.make(coordinatorLayout, "Sent message", Snackbar.LENGTH_LONG).show()
                {
                },
                // ,
                throwable -> {
                    Log.e("MainActivity", "Error on sending message", throwable);

                    if (throwable instanceof GoogleAPIConnectionException) {
                        //              Toast.makeText(getApplicationContext(), "Android Wear app is not installed", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Android Wear app is not installed", Snackbar.LENGTH_LONG).show();
                    } else {
                        //            Toast.makeText(getApplicationContext(), "Could not send message", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Could not send message", Snackbar.LENGTH_LONG).show();
                    }
                });

    }


    private class LocationListener implements android.location.LocationListener
    {
        Location mLastLocation;

        public LocationListener(String provider)
        {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location)
        {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);


            String s2 = "mee" + location.getLatitude();
            sendToWear(s2);


        }

        @Override
        public void onProviderDisabled(String provider)
        {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider)
        {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras)
        {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    LocationListener[] mLocationListeners = new LocationListener[] {
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };

}
