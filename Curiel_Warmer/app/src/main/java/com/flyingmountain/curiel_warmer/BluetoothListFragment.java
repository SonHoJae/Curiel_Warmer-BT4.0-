package com.flyingmountain.curiel_warmer;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.util.ArraySet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import co.lujun.lmbluetoothsdk.BluetoothLEController;


public class BluetoothListFragment extends Fragment {
    private HashMap<String,String> deviceMap;
    private ArrayList<String> hardwareInfo = new ArrayList<String>();
    private HashSet<String> hardwoareinfo_set = new HashSet<String>();
    private static ListView bluetoothlist;
    public OnDataPass dataPasser;
    public BluetoothLEController mBleController;
    public BluetoothListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBleController = Controller.getBleController();
        if(mBleController == null){
            onDetach();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_bluetooth_list, container, false);
        Button btn_scan = (Button)view.findViewById(R.id.btn_scan);
        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "Scanning..", Toast.LENGTH_SHORT).show();
                startDiscovery();
            }
        });

        bluetoothlist = (ListView)view.findViewById(R.id.btListView);
        Bundle getBundle = getArguments();
        if(getBundle != null) {
            Log.i("Fragment_log", "hardwarelist " + getBundle.getStringArrayList("hardwarelist").size());
            hardwareInfo.clear();
            hardwoareinfo_set.clear();
            for (int i = 0; i < getBundle.getStringArrayList("hardwarelist").size(); i++) {
                Log.i("Fragment_log", "hardwarelist " + String.valueOf(getBundle.getStringArrayList("hardwarelist").get(i)));
                hardwoareinfo_set.add(String.valueOf(getBundle.getStringArrayList("hardwarelist").get(i)));
            }
            hardwareInfo = new ArrayList<>(hardwoareinfo_set);
        }
        else{
            Log.i("Fragment_log", String.valueOf("fragment"));
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, hardwareInfo);
        bluetoothlist.setAdapter(arrayAdapter);
        bluetoothlistOnItemClickListener(bluetoothlist);
        return view;
    }

    public void startDiscovery(){
        if (! this.isDetached()) {
            getFragmentManager().beginTransaction()
                    .detach(this)
                    .attach(this)
                    .commit();
        }
        new Handler().post(new Runnable() {
            public void run() {
                mBleController.startScan();
            }
        });
    }

    public void bluetoothlistOnItemClickListener(final ListView listview){
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, final int i, long l) {
                final View selectedItem = listview.getChildAt(i);
                final String hardwareInfoStr = (String)adapter.getItemAtPosition(i);
                selectedItem.setAlpha(.5f);
                ((TextView)selectedItem).setText(adapter.getItemAtPosition(i)+"\n\t\t\tConnecting..");
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        ((TextView) selectedItem).setText(hardwareInfoStr);
                        selectedItem.setAlpha(1);
                    }
                }, 3000);
                Log.i("Fragment_log", "set alpha");
                Log.i("Fragment_log", (String) adapter.getItemAtPosition(i));
                mBleController.connect(((String) adapter.getItemAtPosition(i)).split("@")[1]);
                //passData((HashMap) adapter.getItemAtPosition(i));
                Toast.makeText(getActivity(), "Connecting..", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public interface OnDataPass {
        void onDataPass(HashMap data);
    }

    public void passData(HashMap deviceMap) {
        while(deviceMap.entrySet().iterator().hasNext()) {
            Log.i("fragment", deviceMap.entrySet().iterator().next().toString());
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        //deviceMap = ((MainActivity) getActivity()).sendData();
        //Log.i("bluetooth_fragment",String.valueOf(deviceMap.size()));

        /*
        while (deviceMap.entrySet().iterator().hasNext()) {
            Log.i("bluetooth_fragment", deviceMap.entrySet().iterator().next().toString());

        }*/
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}
