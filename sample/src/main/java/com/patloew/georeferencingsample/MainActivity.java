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
import android.widget.ArrayAdapter;
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
import com.patloew.georeferencingsample.geoData.PointType;
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

import static com.patloew.georeferencingsample.mapsHelpers.SizeHelperKt.getBoundingRect;

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


    private CompositeSubscription subscription = new CompositeSubscription();

    SortableGeoLocationTableView geoTableView = null;


    static private int FILE_CODE_WRITE = 147;
    static private int FILE_CODE_READ = 148;

    private RadioGroup radioGroupOsnowaLampion;

    private MainPresenter presenter;
    private Button buttonAddPoint;
    private Button buttonRemove;
    private Button buttonDrawMarkers;
    private Button buttonComputeDistances;



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



    private void updateAfterChangeNrOfPoints(){
        //redrawTable();
        if(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getLocationPositions().size() >= 2)
            calculateAndShowDistances();
        redrawTable();
        DrawMarkers(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositionsUser());
        fillSpinnersWithPoints();

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

        updateAfterChangeNrOfPoints();

    }

    private void fillSpinnersWithPoints(){

        List<String> categories = new ArrayList<String>();

        String selectedPos11 = "";
        if(binding.spinner11.getCount() > 0)
            selectedPos11 = binding.spinner11.getSelectedItem().toString();
        String selectedPos21 = "";
        if(binding.spinner21.getCount() > 0)
            selectedPos21 = binding.spinner21.getSelectedItem().toString();
        String selectedPos22 = "";
        if(binding.spinner22.getCount() > 0)
            selectedPos22 = binding.spinner22.getSelectedItem().toString();

        for(GeoLocation o :
            com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getLocationPositions().values()){
            String s = "" + o.getNum();
            if(o.getType() == PointType.OSNOWA_GPS || o.getType() == PointType.OSNOWA_MARKER)
                s += " OSN";
            categories.add(s);
        }

        //categories.add("Automobile");
        //categories.add("Business Services");

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, categories);

        binding.spinner11.setAdapter(dataAdapter);
        binding.spinner21.setAdapter(dataAdapter);
        binding.spinner22.setAdapter(dataAdapter);

        if(categories.contains(selectedPos11)) {
            int i = dataAdapter.getPosition(selectedPos11);
            binding.spinner11.setSelection(i);
        }
        if(categories.contains(selectedPos21)) {
            int i = dataAdapter.getPosition(selectedPos21);
            binding.spinner21.setSelection(i);
        }
        if(categories.contains(selectedPos22)) {
            int i = dataAdapter.getPosition(selectedPos22);
            binding.spinner22.setSelection(i);
        }

    }


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

        //Wearable.getMessageClient(this).addListener(this);

        verifyStoragePermissions(this);

        customMapView = findViewById(R.id.mapView);
        if (customMapView != null) {
            customMapView.onCreate(null);
            customMapView.getMapAsync(new OnMapReadyCallback() {

                @Override
                public void onMapReady(GoogleMap googleMap) {

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

        tvKmToCm = findViewById(R.id.textViewKmToCm);

        buttonDrawMarkers = findViewById(R.id.buttonShowPointsOnMap);
        buttonComputeDistances = findViewById(R.id.buttonCalculateDistances);
        mackotext = findViewById(R.id.editTextName);

        radioGroupOsnowaLampion = findViewById(R.id.RadioGrupaOsnowaLampion);

        binding.Grupa1Wartosc.setVisibility(View.VISIBLE);
        binding.Grupa2Wartosci.setVisibility(View.INVISIBLE);
        binding.radioButtonOsnowa.setChecked(true);
        binding.radioButtonOsnowaMarker.setChecked(true);

        checkBoxKeepScreenAlive.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // update your model (or other business logic) based on isChecked
                if (isChecked)
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else {
                    getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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



        binding.radioButtonLampion.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        binding.RadioGrupaLampiony.setVisibility(View.VISIBLE);
                        binding.RadioGrupaOsnowa.setVisibility(View.INVISIBLE);
                    }
                }
        );

        binding.radioButtonOsnowa.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        binding.RadioGrupaLampiony.setVisibility(View.INVISIBLE);
                        binding.RadioGrupaOsnowa.setVisibility(View.VISIBLE);
                    }
                }
        );


        binding.radioButtonOsnowaOdl.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        binding.Grupa1Wartosc.setVisibility(View.VISIBLE);
                        binding.Grupa2Wartosci.setVisibility(View.INVISIBLE);
                        binding.offsetYG1.setVisibility(View.INVISIBLE);
                    }
                }
        );

        binding.radioButtonOsnowaXy.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        binding.Grupa1Wartosc.setVisibility(View.VISIBLE);
                        binding.Grupa2Wartosci.setVisibility(View.INVISIBLE);
                        binding.offsetYG1.setVisibility(View.VISIBLE);
                    }
                }
        );

        binding.radioButtonLampionXy.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        binding.Grupa1Wartosc.setVisibility(View.VISIBLE);
                        binding.Grupa2Wartosci.setVisibility(View.INVISIBLE);
                        binding.offsetYG1.setVisibility(View.VISIBLE);
                    }
                }
        );

        binding.radioButtonLampionOdl.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        binding.Grupa1Wartosc.setVisibility(View.INVISIBLE);
                        binding.Grupa2Wartosci.setVisibility(View.VISIBLE);
