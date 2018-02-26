package com.patloew.georeferencingsample;



import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.databinding.DataBindingUtil;
import android.location.Address;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.nononsenseapps.filepicker.Utils;

import com.patloew.georeferencingsample.databinding.ActivityMainMobileBinding;
import com.patloew.georeferencingsample.event.SimpleEvent;
import com.patloew.georeferencingsample.service.MyService;
import com.patloew.rxlocation.RxLocation;
import com.patloew.georeferencingsample.data.DataFactory;

import com.patloew.georeferencingsample.geoData.CalculateDistancesKt;


import butterknife.BindView;
import butterknife.ButterKnife;
import de.codecrafters.tableview.listeners.OnScrollListener;
import de.codecrafters.tableview.listeners.SwipeToRefreshListener;
import de.codecrafters.tableview.listeners.TableDataClickListener;
import de.codecrafters.tableview.listeners.TableDataLongClickListener;
import droidninja.filepicker.FilePickerBuilder;
import kotlin.Pair;
import rx.subscriptions.CompositeSubscription;
//import com.jakewharton.rxbinding.view.RxView;
//import rx.Observable;
//import rx.subscriptions.CompositeSubscription;

import com.patloew.georeferencingsample.data.Car;
import com.patloew.georeferencingsample.geoData.GeoLocation;


import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.patloew.georeferencingsample.mapsHelpers.SizeHelperKt.getBoundingRect;


//import droidninja.filepicker.FilePickerConst;


//import permissions.dispatcher.NeedsPermission;
//import permissions.dispatcher.RuntimePermissions;

/* Copyright 2016 Patrick Löwenstein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. */

// UWAGA UWAGA w gradle musi byc wszedzie to samo:         applicationId "com.patloew.georeferencingsample"
// dla tel i zegarka, inaczej nie będzie komunikacji!

// po wgraniu app na telefon trzeba nanowo wgrac na zegarek?

