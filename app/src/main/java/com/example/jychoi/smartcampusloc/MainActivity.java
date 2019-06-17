package com.example.jychoi.smartcampusloc;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
//import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
//import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
//import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelUuid;
//import android.os.Parcelable;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
//import java.util.UUID;
//import java.net.URL;
//import java.util.Arrays;


public class MainActivity extends AppCompatActivity {

    //msjang test variable

    File for_HTTP_send = null;


    /**
     * Log TAG
     */
    private static final String TAG1 = "JYCHOI";
    private static final String TAG2 = "BLE";

    /**
     * BLE Module variables & configuration
     */
    class infoBle {
        String mac = null;
        int rssi = 0;
    }

    private String[] addr = {
            "fa:c2:e1:27:25:6d",
            "e3:85:19:54:52:9f",
            "fd:54:78:6c:9e:a6",
            "fc:35:a5:60:19:cd",
            "c9:83:5f:68:4d:60",
            "d4:f2:e3:63:97:bf",
            "dc:fc:1c:d8:9b:4c",
            "cc:09:eb:e6:f3:06",
            "e5:41:92:60:1f:9f",
            "fa:2c:8b:f1:8c:38",
            "fd:38:65:74:45:c2"};


    ArrayList<infoBle> bleList;
    //    private int[] stan = new int[3];
//    private int[] prev = new int[3];
    boolean bleFlag = true;
    static final int REQUEST_CODE_ENABLE_BLE = 1001;
    private static final ParcelUuid EDDYSTONE_SERVICE_UUID =
            ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
    private static final byte EDDYSTONE_URL_FRAME_TYPE = 0x10; // char: A / DEC: 16 / BIN: 0001 0000
    //    private static final byte DATA_PREAMBLE = (byte) 0xAA; // DEC: 170 / BIN: 1010 1010
    private static final byte URL_DATA = (byte) 0x61; // char: a / DEC: 97 / BIN: 0110 0001
    private static final ScanFilter EDDYSTONE_SCAN_FILTER = new ScanFilter.Builder()
            .setServiceUuid(EDDYSTONE_SERVICE_UUID)
            .build();
    private final List<ScanFilter> SCAN_FILTERS = buildScanFilters();

    private static List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<>();
        scanFilters.add(EDDYSTONE_SCAN_FILTER);
        return scanFilters;
    }

    // SCAN SETTINGS
    private ScanSettings SCAN_SETTINGS =
            new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .setReportDelay(0)
                    .build();
    // SCAN MODE: SCAN_MODE_LOW_LATENCY, SCAN_MODE_lOW_POWER, SCAN_MODE_BALANCED

    /**
     * Random File variables
     */
    private int rafNumber = 11;
    private RandomAccessFile[] raf = new RandomAccessFile[rafNumber];

    /**
     * TextView (User interface)
     */
    private TextView[] textView = new TextView[11];


    /**
     * About accelerometer, gyroscope, magnetic field
     */
    private boolean rotationFlag = false;
    private static final double RAD2DGR = 180 / Math.PI;
    private static final float MS2S = 1.0f / 1000.0f;
    private SensorManager sensorManager;
    private Sensor accSensor, gyroSensor, geoSensor;
    private SensorEventListener accLis, gyroLis, geoLis;
    //private boolean stepDetectionFlag = true;
    private float[] aggRotation = new float[3];
    private float[] filteredRotation = new float[3];
    private float[] correctedRotation = new float[3];
    private float[] correctedByDoor = new float[3];
    //private float sumGeo;
    //private float rmsGeo;

    /**
     * Coordination
     */
    private double xCoordinate = 0; // initial point (0,0)
    private double yCoordinate = 0;
    private double x1 = 0;
    private double y1 = 0;
    private double x2 = 0; // corrected when passing door
    private double y2 = 0;
    private float maxGravity = 0;

    /**
     * Localization
     */
    private int mainAxis; // 0 1 2 --> x y z
    private int onHand;
    private int stepCount = 0;
    private double totalLength = 0;
    private double sysTime;
    private static final double aStep = 0.222;
    private static final double bStep = 0.4; // original 0.4

    /**
     * State Update variables
     */

    private int detState = -1;
//    private int stateCount = 0;
//    private int prevState = -1;
//    private int stateChange = 0;
//    private boolean stateFlag = true;


