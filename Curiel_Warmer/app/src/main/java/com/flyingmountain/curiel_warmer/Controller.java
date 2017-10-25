package com.flyingmountain.curiel_warmer;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import co.lujun.lmbluetoothsdk.BluetoothLEController;
import co.lujun.lmbluetoothsdk.base.BluetoothLEListener;

/**
 * Created by pancake on 2017-05-04.
 */

public class Controller {
    private static final String READ_CHARACTERISTIC_ID = "00000011-0000-1000-8000-00805F9B34FB";
    private static final String WRITE_CHARACTERISTIC_ID = "00000011-0000-1000-8000-00805F9B34FB";
    private static final String TAG = "BT_LISTNER";
    static private Context context;
    static private BluetoothLEController mBLEController = null;
    static private Handler handler;

    public Controller(Context context){
        this.context = context;
    }

    public static BluetoothLEController getBleController(Context context, BluetoothLEListener mBluetoothLEListener) {
        if (mBLEController == null) {
            mBLEController = BluetoothLEController.getInstance().build(context);
            mBLEController.setBluetoothListener(mBluetoothLEListener);

        }
        return mBLEController;
    }

    public static BluetoothLEController getBleController() {
        if (mBLEController != null) {
            return mBLEController;
        }else{
            Toast.makeText(context,"Activity is not initiated", Toast.LENGTH_SHORT);
        }
        return null;
    }

    public static Handler getDataPasser() {
        if (handler != null) {
            handler = new Handler();
        }
        return handler;
    }


    public static void bleScan(){
        mBLEController.startScan();
    }
    public static void bleDisconnect(){
        mBLEController.disconnect();
    }
    public static void bleReconnect(){
        mBLEController.reConnect();
    }
    public void writeBytes(byte [] bytes){
        mBLEController.write(bytes);
    }
    public void connectDevice(String deviceMac) {
        mBLEController.connect(deviceMac);
    }
}
