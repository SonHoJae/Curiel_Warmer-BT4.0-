package com.quickble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.quickble.ble.BLEClient;
import com.quickble.ble.BLEServer;
import com.quickble.R;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener, BLEServer.BLEServerDelegate, BLEClient.BLEClientDelegate, Values {

	//MARK: Values
	private final int REQUEST_ENABLE_BLUETOOTH_FOR_SERVER = 2;
	private final int REQUEST_ENABLE_BLUETOOTH_FOR_CLIENT = 3;

	//MARK: UI Elements
	SeekBar slider;
	ListView listView;
	TextView deviceCount;
	TextView dataCoummunication;
	ListView devices;
	Button btn_on;
	EditText edtext_temperature;
	EditText edtext_battery;
	Button btn_send;
	ArrayAdapter<String> deviceNames;
	ArrayList<BluetoothDevice> deviceList;
	ArrayList<String> deviceAddresses;

	//MARK: Bluetooth
	BLEServer bleServer;
	BLEClient bleClient;
	BluetoothGattService comService;
	BluetoothGattCharacteristic sliderCharacteristic;

	//MARK: Activity
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		deviceCount = (TextView) findViewById(R.id.lbl_device_count);
		dataCoummunication = (TextView)findViewById(R.id.DataCommuication);
		slider = (SeekBar) findViewById(R.id.seekBar);
		listView = (ListView) findViewById(R.id.lst_main_options);
		deviceList = new ArrayList<>();
		deviceAddresses = new ArrayList<>();
		deviceNames = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1);
		devices = (ListView) findViewById(R.id.lst_discovered_devices);
		devices.setAdapter(deviceNames);
		listView.setOnItemClickListener(this);

		btn_on = (Button)findViewById(R.id.btn_on);
		btn_on.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				if(btn_on.getText().toString().equals("on"))
					btn_on.setText("off");
				else
					btn_on.setText("on");
			}
		});
		edtext_temperature = (EditText)findViewById(R.id.edtext_temperature);
		edtext_battery = (EditText)findViewById(R.id.edtext_battery);
		btn_send = (Button)findViewById(R.id.btn_send);
		btn_send.setOnClickListener(new View.OnClickListener() {
			@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
			@Override
			public void onClick(View v) {
				byte[] serverData = new byte[7];
				serverData[0] = 0x02;
				serverData[1] = 0x40;
				if(btn_on.getText().toString().equals("on"))
					serverData[2] = 0x31;
				else
					serverData[2] = 0x32;
				serverData[3] = (byte)Integer.parseInt(edtext_temperature.getText().toString());
				serverData[4] = (byte)Integer.parseInt(edtext_battery.getText().toString());
				serverData[5] = 0x03;
				serverData[6] =(byte)(serverData[0] + serverData[1] + serverData[2] + serverData[3] + serverData[4]+ serverData[5]);
				bleServer.setCharacteristicValue(comService,sliderCharacteristic,serverData,true);
			}
		});


		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			bleServer = new BLEServer(MainActivity.this, this);
			sliderCharacteristic = bleServer.buildCharacteristic(sliderCharacteristicUuid, new BluetoothGattDescriptor[0],
					BLEServer.CharProperties.Read | BLEServer.CharProperties.Write | BLEServer.CharProperties.Notify,
					BLEServer.CharPermissions.Read | BLEServer.CharPermissions.Write);
			comService = bleServer.buildService(sliderCharacteristicUuid,
					BLEServer.ServiceType.Primary, new BluetoothGattCharacteristic[]{sliderCharacteristic});
			bleServer.addService(comService);
		}

		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
			bleClient = new BLEClient(MainActivity.this, this);
			bleClient.setUseNewMethod(false); //This requires more work with permissions than the old method

		}

	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()){
			case R.id.mnu_server_test:
				startActivity(new Intent(MainActivity.this, ServerTest.class));
				return true;
			case R.id.mnu_client_test:
				startActivity(new Intent(MainActivity.this, ClientTest.class));
				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_ENABLE_BLUETOOTH_FOR_SERVER && resultCode == RESULT_OK){
			initServer();
		}
		if(requestCode == REQUEST_ENABLE_BLUETOOTH_FOR_CLIENT && resultCode == RESULT_OK){
			initClient();
		}
	}
	@Override
	protected void onStop() {
		super.onStop();
		doStop();
	}
	private void doStop(){
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
			bleServer.stopServer();
		}
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
			bleClient.stopScanning();
		}
		deviceCount.setText("Stopped");
		devices.setOnItemClickListener(null);
		slider.setOnSeekBarChangeListener(null);
		for(int i = deviceList.size() - 1; i >= 0; i--) {
			final int index = i;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					deviceNames.remove(deviceNames.getItem(index));
				}
			});
			deviceList.remove(i);
		}
	}
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		if(position == getResources().getInteger(R.integer.app_mode_server)){
			initServer();
		}else if(position == getResources().getInteger(R.integer.app_mode_client)){
			initClient();
		}else if(position == getResources().getInteger(R.integer.app_mode_stop)){
			doStop();
		}
	}

	//MARK: Bluetooth functions
	void initServer(){
		for(int i = deviceList.size() - 1; i >= 0; i--) {
			final int index = i;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					deviceNames.remove(deviceNames.getItem(index));
				}
			});
			deviceList.remove(i);
		}
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			BLEServer.BtError error = bleServer.startServer();
			if(error != BLEServer.BtError.None){
				handleServerError(error);
			}else{
				Log.i("test","test");
				slider.setEnabled(true);
				slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
						Log.i("test","TTTT");}
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {}
					@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						byte b[] = new byte[4];
						b[0] = 1;
						b[1] = 1;
						b[2] = 20;
						b[3] = 20;
						//bleServer.setCharacteristicValue(comService, sliderCharacteristic,b,true);
						bleServer.setCharacteristicValue(comService,sliderCharacteristic,b,true);
						Log.i("test",Arrays.toString(b));
						//bleServer.setCharacteristicValue(comService, sliderCharacteristic, "haha", true);
						//bleServer.setCharacteristicValue(comService, sliderCharacteristic, 'a', BLEServer.CharFormat.UInt8, 0, true);
					}
				});
				devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
					@Override
					public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
						bleServer.disconnectDevice(deviceList.get(position));
						deviceList.remove(position);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								deviceNames.remove(deviceNames.getItem(position));
							}
						});
					}
				});
				deviceCount.setText("Connected Devices: 0");
				bleServer.setCharacteristicValue(comService, sliderCharacteristic, slider.getProgress(), BLEServer.CharFormat.UInt8, 0, true);
			}
		}else{
			Toast.makeText(MainActivity.this, "Server mode not supported before Android 5.0", Toast.LENGTH_LONG).show();
		}
	}
	public void handleServerError(BLEServer.BtError error){
		if(error == BLEServer.BtError.Disabled)
			startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH_FOR_SERVER);
		else
			Toast.makeText(MainActivity.this, "Error: " + error.toString(), Toast.LENGTH_LONG).show();
	}

	void initClient(){
		for(int i = deviceList.size() - 1; i >= 0; i--) {
			final int index = i;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					deviceNames.remove(deviceNames.getItem(index));
				}
			});
			deviceList.remove(i);
		}
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
			BLEClient.BtError error = bleClient.scanForDevices();
				if(error != BLEClient.BtError.None){
					handleClientError(error);
			}else{
					Log.i("test","test");
				slider.setEnabled(true);
				slider.setOnSeekBarChangeListener(null);
				slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onProgressChanged(SeekBar seekBar, int i, boolean b) {}
					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {}
					@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
						byte b[] = new byte[4];
						b[0] = 1;
						b[1] = 1;
						b[2] = 20;
						b[3] = 20;
						Log.i("test",Arrays.toString(b));
						//bleServer.setCharacteristicValue(comService, sliderCharacteristic,b,true);
						bleServer.setCharacteristicValue(comService, sliderCharacteristic, slider.getProgress(), BLEServer.CharFormat.UInt8, 0, true);
						//bleServer.setCharacteristicValue(comService, sliderCharacteristic, b,true);
					}
				});
				devices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
					@Override
					public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
						Log.i("Names/Devices", deviceNames.getCount() + "/" + deviceList.size());
						bleClient.connectToDevice(deviceList.get(position));
					}
				});
				deviceCount.setText("Scanning for devices...");
				bleClient.setCharacteristicValue(communicationServiceUuid, sliderCharacteristicUuid, slider.getProgress(), BLEClient.CharFormat.UInt8, 0);
			}
		}
	}
	void handleClientError(BLEClient.BtError error){
		if(error == BLEClient.BtError.Disabled)
			startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BLUETOOTH_FOR_CLIENT);
		else
			Toast.makeText(MainActivity.this, "Error: " + error.toString(), Toast.LENGTH_LONG).show();
	}

	//MARK: BLEServerDelegate
	@Override
	public void onAdvertise(BLEServer.AdvertiseError errorCode) {
		switch(errorCode){
			case None:
				Toast.makeText(MainActivity.this, "Advertising Started", Toast.LENGTH_SHORT).show();
			case AlreadyStarted:
				Toast.makeText(MainActivity.this, "Advertise Error: Already Started", Toast.LENGTH_SHORT).show();
				break;
			case DataTooLarge:
				Toast.makeText(MainActivity.this, "Advertise Error: Too Large", Toast.LENGTH_SHORT).show();
				break;
			case FeatureUnsupported:
				Toast.makeText(MainActivity.this, "Advertise Error: Feature Unsupported", Toast.LENGTH_SHORT).show();
				break;
			case InternalError:
				Toast.makeText(MainActivity.this, "Advertise Error: Internal Error", Toast.LENGTH_SHORT).show();
				break;
			case TooManyAdvertisers:
				Toast.makeText(MainActivity.this, "Advertise Error: Too Many Advertisers", Toast.LENGTH_SHORT).show();
				break;
			default:
				Toast.makeText(MainActivity.this, "Advertise Error: No Error Specified", Toast.LENGTH_SHORT).show();
				break;
		}
	}
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onDeviceConnected(final BluetoothDevice device) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if(!deviceAddresses.contains(device.getAddress())) {
					deviceCount.setText("Connected Devices: " + bleServer.getConnectedDevices().size());
					deviceList.add(device);
					deviceAddresses.add(device.getAddress());
					String name = device.getName();
					if (name == null)
						name = "Unknown Device";
					deviceNames.add(name);
				}
			}
		});
	}
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onDeviceDisconnected(BluetoothDevice device) {
		int i = deviceList.indexOf(device);
		if(i >= 0) {

			final int index = i;
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					deviceNames.remove(deviceNames.getItem(index));
					deviceList.remove(index);
					deviceAddresses.remove(index);
				}
			});
		}
		deviceCount.setText("Connected Devices: " + bleServer.getConnectedDevices().size());
	}
	@Override
	public void onDescriptorChanged(BluetoothGattDescriptor descriptor) {}
	@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
	@Override
	public void onCharacteristicChangedServer(BluetoothGattCharacteristic characteristic) {
			Log.i("Changed", characteristic.getUuid().toString());
			if(characteristic.getUuid().equals(sliderCharacteristicUuid)){

				final String temp = Arrays.toString(bleServer.getCharacteristicValue(communicationServiceUuid, sliderCharacteristicUuid));
				Log.i("HEYTEST", temp);
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						dataCoummunication.setText(temp);
					}
				});
			}
	}

	//MARK: BLEClientDelegate
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	public void onConnect(BluetoothDevice device) {
		deviceCount.setText("Connected to " + device.getName() + "(" + device.getAddress() + ")");
		bleClient.stopScanning();
	}
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	public void onDeviceFound(final BluetoothDevice device, int rssi, byte[] scanRecord) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (!deviceAddresses.contains(device.getAddress())) {
					deviceAddresses.add(device.getAddress());
					deviceList.add(device);
					String name = device.getName();
					if (name == null)
						name = "Unknown Device";
					deviceNames.add(name);
				}
			}
		});
	}
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	public void onScanFailed(BLEClient.ScanError errorCode) {
		switch (errorCode){
			case AlreadyStarted:
				Toast.makeText(MainActivity.this, "Error: Scan Already Started", Toast.LENGTH_SHORT).show();
				break;
			case AppRegistrationFailed:
				Toast.makeText(MainActivity.this, "Error: App registration Failed", Toast.LENGTH_SHORT).show();
				break;
			case FeatureUnsupported:
				Toast.makeText(MainActivity.this, "Error: Feature Unsupported", Toast.LENGTH_SHORT).show();
				break;
			case InternalError:
				Toast.makeText(MainActivity.this, "Error: Internal Error", Toast.LENGTH_SHORT).show();
				break;
			default:
				Toast.makeText(MainActivity.this, "Error: No Error Specified", Toast.LENGTH_SHORT).show();
				break;
		}
	}
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	public void onCharacteristicChangedClient(BluetoothGattCharacteristic characteristic) {
		if(characteristic.getUuid().equals(sliderCharacteristicUuid)){
			int a = bleClient.getCharacteristicValueInt(communicationServiceUuid, sliderCharacteristicUuid, BluetoothGattCharacteristic.FORMAT_UINT8, 0);
			Log.i("test","test "+String.valueOf(a));
			slider.setProgress(a);
		}
	}
	@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
	@Override
	public void onServicesDiscovered(List<BluetoothGattService> services) {
		BluetoothGattService service = null;
		for(BluetoothGattService s : services){
			if(s.getUuid().equals(communicationServiceUuid))
				service = s;
		}
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(sliderCharacteristicUuid);
		bleClient.receiveNotifications(characteristic, true);
	}
	@Override
	public void onDisconnected() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				initClient();
			}
		});
	}
}