//    /**
//     * About WiFi
//     */
//    private WifiManager wifiManager;
//    private WifiInfo info;
//    private int rssi;
//    private String ssid;
//    private String bssid;

    private void setBleList(ArrayList bleList) {
        for (String anAddr : addr) {
            infoBle ble = new infoBle();
            ble.mac = anAddr;
            ble.rssi = -105;
            bleList.add(ble);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sysTime = System.currentTimeMillis();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            gyroSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            geoSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }

        accLis = new accelerometerListener();
        gyroLis = new gyroscopeListener();
        //geoLis = new geomagneticListener();

        bleList = new ArrayList<>();
        setBleList(bleList);

//        registerReceiver(rssiReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));

        setContentView(R.layout.main);
        //scrollView = findViewById(R.id.scrollView);
        textView[0] = findViewById(R.id.textTime);
        textView[1] = findViewById(R.id.textStepCount);
        textView[2] = findViewById(R.id.textHeading);
        textView[3] = findViewById(R.id.textLength);
        textView[4] = findViewById(R.id.textPosition);
        textView[5] = findViewById(R.id.textPose);
        textView[6] = findViewById(R.id.textAxis);
        textView[7] = findViewById(R.id.textRoomState);
        textView[8] = findViewById(R.id.textFileState);
        textView[9] = findViewById(R.id.text_HTTP_response);
        textView[10] = findViewById(R.id.text_RTT);

        //Button configuration
        final Button save_button = findViewById(R.id.save_button);
        save_button.setOnClickListener(onClickListener);
        final Button endSave_button = findViewById(R.id.end_button);
        endSave_button.setOnClickListener(onClickListener);
        final Button reset_button = findViewById(R.id.reset_button);
        reset_button.setOnClickListener(onClickListener);
        final Button door_button = findViewById(R.id.door_button);
        door_button.setOnClickListener(onClickListener);

        String[] permissions = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.CHANGE_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.LOCATION_HARDWARE,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_SETTINGS
        };
        ActivityCompat.requestPermissions(this, permissions, 1);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                startActivity(intent);
            }
        }

        BluetoothManager btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        assert btManager != null;
        BluetoothAdapter btAdapter = btManager.getAdapter();
        if (btAdapter == null || !btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_CODE_ENABLE_BLE);
        } else {
            BluetoothLeScanner scanner = btAdapter.getBluetoothLeScanner();
            scanner.startScan(SCAN_FILTERS, SCAN_SETTINGS, scanCallback);
        }
    } // OnCreate

