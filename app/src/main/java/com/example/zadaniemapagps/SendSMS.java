package com.example.zadaniemapagps;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SendSMS extends AppCompatActivity {

    private static final int MY_PERMISSION_SEND_SMS = 5;
    private EditText phoneNumberEditText;
    private Button sendButton;
    private double aktualnaSzerokosc;
    private double aktualnaDlugosc;
    private String numerTelefonu = "";
    private String trescWiadomosci;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_send_sms);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.SEND_SMS},
                MY_PERMISSION_SEND_SMS
        );


        phoneNumberEditText = findViewById(R.id.editTextPhone);
        sendButton = findViewById(R.id.buttonSend);

        aktualnaSzerokosc = getIntent().getDoubleExtra("szerokosc", 0.0);
        aktualnaDlugosc = getIntent().getDoubleExtra("dlugosc", 0.0);

        sendButton.setOnClickListener(v -> sendSmsWithIntent());
    }

    private void sendSmsWithIntent() {
        numerTelefonu = phoneNumberEditText.getText().toString();
        trescWiadomosci = "Moja lokalizacja:\nSzerokość: "  + aktualnaSzerokosc + "\nDługość: " + aktualnaDlugosc;
        if (!numerTelefonu.equals("")) {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + numerTelefonu));
            intent.putExtra("sms_body", trescWiadomosci);
            startActivity(intent);
        }
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