package com.example.mpistask08;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.library.BuildConfig;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_CODE = 1;
    private static final String PREFS_NAME = "RoutePreferences";
    private static final String ROUTE_KEY = "route_data";

    private FusedLocationProviderClient locationClient;
    private MapView mapView;
    private IMapController mapController;
    private List<GeoPoint> geoPoints = new ArrayList<>();
    private Polyline routeLine;
    private GeoPoint currentLocation;
    private Button authorButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);
        setContentView(R.layout.activity_main);

        initializeMapView();
        initializeLocationClient();
        requestLocationPermissions();
        loadSavedRoute();
        setupAuthorButton();
    }

    private void initializeMapView() {
        mapView = findViewById(R.id.map);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);
        mapView.setMultiTouchControls(true);

        mapController = mapView.getController();
        mapController.setZoom(15.0);
    }

    private void initializeLocationClient() {
        locationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void requestLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create()
                .setInterval(5000)
                .setFastestInterval(2000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        }
    }

    private final LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult result) {
            if (result.getLocations().isEmpty()) return;

            for (Location location : result.getLocations()) {
                updateCurrentLocation(new GeoPoint(location.getLatitude(), location.getLongitude()));
            }
            saveCurrentRoute();
        }
    };

    private void updateCurrentLocation(GeoPoint newLocation) {
        currentLocation = newLocation;
        geoPoints.add(currentLocation);
        mapController.setCenter(currentLocation);

        if (routeLine == null) {
            routeLine = new Polyline();
            routeLine.setWidth(15);
            routeLine.setColor(Color.RED);
            mapView.getOverlayManager().add(routeLine);
        }
        routeLine.setPoints(geoPoints);

        updateMapMarker(currentLocation);
        mapView.invalidate();
    }

    private void updateMapMarker(GeoPoint location) {
        mapView.getOverlays().removeIf(overlay -> overlay instanceof Marker);

        Marker marker = new Marker(mapView);
        marker.setPosition(location);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.map_pin));
        mapView.getOverlays().add(marker);
    }

    private void saveCurrentRoute() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        StringBuilder routeData = new StringBuilder();
        for (GeoPoint point : geoPoints) {
            routeData.append(point.getLatitude()).append(",").append(point.getLongitude()).append(";");
        }
        editor.putString(ROUTE_KEY, routeData.toString());
        editor.apply();
    }

    private void loadSavedRoute() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedRoute = prefs.getString(ROUTE_KEY, "");

        if (!savedRoute.isEmpty()) {
            for (String point : savedRoute.split(";")) {
                String[] latLng = point.split(",");
                if (latLng.length == 2) {
                    double lat = Double.parseDouble(latLng[0]);
                    double lng = Double.parseDouble(latLng[1]);
                    geoPoints.add(new GeoPoint(lat, lng));
                }
            }
            if (routeLine == null) {
                routeLine = new Polyline();
                routeLine.setWidth(15);
                routeLine.setColor(Color.RED);
                mapView.getOverlayManager().add(routeLine);
            }
            routeLine.setPoints(geoPoints);
            mapView.invalidate();
        }
    }

    private void setupAuthorButton() {
        authorButton = findViewById(R.id.authorBtn);
        authorButton.setOnClickListener(view -> showAuthorInfo());
    }

    private void showAuthorInfo() {
        showAlertDialog("Разработчик", getString(R.string.author));
    }

    private void showAlertDialog(String title, String message) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationClient.removeLocationUpdates(locationCallback);
    }
}

