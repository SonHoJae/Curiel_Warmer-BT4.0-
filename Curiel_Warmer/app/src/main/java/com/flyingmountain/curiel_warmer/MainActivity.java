package com.flyingmountain.curiel_warmer;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Handler;
import android.support.annotation.BoolRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import co.lujun.lmbluetoothsdk.BluetoothLEController;
import co.lujun.lmbluetoothsdk.base.BluetoothLEListener;

public class MainActivity extends FragmentActivity {

    LocationManager mLM;

    //MARK: Values
    private static final String READ_CHARACTERISTIC_ID = "00000011-0000-1000-8000-00805F9B34FB";
    private static final String WRITE_CHARACTERISTIC_ID = "00000011-0000-1000-8000-00805F9B34FB";
    private static final int MAX_TEMPERATURE = 50;
    private static final int MIN_TEMPERATURE = 37;

    private final String TAG = "BLUETOOTH";
    private static Handler handler;
    private BluetoothLEListener mBluetoothLEListener = new BluetoothLEListener() {

        @Override
        public void onDiscoveringCharacteristics(final List<BluetoothGattCharacteristic> characteristics) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (BluetoothGattCharacteristic characteristic : characteristics) {
                        Log.d(TAG, "onDiscoveringCharacteristics - characteristic : " + characteristic.getUuid());
                    }

                }
            });
        }

        @Override
        public void onDiscoveringServices(final List<BluetoothGattService> services) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (BluetoothGattService service : services) {
//                        Log.d(TAG, "onDiscoveringServices - service : " + service.getUuid());
                    }

                }
            });
        }

        @Override
        public void onReadData(final BluetoothGattCharacteristic characteristic) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, Arrays.toString(characteristic.getValue()) + "\n");
                }
            });
        }

        @Override
        public void onWriteData(final BluetoothGattCharacteristic characteristic) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, Arrays.toString(characteristic.getValue()) + "\n");
                }
            });
        }

        @Override
        public void onDataChanged(final BluetoothGattCharacteristic characteristic) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    /*
                    byte[] getStatus = characteristic.getValue();
                    Log.i(TAG,Arrays.toString(getStatus)+"finaltest");
                    String temp = Arrays.toString(getStatus);

                    Log.i(TAG, "Read" + temp + " " + dataHandler.getChecksum(getStatus) + "\n");
                    if (getStatus[0] == 0x02 && getStatus[1] == 0x40 && getStatus[5] == 0x03 && dataHandler.getChecksum(getStatus) == getStatus[6]) {
                        if (getStatus[2] == 0x31) {
                            onPower();
                        } else {
                            offPower();
                        }
                        temperature_status.setText(String.valueOf(getStatus[3]) + "\u00B0C");
                        battery_status.setText(String.valueOf(getStatus[4]) + "%");
                    }

                    Log.i(TAG, "Read" + temp + "\n");*/
                }
            });
        }

        @Override
        public void onActionStateChanged(int preState, int state) {
            Log.i(TAG, "onActionStateChanged: " + state);
        }

        @Override
        public void onActionDiscoveryStateChanged(String discoveryState) {
            if (discoveryState.equals(BluetoothAdapter.ACTION_DISCOVERY_STARTED)) {
                Toast.makeText(MainActivity.this, "scanning!", Toast.LENGTH_SHORT).show();
            } else if (discoveryState.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)) {
                Toast.makeText(MainActivity.this, "scan finished!", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onActionScanModeChanged(int preScanMode, int scanMode) {
            Log.i(TAG, "onActionScanModeChanged:  " + scanMode);
        }

        @Override
        public void onBluetoothServiceStateChanged(final int state) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String conn_state = Utils.transConnStateAsString(state);
                    Log.i(TAG, "Conn state: " + conn_state);
                    if (conn_state.contains("CONNECTED") && !conn_state.equals("DISCONNECTED")) {
                        isConnected = true;
                        isPowerOn = true;
                        btn_connect.setBackgroundColor(Color.GRAY);
                        btn_on.setBackgroundResource(R.drawable.button_on);
                        btn_off.setBackgroundResource(R.drawable.gray_off);
                    } else {
                        isConnected = false;
                        isPowerOn = false;
                        btn_connect.setBackgroundResource(android.R.drawable.btn_default);
                        btn_on.setBackgroundResource(R.drawable.gray_on);
                        btn_off.setBackgroundResource(R.drawable.button_off);
                    }

                }
            });
        }

        @Override
        public void onActionDeviceFound(final BluetoothDevice device, short rssi) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    hardwareIist_Set.add(device.getName() + "@" + device.getAddress());
                    //Log.i(TAG, String.valueOf(hardwareIist_Set.size()));
                    int i = 0;

                    hardware_list.addAll(hardwareIist_Set);
                }
            });
        }
    };
    private BluetoothLEController mBLEController = null;
    DataHandler dataHandler;
    Controller bluetoothContoller;
    byte requiredTemperature = 0x28;

    private List<String> hardware_list;
    private HashSet<String> hardwareIist_Set;
    Button btn_connect;
    ImageButton btn_on;
    ImageButton btn_off;
    ImageButton btn_up;
    ImageButton btn_down;
    ProgressBar progressbar_control_temp;
    TextView battery_status;
    TextView temperature_status;
    TextView tempView;

    private Boolean isPowerOn = false;
    private Boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        registerLocationUpdates();
        dataHandler = new DataHandler();
        bluetoothContoller = new Controller(this);
        if (mBLEController != null) {
            mBLEController.release();
        }
        mBLEController = bluetoothContoller.getBleController(this, mBluetoothLEListener);
        if (!mBLEController.isSupportBLE()) {
            Toast.makeText(MainActivity.this, "Unsupport BLE!", Toast.LENGTH_SHORT).show();
            finish();
        }

        mBLEController.setReadCharacteristic(READ_CHARACTERISTIC_ID);
        mBLEController.setWriteCharacteristic(WRITE_CHARACTERISTIC_ID);
        hardware_list = new ArrayList<String>();
        hardwareIist_Set = new HashSet<String>();
        btn_connect = (Button) findViewById(R.id.btn_connect);
        btn_on = (ImageButton) findViewById(R.id.btn_on);
        btn_off = (ImageButton) findViewById(R.id.btn_off);
        btn_up = (ImageButton) findViewById(R.id.btn_up);
        btn_down = (ImageButton) findViewById(R.id.btn_down);
        progressbar_control_temp = (ProgressBar) findViewById(R.id.progressbar_control_temp);
        progressbar_control_temp.setMax(MAX_TEMPERATURE - MIN_TEMPERATURE);
        progressbar_control_temp.setProgress(requiredTemperature - MIN_TEMPERATURE);
        temperature_status = (TextView) findViewById(R.id.txtView_Temperature);
        battery_status = (TextView) findViewById(R.id.txtView_battery);
        tempView = (TextView) findViewById(R.id.txtView_set_temp);
        initClient();

    }


    void initClient() {

        btn_on.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected && dataHandler != null) {
                    onPower();
                    Log.i("testtest", Arrays.toString(dataHandler.powerOnData()));
                } else {
                    Toast.makeText(MainActivity.this, "Device is currently disconnected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_off.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected && dataHandler != null) {
                    offPower();
                } else {
                    Toast.makeText(MainActivity.this, "Device is currently disconnected", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btn_up.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected && isPowerOn && requiredTemperature <= MAX_TEMPERATURE) {
                    progressbar_control_temp.setProgress(progressbar_control_temp.getProgress() + 1);
                    //bleClient.setCharacteristicValue(communicationServiceUuid, sliderCharacteristicUuid,  dataHandler.setTemperature(requiredTemperature));
                    mBLEController.write(dataHandler.setTemperature(++requiredTemperature));
                    tempView.setText(String.valueOf(requiredTemperature) + "\u00B0C");
                } else if (requiredTemperature > MAX_TEMPERATURE) {
                    Toast.makeText(MainActivity.this, "Maximum Temperature", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Power is off", Toast.LENGTH_SHORT).show();
                }
            }
        });
        btn_down.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected && isPowerOn && requiredTemperature >= MIN_TEMPERATURE) {
                    progressbar_control_temp.setProgress(progressbar_control_temp.getProgress() - 1);
                    //bleClient.setCharacteristicValue(communicationServiceUuid, sliderCharacteristicUuid,  dataHandler.setTemperature(requiredTemperature));
                    mBLEController.write(dataHandler.setTemperature(--requiredTemperature));
                    tempView.setText(String.valueOf(requiredTemperature) + "\u00B0C");
                } else if (requiredTemperature < MIN_TEMPERATURE) {
                    Toast.makeText(MainActivity.this, "Minimum Temperature", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Power is off", Toast.LENGTH_SHORT).show();
                }
            }
        });

        mBLEController = Controller.getBleController(this, mBluetoothLEListener);
        Log.i(TAG, "CONTROLLER " + String.valueOf(mBLEController));


        btn_connect.setEnabled(true);
        btn_connect.setOnClickListener(null);
        btn_connect.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (mBLEController.startScan()) {
                    Toast.makeText(MainActivity.this, "Scanning!", Toast.LENGTH_SHORT).show();
                    BluetoothListFragment bluetoothListFragment = new BluetoothListFragment();
                    Bundle b = new Bundle();
                    hardware_list.clear();
                    b.putStringArrayList("hardwarelist", (ArrayList<String>) hardware_list);
                    bluetoothListFragment.setArguments(b);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.activity_main, bluetoothListFragment)
                            .addToBackStack("fragment")
                            .commit();

                }
            }
        });

    }


    public void onPower() {
        if (!isPowerOn && dataHandler.powerOnData() != null) {
            try {
                mBLEController.write(dataHandler.powerOnData());
            } catch (NullPointerException e) {
                Log.e("Error", e.getMessage());
            }
            btn_off.setBackgroundResource(R.drawable.gray_off);
            btn_on.setBackgroundResource(R.drawable.button_on);
            isPowerOn = true;
            Toast.makeText(getBaseContext(), "POWER ON", Toast.LENGTH_SHORT).show();
        }
    }

    public void offPower() {
        if (isPowerOn && dataHandler.powerOffData() != null) {
            try {
                mBLEController.write(dataHandler.powerOffData());
            } catch (NullPointerException e) {
                Log.e("Error", e.getMessage());
            }
            btn_on.setBackgroundResource(R.drawable.gray_on);
            btn_off.setBackgroundResource(R.drawable.button_off);
            isPowerOn = false;
            Toast.makeText(getBaseContext(), "POWER OFF", Toast.LENGTH_SHORT).show();
        }
    }

    public void onBackPressed() {
        //Log.i(TAG,"onBackPressed "+String.valueOf(getFragmentManager().getBackStackEntryCount()));
        if (getSupportFragmentManager().getBackStackEntryCount() > 0)
            getSupportFragmentManager().popBackStack();
        else {
            super.onBackPressed();
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBLEController.release();
        btn_connect.setBackgroundResource(android.R.drawable.btn_default);
        isConnected = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void registerLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i("Permission","permission test");
            ActivityCompat.requestPermissions(this, new String[] {
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    1);
            return;
        }else {
            Toast.makeText(this,"location permissinon required",Toast.LENGTH_SHORT).show();
        }
    }

}

