# Quick-Bluetooth-LE
Quick Bluetooth LE Simple API with example, to make creating a Bluetooth Low Energy (BLE) server (peripheral) and Client (central) easier on Android devices. This is not a stand alone library and does require a device with normal android BLE support.

### Bluetooth Low Energy
Bluetooth Low Energy (BLE) uses three main objects. Services, characteristics, and descriptors. Each has its own UUID (usually in the form of 0000xxxx--0000-1000-8000-00805f9b34fb). Characteristics added to services and descriptors to characteristics. Characteristics contain data (the useful info) and descriptors give a human readable description of the characteristic.
The two key roles a device can play in BLE interactions are the Server (usually the peripheral device) or the client (usually the central device such as a phone). The Server has data and the client receives and interprets/handles the data. Only some android devices can act as a server. In order to function as a BLE (Gatt) server a device must run android lollipop or newer and have hardware that can support a Gatt Server. In order to function as a client, a device must run android Jellybean 4.3 (API level 18) and have Bluetooth 4.0 hardware.
### Quick BLE Usage
There are two main classes that will be used in the Quick BLE library, BLEServer for acting as a server and BLEClient for acting as a client. Below is the basic usage. The example in this repository is far more extensive.
#### Server
Start the server in the activity's onStart method and stop it in the activity's onStop method to ensure correct behavior.
~~~~
import com.quick.ble.BLEServer;
public class MyServer implements BLEServer.BLEServerDelegate{
    BlEServer server = new BLEServer(context, this/*delegate*/);

    public MyServer(){
        //Create one service with a single characteristic and add the service to the server
		char = server.buildCharacteristic(charUuid, new BluetoothGattDescriptor[0], BLEServer.CharProperties.Read | BLEServer.CharProperties.Write | BLEServer.CharProperties.Notify, BLEServer.CharPermissions.Read | BLEServer.CharPermissions.Write);
			service = bleServer.buildService(serviceUuid, BLEServer.ServiceType.Primary, new BluetoothGattCharacteristic[]{sliderCharacteristic}); //Array of Characteristics will all be added to service
			server.addService(comService);
			server.startServer(); //This will return any error codes and should be handled
			server.setCharacteristicValue(serviceUuid,characteristicUuid, "Testing char", true); //Set value to characteristic. true at end will notify connected devices that this characteristic has changed
    }

    //BLEServerDelegate
    @Override
	public void onAdvertise(BLEServer.AdvertiseError errorCode) {
		//Handle any error from the advertiser
	}
	@Override
	public void onDeviceConnected(final BluetoothDevice device) {
		//Handle when a device connects
	}
	@Override
	public void onDeviceDisconnected(BluetoothDevice device) {
		//Handle when a device disconnects
	}
	@Override
	public void onDescriptorChanged(BluetoothGattDescriptor descriptor) {
	    //Client wrote value to descriptor
	}
	@Override
	public void onCharacteristicChangedServer(BluetoothGattCharacteristic characteristic) {
		//Client wrote value to characteristic
	}
}
~~~~
#### Cient
Android has been able to function as a Gatt client sense 4.3 Jellybean, however a newer method was added in Lollipop (5.0). Quick bluetooth LE allows selection of a method on device that support the newer method, however it important to know that on device running android Marshmallow (6.0) or newer, the new method requires location permissions. In addition to requiring the permissions in the manifest, the permissions must be [requested at runtime](https://developer.android.com/training/permissions/requesting.html) and location must be turned on. It is recommended to use the old method for compatibility with all devices that support Bluetooth LE. As with the server it is recommended to start scanning in onStart and stop scanning in onStop.
~~~~
import com.quick.ble.BLEClient;
public class MyClient implements BLEClient.BLEClientDelegate{
    BLECLient client = new BLEClient(context, this);
    public MyClient(){
        client.setUseNewMethod(false);
        client.scanForDevices(); //This will return any error codes and shouldbe handled
    }

    //BLEClientDelegate
	@Override
	public void onConnect(BluetoothDevice device) {
		bleClient.stopScanning();
		//Now Connected to device don't need to keep scanning
	}
	@Override
	public void onDeviceFound(final BluetoothDevice device, int rssi, byte[] scanRecord) {
		//Found a device
		//Should add to UI to allow user to select a device to connect
		//To connect:
		//client.connectToDevice(device)
	}
	@Override
	public void onScanFailed(BLEClient.ScanError errorCode) {
		//This will give android error codes if scan fails
		//This only works with the new method!
	}
	@Override
	public void onCharacteristicChangedClient(BluetoothGattCharacteristic characteristic) {
		//Server changed characteristic
		//This will only be called if the client is set to receive notifications from that characteristic
		//To receive notifications, the server must notify and the client must run:
		//client.receiveNotifications(characteristicUuid);
	}
	@Override
	public void onServicesDiscovered(List<BluetoothGattService> services) {
		//Client found services on the server
		//Do not set or get characteristics, services, or descriptors before this point or you will get a NullPointerException
	}
	@Override
	public void onDisconnected() {
		//Disconnected from server
		//May want to start scanning again
	}
}
~~~~
### Add Quick BLE to project
First add the BLE permissions to the Manifest file
~~~~
<uses-permission android:name="android.permission.BLUETOOTH"/>
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN"/>
<!--If needed add location permissions here-->

<!--Setting required to false allows this to run on devices without BLE support-->
<uses-feature android:name="android.hardware.bluetooth_le" android:required="false"/>
~~~~
In order to allow access to the normal android method behind this library and allow full customization, the source code must be added to the project.
You must either clone the repository and copy the quickble.ble folder to your project or download the zip from the [releases page](https://github.com/MB3hel/Quick-Bluetooth-LE/releases). If you download the zip it will only contain the files that are requred.

### Links
[More on Bluetooth Low Energy and Gatt](https://learn.adafruit.com/introduction-to-bluetooth-low-energy/gatt)<br />
[Android Bluetooth LE Tutorial](https://developer.android.com/guide/topics/connectivity/bluetooth-le.html)
