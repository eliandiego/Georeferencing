package com.patloew.georeferencingsample;

import static butterknife.ButterKnife.findById;
import static com.patloew.georeferencingsample.utils.UtilsKt.convertTo360deg;
import static com.patloew.georeferencingsample.utils.UtilsKt.testFormat;


import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RectShape;
import android.location.Location;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.MessageApi;
import com.patloew.rxwear.GoogleAPIConnectionException;
import com.patloew.rxwear.RxWear;
import com.patloew.rxwear.transformers.DataEventGetDataMap;
import com.patloew.rxwear.transformers.MessageEventGetDataMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
//import butterknife.Bind;

import io.reactivex.disposables.CompositeDisposable;

import android.databinding.DataBindingUtil;
//import com.patloew.georeferencingsample.databinding.ActivityMainMobileBinding;

//import com.patloew.georeferencingsample.databinding.ActivityWearMainBinding;
import com.patloew.georeferencingsample.databinding.ActivityWearMainBinding;
public class MainWearActivity extends WearableActivity {

    ActivityWearMainBinding binding;

    private static final String TAG = "WearMainActivity";



    private BoxInsetLayout mContainerView;




    private int currentPointNr = 0;
    private int numOfPoints = 0;

    //@BindView(R.id.textViewBattery) TextView battery;


    // 1.3
    //private CompositeSubscription subscription = new CompositeSubscription();
    private CompositeDisposable subscription = new CompositeDisposable();

    private RxWear rxWear;

    private Location lastValidLocation = null;
    private Location previousValidLocation = null;
    private Boolean isLastValid = false;
    private Time lastValidLocationRcvDate = new Time();
    private Time previousMyCourseRcvDate = new Time();

    private Location pinLocation = null;
    private Location targetLocation = new Location("");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wear_main);

        lastValidLocation = new Location("");

        previousMyCourseRcvDate.setToNow();

        //ButterKnife.bind(this, this.get);
        //ButterKnife.bind(MainWearActivity.this);



        binding = DataBindingUtil.setContentView(this, R.layout.activity_wear_main);

        setAmbientEnabled();
        // musi byc po setContentView!
        ButterKnife.bind(this);

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);



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

Time t = new Time();
t.setToNow();;
                binding.textViewClock.setText(testFormat(2, 0, t.hour) + ":" + testFormat(2, 0, t.minute) + ":" + testFormat(2, 0, t.second));

                handler.postDelayed(this, 1000);
            }
        };
// Run the above code block on the main thread after 2 seconds
        handler.postDelayed(runnableCode, 1000);


        // 1.3
