package com.example.zadaniemapagps;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static String TAG = "2022";
    private String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.SEND_SMS,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    private TextView dostawca;
    private TextView szerokosc;
    private TextView dlugosc;
    private TextView archivaldata;
    private LocationManager locationManager;
    private Criteria criteria;
    private Location location;
    private String bp;
    private int amount;
    private MapView osm;
    private MapController mapController;
    private SwipeRefreshLayout swipeRefreshLayout;
    private TextView textNetwork;
    private TextView textGps;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(findViewById(R.id.toolbar));

        swipeRefreshLayout = findViewById(R.id.main);
        textNetwork = findViewById(R.id.text_network);
        textGps = findViewById(R.id.text_gps);
        updateNetworkAndGpsStatus();
        swipeRefreshLayout.setOnRefreshListener(()-> {
            swipeRefreshLayout.setRefreshing(false);
            boolean connection=isNetworkAvailable(getApplicationContext());
            updateNetworkAndGpsStatus();
        });
        swipeRefreshLayout.setColorSchemeColors(Color.YELLOW);
        dostawca = findViewById(R.id.dostawca);
        szerokosc = findViewById(R.id.szerokosc);
        dlugosc = findViewById(R.id.dlugosc);
        archivaldata = findViewById(R.id.archival_data);

        osm = findViewById(R.id.osm);
        Context context = getApplicationContext();
        Configuration.getInstance().load(context, PreferenceManager.getDefaultSharedPreferences(context));
        osm.setTileSource(TileSourceFactory.MAPNIK);
        osm.setBuiltInZoomControls(true);
        osm.setMultiTouchControls(true);

        mapController = (MapController) osm.getController();
        mapController.setZoom(12);


        ActivityCompat.requestPermissions(
                this,
                permissions,
                PERMISSIONS_REQUEST_CODE
        );


        if (location != null) {
            updateLocationUI(location);
        } else {
            dostawca.setText("Najlepszy dostawca: " + bp);
            szerokosc.setText("Szerokość: nieznana");
            dlugosc.setText("Długość: nieznana");
            archivaldata.setText("Odczyty lokalizacji:\n\n");
        }


        osm.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                Log.i(TAG,"onScroll()");
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                Log.i(TAG,"onZoom()");
                return false;
            }
        });

        osm.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                swipeRefreshLayout.setEnabled(false);
            } else if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                swipeRefreshLayout.setEnabled(true);
            }
            return false;
        });

        ScrollView scrollView = findViewById(R.id.scrollview);

        scrollView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_MOVE) {
                swipeRefreshLayout.setEnabled(false);
            } else if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                swipeRefreshLayout.setEnabled(true);
            }
            return false;
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.wyslijSMS){
            if (location != null) {
                Intent intent = new Intent(this, SendSMS.class);
                intent.putExtra("szerokosc", location.getLatitude());
                intent.putExtra("dlugosc", location.getLongitude());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Brak dostępnej lokalizacji!", Toast.LENGTH_SHORT).show();
            }
        }else if (item.getItemId() == R.id.zapiszKoordynat){
            saveMapSnapshot();
            saveMapToGallery();
        }else if (item.getItemId() == R.id.udostepnijWyniki){
            if (archivaldata.getText().length() > 0) {
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Moje wyniki GPS");
                shareIntent.putExtra(Intent.EXTRA_TEXT, archivaldata.getText().toString());
                startActivity(Intent.createChooser(shareIntent, "Udostępnij wyniki za pomocą"));
            } else {
                Toast.makeText(this, "Brak wyników do udostępnienia!", Toast.LENGTH_SHORT).show();
            }
        }else if (item.getItemId() == R.id.weather){
            if (location != null) {
                Intent intent = new Intent(MainActivity.this, MyWeather.class);
                intent.putExtra("szerokosc", location.getLatitude());
                intent.putExtra("dlugosc", location.getLongitude());
                startActivity(intent);
            } else {
                Toast.makeText(this, "Brak dostępnej lokalizacji!", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network network = cm.getActiveNetwork();
                if (network == null) return false;

                NetworkCapabilities capabilities = cm.getNetworkCapabilities(network);
                return capabilities != null &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
            } else {
                NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }
    public boolean isGpsAvailable(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                criteria = new Criteria();
                locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
                bp = locationManager.getBestProvider(criteria,true);
                Log.d(TAG, "Najlepszy dostawca: " + bp);

                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }

                location = locationManager.getLastKnownLocation(bp);
                updateLocationUI(location);

                locationManager.requestLocationUpdates(bp, 1000, 1, this);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, this);
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 1, this);
            } else {
                Toast.makeText(this, "Brak uprawnień GPS", Toast.LENGTH_LONG).show();
            }
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onLocationChanged(@NonNull Location location) {
        this.location = location;
        updateLocationUI(location);
    }

    private void updateLocationUI(Location location) {
        dostawca.setText("Najlepszy dostawca: " + bp);
        if (location != null) {
            szerokosc.setText("Szerokość: " + location.getLatitude());
            dlugosc.setText("Długość: " + location.getLongitude());
            archivaldata.append(location.getLatitude() + " : " + location.getLongitude() + "\n");

            GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
            mapController.animateTo(geoPoint);
            addMarkerToMap(geoPoint);
        } else {
            szerokosc.setText("Szerokość: nieznana");
            dlugosc.setText("Długość: nieznana");
        }
    }
    @Override
    public void onFlushComplete(int requestCode) {
        LocationListener.super.onFlushComplete(requestCode);
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
        LocationListener.super.onProviderEnabled(provider);
        //Log.d(TAG, "onLProvider: " + location.getLatitude() + ", " + location.getLongitude());
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
        LocationListener.super.onProviderDisabled(provider);
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
    public void addMarkerToMap(GeoPoint center){
        Marker marker = new Marker(osm);
        marker.setPosition(center);
        marker.setAnchor(Marker.ANCHOR_CENTER,Marker.ANCHOR_BOTTOM);
        marker.setIcon(getResources().getDrawable(R.drawable.marker));
        osm.getOverlays().clear();
        osm.getOverlays().add(marker);
        osm.invalidate();
        marker.setTitle("Moja pozycja");
    }

    private void updateNetworkAndGpsStatus() {
        boolean connection = isNetworkAvailable(getApplicationContext());
        if (connection) {
            textNetwork.setText("Internet ON");
            textNetwork.setTextColor(Color.GREEN);
        } else {
            textNetwork.setText("Internet OFF");
            textNetwork.setTextColor(Color.RED);
        }

        boolean connectiongps = isGpsAvailable(getApplicationContext());
        if (connectiongps) {
            textGps.setText("Gps ON");
            textGps.setTextColor(Color.GREEN);
        } else {
            textGps.setText("Gps OFF");
            textGps.setTextColor(Color.RED);
        }
    }


    public void saveMapToGallery() {
        osm.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(osm.getDrawingCache());
        osm.setDrawingCacheEnabled(false);

        String filename = "map_" + System.currentTimeMillis() + ".png";

            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Zdjęcia Mapy");

            Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                    Toast.makeText(this, "Mapa zapisana w galerii!", Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Błąd zapisu mapy!", Toast.LENGTH_SHORT).show();
                }
            }
    }
    public void saveMapSnapshot() {
        osm.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(osm.getDrawingCache());
        osm.setDrawingCacheEnabled(false);

        File path = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "ZdjeciaMapy");
        if (!path.exists()) {
            path.mkdirs();
        }

        String filename = "map_" + System.currentTimeMillis() + ".png";
        File file = new File(path, filename);

        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            Log.d("MAP", "Mapa zapisana w " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Błąd zapisu mapy!", Toast.LENGTH_SHORT).show();
        }
    }
}