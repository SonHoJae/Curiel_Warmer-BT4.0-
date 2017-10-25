package com.quickble.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static android.R.attr.value;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BLEClient {

	//MARK: Values
	public enum BtError {None, NoBluetooth, NoBLE, Disabled, NoLocationPermission, LocationDisabled};
	public enum ScanError{None, AlreadyStarted, AppRegistrationFailed, InternalError, FeatureUnsupported};
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
		public static final int REad = 1;
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
	//MARK:Properties
	private Context context;
	private BLEClientDelegate delegate;
	private int scanMode = ScanSettings.SCAN_MODE_BALANCED;
	private boolean useNewMethod = false;
	private boolean scanning = false;

	//MARK:Bluetooth
	private BluetoothDevice connectedDevice;
	private BluetoothGatt gattConnection;
	private BluetoothManager btManager;
	private BluetoothAdapter btAdapter;
	private BluetoothLeScanner bleScanner;
	private ArrayList<ScanFilter> scanFilters;

	//MARK: Constructor
	public BLEClient(Context context, BLEClientDelegate delegate){
		this.context = context;
		this.delegate = delegate;
	}

	//MARK: Client Config
	public void setScanMode(int scanMode){
		this.scanMode = scanMode;
	}
	public int getScanMode(){
		return scanMode;
	}
	public boolean isUseNewMethod() {
		return useNewMethod;
	}
	public void setUseNewMethod(boolean useNewMethod) {
		this.useNewMethod = useNewMethod;
	}
	public boolean isScanning() {return scanning;}

	//MARK: Bluetooth Control
	public BtError enableDiscovery(){
		BtError error = checkBluetooth();
		if(error != BtError.None)
			return error;
		btAdapter.startDiscovery();
		return BtError.None;
	}
	public BtError disableDiscovery(){
		BtError error = checkBluetooth();
		if(error != BtError.None)
			return error;
		btAdapter.cancelDiscovery();
		return BtError.None;
	}

	public BLEClient.BtError checkBluetooth(){
		btManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
		btAdapter = btManager.getAdapter();
		if(!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH))
			return BLEClient.BtError.NoBluetooth;
		if(!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
			return BLEClient.BtError.NoBLE;
		if(btAdapter == null || !btAdapter.isEnabled())
			return BLEClient.BtError.Disabled;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && useNewMethod){
			if((ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED) &&
					(ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED)){
				return BtError.NoLocationPermission;
			}
			LocationManager lm = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
			if(!(lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) && !(lm.isProviderEnabled(LocationManager.GPS_PROVIDER))){
				return BtError.LocationDisabled;
			}
		}
		return BLEClient.BtError.None;
	}
	public BLEClient.BtError scanForDevices(){
		BtError error = checkBluetooth();
		if(error != BtError.None)
			return error;
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && useNewMethod){
			bleScanner = btAdapter.getBluetoothLeScanner();
			ScanSettings.Builder settings = new ScanSettings.Builder();
			settings.setScanMode(scanMode);
			scanFilters = new ArrayList<>();
			scanCallback = new ScanCallback() {
				@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
				@Override
				public void onScanResult(int callbackType, ScanResult result) {
					super.onScanResult(callbackType, result);
					delegate.onDeviceFound(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
				}
				@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
				@Override
				public void onBatchScanResults(List<ScanResult> results) {
					super.onBatchScanResults(results);
					for(ScanResult result : results){
						delegate.onDeviceFound(result.getDevice(), result.getRssi(), result.getScanRecord().getBytes());
					}
				}

				@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
				@Override
				public void onScanFailed(int errorCode) {
					super.onScanFailed(errorCode);
					delegate.onScanFailed(ScanError.values()[errorCode]);
				}
			};
			bleScanner.startScan(scanFilters, settings.build(), (ScanCallback) scanCallback);
			Log.i("Scan Method", "New Scan");
		}else{
			Log.i("Scan Method", "Old scan");
			btAdapter.startLeScan(leScanCallback);
		}
		scanning = true;
		return BtError.None;
	}
	public void stopScanning(){
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && bleScanner != null && useNewMethod){
			bleScanner.stopScan((ScanCallback) scanCallback);
		}else if(btAdapter != null){
			btAdapter.stopLeScan(leScanCallback);
		}
		scanning = false;
	}
	public void connectToDevice(BluetoothDevice device){
		gattConnection = device.connectGatt(context, false, bluetoothGattCallback);
		if(gattConnection != null){
			delegate.onConnect(device);
		}
	}
	public void disconnectFromDevice(){
		if(gattConnection != null){
			gattConnection.disconnect();
		}
	}

	public boolean setCharacteristicValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic, byte[] value){
		return setCharacteristicValue(service.getUuid(), characteristic.getUuid(), value);
	}
	public boolean setCharacteristicValue(UUID serviceUuid, UUID characteristicUuid, byte[] value){
		if(gattConnection == null)
			return false;
		BluetoothGattService service = gattConnection.getService(serviceUuid);
		if(service == null)
			return false;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return false;
		ch.setValue(value);
		return gattConnection.writeCharacteristic(ch);
	}
	public boolean setCharacteristicValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic, int value, int format, int ofst){
		return setCharacteristicValue(service.getUuid(), characteristic.getUuid(), value, format, ofst);
	}
	public boolean setCharacteristicValue(UUID serviceUuid, UUID characteristicUuid, int value, int format, int ofst){
		if(gattConnection == null)
			return false;
		BluetoothGattService service = gattConnection.getService(serviceUuid);
		if(service == null)
			return false;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return false;
		ch.setValue(value, format, ofst);
		return gattConnection.writeCharacteristic(ch);
	}
	public boolean setCharacteristicValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic, String value){
		return setCharacteristicValue(service.getUuid(), characteristic.getUuid(), value);
	}
	public boolean setCharacteristicValue(UUID serviceUuid, UUID characteristicUuid, String value){
		if(gattConnection == null)
			return false;
		BluetoothGattService service = gattConnection.getService(serviceUuid);
		if(service == null)
			return false;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return false;
		ch.setValue(value);
		return gattConnection.writeCharacteristic(ch);
	}

	//Get Characteristic
	public int getCharacteristicValueInt(UUID serviceUuid, UUID characteristicUuid, int format, int ofst){
		BluetoothGattService service = gattConnection.getService(serviceUuid);
		if(service == null)
			return -1;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return -1;
		return ch.getIntValue(format, ofst);
	}
	public int getCharacteristicValueInt(BluetoothGattService service, BluetoothGattCharacteristic characteristic, int format, int ofst){
		return getCharacteristicValueInt(service.getUuid(), characteristic.getUuid(), format, ofst);
	}
	public String getCharacteristicValueString(UUID serviceUuid, UUID characteristicUuid, int ofst){
		BluetoothGattService service = gattConnection.getService(serviceUuid);
		if(service == null)
			return null;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return null;
		return ch.getStringValue(ofst);
	}
	public String getCharacteristicValueString(BluetoothGattService service, BluetoothGattCharacteristic characteristic, int ofst){
		return getCharacteristicValueString(service.getUuid(), characteristic.getUuid(), value);
	}
	public byte[] getCharacteristicValue(UUID serviceUuid, UUID characteristicUuid){
		BluetoothGattService service = gattConnection.getService(serviceUuid);
		if(service == null)
			return null;
		BluetoothGattCharacteristic ch = service.getCharacteristic(characteristicUuid);
		if(ch == null)
			return null;
		return ch.getValue();
	}
	public byte[] getCharacteristicValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic){
		return getCharacteristicValue(service.getUuid(), characteristic.getUuid());
	}

	//Set descriptor
	public boolean setDescriptorValue(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, byte[] value){
		if(gattConnection == null)
			return false;
		BluetoothGattService service = gattConnection.getService(serviceUuid);
		if(service == null)
			return false;
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
		if(characteristic == null)
			return false;
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUuid);
		if(descriptor == null)
			return false;
		descriptor.setValue(value);
		return gattConnection.writeDescriptor(descriptor);
	}
	public boolean setDescriptorValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, byte[] value){
		return setDescriptorValue(service.getUuid(), characteristic.getUuid(), descriptor.getUuid(), value);
	}
	public boolean setDescriptorValue(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, String value){
		if(gattConnection == null)
			return false;
		BluetoothGattService service = gattConnection.getService(serviceUuid);
		if(service == null)
			return false;
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
		if(characteristic == null)
			return false;
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUuid);
		if(descriptor == null)
			return false;
		descriptor.setValue(value.getBytes());
		return gattConnection.writeDescriptor(descriptor);
	}
	public boolean setDescriptorValue(BluetoothGattService service, BluetoothGattCharacteristic characteristic, BluetoothGattDescriptor descriptor, String value){
		return setDescriptorValue(service.getUuid(), characteristic.getUuid(), descriptor.getUuid(), value);
	}

	//Get descriptor
	public byte[] getDescriptorValue(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid){
		BluetoothGattService service = gattConnection.getService(serviceUuid);
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
		BluetoothGattService service = gattConnection.getService(serviceUuid);
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

	//Get Gatt classes
	public BluetoothGattCharacteristic getCharacteristic(UUID serviceUuid, UUID characteristicUuid){
		if(gattConnection == null)
			return null;
		BluetoothGattService service = gattConnection.getService(serviceUuid);
		if(service == null)
			return null;
		return service.getCharacteristic(characteristicUuid);
	}
	public BluetoothGattService getService(UUID serviceUuid){
		if(gattConnection == null)
			return null;
		BluetoothGattService service = gattConnection.getService(serviceUuid);
		return service;
	}
	public BluetoothGattDescriptor getDescriptor(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid){
		if(gattConnection == null)
			return null;
		BluetoothGattService service = gattConnection.getService(serviceUuid);
		if(service == null)
			return null;
		BluetoothGattCharacteristic characteristic = service.getCharacteristic(characteristicUuid);
		if(characteristic == null)
			return null;
		return characteristic.getDescriptor(descriptorUuid);
	}

	//Notify
	public void receiveNotifications(UUID serviceUuid, UUID characteristicUuid, UUID descriptorUuid, boolean receive){
		BluetoothGattCharacteristic characteristic = getCharacteristic(serviceUuid, characteristicUuid);
		gattConnection.setCharacteristicNotification(characteristic, receive);
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		gattConnection.writeDescriptor(descriptor);
	}
	public void receiveNotifications(BluetoothGattCharacteristic characteristic, boolean receive){
		gattConnection.setCharacteristicNotification(characteristic, receive);
		BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		gattConnection.writeDescriptor(descriptor);
	}

	//MARK: Callbacks
	private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
		@Override
		public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
			delegate.onDeviceFound(device, rssi, scanRecord);
		}
	};

	private Object scanCallback;

	private BluetoothGattCallback bluetoothGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			super.onConnectionStateChange(gatt, status, newState);
			if(newState == BluetoothProfile.STATE_CONNECTED){
				gatt.discoverServices();
				Log.i("Connected", gatt.getDevice().getName());
			}else if(newState == BluetoothProfile.STATE_DISCONNECTED){
				delegate.onDisconnected();
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicChanged(gatt, characteristic);
			Log.i("Characteristic Changed", characteristic.getUuid().toString());
			delegate.onCharacteristicChangedClient(characteristic);
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			super.onServicesDiscovered(gatt, status);
			delegate.onServicesDiscovered(gatt.getServices());
		}

	};

	//MARK: Delegate
	public interface BLEClientDelegate{
		void onConnect(BluetoothDevice device);
		void onServicesDiscovered(List<BluetoothGattService> services);
		void onDeviceFound(BluetoothDevice device, int rssi, byte[] scanRecord);
		void onScanFailed(ScanError error);
		void onCharacteristicChangedClient(BluetoothGattCharacteristic characteristic);
		void onDisconnected();
	}
}