// observable musi byc skonsumowane - wtedy dziala!
// ale sie zatrzymuje przy wygaszeniu ekranu na telefonie? - aktualizacja pozycji?
public class MainActivity extends AppCompatActivity implements MainView,
        MessageClient.OnMessageReceivedListener {

    ActivityMainMobileBinding binding;


    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {

        Log.d("sowa", "onMessageReceived() A message from watch was received:"
                + messageEvent.getRequestId() + " " + messageEvent.getPath() + messageEvent.getData()[0]);
                /*String message = new String(messageEvent.getData());
                Log.v(TAG, "Main activity received message: " + message);
                // Display message in UI
                logthis(message);
                */

        Toast.makeText(this, messageEvent.getData().toString(), Toast.LENGTH_LONG).show();
    }

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

    private Location msLastLocation;

    private GeoLocation navigationTarget = null;

    private TextView lastUpdate;
    private TextView locationText;
    private TextView addressText;
    private TextView navigateTo_TextView;
    private Marker currentMarker = null;
    private List<Marker> markers = new LinkedList<>();

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };


    private CheckBox checkBoxKeepScreenAlive;

    private MapFragment mapFragment = null;
    private MapView mapView = null;
    private CustomMapView customMapView = null;

    private RxLocation rxLocation;

    private TextView tvKmToCm = null;

    private RadioButton radioButtonPositionSource_gps = null;
    private RadioButton radioButtonPositionSource_distance = null;
    private RadioButton radioButtonPositionType_osnowa = null;
    private RadioButton radioButtonPositionType_lampion = null;

    //private RxWear rxWear;
    private CompositeSubscription subscription = new CompositeSubscription();
    //private Observable<Boolean> validator;

    SortableGeoLocationTableView geoTableView = null;

    private EditText offsetX;
    private EditText offsetY;

    static private int FILE_CODE_WRITE = 147;
    static private int FILE_CODE_READ = 148;

    private RadioGroup radioGroupOsnowaLampion;

    private MainPresenter presenter;
    private Button buttonAddPoint;
    private Button buttonRemove;
    private Button buttonDrawMarkers;
    private Button buttonComputeDistances;
    private Button buttonSave, buttonLoad;

    private RadioButton radioGroupReferenceToZeroPoint;
    private EditText editTextReferenceToPointNr;

    private EditText mackotext;
    private GeoLocationDataAdapter geoAdapter = null;

    private GoogleMap mapaGooglowa = null;

    private Button centerMap;

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private Car getRandomCar() {
        final List<Car> carList = DataFactory.createCarList();
        final int randomCarIndex = Math.abs(new Random().nextInt() % carList.size());
        return carList.get(randomCarIndex);
    }

    private class CarClickListener implements TableDataClickListener<Car> {

        @Override
        public void onDataClicked(final int rowIndex, final Car clickedData) {
            final String carString = "Click1: " + clickedData.getProducer().getName() + " " + clickedData.getName();
            Toast.makeText(MainActivity.this, carString, Toast.LENGTH_SHORT).show();
        }
    }

    void setNavigationToPoint(GeoLocation target) {

        navigationTarget = target;
        setNavigationToText(msLastLocation);
    }

    private class GeoClickListener implements TableDataClickListener<GeoLocation> {

        @Override
        public void onDataClicked(final int rowIndex, final GeoLocation clickedData) {
            final String carString = "Click2: " + clickedData.component2() + " " + clickedData.component3();
            setNavigationToPoint(clickedData);
            Toast.makeText(MainActivity.this, carString, Toast.LENGTH_SHORT).show();
        }
    }

    private class CarLongClickListener implements TableDataLongClickListener<Car> {

        @Override
        public boolean onDataLongClicked(final int rowIndex, final Car clickedData) {
            final String carString = "Long Click3: " + clickedData.getProducer().getName() + " " + clickedData.getName();
            Toast.makeText(MainActivity.this, carString, Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private List<GeoLocation> toLista() {
        List<GeoLocation> res = new ArrayList<>();

        for (GeoLocation e : GeoLocation.Repozytorium.getLocationPositions().values()) {
            res.add(e);
        }
        return res;

    }

    private void serializeToFile(File f) {

        HashMap<String, String> saved = new HashMap<String, String>();
        // kotlin: MutableMap<Int, GeoLocation>
        //HashMap<Integer, Location> m = GeoLocation.Repozytorium.getLocationPositions();


        String json = new Gson().toJson(GeoLocation.Repozytorium.getLocationPositions());
        String json2 = new Gson().toJson(GeoLocation.Repozytorium.getPositionsUser());

        saved.put("A", json);
        saved.put("B", json2);

        Context context = this.getApplicationContext();
        try {
            FileOutputStream fos = new FileOutputStream(f);
            //fos.write(json.getBytes());
            //fos.write(json2.getBytes());
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(saved);
            oos.flush();
            oos.close();
            fos.close();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "can write to file", Toast.LENGTH_SHORT).show();
        }

    }

    private void serializeToFile() {
        //Map<Integer, GeoLocation> m = GeoLocation.Repozytorium.getLocationPositions();
        String json = new Gson().toJson(GeoLocation.Repozytorium.getLocationPositions());

        Context context = this.getApplicationContext();
        File path = context.getFilesDir();
        File file = new File(path, "oko.txt");

        //OutputStreamWriter osw = new OutputStreamWriter(

        try {
            file.createNewFile();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(json.getBytes());
            fos.close();

        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "can write to file", Toast.LENGTH_SHORT).show();
        }


        File file2 = new File(path, "oko2.txt");
        String json2 = new Gson().toJson(GeoLocation.Repozytorium.getPositionsUser());
        //OutputStreamWriter osw = new OutputStreamWriter(

        try {
            file2.createNewFile();
            FileOutputStream fos2 = new FileOutputStream(file2);
            fos2.write(json2.getBytes());
            fos2.close();

        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "can write to file", Toast.LENGTH_SHORT).show();
        }
    }

    private String getStringFromFile(File f) {
        Context context = this.getApplicationContext();

        StringBuilder textB = new StringBuilder();


        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            String line;

            while ((line = br.readLine()) != null) {
                textB.append(line);
                textB.append('\n');
            }
            br.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }

        String text = textB.toString();
        return text;
    }

    private String getStringFromFile(String fileName) {
        Context context = this.getApplicationContext();
        File path = context.getFilesDir();
        File file = new File(path, fileName);
        StringBuilder textB = new StringBuilder();


        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;

            while ((line = br.readLine()) != null) {
                textB.append(line);
                textB.append('\n');
            }
            br.close();
        } catch (IOException e) {
            //You'll need to add proper error handling here
        }

        String text = textB.toString();
        return text;
    }

    private void redrawTable() {
        geoTableView.invalidate();
        geoTableView.refreshDrawableState();

        geoAdapter = new GeoLocationDataAdapter(this,
                //        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositions(), // ok ale sa niepotrzebne tez
                //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.giveAllStoredPositions(), // nie uaktualnia sie
                //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.giveAllStoredLocationPositions(), // nie uaktualnia sie
                //toLista(),
                //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositionsUser(),
                GeoLocation.Repozytorium.getPositionsUser(),
                geoTableView);
        geoTableView.setDataAdapter(geoAdapter);
        geoTableView.addDataClickListener(new GeoClickListener());

    }

    private void deserializeFromFile(File f) {


        //String text = getStringFromFile(f);
        try {
            FileInputStream fis = new FileInputStream(f);

            ObjectInputStream ois = new ObjectInputStream(fis);
            HashMap<String, String> retreived = (HashMap<String, String>) ois.readObject();
            String t1 = retreived.get("A");
            String t2 = retreived.get("B");

            fis.close();

            Gson gson = new Gson();
            //Map<Integer, GeoLocation>
            //gson.fromJson(t1, object:MutableMap<Int, GeoLocation>);
            //gson.fromJson(text.toString(), Map<Integer, GeoLocation>);
            GeoLocation.Repozytorium.getMutableMapFromJsonLocationPositions(gson, t1);
            GeoLocation.Repozytorium.getMutableMapFromJsonPositionsUser(gson, t2);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "can read from file", Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(MainActivity.this, "data updated, size of table=" + GeoLocation.Repozytorium.getPositionsUser().size(), Toast.LENGTH_LONG).show();
        redrawTable();
    }

    private void deserializeFromFile() {
        //MutableMap<Int, GeoLocation> = mutableMapOf()
        //String json = new Gson().toJson(GeoLocation.Repozytorium.getLocationPositions());
        String text = getStringFromFile("oko.txt");
        String textP = getStringFromFile("oko2.txt");

        try {
            Gson gson = new Gson();
            //Map<Integer, GeoLocation>
            //gson.fromJson(text.toString(), object:MutableMap<Int, GeoLocation>);
            //gson.fromJson(text.toString(), Map<Integer, GeoLocation>);
            GeoLocation.Repozytorium.getMutableMapFromJsonLocationPositions(gson, text.toString());
            GeoLocation.Repozytorium.getMutableMapFromJsonPositionsUser(gson, textP.toString());
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "can write to file", Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(MainActivity.this, "data updated, size of table=" + GeoLocation.Repozytorium.getPositionsUser().size(), Toast.LENGTH_LONG).show();

        redrawTable();
        //geoAdapter.notifyDataSetInvalidated();
        //geoAdapter.notifyDataSetChanged();

    }




    /*
    protected void setUpMapIfNeeded() {

        // Do a null check to confirm that we have not already instantiated the map.
        if (mapFragment == null) {
            mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView));
            // Check if we were successful in obtaining the map.
            if (mapFragment != null) {
                mapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(GoogleMap map) {
                        loadMap(map);
                    }
                });
            }
        }
    }
    */

    private class MyOnScrollListener implements OnScrollListener {
        @Override
        public void onScroll(final ListView tableDataView, final int firstVisibleItem, final int visibleItemCount, final int totalItemCount) {
            // listen for scroll changes
        }

        @Override
        public void onScrollStateChanged(final ListView tableDateView, final OnScrollListener.ScrollState scrollState) {
            // listen for scroll state changes
        }
    }

    private void configureCameraIdle() {
        //GoogleMap.OnCameraIdleListener
    }


    private void pickFile() {
        ArrayList<String> filePaths = new ArrayList<>();
        FilePickerBuilder.getInstance().setMaxCount(10)
                .setSelectedFiles(filePaths)
                .setActivityTheme(R.style.AppTheme).pickFile(this);
        //.pickFile(this);

    }

    public static Intent createIntent(Context context) {
        return new Intent(context, MainActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);




        setContentView(R.layout.activity_main_mobile);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main_mobile);

        //binding.textViewKmToCm.setText("mru");
        //binding.textViewCmToKm.setText("mru");
                //rxWear = new RxWear(this);


        //Wearable.getMessageClient(this).addListener(this);

        verifyStoragePermissions(this);
/*
        mapView = findViewById(R.id.mapView);
        if( mapView != null) {
            mapView.onCreate(null);
            mapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    //map = googleMap;

                    googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
    //                googleMap.getUiSettings().setZoomGesturesEnabled(true);
                    googleMap.getUiSettings().setCompassEnabled(true);
                    googleMap.getUiSettings().setScrollGesturesEnabled(true);
//                    googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                    googleMap.getUiSettings().setZoomGesturesEnabled(true);
                    googleMap.getUiSettings().setZoomControlsEnabled(true);
                    googleMap.getUiSettings().setMapToolbarEnabled(true);
                    //googleMap.setMinZoomPreference(6.0f);
                    //googleMap.setMaxZoomPreference(14.0f);

                    LatLng l = new LatLng(-21.1, 52.2);
                    //googleMap.moveCamera(CameraUpdateFactory.newLatLng(52.1, -21.9));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(l));


                    //Do what you want with the map!!
                }
            });

            MapsInitializer.initialize(this);
        }
*/

        customMapView = findViewById(R.id.mapView);
        if (customMapView != null) {
            customMapView.onCreate(null);
            customMapView.getMapAsync(new OnMapReadyCallback() {

                @Override
                public void onMapReady(GoogleMap googleMap) {
                    //map = googleMap;

                    googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                    //                googleMap.getUiSettings().setZoomGesturesEnabled(true);
                    googleMap.getUiSettings().setCompassEnabled(true);
                    googleMap.getUiSettings().setScrollGesturesEnabled(true);
//                    googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                    googleMap.getUiSettings().setZoomGesturesEnabled(true);
                    googleMap.getUiSettings().setZoomControlsEnabled(true);
                    googleMap.getUiSettings().setMapToolbarEnabled(true);
                    //googleMap.setMinZoomPreference(6.0f);
                    //googleMap.setMaxZoomPreference(14.0f);

                    LatLng l = new LatLng(52.1, 21.2);

                    //googleMap.moveCamera(CameraUpdateFactory.newLatLng(52.1, -21.9));
                    googleMap.moveCamera(CameraUpdateFactory.newLatLng(l));
                    //googleMap.setZ
                    googleMap.animateCamera(CameraUpdateFactory.zoomTo(17.0f));

                    mapaGooglowa = googleMap;


                    googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
                        @Override
                        public void onMapClick(LatLng arg0) {
                            Log.d("KLIK", "klik");
                            //Toast.makeText(getBaseContext(), "klik", Toast.LENGTH_SHORT);

                        }
                    });

                    googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                        @Override
                        public void onMapLongClick(LatLng arg0) {
                            Log.d("KLIK", "LongKlik");
                            if (googleMap != null) {
                                if (currentMarker != null)
                                    currentMarker.remove();

                                currentMarker = googleMap.addMarker(new MarkerOptions()
                                        .position(arg0).title("mruk")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN))
                                        .draggable(false).visible(true));
                            }
                        }

                    });


                    //Do what you want with the map!!
                }
            });


            MapsInitializer.initialize(this);
        }

        centerMap = findViewById(R.id.buttonCenterMap);


        lastUpdate = findViewById(R.id.tv_last_update);
        locationText = findViewById(R.id.tv_current_location);
        addressText = findViewById(R.id.tv_current_address);
        navigateTo_TextView = findViewById(R.id.textView_goToPoint);

        rxLocation = new RxLocation(this);
        rxLocation.setDefaultTimeout(15, TimeUnit.SECONDS);

        presenter = new MainPresenter(rxLocation);

        checkBoxKeepScreenAlive = findViewById(R.id.checkBoxKeepScreenAlive);

        buttonAddPoint = findViewById(R.id.button2);
        buttonRemove = findViewById(R.id.buttonRemovePoint);

        radioGroupReferenceToZeroPoint = findViewById(R.id.radioButtonPointZero);
        editTextReferenceToPointNr = findViewById(R.id.editTextReferencePoint);


        buttonSave = findViewById(R.id.buttonSave);
        buttonLoad = findViewById(R.id.buttonLoad);


        tvKmToCm = findViewById(R.id.textViewKmToCm);




        buttonDrawMarkers = findViewById(R.id.buttonShowPointsOnMap);
        buttonComputeDistances = findViewById(R.id.buttonCalculateDistances);
        mackotext = findViewById(R.id.editTextName);

        radioButtonPositionSource_gps = findViewById(R.id.radioButtonPositionSource_GPS);
        radioButtonPositionSource_distance = findViewById(R.id.radioButtonPositionSource_measure);

        radioButtonPositionType_lampion = findViewById(R.id.radioButton_lampion);
        radioButtonPositionType_osnowa = findViewById(R.id.radioButton_osnowa);

        offsetX = findViewById(R.id.offsetX);
        offsetY = findViewById(R.id.offsetY);

        radioGroupOsnowaLampion = findViewById(R.id.RadioGrupaOsnowaLampion);

        //Location l = new Location("dd");
        //l.setLatitude(2.2);
        //l.setLongitude(2.1);
        //GeoLocation g1 = new GeoLocation(l, 3.3, 5.5, true, 0);
        //GeoLocation g2 = new GeoLocation(l, 2.4, 4.4, true, 0);

        //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addStartPoint(l);
        //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewLocationPoint(l, -2.2, +3.3);

        checkBoxKeepScreenAlive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // update your model (or other business logic) based on isChecked
                if (isChecked)
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else {
                    getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    //getWindow().addFlags(WindowManager.LayoutParams.FLAG_SC);
                }


            }
        });


        //final SortableGeoLocationTableView geoTableView = findViewById(R.id.tableViewGeo);
        geoTableView = findViewById(R.id.tableViewGeo);
        if (geoTableView != null) {
            geoTableView.setScrollBarSize(3);
            geoTableView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET);
            //geoTableView.setScrollContainer(true);
            //geoTableView.scroll
            geoTableView.addOnScrollListener(new MyOnScrollListener());


            geoAdapter = new GeoLocationDataAdapter(this,
                    //        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositions(), // ok ale sa niepotrzebne tez
                    //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.giveAllStoredPositions(), // nie uaktualnia sie
                    //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.giveAllStoredLocationPositions(), // nie uaktualnia sie
                    //toLista(),
                    //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositionsUser(),
                    GeoLocation.Repozytorium.getPositionsUser(),
                    geoTableView);
            geoTableView.setDataAdapter(geoAdapter);
            geoTableView.addDataClickListener(new GeoClickListener());

        }

        final SortableCarTableView carTableView = (SortableCarTableView) findViewById(R.id.tableView);
        if (carTableView != null) {
            final CarTableDataAdapter carTableDataAdapter = new CarTableDataAdapter(this, DataFactory.createCarList(), carTableView);
            carTableView.setDataAdapter(carTableDataAdapter);
            carTableView.addDataClickListener(new CarClickListener());
            carTableView.addDataLongClickListener(new CarLongClickListener());
            carTableView.setSwipeToRefreshEnabled(true);
            carTableView.setSwipeToRefreshListener(new SwipeToRefreshListener() {
                @Override
                public void onRefresh(final RefreshIndicator refreshIndicator) {
                    carTableView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            final Car randomCar = getRandomCar();
                            carTableDataAdapter.getData().add(randomCar);
                            carTableDataAdapter.notifyDataSetChanged();
                            refreshIndicator.hide();
                            Toast.makeText(MainActivity.this, "Added: " + randomCar, Toast.LENGTH_SHORT).show();
                        }
                    }, 3000);
                }
            });
        }


        radioGroupOsnowaLampion.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (radioButtonPositionType_osnowa.isChecked())
                    radioButtonPositionSource_distance.setText("Marker");
                else
                    radioButtonPositionSource_distance.setText("distance");
            }
        });

        radioGroupReferenceToZeroPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                editTextReferenceToPointNr.setText("");
            }
        });



        /*
        radioGroupReferenceToZeroPoint.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                if(hasFocus)
                    editTextReferenceToPointNr.setText("");
            }
        });
*/

        editTextReferenceToPointNr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                radioGroupReferenceToZeroPoint.setChecked(false);
            }
        });


