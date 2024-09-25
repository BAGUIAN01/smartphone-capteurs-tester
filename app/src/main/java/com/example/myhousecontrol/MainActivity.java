package com.example.myhousecontrol;

import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;
import androidx.annotation.Nullable;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.content.Context;
import android.telephony.SmsManager;
import android.Manifest;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.os.BatteryManager;
import android.content.IntentFilter;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import android.location.Address;
import android.location.Geocoder;

import java.io.IOException;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import java.util.concurrent.Executor;

import android.speech.RecognizerIntent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;





public class MainActivity extends AppCompatActivity {

    ImageView scanQrView;
    ImageView biometricView;
    ImageView speechToMusic;
    ImageView flashTurnOn;
    TextView message_text;
    TextView lightTextView;
    TextView keyTemperature;
    TextView TemperatureValue;
    TextView weatherValue;
    TextView batteryValue;
    TextView locationValue;
    TextView houseTemperatureValue;
    TextView luminosityTextView;
    TextView proximityTextView;



    private CameraManager cameraManager;
    private String cameraId;
    private boolean flashOn = false;
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS = 1;
    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 2;
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    TextView batteryStatusTextView;
    BroadcastReceiver batteryReceiver; // Déclaration du BroadcastReceiver pour l'état de la batterie

    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String API_KEY = "5062e629aff930bbd6e34c1fb2cf028a";
    private TextView weatherTextView;

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;
    private static final int REQUEST_CODE_SPEECH_INPUT = 1000;


    private static final int REQUEST_CODE_PERMISSION_RECORD_AUDIO = 200;

    private static final int REQUEST_CODE_PERMISSION_INTERNET = 100;

    private SensorManager sensorManager;
    private Sensor lightSensor;
    private SensorEventListener lightEventListener;

