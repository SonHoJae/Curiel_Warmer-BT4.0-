package com.flyingmountain.curiel_warmer;
import android.util.Log;

import java.util.Arrays;

/**
 * Created by hojaeson on 1/16/17.
 */

public class DataHandler {
    private final static String DATA_LOG = "data_log";
    private static DataHandler dataHandler;
    //Communication Constant Protocol
    final static byte STX = 0x02;
    final static byte ETX = 0x03;
    final static byte CMD_POWER_ON_OFF = 0x30;
    final static byte DATA_POWER_ON = 0x31;
    final static byte DATA_POWER_OFF = 0x32;
    final static byte CMD_MODE_SETTING = 0x31;
    final static byte DATA_MODE = 0x31;
    final static byte CMD_SET_TEMPERATURE= 0x32;
    final static byte CMD_REQUEST_STATUS = 0x40;

    static public DataHandler SingletonInstance(){
        if(dataHandler == null){
            dataHandler = new DataHandler();
        }
        return dataHandler;
    }
    //TODO: HOW TO TRANSFER CHECKSUM ?
    public byte[] powerOnData(){
        try {
            byte powerOnData[] = new byte[5];
            powerOnData[0] = STX;
            powerOnData[1] = CMD_POWER_ON_OFF;
            powerOnData[2] = DATA_POWER_ON;
            powerOnData[3] = ETX;
            powerOnData[powerOnData.length-1] = getChecksum(Arrays.copyOfRange(powerOnData, 0, powerOnData.length-1));
            Log.i(DATA_LOG,"Power On signal");
            return powerOnData;
        }catch (Exception e){
            Log.e(DATA_LOG,"POWER ON DATA TRANSMISSION FAILED");
            return null;
        }
    }

    public byte[] powerOffData(){
        try {
            byte powerOffData[] = new byte[5];
            powerOffData[0] = STX;
            powerOffData[1] = CMD_POWER_ON_OFF;
            powerOffData[2] = DATA_POWER_OFF;
            powerOffData[3] = ETX;
            powerOffData[powerOffData.length-1] = getChecksum(Arrays.copyOfRange(powerOffData, 0, powerOffData.length-1));
            Log.i(DATA_LOG,"Power Off signal");
            return powerOffData;
        }catch (Exception e){
            Log.e(DATA_LOG,"POWER OFF DATA TRANSMISSION FAILED");
            return null;
        }
    }

    public byte[] setTemperature(byte temperature){
        try {
            byte setTempData[] = new byte[5];
            setTempData[0] = STX;
            setTempData[1] = CMD_SET_TEMPERATURE;
            setTempData[2] = temperature;
            setTempData[3] = ETX;
            setTempData[setTempData.length-1] = getChecksum(Arrays.copyOfRange(setTempData, 0, setTempData.length-1));
            Log.i(DATA_LOG,"Set the temperature"+ Arrays.toString(setTempData));
            return setTempData;
        }catch(Exception e){
            Log.e(DATA_LOG,"SET TEMPERATURE DATA TRANSMISSION FAILED");
            return null;
        }
    }

    public byte[] requestWarmerStatus(){
        try{
            byte requestStatusData[] = new byte[3];
            requestStatusData[0] = STX;
            requestStatusData[1] = CMD_REQUEST_STATUS;
            requestStatusData[2] = ETX;
            requestStatusData[requestStatusData.length-1] = getChecksum(Arrays.copyOfRange(requestStatusData, 0, requestStatusData.length-1));
            Log.i(DATA_LOG,"request warmer status");
            return requestStatusData;
        }catch(Exception e){
            Log.e(DATA_LOG,"REQUEST WARMER STATUS DATA TRANSMISSION FAILED");
            return null;
        }
    }

    public byte[] getRequestedWarmerStatus(byte[] warmerInfo){
        int i = 0;
        while(warmerInfo[i] != ETX){
            if(warmerInfo[i] == STX && warmerInfo[i+1] == CMD_REQUEST_STATUS){
                Log.i(DATA_LOG,"Checksum need to be checked");
                //warmer.setPower(warmerInfo[2]);
                //warmer.setTemperature(warmerInfo[3]);
                //warmer.setBatteryPercent(warmerInfo[4]);
            }
            i++;// timer or something needed
        }
        return warmerInfo;
    }
    public byte getChecksum(byte bytes[]){
        byte checksum = 0;
        for(int i=0; i<bytes.length-1; i++){
            checksum += bytes[i];
        }
        return (byte) (checksum & 0xff);
    }
}
