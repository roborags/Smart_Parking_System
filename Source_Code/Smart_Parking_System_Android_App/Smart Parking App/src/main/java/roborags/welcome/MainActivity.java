package roborags.welcome;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.os.StrictMode;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import it.sauronsoftware.ftp4j.FTPClient;
import it.sauronsoftware.ftp4j.FTPDataTransferListener;

import static android.hardware.SensorManager.SENSOR_DELAY_GAME;

public class MainActivity extends AppCompatActivity {
    WifiManager wifi;
    private RadioGroup Radio_Group_Connection;
    private RadioButton Radio_Button_Connection,Radio_button_WIFI,Radio_button_Cell;
    private Button Button_Connect_Network;//Button_Set_FTP,Button_Set_Storage;
    private ToggleButton Acc_Start_Stop;
    //private TextView Accel_X_Data,Accel_Y_Data,Accel_Z_Data,GPS_Lat_Data,GPS_Long_Data,GPS_Alt_Data;
    //private TextView Storage_Max,Storage_Thresh;
    //private TextView FTP_Host,FTP_Uname,FTP_Pass;
    //private ToggleButton Acc_Start_Stop,GPS_Start_Stop,Acc_Sample,GPS_Sample;
    public boolean Acc_Status,GPS_Status,Gyro_Status,File_First_Write,Acc_Sample_Rate,GPS_Sample_Rate;


    private Sensor Acc_Sensor;
    private SensorManager Acc_Sensor_Manager;

    private Sensor Gyro_Sensor;
    //private SensorManager Gyro_Sensor_Manager;

    private LocationManager GPS_Location_Manager;
    //private LocationListener GPS_Location_Listener;

    private static final String COMMA_DELIMITER = ",";

    private static final String NEW_LINE_SEPARATOR = "\n";

    private Sensor GPS_Sensor;
    private SensorManager GPS_Sensor_Manager;


    private TCPClient mTcpClient;
    private String File_Path;
    static final int READ_BLOCK_SIZE = 100;

    public static final int NOT_CONNECTED = 0;
    public static final int WIFI_CONNECTED = 1;
    public static final int CELLULAR_CONNECTED = 2;
    public int Net_Conn_Status = NOT_CONNECTED;

    public static int STORAGE_MAX_DEF = 100000;
    public static int STORAGE_THRESHOLD_DEF = 10000;
    public String File_Saved_Name,File_Del_Name;

    //public ArrayList<Float> DataList_X,DataList_Y,DataList_Z;
    //public ArrayList<String> DataList_Date;
    public int Storage_Max_Val=STORAGE_MAX_DEF,Storage_Threshold=STORAGE_THRESHOLD_DEF;
    //public int Storage_Index=0;
    public float[] Acc_Data = new float[3];
    public double[] GPS_Data = new double[3];
    public float[] Gyro_Data = new float[3];

    /*********  work only for Dedicated IP ***********/
    static String FTP_HOST= "153.92.11.11";

    static String FTP_HOST_LINK= "files.000webhost.com";

    /*********  FTP USERNAME ***********/
    static String FTP_USER = "roborags";

