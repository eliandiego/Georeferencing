package com.patloew.georeferencingsample;

import static butterknife.ButterKnife.findById;


import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.patloew.georeferencingsample.R;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.MessageApi;
import com.patloew.rxwear.GoogleAPIConnectionException;
import com.patloew.rxwear.RxWear;
import com.patloew.rxwear.transformers.DataEventGetDataMap;
import com.patloew.rxwear.transformers.DataItemGetDataMap;
import com.patloew.rxwear.transformers.MessageEventGetDataMap;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;

import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.disposables.CompositeDisposable;
import rx.Observable;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends WearableActivity {

    private static final String TAG = "WearMainActivity";


    private BoxInsetLayout mContainerView;
    private TextView mTitleText;
    private TextView mMessageText;
    private TextView mPersistentText;
    private TextView mText2;
    private TextView mTextSetDistance;
    // 1.3
    //private CompositeSubscription subscription = new CompositeSubscription();
    private CompositeDisposable subscription = new CompositeDisposable();

    private RxWear rxWear;

    private Location lastValidLocation = null;
    private Boolean isLastValid = false;
    private Time lastValidLocationRcvDate = new Time();
    private Location pinLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        lastValidLocation = new Location("");

        //ButterKnife.bind(this, this.get);
        //ButterKnife.bind(MainActivity.this);

        setContentView(R.layout.activity_main);

        setAmbientEnabled();
        // musi byc po setContentView!
        ButterKnife.bind(this);

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTitleText = (TextView) findViewById(R.id.title);
        mMessageText = (TextView) findViewById(R.id.message);
        mPersistentText = (TextView) findViewById(R.id.persistent);
        mText2 = findViewById(R.id.textView2);
        mTextSetDistance = findViewById(R.id.textViewDistance);


        rxWear = new RxWear(this);

        //EventBus.getDefault().register(this);



        Handler handler = new Handler();
// Define the code block to be executed
        Runnable runnableCode = new Runnable() {
            @Override
            public void run() {
                // Do something here on the main thread
                updateDisplayedData();
                Log.d("Handlers", "Called on main thread");
                handler.postDelayed(this, 1000);
            }
        };
// Run the above code block on the main thread after 2 seconds
        handler.postDelayed(runnableCode, 1000);


        // 1.3
// to message // mee... (z rxLocation)
        subscription.add(rxWear.message().listen("/message", MessageApi.FILTER_LITERAL)
                .compose(MessageEventGetDataMap.noFilter())
                .subscribe(dataMap -> {
                    mTitleText.setText(dataMap.getString("title", getString(R.string.no_message)));
                    mMessageText.setText(dataMap.getString("message", getString(R.string.no_message_info)));
                    Log.d(TAG, "got /message");
                }, throwable -> Log.d(TAG, "Error on message listen for /message")));

/*
        subscription.add(
                Observable.concat(
                        rxWear.data().get("/persistentText").compose(DataItemGetDataMap.noFilter()),
                        rxWear.data().listen("/persistentText", DataApi.FILTER_LITERAL).compose(DataEventGetDataMap.filterByType(DataEvent.TYPE_CHANGED))
                ).map(dataMap -> dataMap.getString("text"))
                .subscribe(text -> mPersistentText.setText(text),
                        throwable -> Toast.makeText(this, "Error on data listen", Toast.LENGTH_LONG))
        );
*/
// data w formie stringu lat/long (nie z rxLocation)
        io.reactivex.Observable<DataMap> o = rxWear.data().get("/persistentText").compose(DataItemGetDataMap.noFilter());

        io.reactivex.Observable<DataMap> m = rxWear.data().listen("/persistentText", DataApi.FILTER_LITERAL).
                compose(DataEventGetDataMap.filterByType(DataEvent.TYPE_CHANGED));
        io.reactivex.Observable<DataMap> om = io.reactivex.Observable.concat(o,m);
        subscription.add(om.map(dataMap -> dataMap.get("text"))
                .subscribe(   text -> {mPersistentText.setText((String)text); mText2.setText((String)text); Log.e(TAG, "got persistentText");},
                        throwable -> Log.d(TAG, "Error on data listen for persistenttext")));



        // ------------------------- location
        // jako data (zle dziala)
        /*
        io.reactivex.Observable<DataMap> ol = rxWear.data().get("/location").compose(DataItemGetDataMap.noFilter());

        io.reactivex.Observable<DataMap> ml = rxWear.data().listen("/location", DataApi.FILTER_LITERAL).
                compose(DataEventGetDataMap.filterByType(DataEvent.TYPE_CHANGED));
        io.reactivex.Observable<DataMap> oml = io.reactivex.Observable.concat(ol,ml);
        subscription.add(oml.map(dataMap -> dataMap.get("latitude"))
                .subscribe(   lat -> { lastValidLocation.setLatitude((Double)lat); updateDisplayedDataUpd(); Log.e(TAG, "got latitude");},
                        throwable -> Log.d(TAG, "Error on data listen for latitude")));

        subscription.add(oml.map(dataMap -> dataMap.get("longitude"))
                .subscribe(   longitude -> { lastValidLocation.setLongitude((Double)longitude);updateDisplayedDataUpd(); Log.e(TAG, "got longitude");},
                        throwable -> Log.d(TAG, "Error on data listen for longitude")));
        subscription.add(oml.map(dataMap -> dataMap.get("accuracy"))
                .subscribe( accuracy -> { lastValidLocation.setAccuracy((float)accuracy);updateDisplayedDataUpd(); Log.e(TAG, "got accuracy");},
                        throwable -> Log.d(TAG, "Error on data listen for accuracy")));
*/
        // jako message
        subscription.add(rxWear.message().listen("/location", MessageApi.FILTER_LITERAL)
                .compose(MessageEventGetDataMap.noFilter())
                .subscribe(dataMap -> {


                    Boolean n = false;

                    Double lat = dataMap.getDouble("latitude");
                    if( lat != 0.0) {
                        lastValidLocation.setLatitude(lat);
                        n=true;
                    }

                    Double lon = dataMap.getDouble("longitude");
                    if( lon != 0.0) {
                        lastValidLocation.setLongitude(lon);
                        n=true;
                    }

                    Float acc = dataMap.getFloat("accuracy");
                    if( acc != 0.0) {
                        lastValidLocation.setAccuracy(acc);
                        n=true;
                    }

                    if(!n)
                        isLastValid = false;
                    else
                        isLastValid = true;

                    updateDisplayedDataUpd();
                    Log.d(TAG, "got /location as message");
                }, throwable -> Log.d(TAG, "Error on message listen for /location")));

    }

    private void updateDisplayedDataUpd() {
        lastValidLocationRcvDate.setToNow();
        //updateDisplayedData();
    }

    private void updateDisplayedData(){


        String text = "";
        if(pinLocation != null) {
            float dist = lastValidLocation.distanceTo(pinLocation);
            float dir = lastValidLocation.bearingTo(pinLocation);
        }

        if(lastValidLocation!= null){
            Time t = new Time();
            t.setToNow();
            long d = (t.toMillis(false) - lastValidLocationRcvDate.toMillis(false)) / 1000;

            boolean[] e = {false};
            String mru ="";
            rxWear.checkConnection().subscribe(() -> {
                e[0] = true;
            }, throwable -> {
                // handle error
                e[0] = false;
            });

            if(e[0] == false){
                mru = "E";
            }

            String isLastValidStr = "";
            if(!isLastValid)
                isLastValidStr = "H";
                    //.doOnComplete(()->{e[0]="e";});
            mTextSetDistance.setText(mru + isLastValidStr + d + "[s]" + lastValidLocation.getAccuracy() + ":" + lastValidLocation.getLatitude() + "@@" + lastValidLocation.getLongitude());
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 1.3
        /*
        if(subscription != null && !subscription.isUnsubscribed()) {
            subscription.unsubscribe();
        }*/
        //2.0
        subscription.clear();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        /*
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
            mTitleText.setTextColor(getResources().getColor(android.R.color.white));
            mMessageText.setTextColor(getResources().getColor(android.R.color.white));
            mPersistentText.setTextColor(getResources().getColor(android.R.color.white));

        } else {
            mContainerView.setBackground(null);
            mTitleText.setTextColor(getResources().getColor(android.R.color.black));
            mMessageText.setTextColor(getResources().getColor(android.R.color.black));
            mPersistentText.setTextColor(getResources().getColor(android.R.color.black));
        }
        */
    }

    void sendTheCurrentPointsToWear(){

    }

    void sendToMobile(String s, String s2){
        rxWear.message().sendDataMapToAllRemoteNodes("/messageFromWear")
                .putString("title", s)
                .putString("message", s2)
                .toObservable().subscribe(requestId -> //Snackbar.make(coordinatorLayout, "Sent message", Snackbar.LENGTH_LONG).show()
                {
                },
                // ,
                throwable -> {
                    Log.e(TAG, "Error on sending message", throwable);

                    if (throwable instanceof GoogleAPIConnectionException) {
                        //              Toast.makeText(getApplicationContext(), "Android Wear app is not installed", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Android Wear app is not installed", Snackbar.LENGTH_LONG).show();
                    } else {
                        //            Toast.makeText(getApplicationContext(), "Could not send message", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Could not send message", Snackbar.LENGTH_LONG).show();
                    }
                });

    }

    @OnClick(R.id.buttonMS)
    public void submit(View view) {
        Log.i(TAG, "klikneli mnie!");
        sendToMobile("baba", "zaba");

    }

    @OnClick(R.id.buttonSetDist)
    public void measureDistance(View view) {
        Log.i(TAG, "klikneli mnie!");

        pinLocation = lastValidLocation;

    }

}
