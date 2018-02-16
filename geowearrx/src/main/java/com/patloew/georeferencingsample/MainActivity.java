package com.patloew.georeferencingsample;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.patloew.georeferencingsample.R;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.MessageApi;
import com.patloew.rxwear.RxWear;
import com.patloew.rxwear.transformers.DataEventGetDataMap;
import com.patloew.rxwear.transformers.DataItemGetDataMap;
import com.patloew.rxwear.transformers.MessageEventGetDataMap;

import io.reactivex.disposables.CompositeDisposable;
import rx.Observable;
import rx.subscriptions.CompositeSubscription;

public class MainActivity extends WearableActivity {

    private BoxInsetLayout mContainerView;
    private TextView mTitleText;
    private TextView mMessageText;
    private TextView mPersistentText;
    private TextView mText2;
    // 1.3
    //private CompositeSubscription subscription = new CompositeSubscription();
    private CompositeDisposable subscription = new CompositeDisposable();

    private RxWear rxWear;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mTitleText = (TextView) findViewById(R.id.title);
        mMessageText = (TextView) findViewById(R.id.message);
        mPersistentText = (TextView) findViewById(R.id.persistent);
        mText2 = findViewById(R.id.textView2);


        rxWear = new RxWear(this);

        // 1.3

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



        io.reactivex.Observable<DataMap> om = io.reactivex.Observable.concat(o,m);
        subscription.add(om.map(dataMap -> dataMap.get("text"))
                .subscribe(   text -> {mPersistentText.setText((String)text); mText2.setText((String)text);},
                throwable -> Toast.makeText(this, "Error on data listen", Toast.LENGTH_LONG))
        );


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
}