/*
        editTextReferenceToPointNr.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
               // radioGroupReferenceToZeroPoint.setChecked(false);
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                                          int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                //radioGroupReferenceToZeroPoint.setChecked(false);
                //if (s.charAt(s.length() - 1) == '\n') {
//                    Log.d("TEST RESPONSE", "Enter was pressed");
//                }
            }
        });
*/

/*
        editTextReferenceToPointNr.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                if(hasFocus){
                    // i dodane android:imeOptions="actionSend" w EditText
                    radioGroupReferenceToZeroPoint.setChecked(false);
                }
            }
        });
*/

        //radioButtonPositionType_osnowa.setOnClickListener(new View);
        radioButtonPositionType_osnowa.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus)
                    radioButtonPositionSource_distance.setText("Marker");
                else
                    radioButtonPositionSource_distance.setText("distance");
            }
        });


        centerMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LatLng l = new LatLng(msLastLocation.getLatitude(), msLastLocation.getLongitude());

                //googleMap.moveCamera(CameraUpdateFactory.newLatLng(52.1, -21.9));
                mapaGooglowa.moveCamera(CameraUpdateFactory.newLatLng(l));
            }
        });


        buttonComputeDistances.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {




                //binding.textViewCmToKm.setText("Ha!");
// 4.3, 3.4 - czd

                Double odl = CalculateDistancesKt.calibrateDistances(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.giveAllStoredLocationPositions());
                GeoLocation.Repozytorium.addNewOsnowaPointWithValidPosFromMarker(0, 8.65, 1, 9.25, 1.0/odl);
                redrawTable();
            }
        });
        buttonDrawMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DrawMarkers(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositionsUser());


            }

        });

        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                serializeToFile();

                startActivity(FILE_CODE_WRITE, FilePickerActivity.class);

