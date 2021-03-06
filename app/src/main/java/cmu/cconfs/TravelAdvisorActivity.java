package cmu.cconfs;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import cmu.cconfs.model.parseModel.FutureWeather;
import cmu.cconfs.parseUtils.helper.DirectionsJSONParser;
import cmu.cconfs.service.WeatherHttpClient;

/**
 * @author jialingliu
 */
public class TravelAdvisorActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        View.OnClickListener {
    final Context context = this;
    static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;

    private GoogleMap mMap;

    private LocationManager locationManager;
    private FutureWeather futureWeather;

    GoogleApiClient mGoogleApiClient;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    LatLng currentLatLng;

    Map<Marker, String> markerWeatherIconMap;
    // text view for distance and duration
    TextView tvDistanceDuration;

    private boolean firstTime = true;
    // location for New Montgomery St, San Francisco, CA
    // todo change to your conference place when ready to deploy
    private final double lat = 37.788019;
    private final double lon = -122.401890;

    private Marker destinationMarker;
    ArrayList<LatLng> markerPoints;
    private boolean isCleared = true;
    private LatLng point;

    String city;

    Geocoder geocoder;

    private ImageView mImageView;

    /**
     * duration[0] = x days,
     * duration[1] = y hours,
     * duration[2] = z mins
     */
    private long durationInS = 0;

