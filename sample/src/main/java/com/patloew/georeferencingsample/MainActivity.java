package com.patloew.georeferencingsample;

import android.content.Context;
import android.location.Address;
import android.location.Location;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.LibsBuilder;
import com.patloew.georeferencingsample.UtilMS.UtilsMS;
import com.patloew.rxlocation.RxLocation;
import com.patloew.georeferencingsample.data.DataFactory;

import de.codecrafters.tableview.listeners.SwipeToRefreshListener;
import de.codecrafters.tableview.listeners.TableDataClickListener;
import de.codecrafters.tableview.listeners.TableDataLongClickListener;
import com.patloew.georeferencingsample.data.Car;
import com.patloew.georeferencingsample.geoData.GeoLocation;


import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

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
public class MainActivity extends AppCompatActivity implements MainView {

    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance();

    private Location msLastLocation;

    private TextView lastUpdate;
    private TextView locationText;
    private TextView addressText;
    private Marker currentMarker = null;

     private MapFragment mapFragment =  null;
     private MapView mapView = null;
     private CustomMapView customMapView = null;

    private RxLocation rxLocation;

    private RadioButton radioButtonPositionSource_gps = null;
    private RadioButton radioButtonPositionSource_distance = null;
    private RadioButton radioButtonPositionType_osnowa = null;
    private RadioButton radioButtonPositionType_lampion = null;

    private EditText offsetX;
    private EditText offsetY;

    private RadioGroup radioGroupOsnowaLampion;

    private MainPresenter presenter;
    private Button button2;
    private EditText mackotext;
    private GeoLocationDataAdapter geoAdapter= null;

    private GoogleMap mapaGooglowa = null;

    private Button centerMap;

    private Car getRandomCar() {
        final List<Car> carList = DataFactory.createCarList();
        final int randomCarIndex = Math.abs(new Random().nextInt() % carList.size());
        return carList.get(randomCarIndex);
    }

    private class CarClickListener implements TableDataClickListener<Car> {

        @Override
        public void onDataClicked(final int rowIndex, final Car clickedData) {
            final String carString = "Click: " + clickedData.getProducer().getName() + " " + clickedData.getName();
            Toast.makeText(MainActivity.this, carString, Toast.LENGTH_SHORT).show();
        }
    }

    private class GeoClickListener implements TableDataClickListener<GeoLocation> {

        @Override
        public void onDataClicked(final int rowIndex, final GeoLocation clickedData) {
            final String carString = "Click: " + clickedData.component2() + " " + clickedData.component3();
            Toast.makeText(MainActivity.this, carString, Toast.LENGTH_SHORT).show();
        }
    }

    private class CarLongClickListener implements TableDataLongClickListener<Car> {

