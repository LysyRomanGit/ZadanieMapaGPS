package com.example.zadaniemapagps;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MyWeather extends AppCompatActivity {

    private TextView textLocation;
    private TextView textWeather;
    private TextView textTemperature;
    private final String API_KEY = "b8edec079bc5c48f146b146bb5b8c352";
    private double szerokosc;
    private double dlugosc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_weather);

        textLocation = findViewById(R.id.text_location);
        textWeather = findViewById(R.id.text_weather);
        textTemperature = findViewById(R.id.text_temperature);


        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        Intent intent = getIntent();
        szerokosc = intent.getDoubleExtra("szerokosc", 0.0);
        dlugosc = intent.getDoubleExtra("dlugosc", 0.0);

        fetchWeather();
    }

    private void fetchWeather() {
        new Thread(() -> {
            try {
                String urlString = "https://api.openweathermap.org/data/2.5/weather?lat=" + szerokosc +
                        "&lon=" + dlugosc + "&appid=" + API_KEY + "&units=metric&lang=pl";
                Log.d("2022", urlString);

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode != 200) {
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(connection.getErrorStream()));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) errorResponse.append(line);
                    errorReader.close();
                    runOnUiThread(() -> Toast.makeText(MyWeather.this, "Błąd: " + responseCode, Toast.LENGTH_LONG).show());
                    Log.d("WEATHER_ERROR", errorResponse.toString());
                    return;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder wynik = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    wynik.append(line);
                }

                JSONObject json = new JSONObject(wynik.toString());

                String miasto = json.getString("name");

                String pogoda = json.getJSONArray("weather")
                        .getJSONObject(0)
                        .getString("description");

                double temperatura = json.getJSONObject("main").getDouble("temp");

                String finalMiasto = miasto;
                runOnUiThread(() -> {
                    textLocation.setText("Miasto: " + finalMiasto);
                    textWeather.setText("Pogoda: " + pogoda);
                    textTemperature.setText("Temperatura: " + temperatura + "°C");
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MyWeather.this, "Błąd pobierania pogody", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
