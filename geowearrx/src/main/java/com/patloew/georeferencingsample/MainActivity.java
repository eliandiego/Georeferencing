package com.patloew.georeferencingsample;

import static butterknife.ButterKnife.findById;


import android.location.Location;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
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

        // 1.3
// to message
        subscription.add(rxWear.message().listen("/message", MessageApi.FILTER_LITERAL)
                .compose(MessageEventGetDataMap.noFilter())
                .subscribe(dataMap -> {
                    mTitleText.setText(dataMap.getString("title", getString(R.string.no_message)));
                    mMessageText.setText(dataMap.getString("message", getString(R.string.no_message_info)));
                }, throwable -> Toast.makeText(this, "Error on message listen", Toast.LENGTH_LONG)));

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

        io.reactivex.Observable<DataMap> o = rxWear.data().get("/persistentText").compose(DataItemGetDataMap.noFilter());

        io.reactivex.Observable<DataMap> m = rxWear.data().listen("/persistentText", DataApi.FILTER_LITERAL).
                compose(DataEventGetDataMap.filterByType(DataEvent.TYPE_CHANGED));


// a to data
        io.reactivex.Observable<DataMap> om = io.reactivex.Observable.concat(o,m);
        subscription.add(om.map(dataMap -> dataMap.get("text"))
                .subscribe(   text -> {mPersistentText.setText((String)text); mText2.setText((String)text); Log.e(TAG, "got persistentText");},
                throwable -> Toast.makeText(this, "Error on data listen", Toast.LENGTH_LONG))
        );


        io.reactivex.Observable<DataMap> ol = rxWear.data().get("/location").compose(DataItemGetDataMap.noFilter());

        io.reactivex.Observable<DataMap> ml = rxWear.data().listen("/location", DataApi.FILTER_LITERAL).
                compose(DataEventGetDataMap.filterByType(DataEvent.TYPE_CHANGED));


// -------------------------
        io.reactivex.Observable<DataMap> oml = io.reactivex.Observable.concat(ol,ml);
        subscription.add(oml.map(dataMap -> dataMap.get("latitude"))
                .subscribe(   lat -> { lastValidLocation.setLatitude((Double)lat); updateDisplayedData(); Log.e(TAG, "got latitude");},
                        throwable -> Toast.makeText(this, "Error on data listen for latitude", Toast.LENGTH_LONG))
        );
        subscription.add(oml.map(dataMap -> dataMap.get("longitude"))
                .subscribe(   longitude -> { lastValidLocation.setLongitude((Double)longitude);updateDisplayedData(); },
                        throwable -> Toast.makeText(this, "Error on data listen for latitude", Toast.LENGTH_LONG))
        );
        subscription.add(oml.map(dataMap -> dataMap.get("accuracy"))
                .subscribe( accuracy -> { lastValidLocation.setAccuracy((float)accuracy);updateDisplayedData(); },
                        throwable -> Toast.makeText(this, "Error on data listen for accuracy", Toast.LENGTH_LONG))
        );

    }


    private void updateDisplayedData(){


        String text = "";
        if(pinLocation != null) {
            float dist = lastValidLocation.distanceTo(pinLocation);
            float dir = lastValidLocation.bearingTo(pinLocation);
        }

        if(lastValidLocation!= null){
            mTextSetDistance.setText(lastValidLocation.getAccuracy() + ":" + lastValidLocation.getLatitude() + "@@" + lastValidLocation.getLongitude());
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