// to message // mee... (z rxLocation)
        /*
        subscription.add(rxWear.message().listen("/message", MessageApi.FILTER_LITERAL)
                .compose(MessageEventGetDataMap.noFilter())
                .subscribe(dataMap -> {
                    mTitleText.setText(dataMap.getString("title", getString(R.string.no_message)));
                    mMessageText.setText(dataMap.getString("message", getString(R.string.no_message_info)));
                    Log.d(TAG, "got /message");
                }, throwable -> Log.d(TAG, "Error on message listen for /message")));
*/
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
        /*
        io.reactivex.Observable<DataMap> o = rxWear.data().get("/persistentText").compose(DataItemGetDataMap.noFilter());

        io.reactivex.Observable<DataMap> m = rxWear.data().listen("/persistentText", DataApi.FILTER_LITERAL).
                compose(DataEventGetDataMap.filterByType(DataEvent.TYPE_CHANGED));
        io.reactivex.Observable<DataMap> om = io.reactivex.Observable.concat(o,m);
        subscription.add(om.map(dataMap -> dataMap.get("text"))
                .subscribe(   text -> {mPersistentText.setText((String)text); mText2.setText((String)text); Log.e(TAG, "got persistentText");},
                        throwable -> Log.d(TAG, "Error on data listen for persistenttext")));
*/

        /* // nie najlepiej gubi...
        io.reactivex.Observable<DataMap> o = rxWear.data().get("/points").compose(DataItemGetDataMap.noFilter());
        io.reactivex.Observable<DataMap> m = rxWear.data().listen("/points", DataApi.FILTER_LITERAL).
                compose(DataEventGetDataMap.filterByType(DataEvent.TYPE_CHANGED));
        io.reactivex.Observable<DataMap> om = io.reactivex.Observable.concat(o,m);
        subscription.add(om.map(dataMap -> dataMap.get("pointsNum"))
                .subscribe(   text -> {mPersistentText.setText("num="+text); Log.e(TAG, "got points num");},
                        throwable -> Log.d(TAG, "Error on data listen for persistenttext")));
*/

       rxWear.data().listen("/points", DataApi.FILTER_LITERAL).
                compose(DataEventGetDataMap.filterByType(DataEvent.TYPE_CHANGED))
                .subscribe(   dataMap -> {
                    if(dataMap.containsKey("pointsNum")) {
                        int i = dataMap.getInt("pointsNum");
                        numOfPoints = i;
                        binding.textViewPoints.setText("sel=" + currentPointNr + "/num=" + i);
                        Log.e(TAG, "got points num");
                    }
                    if(dataMap.containsKey("selectedlatitude")) {
                         Double f = dataMap.getDouble("selectedlatitude");
                        //mMessageText.setText("selected lat=" + f);
                        targetLocation.setLatitude(f);
                        Log.e(TAG, "selected lat=" + f);
                    }
                    if(dataMap.containsKey("selectedlongitude")) {
                        Double f = dataMap.getDouble("selectedlongitude");
                        targetLocation.setLongitude(f);
                        Log.e(TAG, "selected lon=" + f);
                    }
                    if(dataMap.containsKey("selectedaccuracy")) {
                                Float f = dataMap.getFloat("selectedaccuracy");
                                targetLocation.setAccuracy(f);
                                Log.e(TAG, "selected acc=" + f);

                    }
                            if(dataMap.containsKey("battMob")) {
                                int i= dataMap.getInt("battMob");
                                binding.textBatteryMobile.setText(" " + i);
                                Log.e(TAG, "selected acc=" + i);

                            }

                    },
                        throwable -> Log.d(TAG, "Error on data listen for persistenttext"));

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

                    if(!n) {
                        isLastValid = false;
                        previousValidLocation = null;
                    }else {
                        isLastValid = true;
                    }

                    updateDisplayedDataUpd();
                    Log.d(TAG, "got /location as message");
                }, throwable -> Log.d(TAG, "Error on message listen for /location")));

        askMobileAboutNumberOfPoints();
        addBorder(binding.textViewBattery);


        binding.buttonMS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "klikneli mnie!");

                if(currentPointNr < numOfPoints-1)
                    currentPointNr++;
                else
                    currentPointNr= 0;

                //mPersistentText.setText("sel=" + currentPointNr + "/0-" + (numOfPoints-1));
                binding.textViewPoints.setText("sel=" + currentPointNr + "/0-" + (numOfPoints-1));
                sendGiveLocationForSelectedPointToWear();


            }
        });

        binding.buttonSetDist.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "klikneli mnie!");

                pinLocation = new Location(lastValidLocation);
            }
        });

    }

    private void addBorder(TextView tv){
        ShapeDrawable sd = new ShapeDrawable();

        // Specify the shape of ShapeDrawable
        sd.setShape(new RectShape());

        // Specify the border color of shape
        sd.getPaint().setColor(Color.RED);

        // Set the border width
        sd.getPaint().setStrokeWidth(10f);

        // Specify the style is a Stroke
        sd.getPaint().setStyle(Paint.Style.STROKE);

        // Finally, add the drawable background to TextView
        tv.setBackground(sd);
    }

    private void updateDisplayedDataUpd() {
        lastValidLocationRcvDate.setToNow();
        //updateDisplayedData();
    }

    private void updateMyCourse(){

        String text = "--";

        if(previousValidLocation != null && lastValidLocation != null){

            float dir = previousValidLocation.bearingTo(lastValidLocation);
            String dirStr = testFormat(3, 2, convertTo360deg(dir));

            text = "Dir=" + dirStr;//replaceTrailingZerosWithSpaces(dirStr);
        }


        binding.walkDi.setText(text);
    }



    private String replaceTrailingZerosWithSpaces(String s){
        return s.replaceAll("\\G0", " ");
    }

    private void updateDisplayedData(){


        updateMyCourse();

        binding.textViewBattery.setText("b:" + getBatteryLevel() + "b");



        if(lastValidLocation!= null){
            Time t = new Time();
            t.setToNow();
            long d = (t.toMillis(false) - lastValidLocationRcvDate.toMillis(false)) / 1000;

            long d2 = (t.toMillis(false) - previousMyCourseRcvDate.toMillis(false)) / 1000;

            if(d2 > 5){
                previousMyCourseRcvDate.setToNow();
                updateMyCourse();
                previousValidLocation= new Location(lastValidLocation);
            }

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

            String lastTime = "" + d + "[s]" ;
            if(d > 1000.0)
                lastTime = "inf";

            String isLastValidStr = "";
            if(!isLastValid)
                isLastValidStr = "H";
            //.doOnComplete(()->{e[0]="e";});
            binding.textViewTimeFromComm.setText(mru + lastTime );

//            binding.accuracy.setText("acc=" + lastValidLocation.getAccuracy());


            Double acc = (double) lastValidLocation.getAccuracy();

            binding.accuracyTV.setText("acc=" + acc.intValue() + " ");
            binding.currentLat.setText("lat=" + lastValidLocation.getLatitude());
            binding.currentLon.setText(" lon=" + lastValidLocation.getLongitude());
            //mTitleText.setText(   lastValidLocation.getAccuracy() + ":" + lastValidLocation.getLatitude() + "@@" + lastValidLocation.getLongitude());

        }

        String text = "";
        if(pinLocation != null) {
            float dist = lastValidLocation.distanceTo(pinLocation);
            float dir = lastValidLocation.bearingTo(pinLocation);

            String dirStr = testFormat(3, 2, convertTo360deg(dir));
            binding.textViewPinDir.setText("dir=" + dirStr);
            binding.textViewPinDist.setText("dist=" + dist);
            //mTextSetDistance.setText("pin:"+ " dist=" + dist + " dir=" + dir + pinLocation.getLatitude() );
        }

        Log.e("MainWearActivityMuu", "refresh");

        if(targetLocation != null) {
            Log.e("MainWearActivityMuu", "targetLat=" + targetLocation.getLatitude() + " lastValidLoc=" + lastValidLocation.getLatitude());
            float dist = lastValidLocation.distanceTo(targetLocation);
            float dir = lastValidLocation.bearingTo(targetLocation);
            //mMessageText.setText("target:"+ " dist=" + dist + " dir=" + dir + targetLocation.getLatitude() );
            String dirStr = testFormat(3, 2, convertTo360deg(dir));
            binding.textViewTargetDir.setText("dir=" + dirStr);
            binding.textViewTargetDist.setText("dist=" + dist);
        }


    }

    public String getBatteryLevel() {
        Intent batteryIntent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

// Error checking that probably isn't needed but I added just in case.
        if(level == -1 || scale == -1) {
            return "error";
        }


        float batteryLevelWithDecimals = ((float)level / (float)scale) * 100.0f;
        String battery = String.format("%.0f", batteryLevelWithDecimals);
        return battery ;
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

    void askMobileAboutNumberOfPoints(){
        sendToMobileIntegerData("/points", "giveNumOfPoints", 0);
    }

    void sendGiveLocationForSelectedPointToWear(){
        sendToMobileIntegerData("/points", "giveSelectedPoint", currentPointNr);
    }

    public void sendToMobileIntegerData(String path, String topic, int value){
        Log.e("MyService", "sendToMobileIntegerData:" + value );
        rxWear.data().putDataMap().urgent().to(path).putInt(topic, value).toObservable().subscribe(requestId -> //Snackbar.make(coordinatorLayout, "Sent message", Snackbar.LENGTH_LONG).show()
                {
                },
                // ,
                throwable -> {
                    Log.e("MainWearActivity", "Error on sending (Data) data", throwable);

                    if (throwable instanceof GoogleAPIConnectionException) {
                        //              Toast.makeText(getApplicationContext(), "Android Wear app is not installed", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Android Wear app is not installed", Snackbar.LENGTH_LONG).show();
                    } else {
                        //            Toast.makeText(getApplicationContext(), "Could not send message", Toast.LENGTH_LONG).show();
                        //Snackbar.make(coordinatorLayout, "Could not send message", Snackbar.LENGTH_LONG).show();
                    }
                });

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

    /* // butterknife
    @OnClick(R.id.buttonMS)
    public void submit(View view) {

    }
*/ /*
    @OnClick(R.id.buttonSetDist)
    public void measureDistance(View view) {

    }
    */

}
