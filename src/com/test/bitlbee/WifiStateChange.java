//package com.test.bitlbee.IncomingSms;
package com.test.bitlbee;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;


import android.preference.Preference;
import android.content.SharedPreferences;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import android.telephony.SmsManager;
import android.util.Log;
import android.os.Bundle;
import android.widget.Toast;
import java.io.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetAddress;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.SupplicantState;

public class WifiStateChange extends BroadcastReceiver {
    private static final String TAG = "bitlbeewifichange";
    @Override
    public void onReceive(Context context, Intent intent) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        Log.d(TAG, wifiInfo.toString());
        Log.d(TAG, wifiInfo.getSSID());

        //SupplicantState supState;
        //supState = wifiInfo.getSupplicantState();

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            // Do whatever
            Log.d(TAG," Connected");
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean enabled = preferences.getBoolean("checkbox_enabled",false);
            if (enabled)
                context.startService(new Intent(context,BitlDroidService.class));

        }else{
            Log.d(TAG," Not Connected");
            context.stopService(new Intent(context,BitlDroidService.class));
        }
/* This is for network state (not wifi specifically)
          ConnectivityManager connectivityManager = (ConnectivityManager) 
                                       context.getSystemService(Context.CONNECTIVITY_SERVICE );
          NetworkInfo activeNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
          boolean isConnected = activeNetInfo != null && activeNetInfo.isConnectedOrConnecting();   
          if (isConnected)       
              Log.i("NET", "connecte" +isConnected);   
          else Log.i("NET", "not connecte" +isConnected);
        }
*/
    }
}
