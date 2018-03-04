package com.patloew.georeferencingsample.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.os.Bundle;
import android.widget.Toast;


import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.patloew.georeferencingsample.R;
import com.patloew.georeferencingsample.event.ServiceEvent;
import com.patloew.georeferencingsample.event.SimpleEvent;
import com.patloew.rxlocation.RxLocation;
import com.patloew.rxwear.GoogleAPIConnectionException;
import com.patloew.rxwear.RxWear;
import com.patloew.rxwear.transformers.DataEventGetDataMap;
import com.patloew.rxwear.transformers.MessageEventGetDataMap;

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


    public static void sendToWearNumberOfPoints() {
        int numOfPoints = com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getLocationPositions().size();
        MyService.sendToWearPointsNumberData(numOfPoints);

    }

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

    //private CompositeSubscription subscription = new CompositeSubscription();
    private CompositeDisposable subscription = new CompositeDisposable();


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

        subscription.add(rxWear.message().listen("/messageFromWear", MessageApi.FILTER_LITERAL)
                .compose(MessageEventGetDataMap.noFilter())
                .subscribe(dataMap -> {
                    Log.e(TAG, "got from wear title:" + dataMap.getString("title", "no text") );
                    Log.e(TAG, "got from wear title:" + dataMap.getString("message", "no text"));
                }, throwable -> Toast.makeText(this, "Error on message listen", Toast.LENGTH_LONG)));


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

        //
        rxWear.data().listen("/points", DataApi.FILTER_LITERAL).
                compose(DataEventGetDataMap.filterByType(DataEvent.TYPE_CHANGED))
                .subscribe(   dataMap -> {
                             Log.d(TAG, "cos przyszlo");

                            if(dataMap.containsKey("giveSelectedPoint")) {
                                int i = dataMap.getInt("giveSelectedPoint");
                                Log.e(TAG, "asked about selected point: " + i );
                                Location l = com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getLocationPositions().get(i).getLocation();
                                sendToWearLocationData("/points", "selected", l);

                            } else if(dataMap.containsKey("giveNumOfPoints")){

                                //sendToMobileIntegerData("/points", "giveNumOfPoints", 0);
                                sendToWearNumberOfPoints();
                            }
                        },
                        throwable -> Log.d(TAG, "Error on data listen for giveSelectedPoint"));
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

    // rxLocation zdarzenie
    public void onLocationUpdate(Location location) {
       EventBus.getDefault().postSticky(new SimpleEvent(currentMS.addAndGet(1)));

       String s = location.getLatitude() + "/" + location.getLongitude();
       Calendar calendar = Calendar.getInstance();

       String s2 = "sec=" + calendar.get(Calendar.SECOND);
        Log.e("MyService", "poz rxLocation=" + s + "/" + s2);

       //sendToWearMessage(s, "swm_rxLoc" + s2);
       sendToWearLocation(location);
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

    // tego uzywamy do interfejsu nie-rxLocation
    static private void sendToWearData(String s){
        Log.e("MyService", "sendToWearData:" + s );
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

    static private void sendToWearDoubleMessage(String path, String topic, Double value){

        rxWear.message().sendDataMapToAllRemoteNodes(path)
                .putDouble(topic, value)
                .toObservable().subscribe(requestId -> //Snackbar.make(coordinatorLayout, "Sent message", Snackbar.LENGTH_LONG).show()
                {
                    Log.e("MainActivity", "Sent location");
                },
                // ,
                throwable -> {
                    Log.e("MainActivity", "Error on sending location", throwable);

                    if (throwable instanceof GoogleAPIConnectionException) {
                        //              Toast.makeText(getApplicationContext(), "Android Wear app is not installed", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Android Wear app is not installed", Snackbar.LENGTH_LONG).show();
                    } else {
                        //            Toast.makeText(getApplicationContext(), "Could not send message", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Could not send message", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    static public void sendToWearPointsNumberData(int value){
        sendToWearIntegerData("/points", "pointsNum", value);
    }

    static public void sendToWearIntegerData(String path, String topic, int value){
        Log.e("MyService", "sendToWearIntegerData:" + value );
        rxWear.data().putDataMap().urgent().to(path).putInt(topic, value).toObservable().subscribe(requestId -> //Snackbar.make(coordinatorLayout, "Sent message", Snackbar.LENGTH_LONG).show()
                {
                },
                // ,
                throwable -> {
                    Log.e("MainActivity", "Error on sending (Data) data", throwable);

                    if (throwable instanceof GoogleAPIConnectionException) {
                        //              Toast.makeText(getApplicationContext(), "Android Wear app is not installed", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Android Wear app is not installed", Snackbar.LENGTH_LONG).show();
                    } else {
                        //            Toast.makeText(getApplicationContext(), "Could not send message", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Could not send message", Snackbar.LENGTH_LONG).show();
                    }
                });

    }

    static public void sendToWearLocationData(String path, String topic, Location location){
        Log.e("MyService", "sendToWearIntegerData:" + location.toString() );
        rxWear.data().putDataMap().urgent().to(path)
                .putDouble(topic+"latitude", location.getLatitude())
                .putDouble(topic+"longitude", location.getLongitude())
                .putFloat(topic+"accuracy", location.getAccuracy())
                .toObservable().subscribe(requestId -> //Snackbar.make(coordinatorLayout, "Sent message", Snackbar.LENGTH_LONG).show()
                {
                },
                // ,
                throwable -> {
                    Log.e("MainActivity", "Error on sending (Data) data", throwable);

                    if (throwable instanceof GoogleAPIConnectionException) {
                        //              Toast.makeText(getApplicationContext(), "Android Wear app is not installed", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Android Wear app is not installed", Snackbar.LENGTH_LONG).show();
                    } else {
                        //            Toast.makeText(getApplicationContext(), "Could not send message", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Could not send message", Snackbar.LENGTH_LONG).show();
                    }
                });

    }

    static private void sendToWearFloatMessage(String path, String topic, float value){

        rxWear.message().sendDataMapToAllRemoteNodes(path)
                .putFloat(topic, value)
                .toObservable().subscribe(requestId -> //Snackbar.make(coordinatorLayout, "Sent message", Snackbar.LENGTH_LONG).show()
                {
                    Log.e("MainActivity", "Sent location");
                },
                // ,
                throwable -> {
                    Log.e("MainActivity", "Error on sending location", throwable);

                    if (throwable instanceof GoogleAPIConnectionException) {
                        //              Toast.makeText(getApplicationContext(), "Android Wear app is not installed", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Android Wear app is not installed", Snackbar.LENGTH_LONG).show();
                    } else {
                        //            Toast.makeText(getApplicationContext(), "Could not send message", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Could not send message", Snackbar.LENGTH_LONG).show();
                    }
                });


    }



    static private void sendToWearLocation(Location l){

        DataMap dm = new DataMap();


        Log.e("MyService", "sendToWearLocation:" + l.toString());
        /*
        rxWear.message().sendDataMapToAllRemoteNodes("/location")
                .putDouble("latitude", l.getLatitude())
                .putDouble("longitude", l.getLongitude())
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
                */
        // jako data - nie dziala dobrze, tylko na poczatku uaktualnia...
        /*
        rxWear.data().putDataMap().urgent().to("/location")
                .putDouble("latitude", l.getLatitude())
                .putDouble("longitude", l.getLongitude())
                .putFloat("accuracy", l.getAccuracy())
                .toObservable().subscribe(requestId -> //Snackbar.make(coordinatorLayout, "Sent message", Snackbar.LENGTH_LONG).show()
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
*/
// jako message:
/*
        rxWear.message().sendDataMapToAllRemoteNodes("/location")
                .putDouble("latitude", l.getLatitude())
                .putDouble("longitude", l.getLongitude())
                .putFloat("accuracy", l.getAccuracy())
                .toObservable().subscribe(requestId -> //Snackbar.make(coordinatorLayout, "Sent message", Snackbar.LENGTH_LONG).show()
                {
                    Log.e("MainActivity", "Sent location");
                },
                // ,
                throwable -> {
                    Log.e("MainActivity", "Error on sending location", throwable);

                    if (throwable instanceof GoogleAPIConnectionException) {
                        //              Toast.makeText(getApplicationContext(), "Android Wear app is not installed", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Android Wear app is not installed", Snackbar.LENGTH_LONG).show();
                    } else {
                        //            Toast.makeText(getApplicationContext(), "Could not send message", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Could not send message", Snackbar.LENGTH_LONG).show();
                    }
                });
*/

        sendToWearDoubleMessage("/location", "latitude", l.getLatitude());
        sendToWearDoubleMessage("/location", "longitude", l.getLongitude());
        sendToWearFloatMessage("/location", "accuracy", l.getAccuracy());

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
            //sendToWearData(s2);
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
