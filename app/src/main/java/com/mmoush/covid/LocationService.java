package com.mmoush.covid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import static com.mmoush.covid.App.CHANNEL_ID;
import static com.mmoush.covid.App.CHANNEL_NAME;

public class LocationService extends Service {

    public static Handler myHandler = new Handler();
    private static final int TIME_TO_WAIT = 5000;
    Runnable myRunnable = new Runnable() {
        @Override
        public void run() {
            loop();
        }

    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    private void GetDeviceLocation() {
    }

    void loop() {
        // GetDeviceLocation();
        start();
    }

    public void start() {
        Toast.makeText(this, "Start Sending Location", Toast.LENGTH_LONG).show();
        myHandler.postDelayed(myRunnable, TIME_TO_WAIT);
    }

    public void stop() {
        myHandler.removeCallbacks(myRunnable);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String timeInterval = intent.getStringExtra("interval");

        Intent notificationIntent = new Intent(
          this,
          MainActivity.class
        );

        PendingIntent pendingIntent = PendingIntent.getActivity(this,   0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Intelligencia COVID")
                .setContentText("Location is being sent each " + timeInterval + "seconds")
                .setSmallIcon(R.drawable.ic_location)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            startMyOwnForeground();
        }
        else {
            startForeground(1, notification);
        }

        //do a heavy work on background thread

        return START_NOT_STICKY;
    }

    private void startMyOwnForeground(){

        NotificationChannel chan = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_location)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);

        start();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Toast.makeText(this, "Location will stop being sent", Toast.LENGTH_LONG).show();
        stop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
