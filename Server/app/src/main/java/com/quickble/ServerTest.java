package com.quickble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.quickble.ble.BLEServer;
import com.quickble.R;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ServerTest extends AppCompatActivity implements BLEServer.BLEServerDelegate, Values{

	BluetoothGattService service;
	BluetoothGattCharacteristic characteristic;
	BluetoothGattDescriptor descriptor;

	TextView outText;

	BLEServer server;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server_test);

		outText = (TextView) findViewById(R.id.outText);
	}

	@Override
	protected void onStop() {
		super.onStop();
		server.stopServer();
		server = null;
		service = null;
		characteristic = null;
		descriptor = null;
	}

	@Override
	protected void onStart() {
		super.onStart();
		((BluetoothManager)getSystemService(BLUETOOTH_SERVICE)).getAdapter().enable();
		server = new BLEServer(ServerTest.this, this);
		descriptor = server.buildDescriptor(descUuid, BLEServer.DescPermissions.Read | BLEServer.DescPermissions.Write);
		characteristic = server.buildCharacteristic(charUuid, new BluetoothGattDescriptor[]{descriptor}, BLEServer.CharProperties.Read | BLEServer.CharProperties.Write | BLEServer.CharProperties.Notify, BLEServer.CharPermissions.Read | BLEServer.CharPermissions.Write);
		service = server.buildService(serviceUuid, BLEServer.ServiceType.Primary, new BluetoothGattCharacteristic[]{characteristic});
		server.addService(service);
		server.startServer();
		server.setCharacteristicValue(serviceUuid, charUuid, "Test data", true);
		server.setDescriptorValue(serviceUuid, charUuid, descUuid, "Test Characteristic");
	}

	void showToast(final String message){
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(ServerTest.this, message, Toast.LENGTH_LONG).show();
			}
		});
	}

	//MARK: Server Delegate
	@Override
	public void onAdvertise(BLEServer.AdvertiseError error) {
		showToast("Advertise result " + error.name());
	}

	@Override
	public void onDeviceConnected(BluetoothDevice device) {
		showToast(device.getName() + " connected");
	}

	@Override
	public void onDeviceDisconnected(BluetoothDevice device) {
		showToast(device.getName() + " disconnected");
	}

	@Override
	public void onCharacteristicChangedServer(final BluetoothGattCharacteristic characteristic) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				outText.setText("Characteristic\nValue: " + server.getCharacteristicValueString(serviceUuid, charUuid, 0));
			}
		});
	}

	@Override
	public void onDescriptorChanged(final BluetoothGattDescriptor descriptor) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				outText.setText("Descriptor\nValue: " + server.getDescriptorValueString(serviceUuid, charUuid, descUuid));
			}
		});
	}

	public String bytesToString(byte[] input){
		String rtn = "[";
		for(byte b : input){
			rtn += (b & 0xFF);
			rtn += ",";
		}
		rtn = rtn.substring(0, rtn.length() - 1) + "]";
		return rtn;
	}

}