//                        binding.offsetYG1.setVisibility(View.INVISIBLE);
                    }
                }
        );


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


            }
        });
        buttonDrawMarkers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                DrawMarkers(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositionsUser());


            }

        });

        binding.buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                serializeToFile();

                startActivity(FILE_CODE_WRITE, FilePickerActivity.class);

            }
        });

        binding.buttonLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


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

                                                MyService.sendToWearNumberOfPoints();
                                            }
                                        }
        );

        buttonAddPoint.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //addPointOldApproach();
                addPointNewApproach();
                MyService.sendToWearNumberOfPoints();

            }


        });

        startService(new Intent(this, MyService.class)); // ok ale zwraca null w service
        //startService(new Intent(getApplicationContext(), MyService.class));

    }


    private void addPointNewApproach(){

        Location l = new Location("");

        boolean singleXYset = true;
        boolean twoOdl = true;
        Double offX = 0.0;
        Double offY = 0.0;

        Double offX21 = 0.0;
        Double offX22 = 0.0;

        try {
            offX = Double.parseDouble(binding.offsetXG1.getText().toString());
            offY = Double.parseDouble(binding.offsetYG1.getText().toString());
        } catch(Exception e) {
            singleXYset = false;

        }
        try{
            offX21 = Double.parseDouble(binding.wartoscG21.getText().toString());
            offX22 = Double.parseDouble(binding.wartoscG22.getText().toString());
        } catch (Exception e) {
            twoOdl = false;
        }

        int ref22 = -1;
        int ref21 = -1;
        int ref1 = -1;
        //if(binding.spinner11.isSelected()){
        if(binding.spinner11.getCount()> 0){
            String s= binding.spinner11.getSelectedItem().toString();
            ref1 = (int)binding.spinner11.getSelectedItemId();
        }
        //if(binding.spinner21.isSelected()){
        if(binding.spinner21.getCount()> 0){

            ref21 = (int)binding.spinner21.getSelectedItemId();
        }
        //if(binding.spinner22.isSelected()){
        if(binding.spinner22.getCount()> 0){

            ref22 = (int)binding.spinner22.getSelectedItemId();
        }
        ref22 = (int)binding.spinner22.getSelectedItemId();


        if(binding.radioButtonOsnowa.isChecked()){

            if ((offX == 0.0 && offY == 0.0) && !com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.isRepositoryEmpty()) {
                Toast.makeText(getBaseContext(), "both offs can be 0.0 for osnowa!", Toast.LENGTH_LONG).show();
                return;
            }

            if(binding.radioButtonOsnowaXy.isChecked()){

                if (currentMarker == null) {
                    Toast.makeText(getBaseContext(), "no marker!", Toast.LENGTH_LONG).show();
                    return;
                }

                // marker
                l.setLatitude(currentMarker.getPosition().latitude);
                l.setLongitude(currentMarker.getPosition().longitude);
                com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewOsnowaPointWithValidPosFromMarker(l, ref1, offX, offY);

            } else if(binding.radioButtonLampionOdl.isChecked()){

            } else if(binding.radioButtonOsnowaMarker.isChecked()){

//                binding.radioButtonOsnowaMarker.setEnabled(false);

            }

        } else if(binding.radioButtonLampion.isChecked()){
            if(binding.radioButtonLampionOdl.isChecked() && twoOdl && ref21 != -1 && ref22 != -1){
                boolean leftUpper = binding.checkBoxLeftUpper.isChecked();


                Double odl = CalculateDistancesKt.calibrateDistances(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.giveAllStoredLocationPositions());
                GeoLocation.Repozytorium.addNewOsnowaPointWithValidPosFromMarker(ref21, offX21, ref22, offX22, 1.0/odl, leftUpper);
                //GeoLocation.Repozytorium.addNewOsnowaPointWithValidPosFromMarker(0, 5.05, 1, 5.95, 1.0/odl);

            } else if(binding.radioButtonLampionXy.isChecked()){

                //com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.addNewLampionPoint(refPointNr, offX, offY);

            }// else if(binding.)
        }

        updateAfterChangeNrOfPoints();

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

        customMapView.onResume();
        super.onResume();

        checkPlayServicesAvailable();

        Wearable.getMessageClient(this).addListener(this);

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

                    //calculateAndShowDistances();
                    updateAfterChangeNrOfPoints();

                    Pair<Location,Location> m = getBoundingRect(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getLocationPositions());

                    moveMapTo(m);

                    //DrawMarkers(com.patloew.georeferencingsample.geoData.GeoLocation.Repozytorium.getPositionsUser());
                    MyService.sendToWearNumberOfPoints();


                } else if (FILE_CODE_WRITE == requestCode) {

                    String data2 = "bobo";

                    try {
                        if (!f.exists())
                            f.createNewFile();

                        serializeToFile(f);


                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
            return;
        }
        switch (requestCode) {

        }
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
