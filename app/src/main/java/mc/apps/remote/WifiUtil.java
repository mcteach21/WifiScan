package mc.apps.remote;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;

public class WifiUtil {

    private static final String TAG = "wifi";

    public static void scan(Context context){
        WifiManager wManager;
        List<ScanResult> wifiList;

        wManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        // Inside BroadcastReceiver()
        wifiList = wManager.getScanResults();
        for (int i=0; i<wifiList.size(); i++){
            ScanResult scanresult = wifiList.get(i);
            Log.i(TAG , "SSID: "+scanresult.SSID);
            Log.i(TAG , "RSSI: "+scanresult.level);
            Log.i(TAG , "Frequency: "+scanresult.frequency);
            Log.i(TAG , "BSSID: "+scanresult.BSSID);
            Log.i(TAG , "Capability: "+scanresult.capabilities);
        }
    }
}
