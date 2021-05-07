package mc.apps.remote;

import android.Manifest;
import android.app.Notification;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener, WifiP2pManager.PeerListListener {
    private static final String FINE_LOCATION_PERMISSION = Manifest.permission.ACCESS_FINE_LOCATION;
    private final int FINE_LOCATION_PERM_CODE = 1001;
    private static final String TAG = "tests";

    private WifiManager wifiManager;
    private WifiBroadcastReceiver broadcastReceiver;

    Button buttonScan;
    RecyclerView devices;

    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;

    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private List<WifiP2pDevice> peers = new ArrayList();
    private WifiP2pDevice device;
    private ProgressDialog progressDialog = null;

    private final IntentFilter intentP2pFilter = new IntentFilter();
    private WifiP2pBroadcastReceiver broadcastP2pReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // devices = findViewById(R.id.devices);
        /**
         * Wifi Scan
         */
        initWifiScan();

        /**
         * Wifi Direct (p2p : scan devices)
         */
        isWifiP2pEnabled = initWifip2p();
        if (!isWifiP2pEnabled)
            Toast.makeText(this, "WIFI Direct non activé!", Toast.LENGTH_SHORT).show();
    }

    private void initWifiScan() {
        buttonScan = findViewById(R.id.btnScan);
        buttonScan.setEnabled(false);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            Toast.makeText(getApplicationContext(), "Activation WIFI...", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        checkPermissions();
        buttonScan.setOnClickListener(v -> {
            wifiManager.startScan();
        });
    }

    private void checkPermissions() {
        Toast.makeText(getApplicationContext(), "Check persmissions...", Toast.LENGTH_LONG).show();
        if (ActivityCompat.checkSelfPermission(this, FINE_LOCATION_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{FINE_LOCATION_PERMISSION}, FINE_LOCATION_PERM_CODE);
        } else {
            //  Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            //  startActivity(myIntent);
            buttonScan.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case FINE_LOCATION_PERM_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "permission not granted", Toast.LENGTH_SHORT).show();
                    return;
                }
                break;
        }
    }

    /**
     * WIFI devices
     */
    private boolean initWifip2p() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            Log.e(TAG, "Wi-Fi Direct is not supported by this device.");
            return false;
        }
        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            Log.e(TAG, "Cannot get Wi-Fi system service.");
            return false;
        }
        if (!wifiManager.isP2pSupported()) {
            Log.e(TAG, "Wi-Fi Direct is not supported by the hardware or Wi-Fi is off.");
            return false;
        }
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        if (manager == null) {
            Log.e(TAG, "Cannot get Wi-Fi Direct system service.");
            return false;
        }
        channel = manager.initialize(this, getMainLooper(), null);
        if (channel == null) {
            Log.e(TAG, "Cannot initialize Wi-Fi Direct.");
            return false;
        }

        intentP2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentP2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentP2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentP2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        return true;
    }


    @Override
    protected void onPostResume() {
        super.onPostResume();

        broadcastReceiver = new WifiBroadcastReceiver();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);

        Toast.makeText(this, "registerReceiver broadcastReceiver..", Toast.LENGTH_SHORT).show();
        registerReceiver(broadcastReceiver, intentFilter);

        if (isWifiP2pEnabled) {
            broadcastP2pReceiver = new WifiP2pBroadcastReceiver(manager, channel);
            Toast.makeText(this, "registerReceiver broadcastP2pReceiver..", Toast.LENGTH_SHORT).show();
            registerReceiver(broadcastP2pReceiver, intentP2pFilter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Toast.makeText(this, "..unregisterReceiver!", Toast.LENGTH_SHORT).show();

        if (broadcastReceiver != null)
            unregisterReceiver(broadcastReceiver);
        if (broadcastP2pReceiver != null)
            unregisterReceiver(broadcastP2pReceiver);
    }

    @Override
    public void onChannelDisconnected() {
        if (manager != null && !retryChannel) {
            Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
            // resetData();
            retryChannel = true;
            manager.initialize(this, getMainLooper(), this);
        } else {
            Toast.makeText(this, "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        peers.clear();
        peers.addAll(wifiP2pDeviceList.getDeviceList());
        Log.e("Peer Size-* * * *", String.valueOf(peers.size()) + wifiP2pDeviceList.toString());

        //((WiFiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
        if (peers.size() == 0) {
            Toast.makeText(this, "No devices found", Toast.LENGTH_SHORT).show();
            return;
        } else {
            Toast.makeText(this, "devices found! " + peers.size(), Toast.LENGTH_SHORT).show();
            Toast.makeText(this,  peers.size()+" device(s) found..", Toast.LENGTH_SHORT).show();
            Log.i(TAG, "==================================");
            Log.i(TAG, "devices : ");
            for (WifiP2pDevice devices : peers) {
                Log.i(TAG, device.deviceName);
                Toast.makeText(this, "device.deviceName", Toast.LENGTH_SHORT).show();
            }
            Log.i(TAG, "==================================");
        }


    }

    class WifiBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, "WifiBroadcastReceiver..receive!", Toast.LENGTH_SHORT).show();
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                List<ScanResult> wifiList = wifiManager.getScanResults();

                Log.i(TAG, "************************************************");
                for (ScanResult scanResult : wifiList) {
                    Log.i(TAG, "device : SSID=" + scanResult.SSID + " - Capabilities=" + scanResult.capabilities);
                }
                Log.i(TAG, "************************************************");
            }
        }
    }

    class WifiP2pBroadcastReceiver extends BroadcastReceiver {
        private WifiP2pManager manager;
        private WifiP2pManager.Channel channel;

        public WifiP2pBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel) {
            super();
            this.manager = manager;
            this.channel = channel;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // UI update to indicate wifi p2p status.
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    // Wifi Direct mode is enabled
//                    activity.setIsWifiP2pEnabled(true);
                    Toast.makeText(context, "Wifi Direct mode is enabled", Toast.LENGTH_SHORT).show();
                } else {
//                    activity.setIsWifiP2pEnabled(false);
//                    activity.resetData();
                    Toast.makeText(context, "Wifi Direct mode is Not enabled", Toast.LENGTH_SHORT).show();
                }
                Log.d(TAG, "P2P state changed - " + state);
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // request available peers from the wifi p2p manager. This is an
                // asynchronous call and the calling activity is notified with a
                // callback on PeerListListener.onPeersAvailable()
                if (manager != null) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{FINE_LOCATION_PERMISSION}, FINE_LOCATION_PERM_CODE);
                        return;
                    }
                    manager.requestPeers(channel, MainActivity.this);
//                    manager.requestPeers(channel, (PeerListListener) activity.getFragmentManager()
//                            .findFragmentById(R.id.frag_list));
                }
                Log.d(TAG, "P2P peers changed");
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                if (manager == null) {
                    return;
                }
                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
                if (networkInfo.isConnected()) {
                    // we are connected with the other device, request connection
                    // info to find group owner IP
//                    DeviceDetailFragment fragment = (DeviceDetailFragment) activity
//                            .getFragmentManager().findFragmentById(R.id.frag_detail);
//                    manager.requestConnectionInfo(channel, fragment);
                } else {
                    // It's a disconnect
//                    activity.resetData();
                }
                Log.d(TAG, "P2P WIFI_P2P_CONNECTION_CHANGED_ACTION");
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                 Log.d(TAG, "P2P WIFI_P2P_THIS_DEVICE_CHANGED_ACTION");
//                WifiP2pDevice device = (WifiP2pDevice) intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
//                Log.i(TAG, "onReceive: device="+device);

                if (manager != null) {
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{FINE_LOCATION_PERMISSION}, FINE_LOCATION_PERM_CODE);
                        return;
                    }
                    manager.requestPeers(channel, MainActivity.this);
                }

            }
        }
    }


}