    private Sensor proximitySensor;
    private SensorEventListener proximityEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        //5062e629aff930bbd6e34c1fb2cf028a
        message_text = findViewById(R.id.codeTextView);

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];  // Généralement la caméra arrière est à l'index 0
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        lightTextView = findViewById(R.id.lightTextView);
        flashTurnOn = findViewById(R.id.flashTurnOn);
        flashTurnOn.setOnClickListener(v -> {
            if (flashOn) {
                turnOffFlash();
                lightTextView.setText("Flash On");
            } else {
                turnOnFlash();
                lightTextView.setText("Flash Off");
            }
            flashOn = !flashOn;
        });

        scanQrView = findViewById(R.id.scanQrView);
        scanQrView.setOnClickListener(v ->
                initiateScan()
        );

        // Initialiser les éléments de l'interface utilisateur
        speechToMusic = findViewById(R.id.speechToMusic);
        speechToMusic.setOnClickListener(v -> startVoiceRecognition());

        // Récupérer le TextView pour l'état de la batterie
       // batteryStatusTextView = findViewById(R.id.batteryStatusTextView);

        //weatherTextView = findViewById(R.id.weatherTextView);

        //biometric access
        // Initialiser les composants d'authentification biométrique
        initializeBiometricAuthentication();
        biometricView = findViewById(R.id.biometricView);
        biometricView.setOnClickListener(v -> biometricPrompt.authenticate(promptInfo));

        keyTemperature = findViewById(R.id.keyTemperature);
        TemperatureValue = findViewById(R.id.TemperatureValue);
        weatherValue = findViewById(R.id.weatherValue);
        batteryValue = findViewById(R.id.batteryValue);
        locationValue = findViewById(R.id.locationValue);
        houseTemperatureValue = findViewById(R.id.houseTemperatureValue);
        luminosityTextView = findViewById(R.id.luminosityTextView);
        proximityTextView = findViewById(R.id.proximityTextView);

        // Initialiser les variables du capteur de luminosité
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (lightSensor == null) {
            Toast.makeText(this, "luminosity capteur not found !", Toast.LENGTH_SHORT).show();
            return;
        }

        lightEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float lux = event.values[0];
                luminosityTextView.setText(lux + " lux");
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Vous pouvez ignorer ceci
            }
        };

        // Enregistrer le listener pour le capteur de luminosité
        sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);


        // Initialiser les variables du capteur de proximité
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if (proximitySensor == null) {
            Toast.makeText(this, "proximity capteur not found!", Toast.LENGTH_SHORT).show();
        } else {
            proximityEventListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    float distance = event.values[0];
                    // Faites quelque chose avec la distance détectée, par exemple :
                    if (distance < proximitySensor.getMaximumRange()) {
                        // Obstacle détecté à proximité
                        proximityTextView.setText("Object found");
                    } else {
                        // Aucun obstacle à proximité
                        proximityTextView.setText("Object not found");
                    }
                }

                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    // Vous pouvez ignorer ceci
                }
            };
            sensorManager.registerListener(proximityEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // Demandez la permission si elle n'est pas accordée
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, MY_PERMISSIONS_REQUEST_SEND_SMS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, REQUEST_CODE_PERMISSION_INTERNET);
        }

        // Vérifier et demander la permission ACCESS_FINE_LOCATION si nécessaire
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            // Si la permission est déjà accordée, obtenir la localisation actuelle
            getCurrentLocationAndWeather();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION_RECORD_AUDIO);
        }

        // Register BroadcastReceiver pour recevoir les mises à jour de l'état de la batterie
        registerBatteryLevelReceiver();




        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void initiateScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setOrientationLocked(true);  // Optionnel : déverrouiller l'orientation
        integrator.setPrompt("Scan a QR code");
        integrator.setCameraId(0); // Utiliser la caméra arrière
        integrator.setBeepEnabled(true);
        integrator.initiateScan();
    }



    private void sendSMS(String message) {
        try {
            String phoneNumber = "+33745371282"; // Remplacez par le numéro de téléphone de destination

            // Ajouter la date au message
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
            String currentDateandTime = sdf.format(new Date());
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Obtenir les coordonnées de latitude et longitude
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            String cityName = getCityName(latitude, longitude);
                            String messageWithDateAndCity = "Hi! There is a new intrusion in your home at " + cityName + " on - " + currentDateandTime;

                            SmsManager smsManager = SmsManager.getDefault();
                            smsManager.sendTextMessage(phoneNumber, null, messageWithDateAndCity, null, null);


                        } else {
                            Toast.makeText(this, "Unable to determine location", Toast.LENGTH_LONG).show();
                            // Gérer le cas où la localisation n'est pas disponible
                        }
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_SEND_SMS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission accordée", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission refusée", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission de localisation accordée", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permission de localisation refusée", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_PERMISSION_INTERNET) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission INTERNET accordée, vous pouvez maintenant utiliser Retrofit
                Toast.makeText(this, "Permission INTERNET accordée", Toast.LENGTH_SHORT).show();
            } else {
                // Permission INTERNET refusée, gérer le cas où l'application ne peut pas accéder à Internet
                Toast.makeText(this, "Permission INTERNET refusée", Toast.LENGTH_SHORT).show();
            }
        }
        else if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée, obtenir la localisation actuelle et les données météo
                getCurrentLocationAndWeather();
            } else {
                // Permission refusée, afficher un message d'erreur ou une alternative
                Toast.makeText(this, "Permission de localisation refusée, utilisation de la localisation par défaut (Paris)", Toast.LENGTH_SHORT).show();
                // Vous pouvez appeler getCurrentWeather("Paris") ou une ville par défaut ici
            }

        }}

        //weather
        private void getCurrentLocationAndWeather() {
            // Utiliser un gestionnaire de localisation pour obtenir la dernière position connue
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            // Obtenir les coordonnées de latitude et longitude
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            //Toast.makeText(this, "" + longitude, Toast.LENGTH_SHORT).show();
                            // Utiliser les coordonnées pour appeler getCurrentWeather
                            getCurrentWeatherByLocation(latitude, longitude);

                        } else {
                            Toast.makeText(this, "Localisation non disponible", Toast.LENGTH_SHORT).show();
                            // Gérer le cas où la localisation n'est pas disponible
                        }
                    });
        }


    // Méthode pour allumer le flash
    private void turnOnFlash() {
        try {
            cameraManager.setTorchMode(cameraId, true);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "Impossible d'accéder à la lampe", Toast.LENGTH_SHORT).show();
        }
    }

    // Méthode pour éteindre le flash
    private void turnOffFlash() {
        try {
            cameraManager.setTorchMode(cameraId, false);
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "Impossible d'accéder à la lampe", Toast.LENGTH_SHORT).show();
        }
    }

    // Méthode pour enregistrer le BroadcastReceiver pour recevoir les mises à jour de l'état de la batterie
    private void registerBatteryLevelReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                float batteryPct = (level / (float) scale) * 100;
                String batteryPctString = String.valueOf(batteryPct) + " " + "%";

                // Mettre à jour le TextView de l'état de la batterie
                //batteryStatusTextView.setText("Battery Status: " + batteryPct + "%");
                batteryValue.setText(batteryPctString);

                // Obtenez la température de la batterie
                int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                if (temperature != -1) {
                    // La température est donnée en dixièmes de degré Celsius
                    float batteryTemperature = temperature / 10.0f;
                    String batteryTemperatureString = String.valueOf(batteryTemperature) + "" + "°C";
                    houseTemperatureValue.setText(batteryTemperatureString);
                }
            }
        };
        registerReceiver(batteryReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Désenregistrer le listener du capteur de luminosité
        if (sensorManager != null && lightEventListener != null) {
            sensorManager.unregisterListener(lightEventListener);
        }
        // Unregister the batteryReceiver when activity is destroyed
        unregisterReceiver(batteryReceiver);
    }

    private String getCityName(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                return addresses.get(0).getLocality();
            } else {
                Toast.makeText(this, "Impossible de trouver le nom de la ville", Toast.LENGTH_SHORT).show();
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }

    private void getCurrentWeatherByLocation(double latitude, double longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService apiService = retrofit.create(WeatherApiService.class);
        String location = getCityName(latitude,longitude);
        locationValue.setText(location);
        //Toast.makeText(this, ""+location, Toast.LENGTH_SHORT).show();

        Call<WeatherResponse> call = apiService.getCurrentWeather(location, "metric", API_KEY);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weatherResponse = response.body();
                    float temperature = weatherResponse.getMain().getTemp();
                    String weatherDescription = weatherResponse.getWeatherList().get(0).getDescription();

                    String weatherText = "Temperature: " + temperature + "°C, Weather: " + weatherDescription;
                    Toast.makeText(MainActivity.this, weatherText, Toast.LENGTH_SHORT).show();
                    String temperatureString = String.valueOf(temperature) +  " " + "°C";

                    TemperatureValue.setText(temperatureString);
                    weatherValue.setText(weatherDescription);
                    //weatherTextView.setText(weatherText);
                } else {
                    Toast.makeText(MainActivity.this, "Failed to get weather data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    //biometric
    private void initializeBiometricAuthentication() {
        BiometricManager biometricManager = BiometricManager.from(this);
        switch (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                // Le périphérique peut authentifier à l'aide d'une authentification biométrique
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                // Pas de capteur biométrique
                Toast.makeText(this, "No biometric hardware available", Toast.LENGTH_SHORT).show();
                return;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                // Capteur biométrique actuellement indisponible
                Toast.makeText(this, "Biometric hardware currently unavailable", Toast.LENGTH_SHORT).show();
                return;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                // L'utilisateur n'a pas de données biométriques enregistrées
                Toast.makeText(this, "No biometric data enrolled", Toast.LENGTH_SHORT).show();
                return;
        }

        Executor executor = ContextCompat.getMainExecutor(this);
        biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(getApplicationContext(), "Authentication error: " + errString, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(getApplicationContext(), "Authentication succeeded!", Toast.LENGTH_SHORT).show();
                sendSMS("message");
                // Logique de réussite de l'authentification
                // Vous pouvez ajouter du code ici pour exécuter une action après une authentification réussie
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(getApplicationContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
            }
        });

        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric key to my home")
                .setSubtitle("Use biometric credential")
                .setNegativeButtonText("Or Use account password")
                .build();
    }

    //voice recognition
    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");

        try {
            startActivityForResult(intent, REQUEST_CODE_SPEECH_INPUT);
        } catch (Exception e) {
            Toast.makeText(this, "Your device doesn't support speech input", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SPEECH_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (result != null && !result.isEmpty()) {
                    String recognizedText = result.get(0);
                    if (recognizedText.equalsIgnoreCase("Music")) {
                        //playMusic();
                        Toast.makeText(this, "music " , Toast.LENGTH_LONG).show();
                    }
                }
            }
        } else {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() == null) {
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show();
                } else {
                    String scannedContent = result.getContents();
                    Toast.makeText(this, "Scanned: " + scannedContent, Toast.LENGTH_LONG).show();
                    message_text.setText(scannedContent);
                    sendSMS(scannedContent);
                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    /*private void playMusic() {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.your_music_file);
        mediaPlayer.start();
    }*/


}