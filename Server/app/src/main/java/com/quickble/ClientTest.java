package com.quickble;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.quickble.ble.BLEClient;
import com.quickble.R;

import java.util.ArrayList;
import java.util.List;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class ClientTest extends AppCompatActivity implements BLEClient.BLEClientDelegate, Values{


	//MARK: Bluetooth
	BLEClient client;

	//MARK: UI Elements
	ListView listView;
	TextView outText;

	ArrayAdapter<String> bluetoothDeviceNames;
	ArrayList<BluetoothDevice> devicesList = new ArrayList<>();
	ArrayList<String> deviceAddresses = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_client_test);

		listView = (ListView) findViewById(R.id.listView);
		outText = (TextView) findViewById(R.id.outText);

		bluetoothDeviceNames = new ArrayAdapter<>(ClientTest.this, android.R.layout.simple_list_item_1);
		listView.setAdapter(bluetoothDeviceNames);
		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
				client.connectToDevice(devicesList.get(i));
			}
		});

	}

	@Override
	protected void onStart() {
		super.onStart();
		client = new BLEClient(ClientTest.this, this);
		client.setUseNewMethod(false);
		client.scanForDevices();
	}

	@Override
	protected void onStop() {
		super.onStop();
		client.stopScanning();
		client = null;
	}

	//BLEClientDelegate
	@Override
	public void onConnect(BluetoothDevice device) {
		client.stopScanning();
	}
	@Override
	public void onServicesDiscovered(final List<BluetoothGattService> services) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				String data = "";
				for(BluetoothGattService service : services){
					data += service.getUuid().toString().toUpperCase();
				}
			}
		});
	}
	@Override
	public void onDeviceFound(final BluetoothDevice device, int rssi, byte[] scanRecord) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(!deviceAddresses.contains(device.getAddress())) {
					devicesList.add(device);
					deviceAddresses.add(device.getAddress());
					String name = device.getName();
					bluetoothDeviceNames.add(name == null ? "Unknown Device" : name);
					bluetoothDeviceNames.notifyDataSetChanged();
				}
			}
		});
	}
	@Override
	public void onScanFailed(BLEClient.ScanError error) {
		//Will never be called using old(API18) method
	}
	@Override
	public void onCharacteristicChangedClient(BluetoothGattCharacteristic characteristic) {	}
	@Override
	public void onDisconnected() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				devicesList.clear();
				deviceAddresses.clear();
				bluetoothDeviceNames.clear();
				bluetoothDeviceNames.notifyDataSetChanged();
			}
		});
		client.scanForDevices();
	}
}
