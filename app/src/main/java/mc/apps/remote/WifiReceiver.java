package mc.apps.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

class WifiReceiver extends BroadcastReceiver {

    private static final String TAG = "wifi";
    WifiManager wifiManager;
    StringBuilder sb;
    RecyclerView wifiDeviceList;

    public WifiReceiver(WifiManager wifiManager, RecyclerView wifiDeviceList) {
        this.wifiManager = wifiManager;
        this.wifiDeviceList = wifiDeviceList;
    }

    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            List<ScanResult> wifiList = wifiManager.getScanResults();

//            sb = new StringBuilder();
//            List<String> deviceList = new ArrayList<>();

            for (ScanResult scanResult : wifiList) {
//                sb.append("\n").append(scanResult.SSID).append(" - ").append(scanResult.capabilities);
//                deviceList.add(scanResult.SSID + " - " + scanResult.capabilities);

                Log.i(TAG , "device : SSID="+scanResult.SSID + " - Capabilities=" + scanResult.capabilities);
            }
            //Toast.makeText(context, sb, Toast.LENGTH_SHORT).show();

            //            ArrayAdapter arrayAdapter = new ArrayAdapter(context, android.R.layout.simple_list_item_1, deviceList.toArray());
            //            wifiDeviceList.setAdapter(arrayAdapter);
        }
    }
}