    /**
     * time pickers
     */
    Button btnDatePicker;
    EditText txtDate;
    private int mYear, mMonth, mDay, mHour, mMinute;
    private long departInS = System.currentTimeMillis() / 1000;
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    long forecastPeriod = 12 * 60 * 60;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_travel);
        geocoder = new Geocoder(this);

        markerWeatherIconMap = new HashMap<>();

        tvDistanceDuration = (TextView) findViewById(R.id.distance_duration);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        ProgressDialog progressDialog = new ProgressDialog(TravelAdvisorActivity.this);
        progressDialog.setMessage("loading Map");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);

        new Load(progressDialog).execute();
        // setup googlemaps
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        markerPoints = new ArrayList<>();

        city = "Mountain View, United States";

        // pickers
        btnDatePicker=(Button)findViewById(R.id.btn_date);
        txtDate=(EditText)findViewById(R.id.in_time);

        btnDatePicker.setOnClickListener(this);
        txtDate.bringToFront();
    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    public void onMapSearch(View view) {
        EditText locationSearch = (EditText) findViewById(R.id.editText);
        System.out.println(locationSearch);
        String location = locationSearch.getText().toString().trim();
        List<Address> addressList = null;

        if (location.isEmpty() || location.length() == 0 || location.equals("")) {
            return;
        }
        try {
            addressList = geocoder.getFromLocationName(location, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Address address;
        LatLng latLng = new LatLng(lat, lon); // default location: conference location
        if (addressList != null) {
            address = addressList.get(0);
            latLng = new LatLng(address.getLatitude(), address.getLongitude());
        }
        destinationMarker.remove();
        destinationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("destinationMarker"));
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        initMap();

        if (mMap != null) {
            //Initialize Google Play Services
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    buildGoogleApiClient();
                    mMap.setMyLocationEnabled(true);
                }
            } else {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }

            mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());

            // Enable MyLocation Button in the Map
            // Setting onclick event listener for the map
            mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

                @Override
                public void onMapClick(LatLng point1) {
                    point = point1;
                    if (!isCleared) {
                        try {
                            List<Address> addresses = geocoder.getFromLocation(point.latitude, point.longitude, 1);
                            String cityName = addresses.get(0).getLocality();
                            String countryName= addresses.get(0).getCountryName();
                            city = cityName + ", " + countryName;
                            LatLng origin = markerPoints.get(0);

                            // Getting URL to the Google Directions API
                            String url = getDirectionsUrl(origin, point);
                            DownloadTask2 downloadTask2 = new DownloadTask2();
                            downloadTask2.execute(url);
                        } catch (IOException e) {
                            System.out.println("Failed to get address for this point");
                        }
                        return;
                    }
                    destinationMarker.remove();

                    // Adding new item to the ArrayList
                    markerPoints.add(point);

                    // Creating MarkerOptions
                    MarkerOptions options = new MarkerOptions();

                    // Setting the position of the marker
                    options.position(point);
                    /**
                     * For the start location, the color of marker is ROSE and
                     * for the end location, the color of marker is GREEN.
                     */
                    if(markerPoints.size()==1){
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                    }else if(markerPoints.size()==2){
                        options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
                        drawPolyLine();
                    }
                    // Add new marker to the Google Map Android API V2
                    mMap.addMarker(options);
                }
            });
        }
    }

    private void drawPolyLine() {
        // Checks, whether start and end locations are captured
        if (markerPoints.size() < 2) {
            return;
        }
        LatLng origin = markerPoints.get(0);
        LatLng dest = markerPoints.get(1);

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(origin, dest);

        DownloadTask downloadTask = new DownloadTask();
        // Start downloading json data from Google Directions API
        downloadTask.execute(url);
        isCleared = false;
    }

    private void initMap() {
        mMap.getUiSettings().setZoomControlsEnabled(true);
        // Add a marker in Sydney and move the camera
        LatLng latLng = new LatLng(lat, lon);
        destinationMarker = mMap.addMarker(
                new MarkerOptions().position(latLng).title("New Montgomery St, San Francisco, CA"));
        destinationMarker.showInfoWindow();
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));

        // permission check to enable my location
        if (ActivityCompat.checkSelfPermission(
                this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    public void clear(View view) {
        // clear the map
        mMap.clear();
        markerPoints.clear();
        isCleared = true;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    class Load extends AsyncTask<String, String, String> {
        private ProgressDialog progressDialog;

        Load(ProgressDialog progressDialog) {
            this.progressDialog = progressDialog;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog.show();

        }

        @Override
        protected String doInBackground(String... params) {
            // test initialization
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            boolean enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // check if enabled and if not send user to the GSP settings
            // Better solution would be to display a dialog and suggesting to
            // go to the settings
            if (!enabled) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
            checkPlayServices();

            return null;

        }

        @Override
        protected void onPostExecute(String file_url) {
            progressDialog.dismiss();
        }
    }
    private boolean checkPlayServices() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                showErrorDialog(status);
            } else {
                Toast.makeText(this, "This device is not supported.", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    void showErrorDialog(int code) {
        GooglePlayServicesUtil.getErrorDialog(code, this, REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
    }

    private String getDirectionsUrl(LatLng origin,LatLng dest){

        // Origin of route
        String str_origin = "origin="+origin.latitude+","+origin.longitude;

        // Destination of route
        String str_dest = "destination="+dest.latitude+","+dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin+"&"+str_dest+"&"+sensor;

        // Output format
        String output = "json";

        // Building the url to the web service

        return String.format("https://maps.googleapis.com/maps/api/directions/%s?%s",output, parameters);
    }
    /** A method to download json data from url */
    private String downloadUrl(String strUrl) throws IOException{
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try{
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuilder sb = new StringBuilder();

            String line;
            while( ( line = br.readLine()) != null){
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        }catch(Exception e){
            Log.d("downloading url failed", e.toString());
        }finally{
            if (iStream != null) {
                iStream.close();
            }
            if (urlConnection != null) {
                urlConnection.disconnect();
            }

        }
        return data;
    }

    /**
     * http://apidev.accuweather.com/locations/v1/search?q=mountain%20view,United%20States&apikey=hoArfRosT1215
     * http://dataservice.accuweather.com/forecasts/v1/hourly/12hour/337169?apikey=2HbZMIUw4YGYi1GQ9km1jEuLpxtFdtrK&details=true&metric=true
     * http://developer.accuweather.com/
     */
    private class DownloadTask2 extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask2 parserTask2 = new ParserTask2();

            // Invokes the thread for parsing the JSON data
            parserTask2.execute(result);
        }
    }

    /** A class to parse the Google Places in JSON format to get duration **/
    private class ParserTask2 extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> >{

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            String rawDuration = "";

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);
                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    if (j != 1) {
                        continue;
                    }
                    HashMap<String,String> point = path.get(j);
                    // Get duration from the list
                    rawDuration = point.get("duration");
                }
            }
            System.out.println("rawDuration is: " + rawDuration);
            parseDuration(rawDuration);
            System.out.println("durationInS is: " + durationInS);
            JsonFutureWeatherTask jsonFutureWeatherTask = new JsonFutureWeatherTask();
            jsonFutureWeatherTask.execute(city, Long.toString(durationInS));
        }

        private void parseDuration(String rawDuration) {
            String[] myArr = rawDuration.split("\\s+");
            int[] myIntArr = new int[3];
            for (int i = 0; i < myArr.length; i++) {
                switch (myArr[i]) {
                    case "day":
                    case "days":
                        myIntArr[0] = Integer.parseInt(myArr[i - 1]);
                        break;
                    case "hour":
                    case "hours":
                        myIntArr[1] = Integer.parseInt(myArr[i - 1]);
                        break;
                    case "min":
                    case "mins":
                        myIntArr[2] = Integer.parseInt(myArr[i - 1]);
                        break;
                }
            }
            durationInS = (long)86400 * myIntArr[0] + (long)3600 * myIntArr[1] + (long)60 * myIntArr[2];
//            durationInS += System.currentTimeMillis() / 1000;
            durationInS += departInS;
        }
    }

    // Fetches data from url passed
    // for poly line drawing
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";
            try{
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            }catch(Exception e){
                Log.d("Background Task",e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);
        }
    }

    /** A class to parse the Google Places in JSON format */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String,String>>> > {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try{
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            }catch(Exception e){
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;
            String distance = "";
            String duration = "";

            if(result.size()<1){
                Toast.makeText(getBaseContext(), "No Points", Toast.LENGTH_SHORT).show();
                return;
            }

            // Traversing through all the routes
            for(int i=0;i<result.size();i++){
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for(int j=0;j<path.size();j++){
                    HashMap<String,String> point = path.get(j);
                    if(j==0){    // Get distance from the list
                        distance = point.get("distance");
                        continue;
                    }else if(j==1){ // Get duration from the list
                        duration = point.get("duration");
                        continue;
                    }

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(15);
                lineOptions.color(Color.GRAY);
            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions == null) {
                System.out.println("got null polyLineOptions");
                Toast.makeText(getApplicationContext(),
                        "Invalid starting point or destination", Toast.LENGTH_SHORT).show();
            } else {
                tvDistanceDuration.setText("Distance:"+distance + "\nDuration:"+duration);
                System.out.println("Got distance and duration successfully");
                System.out.println("Distance:"+distance + ", Duration:"+duration);
                tvDistanceDuration.bringToFront();
                tvDistanceDuration.setTypeface(null, Typeface.BOLD);
                mMap.addPolyline(lineOptions);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        //Place current location marker
        currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        if (firstTime) {
            if (markerPoints.size() == 2) {
                // update current latLng in list
                markerPoints.remove(0);
                markerPoints.add(0, currentLatLng);
            } else if (markerPoints.isEmpty()) {
                markerPoints.add(currentLatLng);
            }
        }
        firstTime = false;

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(currentLatLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {
                    // Permission denied, Disable the functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
            }
            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    // future weather task
    private class JsonFutureWeatherTask extends AsyncTask<String, Void, FutureWeather> {
        @Override
        protected FutureWeather doInBackground(String... params) {
            futureWeather = (new WeatherHttpClient()).getWeatherData(params[0], params[1]);
            return futureWeather;
        }
        @Override
        protected void onPostExecute(FutureWeather futureWeather1) {
            super.onPostExecute(futureWeather);
            // custom dialog
            final Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.custom);
            dialog.setTitle("Weather");
            Date date = new Date(durationInS * 1000L);
            format.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
            String formatted = format.format(date);
            formatted = "Estimated arrival time: " + formatted;
            // set the custom dialog components - text, image and button
            TextView text = (TextView) dialog.findViewById(R.id.text);
            text.setMovementMethod(LinkMovementMethod.getInstance());
            mImageView = (ImageView) dialog.findViewById(R.id.weatherimg);
            text.setText(formatted + futureWeather.display());
            Button dialogButton = (Button) dialog.findViewById(R.id.dialogButtonOK);
            // if button is clicked, close the custom dialog
            dialogButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) { dialog.dismiss();}
            });
            dialog.show();
            Marker currentWeatherInfoMaker = mMap.addMarker(new MarkerOptions().position(
                    new LatLng(point.latitude, point.longitude)));
            currentWeatherInfoMaker.setTitle(city);
            markerWeatherIconMap.put(currentWeatherInfoMaker, futureWeather.getWeatherIcon());
            setImgView(mImageView, currentWeatherInfoMaker);
            currentWeatherInfoMaker.showInfoWindow();
        }
    }
    private void setImgView(ImageView imageView, Marker marker) {
        if (!markerWeatherIconMap.containsKey(marker)) {
            marker.hideInfoWindow();
            return;
        }
        String icon = markerWeatherIconMap.get(marker);
        switch (icon) {
            case "1":
                imageView.setImageResource(R.drawable.one);
                return;
            case "2":
                imageView.setImageResource(R.drawable.two);
                return;
            case "3":
                imageView.setImageResource(R.drawable.three);
                return;
            case "4":
                imageView.setImageResource(R.drawable.four);
                return;
            case "5":
                imageView.setImageResource(R.drawable.five);
                return;
            case "6":
                imageView.setImageResource(R.drawable.six);
                return;
            case "7":
                imageView.setImageResource(R.drawable.seven);
                return;
            case "8":
                imageView.setImageResource(R.drawable.eight);
                return;
            case "11":
                imageView.setImageResource(R.drawable.eleven);
                return;
            case "12":
                imageView.setImageResource(R.drawable.twelve);
                return;
            case "13":
                imageView.setImageResource(R.drawable.thirteen);
                return;
            case "14":
                imageView.setImageResource(R.drawable.fourteen);
                return;
            case "15":
                imageView.setImageResource(R.drawable.fifteen);
                return;
            case "16":
                imageView.setImageResource(R.drawable.sixteen);
                return;
            case "17":
                imageView.setImageResource(R.drawable.seventeen);
                return;
            case "18":
                imageView.setImageResource(R.drawable.eighteen);
                return;
            case "19":
                imageView.setImageResource(R.drawable.nineteen);
                return;
            case "20":
                imageView.setImageResource(R.drawable.twenty);
                return;
            case "21":
                imageView.setImageResource(R.drawable.twenty_one);
                return;
            case "22":
                imageView.setImageResource(R.drawable.twenty_two);
                return;
            case "23":
                imageView.setImageResource(R.drawable.twenty_three);
                return;
            case "24":
                imageView.setImageResource(R.drawable.twenty_four);
                return;
            case "25":
                imageView.setImageResource(R.drawable.twenty_five);
                return;
            case "26":
                imageView.setImageResource(R.drawable.twenty_six);
                return;
            case "29":
                imageView.setImageResource(R.drawable.twenty_nine);
                return;
            case "30":
                imageView.setImageResource(R.drawable.thirty);
                return;
            case "31":
                imageView.setImageResource(R.drawable.thirty_one);
                return;
            case "32":
                imageView.setImageResource(R.drawable.thirty_two);
                return;
            case "33":
                imageView.setImageResource(R.drawable.thirty_three);
                return;
            case "34":
                imageView.setImageResource(R.drawable.thirty_four);
                return;
            case "35":
                imageView.setImageResource(R.drawable.thirty_five);
                return;
            case "36":
                imageView.setImageResource(R.drawable.thirty_six);
                return;
            case "37":
                imageView.setImageResource(R.drawable.thirty_seven);
                return;
            case "38":
                imageView.setImageResource(R.drawable.thirty_eight);
                return;
            case "39":
                imageView.setImageResource(R.drawable.thirty_nine);
                return;
            case "40":
                imageView.setImageResource(R.drawable.forty);
                return;
            case "41":
                imageView.setImageResource(R.drawable.forty_one);
                return;
            case "42":
                imageView.setImageResource(R.drawable.forty_two);
                return;
            case "43":
                imageView.setImageResource(R.drawable.forty_three);
                return;
            case "44":
                imageView.setImageResource(R.drawable.forty_four);
                return;
            default:
                imageView.setImageResource(R.drawable.one);
        }
    }

    class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {
        private View view;
        CustomInfoWindowAdapter() {
            view = getLayoutInflater().inflate(R.layout.custom_info_window, null);
        }
        @Override
        public View getInfoWindow(Marker marker) {
            ImageView imageView = ((ImageView) view.findViewById(R.id.badge));
            setImgView(imageView, marker);
            return view;
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        if (v == btnDatePicker) {
            // Get Current Date
            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    this, new DatePickerDialog.OnDateSetListener() {
                @Override
                public void onDateSet(DatePicker view, int year,
                                      int monthOfYear, int dayOfMonth) {
                    final String dateString = Integer.toString(year) + "-" +
                            ((monthOfYear + 1) < 10 ? "0" + (monthOfYear + 1) : (monthOfYear + 1)) +
                            "-" + (dayOfMonth < 10 ? "0" + dayOfMonth : dayOfMonth);
                    // Get Current Time
                    mHour = c.get(Calendar.HOUR_OF_DAY);
                    mMinute = c.get(Calendar.MINUTE);

                    // Launch Time Picker Dialog
                    TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                            new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    String departString = dateString + " " +
                                            (hourOfDay < 10 ? "0" + hourOfDay : hourOfDay) + ":" +
                                            (minute < 10 ? "0" + minute : minute);
                                    txtDate.setText(departString);
                                    departString += ":00";
                                    Date now = Calendar.getInstance().getTime();
                                    Date then = null;
                                    try {
                                        then = format.parse(departString);
                                    } catch (ParseException e) {
                                        e.printStackTrace();
                                    }
                                    assert then != null;
                                    System.out.println("then is:" + then.getTime());
                                    if ((then.getTime() - now.getTime()) / 1000 > forecastPeriod) {
                                        Toast.makeText(context,
                                                "Unable to forecast that long\n" +
                                                        "Please choose departure time again",
                                                Toast.LENGTH_LONG).show();
                                        txtDate.setText("Now");
                                    }
                                    if (then.getTime() - now.getTime() < 0) {
                                        Toast.makeText(context,
                                                "Cannot leave at past time",
                                                Toast.LENGTH_LONG).show();
                                        txtDate.setText("Now");
                                    }
                                    departInS = then.getTime() / 1000;
                                }
                            }, mHour, mMinute, false);
                    timePickerDialog.show();
                }
            }, mYear, mMonth, mDay);
            datePickerDialog.show();
        }
    }
}

/**
 * load image from url
 */
//class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
//
//    public LoadImageTask(Listener listener) {
//
//        mListener = listener;
//    }
//
//    public interface Listener{
//
//        void onImageLoaded(Bitmap bitmap);
//        void onError();
//    }
//
//    private Listener mListener;
//    @Override
//    protected Bitmap doInBackground(String... args) {
//
//        try {
//
//            return BitmapFactory.decodeStream((InputStream)new URL(args[0]).getContent());
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
//
//    @Override
//    protected void onPostExecute(Bitmap bitmap) {
//
//        if (bitmap != null) {
//
//            mListener.onImageLoaded(bitmap);
//
//        } else {
//            mListener.onError();
//        }
//    }
//}
