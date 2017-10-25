package com.quickble.ble;

import java.util.HashMap;

public class SampleGattAttributes {
	private static HashMap<String, String> attributes = new HashMap();
	public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
	public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
	public String BLE_BAASE_UUID = "-0000-1000-8000-00805f9b34fb";
	//Adopted Services start with 000018xx
	//Adopted characteristics start with 00002Axx
	//Always use 0000xxxx-0000-1000-8000-00805f9b34fb
	//Change only 2 bytes that are x's

	static {
		// Sample Services.
		attributes.put("0000180d-0000-1000-8000-00805f9b34fb", "Heart Rate Service");
		attributes.put("0000180a-0000-1000-8000-00805f9b34fb", "Device Information Service");
		// Sample Characteristics.
		attributes.put(HEART_RATE_MEASUREMENT, "Heart Rate Measurement");
		attributes.put("00002a29-0000-1000-8000-00805f9b34fb", "Manufacturer Name String");
	}

	public static String lookup(String uuid, String defaultName) {
		String name = attributes.get(uuid);
		return name == null ? defaultName : name;
	}
}