        @Override
        public boolean onDataLongClicked(final int rowIndex, final Car clickedData) {
            final String carString = "Long Click: " + clickedData.getProducer().getName() + " " + clickedData.getName();
            Toast.makeText(MainActivity.this, carString, Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    private List<GeoLocation> toLista(){
        List<GeoLocation> res = new ArrayList<>();

        for (GeoLocation e : GeoLocation.Repozytorium.getLocationPositions().values()) {
            res.add(e);
        }
        return res;

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

    private void configureCameraIdle(){
        //GoogleMap.OnCameraIdleListener
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        if( customMapView != null) {
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
                    googleMap.animateCamera( CameraUpdateFactory.zoomTo( 17.0f ) );

                    mapaGooglowa = googleMap;





                    googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener(){
                        @Override
                        public void onMapClick(LatLng arg0)
                        {
                            Log.d("KLIK", "klik");
                            //Toast.makeText(getBaseContext(), "klik", Toast.LENGTH_SHORT);

                        }
                    });

                    googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                        @Override
                        public void onMapLongClick(LatLng arg0) {
                            Log.d("KLIK", "LongKlik");
                            if (googleMap != null) {
                                if(currentMarker != null)
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

        rxLocation = new RxLocation(this);
        rxLocation.setDefaultTimeout(15, TimeUnit.SECONDS);

        presenter = new MainPresenter(rxLocation);

        button2 = findViewById(R.id.button2);
        mackotext = findViewById(R.id.editText);

        radioButtonPositionSource_gps = findViewById(R.id.radioButtonPositionSource_GPS);
        radioButtonPositionSource_distance = findViewById(R.id.radioButtonPositionSource_measure);

        radioButtonPositionType_lampion = findViewById(R.id.radioButton_lampion);
        radioButtonPositionType_osnowa = findViewById(R.id.radioButton_osnowa);

        offsetX = findViewById(R.id.offsetX);
        offsetY = findViewById(R.id.offsetY);

        radioGroupOsnowaLampion = findViewById(R.id.RadioGrupaOsnowaLampion);

        Location l = new Location("dd");
        l.setLatitude(2.2);
        l.setLongitude(2.1);
        //GeoLocation g1 = new GeoLocation(l, 3.3, 5.5, true, 0);
        //GeoLocation g2 = new GeoLocation(l, 2.4, 4.4, true, 0);

        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addStartPoint(l);
        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewLocationPoint(l, -2.2, +3.3);


        final SortableGeoLocationTableView geoTableView = findViewById(R.id.tableViewGeo);
        if(geoTableView != null){
            geoAdapter = new GeoLocationDataAdapter(this,
            //        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositions(), // ok ale sa niepotrzebne tez
                    //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.giveAllStoredPositions(), // nie uaktualnia sie
                    //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.giveAllStoredLocationPositions(), // nie uaktualnia sie
                    //toLista(),
                    com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositionsUser(),
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
                if(radioButtonPositionType_osnowa.isChecked())
                    radioButtonPositionSource_distance.setText("Marker");
                else
                    radioButtonPositionSource_distance.setText("distance");
            }
        });

        //radioButtonPositionType_osnowa.setOnClickListener(new View);
        radioButtonPositionType_osnowa.setOnFocusChangeListener(new View.OnFocusChangeListener(){
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                if(hasFocus)
                    radioButtonPositionSource_distance.setText("Marker");
                else
                    radioButtonPositionSource_distance.setText("distance");
            }
        });


        centerMap.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                LatLng l = new LatLng(msLastLocation.getLatitude(), msLastLocation.getLongitude());

                //googleMap.moveCamera(CameraUpdateFactory.newLatLng(52.1, -21.9));
                mapaGooglowa.moveCamera(CameraUpdateFactory.newLatLng(l));
            }
        });

        button2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick (View v){
                Log.d("SSR", "blebe");
                Location l = new Location("");

                //


                if(radioButtonPositionType_osnowa.isChecked()){
                    Double offX = Double.parseDouble(offsetX.getText().toString());
                    Double offY = Double.parseDouble(offsetY.getText().toString());
                    if(offX == 0.0 && offY == 0.0){
                        Toast.makeText(getBaseContext(), "both offs can be 0.0 for osnowa!", Toast.LENGTH_LONG).show();
                        return;
                    }



                    if(radioButtonPositionSource_gps.isChecked()){

                        // gps

                        //rxLocation.

                        //if(msLastLocation.)
                        l.setLatitude(msLastLocation.getLatitude());
                        l.setLongitude(msLastLocation.getLongitude());

                        //l.setLa
                        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewOsnowaPointWithValidPos(l, 0, offX, offY);



                    } else if(radioButtonPositionSource_distance.isChecked()){

                        if(currentMarker == null){
                            Toast.makeText(getBaseContext(), "no marker!", Toast.LENGTH_LONG).show();
                            return;
                        }


                        // marker
                        l.setLatitude(currentMarker.getPosition().latitude);
                        l.setLongitude(currentMarker.getPosition().longitude);
                        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewOsnowaPointWithValidPos(l, 0, offX, offY);

                        //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewOsnowaPoint(0, );
                    }

                } else if(radioButtonPositionType_lampion.isChecked()){
                    if(radioButtonPositionSource_gps.isChecked()){

                    } else if(radioButtonPositionSource_distance.isChecked()){

                    }
                }

                /*
                if(radioButtonPositionSource_gps.isChecked()) {
                    if(currentMarker != null) {
                        l.setLongitude(currentMarker.getPosition().longitude);
                        l.setLatitude(currentMarker.getPosition().latitude);
                        com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewLocationPoint(l, currentMarker.getPosition().latitude, currentMarker.getPosition().longitude);
                    }} else {

                    //if()

                } //else if(rad)
*/

                // geoTableView. - redraw?
                geoAdapter.notifyDataSetInvalidated();
                geoAdapter.notifyDataSetChanged();


                    //geoTableView.refreshDrawableState();

                GeoLocation.Repozytorium.writePositionsToFile();
                String s = GeoLocation.Repozytorium.readFromFile();
                mackotext.setText("dd" + s);

            }
        });

    }




    @Override
    protected void onStart() {
        super.onStart();
        presenter.attachView(this);
    }

    @Override
    protected void onResume() {
        //mapView.onResume();
        customMapView.onResume();
        super.onResume();

        checkPlayServicesAvailable();
    }

    private void checkPlayServicesAvailable() {
        final GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int status = apiAvailability.isGooglePlayServicesAvailable(this);

        if(status != ConnectionResult.SUCCESS) {
            if(apiAvailability.isUserResolvableError(status)) {
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
        if(item.getItemId() == R.id.menu_licenses) {
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

    @Override
    public void onLocationUpdate(Location location) {

        lastUpdate.setText(DATE_FORMAT.format(new Date()));

        //Location anin = new Location("");
        //anin.setLatitude()

        msLastLocation = location;

        locationText.setText(location.getLatitude() + ", " + location.getLongitude());

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

    private String getAddressText(Address address) {
        String addressText = "";
        final int maxAddressLineIndex = address.getMaxAddressLineIndex();

        for(int i=0; i<=maxAddressLineIndex; i++) {
            addressText += address.getAddressLine(i);
            if(i != maxAddressLineIndex) { addressText += "\n"; }
        }

        return addressText;
    }

}
