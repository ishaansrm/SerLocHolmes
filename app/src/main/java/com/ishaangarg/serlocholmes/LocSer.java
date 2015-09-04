package com.ishaangarg.serlocholmes;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;

public class LocSer extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    final static String TAG = "LocSer";

    Location mCurrentLocation;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest;
    static Location lastLoc = new Location("PrevLoc");

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(30000); //30 seconds
        mLocationRequest.setFastestInterval(30000); //30 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "CONNECTED");
        mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);

        createLocationRequest();
        startLocationUpdates();
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    String mLastUpdateTime;
    static Boolean stale = true;

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Last LOC: " + lastLoc.getLatitude() + " | " + lastLoc.getLongitude());
        Log.d(TAG, "New LOC: " + location.getLatitude() + " | " + location.getLongitude());

        if (location.distanceTo(lastLoc) > 100 || stale) {
            if (stale)
                Log.d(TAG, "Forcibly polling location");
            mCurrentLocation = location;
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            updateUI();
            stale = false;
        } else {
            Log.d(TAG, "Not moved 100 meters");
            stale = true;
        }
        lastLoc = location;
    }

    private void updateUI() {

        Log.d(TAG, "Moved >100 meters");

        String locStr = mLastUpdateTime + ": " + mCurrentLocation.getLatitude() + ", " + mCurrentLocation.getLongitude();

        Intent intent = new Intent("DrWatson");
        intent.putExtra("loc", locStr);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        Log.d(TAG, "Time: " + mLastUpdateTime + " | Lat: "
                + String.valueOf(mCurrentLocation.getLatitude()) + " | Long: "
                + String.valueOf(mCurrentLocation.getLongitude()));
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");

        lastLoc.setLatitude(0);
        lastLoc.setLongitude(0);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("SerLock")
                .setContentText("SerLock has your back, I mean your Location")
                .setContentIntent(pendingIntent).build();

        startForeground(47, notification);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection Failed: " + connectionResult);
    }

    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();
    }
}
