package com.mmoush.covid;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Queue;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

import static com.mmoush.covid.App.CHANNEL_ID;
import static com.mmoush.covid.App.CHANNEL_NAME;

public class LocationService extends Service {

    private FusedLocationProviderClient fusedLocationClient;
    private String android_id;
    public Queue<Coordinate> SentCoordinates;
    public Queue<Coordinate> NonSentCoordinates;
    private Coordinate previousCoordinate = null;

    //private static String url = "http://23.96.61.115:8000/log/";
    private static String url = "http://192.168.0.114:8000/log/";
    public static Handler myHandler = new Handler();
    AsyncHttpClient client = new AsyncHttpClient();
    private static int TIME_TO_WAIT = 10000;
    Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            loop();
        }

    };

    @Override
    public void onCreate() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        SentCoordinates = new LinkedList<>();
        NonSentCoordinates = new LinkedList<>();
        super.onCreate();
    }

    private void GetDeviceLocation() {
        int isPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if (isPermissionGranted != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location Permission Is Not Granted!", Toast.LENGTH_LONG).show();
        } else {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            if (location != null) {
                                Log.d("Location", location.toString());

                                //Get timestamp
                                Calendar cal = Calendar.getInstance();
                                int hour = cal.get(Calendar.HOUR);
                                int minutes = cal.get(Calendar.MINUTE);
                                int seconds = cal.get(Calendar.SECOND);
                                int year = cal.get(Calendar.YEAR);
                                int month = cal.get(Calendar.MONTH);
                                int day = cal.get(Calendar.DAY_OF_MONTH);
                                String timeStamp = year + "-" + (month + 1) + "-" + day + "T" + hour + ":" + minutes + ":" + seconds;

                                // Logic to handle location object
                                Coordinate newCoordinate = new Coordinate();
                                newCoordinate.latitude = location.getLatitude();
                                newCoordinate.longitude = location.getLongitude();
                                newCoordinate.timestamp = timeStamp;
                                newCoordinate.user_id = android_id;
                                Toast.makeText(LocationService.this, "Sending Lat: " + newCoordinate.latitude + " Long: " + newCoordinate.longitude, Toast.LENGTH_SHORT).show();
                                try {
                                    SendLocationToServer(newCoordinate);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                Toast.makeText(LocationService.this, "Please turn on your Location and try again!", Toast.LENGTH_LONG).show();
                            }
                        }
                    });
        }

    }

    private void ResendCoordinates() throws JSONException, UnsupportedEncodingException {
        while (!NonSentCoordinates.isEmpty()) {
            Coordinate notSentElement = NonSentCoordinates.poll();
            SendCoordinate(notSentElement);
        }
    }

    private void SendCoordinate(final Coordinate sentCoordinate) throws JSONException, UnsupportedEncodingException {
        JSONObject jsonParams = new JSONObject();
        jsonParams.put("lon", sentCoordinate.longitude);
        jsonParams.put("lat", sentCoordinate.latitude);
        jsonParams.put("prev_lon", sentCoordinate.previousLongitude);
        jsonParams.put("prev_lat", sentCoordinate.previousLatitude);
        jsonParams.put("timestamp", sentCoordinate.timestamp);
        jsonParams.put("user_id", sentCoordinate.user_id);
        StringEntity entity = new StringEntity(jsonParams.toString());
        client.post(this, url, entity, "application/json", new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(LocationService.this, "Location is sent successfully", Toast.LENGTH_SHORT).show();
                SentCoordinates.add(sentCoordinate);
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(LocationService.this, "Please make sure you're connected to the internet!", Toast.LENGTH_SHORT).show();
                NonSentCoordinates.add(sentCoordinate);
            }
        });
    }

    private void SendLocationToServer(final Coordinate coordinate) throws JSONException, UnsupportedEncodingException {
        coordinate.previousLongitude = coordinate.longitude;
        coordinate.previousLatitude = coordinate.latitude;
        if (previousCoordinate != null) {
            // Setting the previous coordinates to the same coordinates since there's no previous location
            coordinate.previousLongitude = previousCoordinate.longitude;
            coordinate.previousLatitude = previousCoordinate.latitude;
        }
        if (NonSentCoordinates != null && NonSentCoordinates.isEmpty()) {
            ResendCoordinates();
        }

        SendCoordinate(coordinate);
    }

    void loop() {
        GetDeviceLocation();
        // start();
    }

    public void start() {
        Toast.makeText(this, "Start Sending Location", Toast.LENGTH_SHORT).show();
        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
    }

    public void stop() {
        myHandler.removeCallbacks(myRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String timeInterval = intent.getStringExtra("interval");
        if(timeInterval != null){
            Integer interval = Integer.parseInt(timeInterval);
            TIME_TO_WAIT = interval * 1000;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startMyOwnForeground();
        } else {

            Intent notificationIntent = new Intent(
                    this,
                    MainActivity.class
            );

            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Intelligencia COVID")
                    .setContentText("Location is being sent each " + TIME_TO_WAIT / 1000 + "seconds")
                    .setSmallIcon(R.drawable.ic_location)
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(1, notification);
        }

        //do a heavy work on background thread

        return START_NOT_STICKY;
    }

    private void startMyOwnForeground() {


        NotificationChannel chan = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        Intent notificationIntent = new Intent(
                this,
                MainActivity.class
        );
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_location)
                .setContentTitle("Intelligencia COVID")
                .setContentText("Location is being sent each " + TIME_TO_WAIT / 1000 + " seconds")
                .setContentIntent(pendingIntent)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();

        startForeground(2, notification);
        start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Location will stop being sent", Toast.LENGTH_SHORT).show();
        stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
