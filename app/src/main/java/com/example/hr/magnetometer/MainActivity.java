package com.example.hr.magnetometer;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.TimeZone;
import java.util.logging.Formatter;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;


import androidx.core.app.ActivityCompat;
import de.siegmar.fastcsv.writer.CsvWriter;

import static android.os.Environment.*;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor magnetic;

    private int counter = 1;

    private boolean recording = false;
    private boolean counterOn = false;

    private float magValues[] = new float[3];

    private Context context;

    private static final int REQUESTCODE_STORAGE_PERMISSION = 1;

    Collection<String[]> magneticData = new ArrayList<>();

    private CsvWriter csvWriter = null;

    public static DecimalFormat DECIMAL_FORMATTER;

    TextView stateText;
    EditText fileIDEdit;

    //Location
    TextView lat;
    TextView lon;
    //--o

    TextView magText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        findViewById(R.id.button).setOnClickListener(listenerStartButton);
        findViewById(R.id.button2).setOnClickListener(listenerStopButton);

        fileIDEdit = (EditText) findViewById(R.id.editText);
        magText = (TextView) findViewById(R.id.textView3);

        stateText = (TextView) findViewById(R.id.textView);
        stateText.setText("Stand by");

        context = this;

        // Sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMATTER = new DecimalFormat("#.000", symbols);


    }

    private View.OnClickListener listenerStartButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            recording = true;
            stateText.setText("Recording started");
            stateText.setTextColor(Color.parseColor("#FF0000"));
        }
    };

    private int REQUEST_CODE = 1;
    private View.OnClickListener listenerStopButton = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (recording == true) {
                recording = false;

                counter = 0;

                String value = fileIDEdit.getText().toString();

                stateText.setText("Recording Stopped");
                stateText.setTextColor(Color.parseColor("#0000FF"));

                if (storagePermitted((Activity) context)) {
                    csvWriter = new CsvWriter();
                    File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "magnetic" + value + ".csv");
                    //File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "magnetic" + value + ".csv");

                    try {
                        csvWriter.write(file, StandardCharsets.UTF_8, magneticData);
                        Toast.makeText(MainActivity.this, "File is recorded in memory.", Toast.LENGTH_LONG).show();
                    } catch (IOException io) {
                        Log.d("Error", io.getLocalizedMessage());
                    }
                }
            } else {
                Toast.makeText(MainActivity.this, "Nothing to save. Recording was not started.", Toast.LENGTH_LONG).show();
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        sensorManager.registerListener(this, magnetic, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long timeInMillisec = (new Date()).getTime() + (event.timestamp - System.nanoTime()) / 1000000L;

        if (recording) {
            float x = 0;
            float y = 0;
            float z = 0;
            double magnitude = 0;
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                // New Code:
                x = event.values[0];
                y = event.values[1];
                z = event.values[2];
                magnitude = Math.sqrt((x * x) + (y + y)
                        + (z * z));
                //magText.setText("Magnetometer:  " + timeInMillisec+"   X= " + roundThis(event.values[0]) + "   Y= " + roundThis(event.values[1]) + "   Z= " + roundThis(event.values[2]));
                magText.setText("Magnetometer:   X= " + x + "   Y= " + y + "   Z= " + z + "  Magnitude: " + DECIMAL_FORMATTER.format(magnitude) + "\u00b5Tesla");

                Log.d("Record", "Magnetometer" + String.valueOf(counter));
                magValues = event.values;
            }


            //magneticData.add(new String[]{String.valueOf(timeInMillisec), String.valueOf(magValues[0]), String.valueOf(magValues[1]), String.valueOf(magValues[2])});
            @SuppressLint("SimpleDateFormat") SimpleDateFormat logLineStamp = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss:SSS", Locale.getDefault());
            //logLineStamp.setTimeZone(TimeZone.getTimeZone("UTC"));

            magneticData.add(new String[]{logLineStamp.format(new Date(timeInMillisec)), String.valueOf(x), String.valueOf(y), String.valueOf(z), String.valueOf(magnitude)});
            counter++;
        }
    }

    // Checks if the the storage permissions are given or not by the user
    // It will request the use if not
    private static boolean storagePermitted(Activity activity) {
        // Check read write permission
        Boolean readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        Boolean writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (readPermission && writePermission) {
            return true;
        }
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUESTCODE_STORAGE_PERMISSION);
        //ActivityCompat.requestPermissions(activity, new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUESTCODE__LOCATION_PERMISSION);
        return false;
    }




    public static float roundThis(float value) {
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(4, RoundingMode.HALF_UP);
        return bd.floatValue();
    }
    // Added Later

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}