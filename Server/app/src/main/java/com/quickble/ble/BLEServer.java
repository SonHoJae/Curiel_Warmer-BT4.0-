package com.quickble.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class BLEServer{

	//MARK: Values
	public static enum BtError {None, NoBluetooth, NoBLE, Disabled, NoServer};
	public static enum AdvertiseError{None, DataTooLarge, TooManyAdvertisers, AlreadyStarted, InternalError, FeatureUnsupported};
	public static enum ServiceType{Primary, Secondary};

	public static interface CharFormat {
		public static final int Float = 52;
		public static final int SFloat = 50;
		public static final int SInt16 = 34;
		public static final int SInt32 = 36;
		public static final int SInt8 = 33;
		public static final int UInt16 = 18;
		public static final int UInt32 = 20;
		public static final int UInt8 = 17;
	}
	public static interface DescPermissions{
		public static final int Read = 1;
		public static final int ReadEncrypted = 2;
		public static final int ReadEncryptedMitm = 4;
		public static final int Write = 16;
		public static final int WriteEncrypted = 32;
		public static final int WriteEncryptedMitm = 64;
		public static final int WriteSigned = 128;
		public static final int WriteSignedMitm = 256;
	}
	public static interface CharPermissions{
		public static final int Read = 1;
		public static final int ReadEncrypted = 2;
		public static final int ReadEncryptedMitm = 4;
		public static final int Write = 16;
		public static final int WriteEncrypted = 32;
		public static final int WriteEncryptedMitm = 64;
		public static final int WriteSigned = 128;
		public static final int WriteSignedMitm = 256;
	}
	public static interface CharProperties{
		public static final int Broadcast = 1;
		public static final int ExtendedProps = 128;
		public static final int Indicate = 32;
		public static final int Notify = 16;
		public static final int Read = 2;
		public static final int SignedWrite = 64;
		public static final int Write = 8;
		public static final int WriteNoResponse = 4;
	}

	//MARK: Properties
	private Context context;
	private BLEServerDelegate delegate;
	private ArrayList<BluetoothGattService> services = new ArrayList<>();
	private ArrayList<BluetoothDevice> connectedDevices = new ArrayList<>();
	private int advertiseMode = AdvertiseSettings.ADVERTISE_MODE_BALANCED;
	private int advertiseTransmitPower = AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM;
	private boolean advertiseDeviceName = true;
	private boolean serverRunning = false;

	//MARK: Bluetooth
	private BluetoothManager btManager;
	private BluetoothAdapter btAdapter;
	private BluetoothGattServer gattServer;
	private BluetoothLeAdvertiser bleAdvertiser;

	//MARK: Constructor
	public BLEServer(Context context, BLEServerDelegate delegate){
		this.context = context;
		this.delegate = delegate;
		if(this.delegate == null){
			this.delegate = new BLEServerDelegate() {
				@Override
				public void onAdvertise(AdvertiseError error) {}
				@Override
				public void onDeviceConnected(BluetoothDevice device) {}
				@Override
				public void onDeviceDisconnected(BluetoothDevice device) {}
				@Override
				public void onCharacteristicChangedServer(BluetoothGattCharacteristic characteristic) {}
				@Override
				public void onDescriptorChanged(BluetoothGattDescriptor descriptor) {}
			};
		}
	}

	//MARK: Server Config
	public void addService(BluetoothGattService service){
		if(!services.contains(service)) {
			services.add(service);
		}
	}
	public void removeService(BluetoothGattService service){
		services.remove(service);
	}
	public ArrayList<BluetoothGattService> getServices(){
		return services;
	}
	public void setServices(ArrayList<BluetoothGattService> services){
		this.services = services;
	}
	public ArrayList<BluetoothDevice> getConnectedDevices(){
		return connectedDevices;
	}
	public BLEServerDelegate getDelegate() {
		return delegate;
	}
	public void setDelegate(BLEServerDelegate delegate) {
		this.delegate = delegate;
	}
	public int getAdvertiseMode() {
		return advertiseMode;
	}
	public void setAdvertiseMode(int advertiseMode) {
		this.advertiseMode = advertiseMode;
	}
	public int getAdvertiseTransmitPower() {
		return advertiseTransmitPower;
	}
	public void setAdvertiseTransmitPower(int advertiseTransmitPower) {
		this.advertiseTransmitPower = advertiseTransmitPower;
	}
	public boolean isAdvertiseDeviceName() {
		return advertiseDeviceName;

	}
	public void setAdvertiseDeviceName(boolean advertiseDeviceName) {
		this.advertiseDeviceName = advertiseDeviceName;
	}
	public boolean isServerRunning() {
		return serverRunning;
	}

	//MARK: Bluetooth Control
	public void notifyDevices(BluetoothGattCharacteristic changedCharacteristic) {
		if (!connectedDevices.isEmpty() && gattServer != null) {
			boolean indicate = (changedCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE;
			for (BluetoothDevice device : connectedDevices) {

				gattServer.notifyCharacteristicChanged(device, changedCharacteristic, indicate);

			}
		}
	}

	//Set Characteristic
	public boolean setCharacteristicValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic, byte[] value, boolean notify){
		return setCharacteristicValue(service.getUuid(), characteristic.getUuid(), value, notify);
	}
	public boolean setCharacteristicValue(UUID serviceUuid, UUID characteristicUuid, byte[] value, boolean notify){
		BluetoothGattService service = gattServer.getService(serviceUuid);
		if(service == null)
			return false;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return false;
		boolean rtn = ch.setValue(value);
		if(rtn && notify){
			Log.i("test","setvalue "+Arrays.toString(value));
			Log.i("test","setvalue "+ch.toString());
			notifyDevices(ch);
		}
		return rtn;
	}
	public boolean setCharacteristicValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic, int value, int format, int ofst, boolean notify){
		return setCharacteristicValue(service.getUuid(), characteristic.getUuid(), value, format, ofst, notify);
	}
	public boolean setCharacteristicValue(UUID serviceUuid, UUID characteristicUuid, int value, int format, int ofst, boolean notify) {
		BluetoothGattService service = gattServer.getService(serviceUuid);
		if(service == null)
			return false;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return false;
		boolean rtn = ch.setValue(value, format, ofst);
		if(rtn && notify){
			notifyDevices(ch);
		}
		return rtn;
	}
	public boolean setCharacteristicValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic, String value, boolean notify){
		return setCharacteristicValue(service.getUuid(), characteristic.getUuid(), value, notify);
	}
	public boolean setCharacteristicValue(UUID serviceUuid, UUID characteristicUuid, String value, boolean notify){
		BluetoothGattService service = gattServer.getService(serviceUuid);
		if(service == null)
			return false;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return false;
		boolean rtn = ch.setValue(value);
		if(rtn && notify){
			notifyDevices(ch);
		}
		return rtn;
	}

	//Get Characteristic
	public byte[] getCharacteristicValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic){
		return getCharacteristicValue(service.getUuid(), characteristic.getUuid());
	}
	public byte[] getCharacteristicValue(UUID serviceUuid, UUID characteristicUuid){
		Log.i("Name","test");
		BluetoothGattService service = gattServer.getService(serviceUuid);
		if(service == null)
			return null;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return null;
		Log.i("Name", Arrays.toString(ch.getValue()));
		return ch.getValue();
	}
	public int getCharacteristicValueInt(BluetoothGattService service, BluetoothGattCharacteristic characteristic, int format, int ofst){
		return getCharacteristicValueInt(service.getUuid(), characteristic.getUuid(), format, ofst);
	}
	public int getCharacteristicValueInt(UUID serviceUuid, UUID characteristicUuid, int format, int ofst){
		BluetoothGattService service = gattServer.getService(serviceUuid);
		if(service == null)
			return -1;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return -1;
		return ch.getIntValue(format, ofst);
	}
	public String getCharacteristicValueString(BluetoothGattService service, BluetoothGattCharacteristic characteristic, int ofst){
		int i = services.indexOf(service);
		if(i >= services.size())
			return null;
		return services.get(i).getCharacteristic(characteristic.getUuid()).getStringValue(ofst);
	}
	public String getCharacteristicValueString(UUID serviceUuid, UUID characteristicUuid, int ofst){
		BluetoothGattService service = gattServer.getService(serviceUuid);
		if(service == null)
			return null;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return null;
		return ch.getStringValue(ofst);
	}

	//Set descriptor
	public boolean setDescriptorValue(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] value){
		BluetoothGattService service = gattServer.getService(serviceUuid);
		if(service == null)
			return false;
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
		if(characteristic == null)
			return false;
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUuid);
		if(descriptor == null)
			return false;
		return descriptor.setValue(value);
	}
	public boolean setDescriptorValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, byte[] value){
		return setDescriptorValue(service.getUuid(), characteristic.getUuid(), descriptor.getUuid(), value);
	}
	public boolean setDescriptorValue(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, String value){
		BluetoothGattService service = gattServer.getService(serviceUuid);
		if(service == null)
			return false;
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
		if(characteristic == null)
			return false;
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUuid);
		if(descriptor == null)
			return false;
		return descriptor.setValue(value.getBytes());
	}
	public boolean setDescriptorValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, String value){
		return setDescriptorValue(service.getUuid(), characteristic.getUuid(), descriptor.getUuid(), value);
	}

	//Get descriptor
	public byte[] getDescriptorValue(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid){
		BluetoothGattService service = gattServer.getService(serviceUuid);
		if(service == null)
			return null;
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
		if(characteristic == null)
			return null;
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUuid);
		if(descriptor == null)
			return null;
		return descriptor.getValue();
	}
	public byte[] getDescriptorValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor){
		return getDescriptorValue(service.getUuid(), characteristic.getUuid(), descriptor.getUuid());
	}
	public String getDescriptorValueString(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid){
		BluetoothGattService service = gattServer.getService(serviceUuid);
		if(service == null)
			return null;
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
		if(characteristic == null)
			return null;
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUuid);
		if(descriptor == null)
			return null;
		return new String(descriptor.getValue());
	}
	public String getDescriptorValueString(BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor){
		return getDescriptorValueString(service.getUuid(), characteristic.getUuid(), descriptor.getUuid());
	}

	//Build Gatt classes
	public BluetoothGattDescriptor buildDescriptor(UUID descriptorUuid, int permissions){
		return new BluetoothGattDescriptor(descriptorUuid, permissions);
	}

	public BluetoothGattCharacteristic buildCharacteristic(UUID characteristicUuid, BluetoothGattDescriptor[] descriptors, int properties, int permissions){
		BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(characteristicUuid, properties, permissions);
		byte []b = new byte[1];
		b[0] = 1;
		characteristic.setValue(b);
		for(BluetoothGattDescriptor descriptor : descriptors){
			characteristic.addDescriptor(descriptor);
		}
		return characteristic;
	}

	public BluetoothGattService buildService(UUID serviceUuid, ServiceType serviceType, BluetoothGattCharacteristic[] characteristics){
		BluetoothGattService service = new BluetoothGattService(serviceUuid, serviceType.ordinal());
		for(BluetoothGattCharacteristic characteristic : characteristics){
			service.addCharacteristic(characteristic);
		}
		return service;
	}

	public BtError checkBluetooth(){
		btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		btAdapter = btManager.getAdapter();
		if(!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
			return BtError.NoBluetooth;
		if(!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
			return BtError.NoBLE;
		if(btAdapter == null || !btAdapter.isEnabled())
			return BtError.Disabled;
		if(!btAdapter.isMultipleAdvertisementSupported())
			return BtError.NoServer;
		return BtError.None;
	}

	public BtError startServer(){
		BtError error = checkBluetooth();
		if(error != BtError.None)
			return error;
		bleAdvertiser = btAdapter.getBluetoothLeAdvertiser();
		gattServer = btManager.openGattServer(context, gattServerCallback);
		for(BluetoothGattService service : services){
			gattServer.addService(service);
		}
		if(bleAdvertiser == null)
			return BtError.NoServer;
		AdvertiseSettings.Builder settings = new AdvertiseSettings.Builder();
		settings.setAdvertiseMode(advertiseMode);
		settings.setConnectable(true);
		settings.setTimeout(0);
		settings.setTxPowerLevel(advertiseTransmitPower);
		AdvertiseData.Builder data = new AdvertiseData.Builder();
		data.setIncludeDeviceName(advertiseDeviceName);
		//data.addServiceUuid(new ParcelUuid(SERVICE_COMMUNICATION));
		bleAdvertiser.startAdvertising(settings.build(), data.build(), advertiseCallback);
		serverRunning = true;
		return BtError.None;
	}

	public void stopServer(){
		if(gattServer != null){
			gattServer.close();
		}
		if(bleAdvertiser != null){
			bleAdvertiser.stopAdvertising(advertiseCallback);
		}
		serverRunning = false;
	}

	public void disconnectDevice(BluetoothDevice device){
		gattServer.cancelConnection(device);
	}

	//MARK: Callbacks
	private BluetoothGattServerCallback gattServerCallback = new BluetoothGattServerCallback() {
		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
			super.onConnectionStateChange(device, status, newState);
			if(status == BluetoothGatt.GATT_SUCCESS){
				if(newState == BluetoothGatt.STATE_CONNECTED){
					connectedDevices.add(device);
					delegate.onDeviceConnected(device);
				}else if(newState == BluetoothGatt.STATE_DISCONNECTED){
					connectedDevices.remove(device);
					delegate.onDeviceDisconnected(device);
				}
			}
		}
		@Override
		public void onServiceAdded(int status, BluetoothGattService service) {
			super.onServiceAdded(status, service);
		}
		@Override
		public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int ofst, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicReadRequest(device, requestId, ofst, characteristic);
			Log.i("read_request","read_request "+Arrays.toString(characteristic.getValue()));
			gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, ofst, characteristic.getValue());
		}
		@Override
		public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int ofst, byte[] value) {
			super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, ofst, value);
			characteristic.setValue(value);
			if(responseNeeded){
				gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, ofst, value);
			}
			delegate.onCharacteristicChangedServer(characteristic);
		}
		@Override
		public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int ofst, BluetoothGattDescriptor descriptor) {
			super.onDescriptorReadRequest(device, requestId, ofst, descriptor);
			gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, ofst, descriptor.getValue());
		}
		@Override
		public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int ofst, byte[] value) {
			super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, ofst, value);
			descriptor.setValue(value);
			if(responseNeeded){
				gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, ofst, value);
			}
			delegate.onDescriptorChanged(descriptor);
		}
		@Override
		public void onExecuteWrite(BluetoothDevice device, int requestId, boolean execute) {
			super.onExecuteWrite(device, requestId, execute);
		}
		@Override
		public void onNotificationSent(BluetoothDevice device, int status) {
			super.onNotificationSent(device, status);
		}
		@Override
		public void onMtuChanged(BluetoothDevice device, int mtu) {
			super.onMtuChanged(device, mtu);
		}
	};
	private AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			super.onStartSuccess(settingsInEffect);
			if(delegate != null)
				delegate.onAdvertise(AdvertiseError.None);
		}

		@Override
		public void onStartFailure(int errorCode) {
			super.onStartFailure(errorCode);
			if(delegate != null)
				delegate.onAdvertise(AdvertiseError.values()[errorCode]);
		}
	};

	//MARK: Delegate
	public interface BLEServerDelegate{
		void onAdvertise(AdvertiseError error);
		void onDeviceConnected(BluetoothDevice device);
		void onDeviceDisconnected(BluetoothDevice device);
		void onCharacteristicChangedServer(BluetoothGattCharacteristic characteristic);
		void onDescriptorChanged(BluetoothGattDescriptor descriptor);
	}
}