    /*********  FTP PASSWORD ***********/
    static String FTP_PASS ="Rags@420";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);

            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            Radio_Group_Connection = (RadioGroup) findViewById(R.id.radioGroup);
            Button_Connect_Network = (Button) findViewById(R.id.button1);
            /*
            Button_Set_FTP = (Button) findViewById(R.id.button3);
            Button_Set_Storage = (Button) findViewById(R.id.button2);
            Accel_X_Data = (TextView)findViewById(R.id.editText1);
            Accel_Y_Data = (TextView)findViewById(R.id.editText2);
            Accel_Z_Data = (TextView)findViewById(R.id.editText3);
            GPS_Lat_Data = (TextView)findViewById(R.id.editText4);
            GPS_Long_Data = (TextView)findViewById(R.id.editText5);
            GPS_Alt_Data = (TextView)findViewById(R.id.editText6);
            Storage_Max = (TextView)findViewById(R.id.editText7);
            Storage_Thresh = (TextView)findViewById(R.id.editText8);
            FTP_Host = (TextView)findViewById(R.id.editText9);
            FTP_Uname = (TextView)findViewById(R.id.editText10);
            FTP_Pass = (TextView)findViewById(R.id.editText11);
            Acc_Start_Stop = (ToggleButton)findViewById(R.id.toggleButton1);
            GPS_Start_Stop = (ToggleButton)findViewById(R.id.toggleButton2);

            GPS_Sample = (ToggleButton)findViewById(R.id.toggleButton4);
            */
            Acc_Start_Stop = (ToggleButton)findViewById(R.id.toggleButton1);
            Radio_button_WIFI = (RadioButton) findViewById(R.id.radioButton);
            Radio_button_Cell = (RadioButton) findViewById(R.id.radioButton2);

            //Storage_Max.setText(""+STORAGE_MAX_DEF/1000);
            //Storage_Thresh.setText(""+STORAGE_THRESHOLD_DEF/1000);
           // FTP_Host.setText(FTP_HOST);
           // FTP_Uname.setText(FTP_USER);
           // FTP_Pass.setText(FTP_PASS);

            File_Path = this.getFilesDir().toString();

            Acc_Sensor_Manager = (SensorManager) getSystemService(SENSOR_SERVICE);
            Acc_Sensor = Acc_Sensor_Manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Gyro_Sensor = Acc_Sensor_Manager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            GPS_Location_Manager =(LocationManager) getSystemService(LOCATION_SERVICE);

            File_First_Write = true;
            Acc_Sample_Rate = true;
            GPS_Sample_Rate = true;

            CheckNetworkConnection();

            if(Net_Conn_Status == WIFI_CONNECTED)
            {
                Radio_button_WIFI.setChecked(true);
                Radio_button_Cell.setChecked(false);
                Log.i("onCreate","Conn Status"+Net_Conn_Status);
            }
            else if (Net_Conn_Status == CELLULAR_CONNECTED)
            {
                Radio_button_Cell.setChecked(true);
                Radio_button_WIFI.setChecked(false);
                Log.i("onCreate","Conn C Status"+Net_Conn_Status);
            }
        /*
            Button_Set_FTP.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    try {
                        FTP_HOST = FTP_Host.toString();
                        FTP_USER = FTP_Uname.toString();
                        FTP_PASS = FTP_Pass.toString();

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("OnCreate","exception e = "+e);
                    }

                }
            });

            Button_Set_Storage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    try {
                        if(Integer.parseInt(Storage_Max.getText().toString())<Integer.parseInt(Storage_Thresh.getText().toString())
                                ||Integer.parseInt(Storage_Max.getText().toString()) < 0
                                 || Integer.parseInt(Storage_Thresh.getText().toString()) < 0)
                        {
                            Toast.makeText(getApplicationContext(), "Invalid Value Storage", Toast.LENGTH_LONG).show();
                            Log.i("Button_Set_Storage","Invalid Value");
                            Storage_Max.setText(Storage_Max_Val);
                            Storage_Thresh.setText(Storage_Threshold);
                        }
                        else
                        {
                            Storage_Max_Val = Integer.parseInt(Storage_Max.getText().toString());
                            Storage_Threshold = Integer.parseInt(Storage_Thresh.getText().toString());
                            Acc_Start_Stop.setChecked(false);
                            Acc_Start_Stop.callOnClick();
                            GPS_Start_Stop.setChecked(false);
                            GPS_Start_Stop.callOnClick();
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("OnCreate","exception e = "+e);
                    }

                }
            });
            */
        }
    }
    /*
    public void ACCSampleButtonClick(View view)
    {
        boolean Status_Check;
        Status_Check = Acc_Sample.isChecked();

        Acc_Start_Stop.setChecked(false);
        Acc_Start_Stop.callOnClick();

        if(Status_Check == true)
        {
            Acc_Sample_Rate = false;
        }
        else
        {
            Acc_Sample_Rate  =true;
        }
    }
    */
    public void ACCButtonClick(View view)
    {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                boolean Status_Check;
                Status_Check = Acc_Start_Stop.isChecked();
                if(Status_Check == true)
                {
                    Acc_Status = true;
                    //Toast.makeText(getApplicationContext(), "Accelerometer Started", Toast.LENGTH_LONG).show();
                    Log.i("ACCButtonClick","Accelerometer Started");
                    /*
                    if(Acc_Sample_Rate == true)
                    {
                        Acc_Sensor_Manager.registerListener(_SensorEventListener , Acc_Sensor, SENSOR_DELAY_NORMAL);
                        Log.i("ACCButtonClick","Accelerometer Normal Sampling");
                    }
                    else
                    { */
                    Acc_Sensor_Manager.registerListener(_SensorEventListener , Acc_Sensor, SENSOR_DELAY_GAME);
                    //Log.i("ACCButtonClick","Accelerometer MAX Sampling");
                    //}

                    Gyro_Status = true;
                    Log.i("ACCButtonClick","Gyroscope Started");

                    Acc_Sensor_Manager.registerListener(_SensorEventListener , Gyro_Sensor, SENSOR_DELAY_GAME);

                    GPS_Status = true;
                    Log.i("GPSButtonClick","Starting GPS...");
                    Looper.prepare();
                    //Toast.makeText(getApplicationContext(), "GPS Started", Toast.LENGTH_LONG).show();

                    if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                                    ,10);
                        }
                        return;
                    }
                    //Log.i("GPSButtonClick","GPS Loc Update Call ++");

                    //if(GPS_Sample_Rate == true)
                    //{
                        GPS_Location_Manager.requestLocationUpdates("gps", 10, 0, GPS_Location_Listener);
                        Log.i("ACCButtonClick","GPS Normal Sampling");
                    /*}
                    else
                    {
                        GPS_Location_Manager.requestLocationUpdates("gps", 1000, 0, GPS_Location_Listener);
                        Log.i("ACCButtonClick","GPS Fast Sampling");
                    }
                    */
                    Looper.loop();
                    GPS_Location_Manager.removeUpdates(GPS_Location_Listener);

                }
                else
                {
                    Acc_Status = false;
                    //Toast.makeText(getApplicationContext(), "Accelerometer Stopped", Toast.LENGTH_LONG).show();
                    Log.i("ACCButtonClick","Accelerometer Stopped");
                    Acc_Sensor_Manager.unregisterListener(_SensorEventListener);

                    Gyro_Status = false;

                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    GPS_Status = false;

                    File_First_Write = true;
                    new FileUpload().execute("");
                }
            }
        };

        Thread Accthread = new Thread(runnable);
        Accthread.start();

        Thread GPSthread = new Thread(runnable);
        GPSthread.start();
    }

    /*
    public void GPSSampleButtonClick(View view)
    {
        boolean Status_Check;
        Status_Check = GPS_Sample.isChecked();

        GPS_Start_Stop.setChecked(false);
        GPS_Start_Stop.callOnClick();

        if(Status_Check == true)
        {
            GPS_Sample_Rate = false;
        }
        else
        {
            GPS_Sample_Rate  =true;
        }
    }

    public void GPSButtonClick(View v) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                boolean Status_Check;
                Status_Check = GPS_Start_Stop.isChecked();

                if(Status_Check == true)
                {
                    GPS_Status = true;
                    Log.i("GPSButtonClick","Starting GPS...");
                    Looper.prepare();
                    //Toast.makeText(getApplicationContext(), "GPS Started", Toast.LENGTH_LONG).show();

                    if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                                    ,10);
                        }
                        return;
                    }
                    //Log.i("GPSButtonClick","GPS Loc Update Call ++");

                    if(GPS_Sample_Rate == true)
                    {
                        GPS_Location_Manager.requestLocationUpdates("gps", 500, 0, GPS_Location_Listener);
                        Log.i("ACCButtonClick","GPS Normal Sampling");
                    }
                    else
                    {
                        GPS_Location_Manager.requestLocationUpdates("gps", 1000, 0, GPS_Location_Listener);
                        Log.i("ACCButtonClick","GPS Fast Sampling");
                    }

                    Looper.loop();
                    GPS_Location_Manager.removeUpdates(GPS_Location_Listener);

                    //Log.i("GPSButtonClick","GPS Loc Update Call --");
                }
                else
                {
                    GPS_Status = false;
                }
            }
        };
        Thread GPSthread = new Thread(runnable);
        GPSthread.start();
    }
    */
    LocationListener GPS_Location_Listener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            try {
                //float[] Temp_Store = new float[3];
                GPS_Data[0] = location.getLatitude();
                GPS_Data[1] = location.getLongitude();
                GPS_Data[2] = location.getAltitude();

                //Log.i("GPSEventListerner", "GPS Data " + GPS_Data[0] + "#" + GPS_Data[1] + "#" + GPS_Data[2]);
                /*
                Message msg = GPS_handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("Lat", "" + GPS_Data[0]);
                bundle.putString("Long", "" + GPS_Data[1]);
                bundle.putString("Alt", "" + GPS_Data[2]);

                msg.setData(bundle);
                */
                //Log.i("GPS_Location_Listener", "Handler message sent --");
                //GPS_handler.sendMessage(msg);

                if(GPS_Status == false) {
                    Looper.myLooper().quit();

                    try {
                        AddDataToFile(2, (float) GPS_Data[0], (float) GPS_Data[1], (float) GPS_Data[2]);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }


            }catch (Exception e){
                e.printStackTrace();
                Log.e("GPS_Location_Listener", "Exception e = "+e);
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
            //Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            //startActivity(i);
        }
    };
    /*
    public Handler GPS_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            //Log.i("GPS_handler","GPS_handler ++");
            Bundle bundle = msg.getData();

            GPS_Lat_Data.setText(""+bundle.getString("Lat"));
            GPS_Long_Data.setText(""+bundle.getString("Long"));
            GPS_Alt_Data.setText(""+bundle.getString("Alt"));
            //Log.i("GPS_handler","GPS_handler --");

        }
    };
    */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case 10:
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.INTERNET}
                                ,10);
                    }
                    return;
                }
                break;
            default:
                break;
        }
    }

    public void ConnectButtonClick(View v) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                int Select_Conn_Option = Radio_Group_Connection.getCheckedRadioButtonId();
                View Select_Radio_Button = Radio_Group_Connection.findViewById(Select_Conn_Option);
                int Select_Radio_Button_ID = Radio_Group_Connection.indexOfChild(Select_Radio_Button);
                if (Select_Radio_Button_ID == 0) {
                    //Toast.makeText(getApplicationContext(), "Cellular Option", Toast.LENGTH_LONG).show();
                    //startActivity(new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS));
                    if(Net_Conn_Status == WIFI_CONNECTED)
                    {
                        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        wifiManager.setWifiEnabled(false);
                        //Toast.makeText(getApplicationContext(), "Disabling WIFI swiching to cellular", Toast.LENGTH_LONG).show();
                        Log.i("onClick","Disabling WIFI swiching to cellular");
                        Log.i("ConnectButtonClick","Conn W Status"+Net_Conn_Status);
                    }
                    Intent intent = new Intent(Intent.ACTION_MAIN);
                    intent.setComponent(new ComponentName("com.android.settings",
                            "com.android.settings.Settings$DataUsageSummaryActivity"));
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);


                    Net_Conn_Status = CELLULAR_CONNECTED;
                    Log.i("ConnectButtonClick","Cellular Option");
                    Log.i("ConnectButtonClick","Conn C Status"+Net_Conn_Status);
                }
                else if (Select_Radio_Button_ID == 1) {
                    //Toast.makeText(getApplicationContext(), "Wifi Option", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                    Net_Conn_Status = WIFI_CONNECTED;
                    Log.i("ConnectButtonClick","Wifi Option");
                    Log.i("ConnectButtonClick","Conn W Status"+Net_Conn_Status);
                }
            }
        };
        Thread Netthread = new Thread(runnable);
        Netthread.start();
    }

    public void CheckNetworkConnection()
    {
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                Toast.makeText(this, activeNetwork.getTypeName(), Toast.LENGTH_SHORT).show();
                Net_Conn_Status = WIFI_CONNECTED;
                Log.i("CheckNetworkConnection","Conn W Status"+Net_Conn_Status);
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                Toast.makeText(this, activeNetwork.getTypeName(), Toast.LENGTH_SHORT).show();
                Net_Conn_Status = CELLULAR_CONNECTED;
                Log.i("CheckNetworkConnection","Conn C Status"+Net_Conn_Status);
            }
        }
        else {
            // not connected to the internet
            Toast.makeText(this, "Not Connected to Any Network", Toast.LENGTH_SHORT).show();
            Net_Conn_Status = NOT_CONNECTED;
        }
    }



    SensorEventListener _SensorEventListener=   new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if(event.sensor == Acc_Sensor) {
                Acc_Data[0] = event.values[0];
                Acc_Data[1] = event.values[1];
                Acc_Data[2] = event.values[2];
                /*
                Message msg = Acc_handler.obtainMessage();
                Bundle bundle= new Bundle();
                bundle.putString("AccX",""+Acc_Data[0]);
                bundle.putString("AccY",""+Acc_Data[1]);
                bundle.putString("AccZ",""+Acc_Data[2]);

                msg.setData(bundle);
                Acc_handler.sendMessage(msg);
                */
                //Log.i("SensorEventListener","Acc Data added "+Acc_Data[0]+"#"+Acc_Data[1]+"#"+Acc_Data[2]);
                try {
                    AddDataToFile(1, Acc_Data[0], Acc_Data[1], Acc_Data[2]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if(event.sensor == Gyro_Sensor) {
                Gyro_Data[0] = event.values[0];
                Gyro_Data[1] = event.values[1];
                Gyro_Data[2] = event.values[2];

                //Log.i("SensorEventListener","Gyro Data added "+Gyro_Data[0]+"#"+Gyro_Data[1]+"#"+Gyro_Data[2]);
                try {
                    AddDataToFile(3, Gyro_Data[0], Gyro_Data[1], Gyro_Data[2]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };
    /*
    Handler Acc_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();

            Accel_X_Data.setText(""+bundle.getString("AccX"));
            Accel_Y_Data.setText(""+bundle.getString("AccY"));
            Accel_Z_Data.setText(""+bundle.getString("AccZ"));

        }
    };
    */
    public void AddDataToFile(int SenIdent,float Data_1,float Data_2,float Data_3)throws IOException
    {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
            String currentDateandTime = sdf.format(new Date());
            long TimeMsVal = System.currentTimeMillis();
            File fileIn;

            if (File_First_Write == true) {
                fileIn = new File(File_Path, "Saved_Sensor_Values" + ".csv");
                if (!fileIn.exists()) {
                    fileIn.createNewFile();
                    Log.i("FILE_PATH", "The File Path is:" + fileIn.getAbsolutePath().toString());
                }
                File_Del_Name = File_Saved_Name = "Saved_Sensor_Values";
                File_First_Write = false;
            }
            else
            {
                fileIn = new File(File_Path, File_Saved_Name + ".csv");
                if (!fileIn.exists()) {
                    //fileIn.createNewFile();
                    Log.i("FILE_PATH", "File Open Error: " + File_Saved_Name);
                }
            }
            BufferedWriter writer = new BufferedWriter(new FileWriter(fileIn, true));
            if(SenIdent == 1) {
                writer.write("A"+COMMA_DELIMITER+currentDateandTime+COMMA_DELIMITER+TimeMsVal
                                +COMMA_DELIMITER+Data_1+COMMA_DELIMITER+Data_2
                                    +COMMA_DELIMITER+Data_3+NEW_LINE_SEPARATOR);
            }else if(SenIdent == 2){
                writer.write("G"+COMMA_DELIMITER+currentDateandTime+COMMA_DELIMITER+TimeMsVal
                                +COMMA_DELIMITER+Data_1+COMMA_DELIMITER+Data_2
                                    +COMMA_DELIMITER+Data_3+NEW_LINE_SEPARATOR);
            }else if(SenIdent == 3){
                writer.write("Y"+COMMA_DELIMITER+currentDateandTime+COMMA_DELIMITER+TimeMsVal
                        +COMMA_DELIMITER+Data_1+COMMA_DELIMITER+Data_2
                        +COMMA_DELIMITER+Data_3+NEW_LINE_SEPARATOR);
            }
            writer.close();
            //Log.i("AddDataToFile","File Size = "+fileIn.length());
            /*
            if(fileIn.length()>= Storage_Threshold)
            {
                File_First_Write = true;
                new FileUpload().execute("");
                //Log.i("AddDataToFile","Main thread End");
            }
            */
        } catch (IOException e) {
            Log.e("AddDataToFile", "Unable to write to the file.");
        }
    }

    public class FileUpload extends AsyncTask<String,String,File> {

        protected File doInBackground(String... message) {
            File fileIn;
            fileIn = new File(File_Path, File_Saved_Name + ".csv");
            if(!fileIn.exists())
            {
                Log.i("FileUpload","File Open Error");
                return null;
            }
            File_Saved_Name ="";
            uploadFile(fileIn);
            //Log.i("AddDataToFile","Runnable End");
            return null;
        }
    }

    public void uploadFile(File fileName){
        FTPClient client = new FTPClient();

        try {
            Log.i("uploadFile","Conn Status"+Net_Conn_Status);
            if(Net_Conn_Status == CELLULAR_CONNECTED) {
                client.setPassive(false);
                Log.e("uploadFile","Cellular Network Active Connection");
            }
            else if(Net_Conn_Status == WIFI_CONNECTED){
                client.setPassive(true);
                Log.e("uploadFile","Wifi Network Passive Connection");
            }
            else
            {
                CheckNetworkConnection();
                if(Net_Conn_Status == NOT_CONNECTED)
                {
                    //Toast.makeText(this, "Not connected to any network", Toast.LENGTH_SHORT).show();
                    Log.e("uploadFile","Not connected to any network");
                    return;
                }
            }
            client.connect(FTP_HOST,21);
            //client.connect(FTP_HOST_LINK);
            client.login(FTP_USER, FTP_PASS);
            client.setType(FTPClient.TYPE_BINARY);
            client.changeDirectory("/upload/");
            client.upload(fileName, new MyTransferListener());
            Log.i("uploadFile","File upload started");

        } catch (Exception e) {
            e.printStackTrace();
            Log.i("uploadFile","exception e = "+e);
            try {
                client.disconnect(true);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    /*******  Used to file upload and show progress  **********/

    public class MyTransferListener implements FTPDataTransferListener {
        public void started() {
            //btn.setVisibility(View.GONE);
            // Transfer started
            //Toast.makeText(getBaseContext(), " Upload Started ...", Toast.LENGTH_SHORT).show();
            Log.i("MyTransferListener"," Upload Started ...");
            //System.out.println(" Upload Started ...");
        }

        public void transferred(int length) {
            // Yet other length bytes has been transferred since the last time this
            // method was called
            //Toast.makeText(getBaseContext(), " transferred ..." + length, Toast.LENGTH_SHORT).show();
            Log.i("MyTransferListener"," Transferred ..." + length);
            //System.out.println(" transferred ..." + length);
        }

        public void completed() {
            //btn.setVisibility(View.VISIBLE);
            // Transfer completed

            //Toast.makeText(getBaseContext(), " completed ...", Toast.LENGTH_SHORT).show();
            File filein = new File(File_Path,File_Del_Name+".csv");
            boolean Delete = filein.delete();
            Log.i("MyTransferListener"," Upload Completed ...Del = "+Delete);
            //System.out.println(" completed ..." );
        }

        public void aborted() {
           // btn.setVisibility(View.VISIBLE);
            // Transfer aborted
            //Toast.makeText(getBaseContext()," transfer aborted , please try again...", Toast.LENGTH_SHORT).show();
            File filein = new File(File_Path,File_Del_Name+".csv");
            boolean Delete = filein.delete();
            Log.i("MyTransferListener"," Transfer Aborted ...Del = "+Delete);
            //System.out.println(" aborted ..." );
        }

        public void failed() {
            //btn.setVisibility(View.VISIBLE);
            // Transfer failed
            Log.i("MyTransferListener"," Upload Failed ...");
            //System.out.println(" failed ..." );
        }
    }


/*
    public class connectTask extends AsyncTask<String,String,TCPClient> {

        @Override
        protected TCPClient doInBackground(String... message) {

            //we create a TCPClient object and
            mTcpClient = new TCPClient(new TCPClient.OnMessageReceived() {
                @Override
                //here the messageReceived method is implemented
                public void messageReceived(String message) {
                    //this method calls the onProgressUpdate
                    //publishProgress(message);
                    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                }
            });
            mTcpClient.run();

            return null;
        }
    }
*/
    protected void onPause() {
        super.onPause();
    }

    protected void onResume() {
        CheckNetworkConnection();
        super.onResume();
    }
/*

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
     // Inflate the menu; this adds items to the action bar if it is present.
     getMenuInflater().inflate(R.menu.menu_main, menu);
     return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
     // Handle action bar item clicks here. The action bar will
     // automatically handle clicks on the Home/Up button, so long
     // as you specify a parent activity in AndroidManifest.xml.

     int id = item.getItemId();

     //noinspection SimplifiableIfStatement
     if (id == R.id.action_settings) {
         return true;
     }
     return super.onOptionsItemSelected(item);
    }
*/

}