//    private BroadcastReceiver rssiReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (wifiManager != null) {
//                info = wifiManager.getConnectionInfo();
//                rssi = info.getRssi();
//                ssid = info.getSSID();
//                bssid = info.getBSSID();
//            }
//        }
//    };

    private Button.OnClickListener onClickListener;

    {
        onClickListener = new Button.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                switch (view.getId()) {
                    case R.id.save_button:
                        Log.i(TAG1, "Call save");
                        textView[8].setText(" File State: Save");
                        resetParameter();
                        saveLog();
                        break;
                    case R.id.end_button:
                        Log.i(TAG1, "Call end");
                        textView[8].setText(" File State: End");

                        //msjang for save

                        try {
                            endSave();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //// msjang end

                        resetParameter();
                        break;
                    case R.id.reset_button:
                        textView[8].setText(" File State: Reset");
                        resetParameter();
                        resetTextView();
                        break;
                    case R.id.door_button:
                        String log = String.valueOf((System.currentTimeMillis() - sysTime) / 1000);
                        writeLog(raf[9], log);
                        break;
                }
            }
        };
    }

    private void resetParameter() {
        sysTime = System.currentTimeMillis();
        xCoordinate = 0;
        yCoordinate = 0;
        x1 = 0;
        y1 = 0;
        x2 = 0;
        y2 = 0;
        stepCount = 0;
        totalLength = 0;
        //sumGeo = 0;
        //rmsGeo = 0;
        maxGravity = 0;
        onHand = 0; // false = 0, true = 1
        for (int i = 0; i < 3; i++) {
            aggRotation[i] = 90;
            filteredRotation[i] = 90;
            correctedRotation[i] = 90;
            correctedByDoor[i] = 90;
        }
        bleList.clear();
        setBleList(bleList);
        bleFlag = true;
        // Room state variables
        detState = -1;
//        stateCount = 0;
//        prevState = -1;
//        stateChange = 0;
//        stateFlag = true;
    }

    @SuppressLint({"SetTextI18n", "DefaultLocale"})
    private void resetTextView() {
        textView[0].setText(" Time: 0");
        textView[1].setText(" Step count: " + String.valueOf(stepCount));
        textView[2].setText(" Heading Direction: " + String.format("%.0f", filteredRotation[mainAxis]));
        textView[3].setText(" Total Length: " + String.format("%.1f", totalLength));
        textView[4].setText(" Position: (" + String.format("%.1f", x1) + ", " + String.format("%.1f", y1) + ")"
                + " (" + String.format("%.1f", x2) + ", " + String.format("%.1f", y2) + ")");
//        textView[4].setText(" Position: (" + String.format("%.1f", x1) + ", " + String.format("%.1f", y1) + ")");
        textView[7].setText(" Room State " + detState);
        switch (onHand) {
            case 0:
                textView[5].setText(" Pose: Pocket");
                break;
            case 1:
                textView[5].setText(" Pose: Hand");
                break;
        }
        switch (mainAxis) {
            case 0:
                textView[6].setText(" Main Axis: X");
                break;
            case 1:
                textView[6].setText(" Main Axis: Y");
                break;
            case 2:
                textView[6].setText(" Main Axis: Z");
                break;
        }
    }

    public void saveLog() {
        for (int i = 0; i < rafNumber; i++) {
            raf[i] = null; //initialize
        }

        String[] filename = new String[rafNumber];
        File[] directory = new File[rafNumber];
        String date = new SimpleDateFormat("yyMMddhhmmss", Locale.KOREA).format(new Date());
        File file = new File(Environment.getExternalStorageDirectory().getPath() + "/pathwaymapping/" + date);
        boolean mkdirsucc = file.mkdirs();

        filename[0] = "/raw_data.csv";
        filename[1] = "/peak_detection.csv";
        filename[2] = "/rotation.csv";
        filename[3] = "/magnetic.csv";
        filename[4] = "/gyro_x.csv";
        filename[5] = "/gyro_y.csv";
        filename[6] = "/gyro_z.csv";
        filename[7] = "/ble_rssi.csv";
        filename[8] = "/diss.csv";
        filename[9] = "/door.csv";
        filename[10] = "/ble_mac.csv";

        if (mkdirsucc) {
            try {

                for (int i = 0; i < rafNumber; i++) {
                    directory[i] = new File(file.getCanonicalPath() + filename[i]);

                    //msjang test variable
                    for_HTTP_send = directory[4];

                    raf[i] = new RandomAccessFile(directory[i], "rw");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeLog(RandomAccessFile file, String log) {
        if (file != null) {
            for (int i = 0; i < rafNumber; i++) {
                if (file == raf[i]) {
                    try {
                        raf[i].writeBytes(log + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void endSave() throws IOException {

        // msjang HTTP file POST
        String url = "http://sim5.mwnl.snu.ac.kr/upload6.php";
        ContentValues values = new ContentValues();
        values.put("TEST_KEY" , "TEST_VALUE");

        NetworkTask networkTask = new NetworkTask(url, values);
        networkTask.execute();

        for (int i = 0; i < rafNumber; i++) {
            raf[i] = null;
        }
    }

    public float lowPassFilter(float weight, float x, float y) {
        return weight * x + (1 - weight) * y;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // SensorDelay = FASTEST
        if (accSensor != null)
            sensorManager.registerListener(accLis, accSensor, SensorManager.SENSOR_DELAY_FASTEST);
        if (gyroSensor != null)
            sensorManager.registerListener(gyroLis, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST);
        if (geoSensor != null)
            sensorManager.registerListener(geoLis, geoSensor, SensorManager.SENSOR_DELAY_FASTEST);
        //wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sensorManager != null) {
            sensorManager.unregisterListener(accLis);
            sensorManager.unregisterListener(gyroLis);
            sensorManager.unregisterListener(geoLis);
        }
    }

    /**
     * Accelerometer Sensor
     **/

    private class accelerometerListener implements SensorEventListener {

        private double lastPeakTime, stepLength, preLogTime, preLogTime2;
        private float slope, prevAcc;
        float[] gravity = new float[3];
        float[] linear_acceleration = new float[3];
        float[] filteredAcc = new float[3];
        float curSlope, sqrtAcc;

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @SuppressLint({"DefaultLocale", "SetTextI18n"})
        @Override
        public void onSensorChanged(SensorEvent event) {
            int refStepFreq = 500;
            int maxStepFreq = 2000;
            double accMin = 0.5, accMax = 3;
            double logInterval = 30; //단위 ms
            double logInterval2 = 300; //단위 ms
            final float alpha = (float) 0.2;
            double curTime = System.currentTimeMillis();
            double stepInterval = preLogTime - lastPeakTime;

            // Isolate the force of gravity with the low-pass filter.
            for (int i = 0; i < 3; i++) {
                gravity[i] = lowPassFilter(alpha, event.values[i], gravity[i]);
            }

            // Remove the gravity contribution with the high-pass filter.
            for (int i = 0; i < 3; i++) {
                linear_acceleration[i] = event.values[i] - gravity[i];
            }

            // Low-pass filter
            for (int i = 0; i < 3; i++) {
                filteredAcc[i] = lowPassFilter(alpha, linear_acceleration[i], filteredAcc[i]);
            }

            // Find max gravity axis
            for (int i = 0; i < 3; i++) {
                if (maxGravity < Math.abs(gravity[i])) {
                    maxGravity = Math.abs(gravity[i]);
                    mainAxis = i;
                }
            }

            sqrtAcc = (float) Math.sqrt((filteredAcc[0] * filteredAcc[0] + filteredAcc[1] * filteredAcc[1] + filteredAcc[2] * filteredAcc[2]));

            if (curTime - preLogTime > logInterval) {
                curSlope = sqrtAcc - prevAcc;
                if ((slope > 0) && (curSlope < 0)) {
                    if ((prevAcc > accMin) && (prevAcc < accMax)) {
                        if (stepInterval > refStepFreq && !rotationFlag) {
                            //stepDetectionFlag = true;
                            stepCount = stepCount + 1;
                            stepLength = aStep * (1000 / stepInterval) + bStep;
                            stepLength = 0.7; // fixed value
                            totalLength = totalLength + stepLength;
                            lastPeakTime = preLogTime;

                            //xCoordinate = (xCoordinate + stepLength * Math.cos(aggRotation[2]/RAD2DGR + Math.PI / 2));
                            //yCoordinate = (yCoordinate + stepLength * Math.sin(aggRotation[2]/RAD2DGR + Math.PI / 2));

                            xCoordinate = (xCoordinate + stepLength * Math.cos(aggRotation[mainAxis] * maxGravity / Math.abs(maxGravity) / RAD2DGR)); // 처음 직진하는 축을 x 축이라고 생각
                            yCoordinate = (yCoordinate + stepLength * Math.sin(aggRotation[mainAxis] * maxGravity / Math.abs(maxGravity) / RAD2DGR));

                            x1 = (x1 + stepLength * Math.cos(correctedRotation[mainAxis] * maxGravity / Math.abs(maxGravity) / RAD2DGR)); // 처음 직진하는 축을 x 축이라고 생각
                            y1 = (y1 + stepLength * Math.sin(correctedRotation[mainAxis] * maxGravity / Math.abs(maxGravity) / RAD2DGR));

                            x2 = (x2 + stepLength * Math.cos(correctedByDoor[mainAxis] * maxGravity / Math.abs(maxGravity) / RAD2DGR)); // 문 지나가는 순간으로 보정한 결과
                            y2 = (y2 + stepLength * Math.sin(correctedByDoor[mainAxis] * maxGravity / Math.abs(maxGravity) / RAD2DGR));

                            String log = String.valueOf((preLogTime - sysTime) / 1000) //로그 찍는 타임 싱크를 맞춰야함
                                    .concat(",step," + String.valueOf(stepCount))
                                    .concat(",acc," + String.format("%.2f", prevAcc))
                                    .concat(",len," + String.format("%.2f", totalLength))
                                    .concat(",x1," + String.format("%.2f", x1))
                                    .concat(",y1," + String.format("%.2f", y1))
                                    .concat(",x2," + String.format("%.2f", x2))
                                    .concat(",y2," + String.format("%.2f", y2))
                                    .concat(",axis," + mainAxis)
                                    .concat(",onHand," + onHand)
                                    .concat(",degree," + correctedRotation[mainAxis]);
                            Log.i(TAG1, log);
                            writeLog(raf[1], log);
                            textView[0].setText(" Time: " + Long.toString((long) (preLogTime - sysTime) / 1000));
                            textView[1].setText(" Step count: " + String.valueOf(stepCount));
                            textView[2].setText(" Heading Direction: " + String.format("%.0f", filteredRotation[mainAxis]));
                            textView[3].setText(" Total Length: " + String.format("%.1f", totalLength));
                            textView[4].setText(" Position: (" + String.format("%.1f", x1) + ", " + String.format("%.1f", y1) + ")"
                                    + " (" + String.format("%.1f", x2) + ", " + String.format("%.1f", y2) + ")");
//                            textView[4].setText(" Position: (" + String.format("%.1f", x1) + ", " + String.format("%.1f", y1) + ")");
                            switch (onHand) {
                                case 0:
                                    textView[5].setText(" Pose: Pocket");
                                    break;
                                case 1:
                                    textView[5].setText(" Pose: Hand");
                                    break;
                            }
                            switch (mainAxis) {
                                case 0:
                                    textView[6].setText(" Main Axis: X");
                                    break;
                                case 1:
                                    textView[6].setText(" Main Axis: Y");
                                    break;
                                case 2:
                                    textView[6].setText(" Main Axis: Z");
                                    break;
                            }
                            //textView.append(log + "\n");
                            //scrollView.scrollTo(0, textView.getHeight());
                        } else if (stepInterval > maxStepFreq) {
                            lastPeakTime = curTime;
                            //stepDetectionFlag = false;
                        }
                    }
                }
//                String log = (Long.toString((long) (curTime - sysTime)))
//                        .concat(",xG," + String.format("%.2f", gravity[0]))
//                        .concat(",yG," + String.format("%.2f", gravity[1]))
//                        .concat(",zG," + String.format("%.2f", gravity[2]))
//                        .concat(",lX," + String.format("%.2f", linear_acceleration[0]))
//                        .concat(",lY," + String.format("%.2f", linear_acceleration[1]))
//                        .concat(",lZ," + String.format("%.2f", linear_acceleration[2]))
//                        .concat(",fX," + String.format("%.2f", filteredAcc[0]))
//                        .concat(",fY," + String.format("%.2f", filteredAcc[1]))
//                        .concat(",fZ," + String.format("%.2f", filteredAcc[2]))
//                        .concat(",sqrt," + String.format("%.2f", sqrtAcc))
//                        .concat(",pre," + String.format("%.2f", prevAcc))
//                        .concat(",stepInt," + String.valueOf(stepInterval))
//                        .concat(",x," + String.format("%.2f", xCoordinate))
//                        .concat(",y," + String.format("%.2f", yCoordinate))
//                        .concat(",x1," + String.format("%.2f", x1))
//                        .concat(",y1," + String.format("%.2f", y1));
//                writeLog(raf[0], log);
                slope = curSlope;
                prevAcc = sqrtAcc;
                preLogTime = curTime;

//                if (curTime - preLogTime2 > logInterval2) {
//                    String log = String.valueOf((preLogTime2 - sysTime) / 1000) //로그 찍는 타임 싱크를 맞춰야함
//                            .concat(",step," + String.valueOf(stepCount))
//                            .concat(",acc," + String.format("%.2f", prevAcc))
//                            .concat(",len," + String.format("%.2f", totalLength))
//                            .concat(",x1," + String.format("%.2f", x1))
//                            .concat(",y1," + String.format("%.2f", y1))
//                            .concat(",x2," + String.format("%.2f", x2))
//                            .concat(",y2," + String.format("%.2f", y2))
//                            .concat(",axis," + mainAxis)
//                            .concat(",onHand," + onHand)
//                            .concat(",degree," + correctedRotation[mainAxis]);
//
//                    textView[0].setText(" Time: " + Long.toString((long) (preLogTime2 - sysTime) / 1000));
//                    preLogTime2 = curTime;
//                    writeLog(raf[1], log);
//                }
            }
        }
    }

    /**
     * Gyroscope Sensor
     **/

    private class gyroscopeListener implements SensorEventListener {
        private double preTime, dt, preLogTime, preLogTime_1;
        private double[] lastPeakTime = new double[3];
        private boolean firstFlag = true;
        private float[] curSlope = new float[3];
        private float[] slope = new float[3];
        private float[] prevFilteredRotation = new float[3];
        private int[] inPocket = new int[3];

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            double curTime = System.currentTimeMillis();
            double rotationThreshold = 0.115; // 단위: Deg/s
            double positionThreshold = 0.25; // 단위: Deg/s
            double logInterval = 10; //단위 ms
            double logInterval_1 = 250; //단위 ms
            double detectionInterval = 400;
            int minPoseDuration = 3000; //단위 sec
            float[] rotation = new float[3];
            float alpha = (float) 0.1;

            /* Data Processing */
            if (firstFlag) {
                preTime = curTime;
                for (int i = 0; i < 3; i++) {
                    lastPeakTime[i] = curTime;
                }
                firstFlag = false;
            } else {
                dt = curTime - preTime;
                for (int i = 0; i < 3; i++) {
                    event.values[i] = (float) (event.values[i] * RAD2DGR); //Deg/s 로 변환
                    rotation[i] = event.values[i] * (float) dt * MS2S; // 적분
                    aggRotation[i] = aggRotation[i] + rotation[i]; // just summation
                    filteredRotation[i] = lowPassFilter(alpha, rotation[i], filteredRotation[i]); //weighted moving average
                    /* 360도 보정*/
                    if (aggRotation[i] > 360)
                        aggRotation[i] = aggRotation[i] - 360;
                    if (aggRotation[i] < -360)
                        aggRotation[i] = aggRotation[i] + 360;
                }
                preTime = curTime;
            }
            /* Data Processing */

            /* Turn Detection */
            if (Math.abs(filteredRotation[mainAxis]) > rotationThreshold) {
                correctedRotation[mainAxis] += rotation[mainAxis];
                correctedByDoor[mainAxis] += rotation[mainAxis];
                rotationFlag = true;
            } else {
                rotationFlag = false;
            }
            /* Turn Detection */

            /* Peak Detection */
            if (curTime - preLogTime > logInterval) {
                for (int i = 0; i < 3; i++) {
                    curSlope[i] = filteredRotation[i] - prevFilteredRotation[i];
                    if (slope[i] > 0 && curSlope[i] < 0 && preLogTime - lastPeakTime[i] > detectionInterval) {
                        if (prevFilteredRotation[i] > positionThreshold) {
                            lastPeakTime[i] = preLogTime;
                            inPocket[i] = 0; // true = 0, false = 1
                            //inPocket[i] = Math.abs(prevFilteredRotation[i]) > positionThreshold;
                            @SuppressLint("DefaultLocale")
                            String log = String.valueOf((preLogTime - sysTime) / 1000) //로그 찍는 타임 싱크를 맞춰야함
                                    .concat(",filtered," + String.format("%.2f", prevFilteredRotation[i]))
                                    .concat(",inPocket," + inPocket[i]);
                            writeLog(raf[i + 4], log);
                        }
                    }
                    if (preLogTime - lastPeakTime[i] > minPoseDuration) {
                        inPocket[i] = 1;
                        lastPeakTime[i] = preLogTime;
                    }
                    prevFilteredRotation[i] = filteredRotation[i];
                    slope[i] = curSlope[i];
                }

                /* Pose Detection */
                switch (mainAxis) {
                    case 0:
                        onHand = inPocket[1] * inPocket[2];
                        break;
                    case 1:
                        onHand = inPocket[0] * inPocket[2];
                        break;
                    case 2:
                        onHand = inPocket[0] * inPocket[1];
                        break;
                }
                /* Pose Detection */
                preLogTime = curTime;
            }
            /* Peak Detection */
            if (curTime - preLogTime_1 > logInterval_1) {
                @SuppressLint("DefaultLocale")
                String log = String.valueOf((curTime - sysTime) / 1000)
                        .concat(",raw_X," + String.format("%.2f", event.values[0]))
                        .concat(",raw_Y," + String.format("%.2f", event.values[1]))
                        .concat(",raw_Z," + String.format("%.2f", event.values[2]))
                        .concat(",deg_X, " + String.format("%.2f", rotation[0]))
                        .concat(",deg_Y," + String.format("%.2f", rotation[1]))
                        .concat(",deg_Z," + String.format("%.2f", rotation[2]))
                        .concat(",agg_X," + String.format("%.2f", aggRotation[0]))
                        .concat(",agg_Y," + String.format("%.2f", aggRotation[1]))
                        .concat(",agg_Z," + String.format("%.2f", aggRotation[2]))
                        .concat(",filtered_X," + String.format("%.2f", filteredRotation[0]))
                        .concat(",filtered_Y," + String.format("%.2f", filteredRotation[1]))
                        .concat(",filtered_Z," + String.format("%.2f", filteredRotation[2]))
                        .concat(",door_X," + String.format("%.2f", correctedByDoor[0]))
                        .concat(",door_Y," + String.format("%.2f", correctedByDoor[1]))
                        .concat(",door_Z," + String.format("%.2f", correctedByDoor[2]))
                        .concat(",corrected_X," + String.format("%.2f", correctedRotation[0]))
                        .concat(",corrected_Y," + String.format("%.2f", correctedRotation[1]))
                        .concat(",corrected_Z," + String.format("%.2f", correctedRotation[2]));
                writeLog(raf[2], log);
                preLogTime_1 = curTime;
            }
        }
    }


    /**
     * Geomagnetic Sensor
     **/

//    private class geomagneticListener implements SensorEventListener {
//        private float[] geo = new float[3];
//        private double preTIme;
//
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        }
//
//        @Override
//        public void onSensorChanged(SensorEvent event) {
//            double curTime = System.currentTimeMillis();
//            double logInterval = 50; //단위 ms
//            float alpha = (float) 0.25;
//            for (int i = 0; i < 3; i++) {
//                geo[i] = lowPassFilter(alpha, event.values[i], geo[i]);
//            }
//
//            sumGeo = geo[0] + geo[1] + geo[2];
//            rmsGeo = (float) Math.sqrt((geo[0] * geo[0] + geo[1] * geo[1] + geo[2] * geo[2]));
//
//            if (curTime - preTIme > logInterval) {
//                @SuppressLint("DefaultLocale")
//                String log = (Long.toString((long) (curTime - sysTime)))
//                        .concat(",X, " + String.format("%.2f", geo[0]))
//                        .concat(",Y," + String.format("%.2f", geo[1]))
//                        .concat(",Z," + String.format("%.2f", geo[2]))
//                        .concat(",sum," + String.format("%.2f", sumGeo))
//                        .concat(",rms," + String.format("%.2f", rmsGeo));
//                writeLog(raf[3], log);
//                Log.i(TAG1, log);
//                preTIme = curTime;
//            }
//        }
//    }

    // BLE scanCallback function //
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord == null) {
                Log.i(TAG2, "jychoi Null ScanRecord for device " + result.getDevice().getAddress());
                return;
            }
            byte[] serviceData = scanRecord.getServiceData(EDDYSTONE_SERVICE_UUID);
            if (serviceData == null) {
                return;
            }

            if (serviceData[0] == EDDYSTONE_URL_FRAME_TYPE && serviceData.length == 5 && serviceData[3] == URL_DATA && serviceData[4] == URL_DATA) {
                String mac = result.getDevice().getAddress().toLowerCase();
//                String time = Long.toString(System.currentTimeMillis());
                int rssi = result.getRssi();
                checkBleInfo(mac, rssi);
//                Log.i(TAG2, "MAC: " + mac);

//                int dataLength = serviceData.length;
//                Log.i(TAG2, "MAC: " + mac +" length: " + dataLength + " ServiceUUids " + byteArrayToHex(scanRecord.getBytes()));
//            Log.i(TAG2, "Service Data Size: " + dataLength + " " +  mac);
//            for(int i = 0; i < dataLength; i++){
//                Log.i(TAG2, "Service Data Size: " + dataLength + " " + serviceData[0] + " " + serviceData[1] + " " + serviceData[2] + " "+ serviceData[3] + " " + serviceData[4]);
//            }
//            Log.i(TAG2, "\n");

            }
        }
    }; // BLE scanCallback function //

    // update BLE Info
    private void checkBleInfo(String mac, int rssi) {
        infoBle ble = new infoBle();
        ble.mac = mac;
        ble.rssi = rssi;
        int size = bleList.size();
        float weight = (float) 1;


//        double diss1 = 0;
//        double diss2 = 0;
//        int dist[] = new int[3];

//        if (size == 3) {
//            String log = String.valueOf((System.currentTimeMillis() - sysTime) / 1000)
//                    .concat("," + bleList.get(0).rssi)
//                    .concat("," + bleList.get(1).rssi)
//                    .concat("," + bleList.get(2).rssi);
//            writeLog(raf[7], log);
//
//            for (int i = 0; i < size; i++) {
//                for (int j = 0; j < size; j++) {
//                    if ((bleList.get(i).rssi - bleList.get(j).rssi) > 0) {
//                        dist[i] = dist[i] + bleList.get(i).rssi - bleList.get(j).rssi;
//                    }
//                }
//            }
//
//            if (bleFlag) {
//                System.arraycopy(dist, 0, stan, 0, size);
//                System.arraycopy(dist, 0, prev, 0, size);
//                bleFlag = false;
//            } else {
//                for (int i = 0; i < size; i++) {
//                    diss1 = diss1 + (dist[i] - stan[i]) * (dist[i] - stan[i]);
//                    diss2 = diss2 + (dist[i] - prev[i]) * (dist[i] - prev[i]);
//                }
//                diss1 = Math.sqrt(diss1);
//                diss2 = Math.sqrt(diss2);
//            }
//
//            stateUpdate(diss1);
//
//            @SuppressLint("DefaultLocale")
//            String log1 = String.valueOf((System.currentTimeMillis() - sysTime) / 1000)
//                    .concat("," + String.format("%.2f", diss1))
//                    .concat("," + String.format("%.2f", diss2));
//            writeLog(raf[8], log1);
//            System.arraycopy(dist, 0, prev, 0, size);
//        }

        for (int i = 0; i < size; i++) {
//            Log.i(TAG2, " HI");
            if (bleList.get(i).mac.equals(ble.mac)) {
//                Log.i(TAG2, " HI2");
                ble.rssi = (int) lowPassFilter(weight, rssi, bleList.get(i).rssi);
                bleList.set(i, ble);
//                return;
            }
        }
//        bleList.add(ble);
//        Log.i(TAG2, "MAC " + ble.mac);

//        size = bleList.size();

        String log = String.valueOf((System.currentTimeMillis() - sysTime) / 1000);
//        String log2 = String.valueOf((System.currentTimeMillis() - sysTime) / 1000);
        for (int i = 0; i < size; i++) {
              log = log.concat("," + bleList.get(i).rssi);
//            log2 = log2.concat("," + bleList.get(i).mac);
        }
        String log2 = log.concat("," + x1)
                .concat("," + y1)
                .concat("," + correctedRotation[mainAxis]);

        writeLog(raf[7], log2);
//        writeLog(raf[10], log2);
    }

//    @SuppressLint("SetTextI18n")
//    private void stateUpdate(double diss) {
//        int stateCountThre = 6;
//        int dissThreshold = 40;
//        int currState;
//
//        // Determine the state
//        if(diss < dissThreshold){
//            currState = 0;
//        }
//        else{
//            currState = 1;
//        }
//        // Check the initial state
//        if(stateFlag){
//            prevState = currState;
//            stateCount = 1;
//            stateFlag = false;
//        } // check the following state
//        else{
//            if(currState == prevState){
//                stateCount = stateCount + 1;
//            }
//            else {
//                stateCount = stateCount - 1;
//            }
//
//            if(stateCount >= stateCountThre){
//                stateCount = stateCountThre;
//                detState = currState;
//                prevState = currState;
//                textView[7].setText(" Room State: " + detState);
//            }
//            else if (stateCount <= 0){
//                if(detState != -1){ // Change state if state = 1 in-->out state = 0 out-->in
//                    stateChange = stateChange + 1;
//                    detState = currState;
//                    stateCount = stateCountThre;
//                    // 문 지나는 순간 값 보정
//                    x2 = 13.05;
//                    y2 = 0;
//                    if(detState == 0)
//                        correctedByDoor[mainAxis] = -180;
//                    if(detState == 1)
//                        correctedByDoor[mainAxis] = 0;
//                    // 문 지나는 순간 값 보정
//                    textView[7].setText(" Room State: " + detState);
//                    String log = String.valueOf((System.currentTimeMillis() - sysTime) / 1000)
//                            .concat("," + prevState)
//                            .concat("," + detState)
//                            .concat("," + stateChange);
//                    writeLog(raf[9], log);
//                }
//                prevState = currState;
//            }
//        }
//    }

//    String byteArrayToHex(byte[] a) {
//        StringBuilder sb = new StringBuilder();
//        for(final byte b: a)
//            sb.append(String.format("%02x ", b&0xff));
//        return sb.toString();
//    }


    // msjang http post

    public class NetworkTask extends AsyncTask<Void, Void, String> {

        private String url;
        private ContentValues values;
        private long RTT_calc = 0; //RTT 체크용

        public NetworkTask(String url, ContentValues values) {

            this.url = url;
            this.values = values;
            this.RTT_calc =  System.currentTimeMillis(); //RTT 체크용
        }

        @Override
        protected String doInBackground(Void... params) {

            //String result; // 요청 결과를 저장할 변수.


            //String server_file_name = "data.csv";   // 서버에 저장되는 파일의 이름.

            /*
            // 테스트용 더미 파일 생성

            String dirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + "TEST_HTTP_POST" + "/";
            File file_dir = new File(dirPath);
            if (!file_dir.exists())
                file_dir.mkdirs();

            BufferedWriter bfw;

            try
            {
                bfw = new BufferedWriter(new FileWriter(dirPath + "TEST" +".txt", false));
                bfw.write("TESTABAAA");
                bfw.flush();
                bfw.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }

            File file = new File(dirPath+"TEST" +".txt");

            // 테스트용 더미 파일 생성 종료



            */

            String php_key = "userfile";  // 서버의 php파일에 있는 key와 동일하게 만들어야함
            String server_file_name = for_HTTP_send.getName();   // 서버에 저장되는 파일의 이름.

            try {
                URL url_ = new URL(this.url);
                String boundary = "SpecificString";
                URLConnection con = url_.openConnection();
                con.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                con.setDoOutput(true);

                DataOutputStream wr = new DataOutputStream(con.getOutputStream());

                wr.writeBytes("\r\n--" + boundary + "\r\n");
                wr.writeBytes( "Content-Disposition: form-data; name=\"" + php_key + "\";filename=\"" + server_file_name + "\"" + "\r\n" ) ;
                wr.writeBytes("Content-Type: application/octet-stream\r\n\r\n");

                FileInputStream fileInputStream = new FileInputStream(for_HTTP_send.getPath());  //파일의 절대경로
                int bytesAvailable = fileInputStream.available();
                int maxBufferSize = 1024;
                int bufferSize = Math.min(bytesAvailable, maxBufferSize);
                byte[] buffer = new byte[bufferSize];

                int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                while (bytesRead > 0)
                {
                    // Upload file part(s)
                    DataOutputStream dataWrite = new DataOutputStream(con.getOutputStream());
                    dataWrite.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }
                fileInputStream.close();

                wr.writeBytes("\r\n--" + boundary + "--\r\n");
                wr.flush();

                BufferedReader reader = null;

                reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"));
                String line;
                String page = "";

                // 라인을 받아와 합친다.
                while ((line = reader.readLine()) != null){
                    page += line;
                }

                return page;

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();

            }

            return null;
        }



        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            textView[9].setText(s);
            textView[10].setText(String.valueOf(System.currentTimeMillis()-RTT_calc));
            /*
             // 데이터 처리 관련 주석처리 하였음.

            int x = 0;
            int y = 0;

            Pattern groupPattern = Pattern.compile("x=(.*?);");
            Matcher groupMatcher = groupPattern.matcher(s);

            if(groupMatcher.find())
            {
                x = Integer.parseInt(groupMatcher.group(1));
            }

            groupPattern = Pattern.compile("y=(.*?);");
            groupMatcher = groupPattern.matcher(s);

            if(groupMatcher.find())
            {
                y  = Integer.parseInt(groupMatcher.group(1));
            }

            x_coord.setText(String.valueOf(x));
            y_coord.setText(String.valueOf(y));
            RTT = (TextView) findViewById(R.id.RTT);
            RTT.setText(String.valueOf(System.currentTimeMillis()-time) + " ms");

            */
        }
    }

    @Override
    protected void onDestroy() {
//        unregisterReceiver(rssiReceiver);
        super.onDestroy();
    }
}