/*
                Intent i = new Intent(getApplicationContext(), FilePickerActivity.class);
                // This works if you defined the intent filter
                // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

                // Set these depending on your use case. These are the defaults.

                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_NEW_FILE);

                // Configure initial directory by specifying a String.
                // You could specify a String like "/storage/emulated/0/", but that can
                // dangerous. Always use Android's API calls to get paths to the SD-card or
                // internal memory.
                i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());

                startActivityForResult(i, FILE_CODE );
*/

            }




        });




        buttonLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


//                String s2 = DATE_FORMAT.format(new Date());


        //        Toast.makeText(getApplicationContext(), "sent message?", Toast.LENGTH_LONG).show();
                //startActivity(FILE_CODE_READ, FilePickerActivity.class);


                Intent i = new Intent(getBaseContext(), FilePickerActivity.class);
                // This works if you defined the intent filter
                // Intent i = new Intent(Intent.ACTION_GET_CONTENT);

                // Set these depending on your use case. These are the defaults.
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
                i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
                i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE);

                // Configure initial directory by specifying a String.
                // You could specify a String like "/storage/emulated/0/", but that can
                // dangerous. Always use Android's API calls to get paths to the SD-card or
                // internal memory.
                //i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath());
                i.putExtra(FilePickerActivity.EXTRA_START_PATH,getFilesDir().getAbsolutePath() + "/manewry");


                startActivityForResult(i, FILE_CODE_READ);


                //deserializeFromFile();
                //pickFile();

            }
        });

        buttonRemove.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                if (navigationTarget == null)
                                                    return;

                                                int nr = navigationTarget.getNum();

                                                GeoLocation.Repozytorium.getPositionsUser().remove(navigationTarget);
                                                GeoLocation.Repozytorium.getLocationPositions().remove(nr);
                                                geoAdapter.notifyDataSetInvalidated();
                                                geoAdapter.notifyDataSetChanged();

                                                int numOfPoints = com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getLocationPositions().size();
                                                MyService.sendToWearPointsNumberData(numOfPoints);

                                            }
                                        }
        );

        buttonAddPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("SSR", "blebe");
                Location l = new Location("");

                //

                Double offX = Double.parseDouble(offsetX.getText().toString());
                Double offY = Double.parseDouble(offsetY.getText().toString());

                int refPointNr = 0;
                if (!radioGroupReferenceToZeroPoint.isChecked()) {
                    String s = editTextReferenceToPointNr.getText().toString();
                    try {
                        refPointNr = Integer.parseInt(s);
                    } catch (Exception e) {
                        refPointNr = 0;
                    }
                }

                if (radioButtonPositionType_osnowa.isChecked()) {

                    if ((offX == 0.0 && offY == 0.0) && !com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.isRepositoryEmpty()) {
                        Toast.makeText(getBaseContext(), "both offs can be 0.0 for osnowa!", Toast.LENGTH_LONG).show();
                        return;
                    }


                    if (radioButtonPositionSource_gps.isChecked()) {

                        // gps

                        //rxLocation.

                        //if(msLastLocation.)
                        l.setLatitude(msLastLocation.getLatitude());
                        l.setLongitude(msLastLocation.getLongitude());

                        //l.setLa
                        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewOsnowaPointWithValidPosFromGPS(l, refPointNr, offX, offY);


                    } else if (radioButtonPositionSource_distance.isChecked()) {

                        if (currentMarker == null) {
                            Toast.makeText(getBaseContext(), "no marker!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // marker
                        l.setLatitude(currentMarker.getPosition().latitude);
                        l.setLongitude(currentMarker.getPosition().longitude);
                        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewOsnowaPointWithValidPosFromMarker(l, refPointNr, offX, offY);

                    }

                } else if (radioButtonPositionType_lampion.isChecked()) {
                    // lampion


                    if (radioButtonPositionSource_gps.isChecked()) {


                    } else if (radioButtonPositionSource_distance.isChecked()) {

                        if ((offX == 0.0 && offY == 0.0) && !com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.isRepositoryEmpty()) {
                            Toast.makeText(getBaseContext(), "both offs can be 0.0 for lampion!", Toast.LENGTH_LONG).show();
                            return;
                        }

                        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewLampionPoint(refPointNr, offX, offY);
                    }
                }

                /*
                if(radioButtonPositionSource_gps.isChecked()) {
                    if(currentMarker != null) {
                        l.setLongitude(currentMarker.getPosition().longitude);
                        l.setLatitude(currentMarker.getPosition().latitude);
                        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewLocationPoint(l, currentMarker.getPosition().latitude, currentMarker.getPosition().longitude);
                    }} else {



                } //else if(rad)
*/

                calculateAndShowDistances();

                //tvKmToCm.setText("Ho" + odl);


                //@BindView(R.id.textViewCmToKm) TextView button1;

                // geoTableView. - redraw?
                geoAdapter.notifyDataSetInvalidated();
                geoAdapter.notifyDataSetChanged();


                //geoTableView.refreshDrawableState();

                GeoLocation.Repozytorium.writePositionsToFile();
                String s = GeoLocation.Repozytorium.readFromFile();
                mackotext.setText("dd" + s);

                int numOfPoints = com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getLocationPositions().size();
                MyService.sendToWearPointsNumberData(numOfPoints);


                DrawMarkers(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositionsUser());



            }
        });

        startService(new Intent(this, MyService.class)); // ok ale zwraca null w service
        //startService(new Intent(getApplicationContext(), MyService.class));

    }


    protected void calculateAndShowDistances(){
        Double odl = CalculateDistancesKt.calibrateDistances(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.giveAllStoredLocationPositions());
        Log.d("SSR", "odleglosci");
        CalculateDistancesKt.computeLampionDistances(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.giveAllStoredLocationPositions(), odl);
        Log.d("SSR", "lampiony obliczone");


        binding.textViewCmToKm.setText("" + odl);
        binding.textViewKmToCm.setText("" + 1000.0 / odl);

    }

    protected void moveMapTo(Pair<Location, Location> pair){
        LatLng l = new LatLng(pair.getFirst().getLatitude(), pair.getFirst().getLongitude());

        //googleMap.moveCamera(CameraUpdateFactory.newLatLng(52.1, -21.9));
        mapaGooglowa.moveCamera(CameraUpdateFactory.newLatLng(l));
        //googleMap.setZ
        mapaGooglowa.animateCamera(CameraUpdateFactory.zoomTo(17.0f));
    }

    // onCreate end

    protected void startActivity(final int code, final Class<?> klass) {
        final Intent i = new Intent(this, klass);

        i.setAction(Intent.ACTION_GET_CONTENT);
//FilePickerActivity.EXTRA_START_PATH
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);


        //mode =


        i.putExtra(FilePickerActivity.EXTRA_MODE, AbstractFilePickerFragment.MODE_FILE);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        i.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, true);
        i.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_NEW_FILE);

        File manewryFolder = new File(getFilesDir().getAbsolutePath() + "/manewry");
        if (!manewryFolder.exists())
            manewryFolder.mkdir();

        addressText.setText(getFilesDir().getAbsolutePath());
        i.putExtra(FilePickerActivity.EXTRA_START_PATH, getFilesDir().getAbsolutePath() + "/manewry/ee.txt");
