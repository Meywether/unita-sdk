/*
 * Copyright (c) 2019. Florian Taurer.
 *
 * This file is part of Unita SDK.
 *
 * Unita is free a SDK: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Unita is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Unita.  If not, see <http://www.gnu.org/licenses/>.
 */

package at.floriantaurer.unitabeaconmodule.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import at.floriantaurer.unitabeaconmodule.SocketController;

public class LocationController extends Service implements LocationListener {

    private static LocationController instance;
    private Context currentContext;


    //private boolean isGPSEnabled = false;
    private boolean isNetworkEnabled = false;
    private LocationManager locationManager;
    Location location;
    double latitude;
    double longitude;
    private static final float MIN_DISTANCE_CHANGE_FOR_UPDATES = 0.0f; // 15m
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 10; // 10min
    private String provider_info_network = "";

    private LocationController(){
    }

    public static LocationController getInstance() {
        if(instance == null) {
            instance = new LocationController();
        }
        return instance;
    }

    public void setCurrentContext(Context context){
        this.currentContext = context;
    }

    private JSONObject requestLocation(String socketId){
        requestGPSUpdates();
        JSONObject locationUpdateJSON = new JSONObject();
        JSONObject locationDataJSON = new JSONObject();
        try {
            locationDataJSON.put("longitude", getLongitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            locationDataJSON.put("latitude", getLatitude());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            locationUpdateJSON.put("location", locationDataJSON);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            locationUpdateJSON.put("socketId", socketId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            locationUpdateJSON.put("beaconId", LoginUtils.getLoggedInBeacon(currentContext).getId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("LocationController", String.valueOf(LoginUtils.getLoggedInBeacon(currentContext).getId()));
        return locationUpdateJSON;
    }

    public void requestLocationCheckForServer(String socketId){
        SocketController socket = SocketController.getInstance();
        socket.sendLocationCheckUpdateToServer(requestLocation(socketId));
    }


    public void requestLocationForServer(String socketId){
        SocketController socket = SocketController.getInstance();
        socket.sendLocationUpdateToServer(requestLocation(socketId));
    }

    @SuppressLint("MissingPermission")
    public void initLocationTracker() {
        try {
            locationManager = (LocationManager) currentContext.getSystemService(LOCATION_SERVICE);

            Location locationNetwork = null;


            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (isNetworkEnabled) {
                this.isNetworkEnabled = true;
                provider_info_network = LocationManager.NETWORK_PROVIDER;
            }

            if (!provider_info_network.isEmpty()) {
                locationManager.requestLocationUpdates(
                        provider_info_network,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        this
                );
                if (locationManager != null) {
                    locationNetwork = locationManager.getLastKnownLocation(provider_info_network);
                }

                location = locationNetwork;
                updateGPSCoordinates();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            Log.e("LocationController", "Impossible to connect to LocationManager", e);
        }
    }

    public void updateGPSCoordinates() {
        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }
    }

    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }
        return latitude;
    }

    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }
        return longitude;
    }

    @SuppressLint("MissingPermission")
    public void requestGPSUpdates() {
        Location locationNetwork = null;
        if (!provider_info_network.isEmpty()) {
            locationManager.requestLocationUpdates(
                    provider_info_network,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES,
                    this
            );
            if (locationManager != null) {
                locationNetwork = locationManager.getLastKnownLocation(provider_info_network);
            }

            location = locationNetwork;
            updateGPSCoordinates();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if(isBetterLocation(location,this.location)){
            this.location = location;
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > MIN_TIME_BW_UPDATES;
        boolean isSignificantlyOlder = timeDelta < -MIN_TIME_BW_UPDATES;
        boolean isNewer = timeDelta > 0;

        if (isSignificantlyNewer) {
            return true;
        } else if (isSignificantlyOlder) {
            return false;
        }

        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate) {
            return true;
        }
        return false;
    }
}