//        i.putExtra(FilePickerActivity.EXTRA_START_PATH, Environment.getExternalStorageDirectory().getPath() + "/manewry/ee.txt");

        // This line is solely so that test classes can override intents given through UI
        i.putExtras(getIntent());

        startActivityForResult(i, code);
    }


    @Override
    protected void onStart() {
        super.onStart();
        presenter.attachView(this);

        if(!EventBus.getDefault().isRegistered(this))
            EventBus.getDefault().register(this);


    }

    @Override
    protected void onResume() {
        //mapView.onResume();
        customMapView.onResume();
        super.onResume();

        checkPlayServicesAvailable();

        Wearable.getMessageClient(this).addListener(this);

    }

    public String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};

        //This method was deprecated in API level 11
        //Cursor cursor = managedQuery(contentUri, proj, null, null, null);

        CursorLoader cursorLoader = new CursorLoader(
                this,
                contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        int column_index =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        locationText.setText("got file!");
        //Toast.makeText(this, "pliki wynik ale jaki", Toast.LENGTH_LONG);
        if ((FILE_CODE_WRITE == requestCode || FILE_CODE_READ == requestCode) && resultCode == Activity.RESULT_OK) {
            //locationText.setText("got file!" + "req=" + requestCode + " res=" + resultCode);

            //Toast.makeText(this, "pliki wynik jest=", Toast.LENGTH_LONG);
            // Use the provided utility method to parse the
            //com.nononsenseapps.filepicker.Utils.
            List<Uri> files = Utils.getSelectedFilesFromResult(data);
            locationText.setText(" S=" + files.size());
            for (Uri uri : files) {
                locationText.setText(locationText.getText() + uri.toString());

                File f = null;
                try {
                    verifyStoragePermissions(this);
                    //FileOutputStream fos = openFileOutput(getRealPathFromURI(uri), MODE_PRIVATE);
                    //fos.write(data2.getBytes());
                    //fos.close();


                    String path = uri.getPath();
                    // content://com.patloew.rxlocationsample.provider/root/data/data/com.patloew.rxlocationsample/files/manewry/ee.txt
                    //String path = "/data/data/com.patloew.rxlocationsample/files/manewry/ee.txt"; // OK!
                    path = path.substring(path.indexOf("data") - 1);
                    f = new File(new URI("file:" + path));
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if (FILE_CODE_READ == requestCode) {
                    deserializeFromFile(f);

                    calculateAndShowDistances();

                    Pair<Location,Location> m = getBoundingRect(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getLocationPositions());

                    moveMapTo(m);

                    DrawMarkers(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositionsUser());

                } else if (FILE_CODE_WRITE == requestCode) {

                    String data2 = "bobo";

                    try {
                        if (!f.exists())
                            f.createNewFile();

                        /*
                        OutputStreamWriter outputStreamWriter =
                                new OutputStreamWriter(new FileOutputStream(f));
                        outputStreamWriter.write(data2);
                        outputStreamWriter.close();
                        */
                        serializeToFile(f);
                        //InputStream in =  getContentResolver().openInputStream(uri);


                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                //Toast.makeText(this, "plik=" + uri.toString(), Toast.LENGTH_LONG);
                //File file = Utils.getFileForUri(uri);
                // Do something with the result...
            }
            return;
        }
        switch (requestCode) {

            /*
            case FilePickerConst.REQUEST_CODE_DOC:
                if(resultCode== Activity.RESULT_OK && data!=null)
                {
//                    docPaths = new ArrayList<>();
//                    docPaths.addAll(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS));
                }
                break;
                */
        }


        //
        //        addThemToView(photoPaths,docPaths);
    }


    private void checkPlayServicesAvailable() {
        final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int status = apiAvailability.isGooglePlayServicesAvailable(this);

        if (status != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(status)) {
                apiAvailability.getErrorDialog(this, status, 1).show();
            } else {
                Snackbar.make(lastUpdate, "Google Play Services unavailable. This app will not work", Snackbar.LENGTH_INDEFINITE).show();
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        presenter.detachView();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MyApplication.getRefWatcher().watch(presenter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_licenses) {
            new LibsBuilder()
                    .withFields(Libs.toStringArray(R.string.class.getFields()))
                    .withActivityStyle(Libs.ActivityStyle.LIGHT_DARK_TOOLBAR)
                    .withActivityTitle("Open Source Licenses")
                    .withLicenseShown(true)
                    .start(this);

            return true;
        }

        return false;
    }

    // View Interface

    private void setNavigationToText(Location loc) {

        if (navigationTarget == null) {
            navigateTo_TextView.setText("no point set");
            return;
        }

        Location target = navigationTarget.getLocation();
        float dist = loc.distanceTo(target);
        float dir = loc.bearingTo(target);

        navigateTo_TextView.setText("nav to pkt:" + navigationTarget.getNum() + " dist=" + dist + " dir=" + dir);


    }

    @Override
    public void onLocationUpdate(Location location) {

        lastUpdate.setText(DATE_FORMAT.format(new Date()));

        msLastLocation = location;

        locationText.setText(location.getLatitude() + ", " + location.getLongitude());

        setNavigationToText(location);
    }

    @Override
    public void onAddressUpdate(Address address) {
        addressText.setText(getAddressText(address));
    }

    @Override
    public void onLocationSettingsUnsuccessful() {
        Snackbar.make(lastUpdate, "Location settings requirements not satisfied. Showing last known location if available.", Snackbar.LENGTH_INDEFINITE)
                .setAction("Retry", view -> presenter.startLocationRefresh())
                .show();
    }


    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    public void onEvent(SimpleEvent event) {

        //@Bind(R.id.editTextName) EditText editTextName;
        EditText et = ButterKnife.findById(this, R.id.editTextName);
        et.setText(event.getDate().toString());

    }

    private String getAddressText(Address address) {
        String addressText = "";
        final int maxAddressLineIndex = address.getMaxAddressLineIndex();

        for (int i = 0; i <= maxAddressLineIndex; i++) {
            addressText += address.getAddressLine(i);
            if (i != maxAddressLineIndex) {
                addressText += "\n";
            }
        }

        return addressText;
    }

    private void DrawMarkers(List<GeoLocation> locations) {

        for (GeoLocation g : locations) {

            Location l = g.getLocation();
            if (l != null) {

                LatLng ll = new LatLng(l.getLatitude(), l.getLongitude());

                String desc = "";
                float kolor = BitmapDescriptorFactory.HUE_CYAN;

                switch (g.getType()) {
                    case OSNOWA_MARKER:
                        desc = "OS_mark_";
                        kolor = BitmapDescriptorFactory.HUE_BLUE;
                        break;
                    case OSNOWA_GPS:
                        desc = "OS_gps_";
                        kolor = BitmapDescriptorFactory.HUE_AZURE;
                        break;
                    case LAMPION_MARKER:
                        desc = "LA_mark_";
                        kolor = BitmapDescriptorFactory.HUE_ORANGE;
                        break;
                    case LAMPION_OFF:
                        desc = "LA_off_";
                        kolor = BitmapDescriptorFactory.HUE_RED;
                        break;
                }


                desc += g.getNum();

                currentMarker = mapaGooglowa.addMarker(new MarkerOptions()
                        .position(ll).title(desc)
                        .icon(BitmapDescriptorFactory.defaultMarker(kolor))
                        .draggable(false).visible(true));
            }

        }


    }

}
