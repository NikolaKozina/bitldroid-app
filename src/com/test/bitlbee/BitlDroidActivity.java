package com.test.bitlbee;

import android.app.Activity;
import android.os.Bundle;
import java.io.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import android.util.Log;
import android.telephony.SmsMessage;
import android.telephony.SmsManager;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.app.PendingIntent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.ContactsContract;

import android.widget.Toast;
import android.content.ContentValues;
import android.content.Context;

import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import android.net.wifi.WifiManager;
import android.net.DhcpInfo;

import android.widget.CheckBox;
import android.widget.CompoundButton;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;


public class BitlDroidActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener
{
    private static final String TAG = "bitlbee_preference";
    private WifiManager mWifi;
    private SharedPreferences mPref;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        final Context context=this;
        Log.i(TAG, " onCreate(" + isMyServiceRunning(BitlDroidService.class));

        addPreferencesFromResource(R.xml.preferences);
        mPref=PreferenceManager.getDefaultSharedPreferences(context);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean servicerunning = isMyServiceRunning(BitlDroidService.class);

        boolean lastenabled = preferences.getBoolean("checkbox_enabled",false);
        Log.d(TAG, "lastenabled: " + lastenabled);
        Log.d(TAG, "servicerunning: " + servicerunning);
        if (lastenabled!=isMyServiceRunning(BitlDroidService.class)){
            SharedPreferences.Editor editor1 = preferences.edit();
            editor1.putBoolean("checkbox_enabled",isMyServiceRunning(BitlDroidService.class));
            editor1.commit();
            recreate();
        }


        mWifi=(WifiManager) getSystemService(Context.WIFI_SERVICE);

        preferences.registerOnSharedPreferenceChangeListener(this);
        //ZZpreferences.registerOnSharedPreferenceChangeListener(this);
        Preference button=(Preference)findPreference(getString(R.string.myCoolButton));
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                try{
                    String messageStr="A UDP MESSAGE\n";
                    int server_port=8889;
                    DatagramSocket s = new DatagramSocket();
                    //InetAddress local = InetAddress.getByName("192.168.0.255"); //ZZ
                    InetAddress local =getBroadcastAddress();
                    int msg_length=messageStr.length();
                    byte[] message = messageStr.getBytes();
                    DatagramPacket p = new DatagramPacket (message,msg_length,local,server_port);
                    s.send(p);
                    Log.d(TAG,"DATAGRAM sent");
                } catch (Exception e){
                    Log.d(TAG,"UDP BROKEN");
                }

                Log.d(TAG,"CLICKEEEEED");
                return true;
            }
        });

        String serverip = preferences.getString("edittext_serverip","null");
        String serverport = preferences.getString("edittext_serverport","null");
        boolean enabled = preferences.getBoolean("checkbox_enabled",false);
        Log.d(TAG,"-------serverip: " + serverip);
        Log.d(TAG,"-------serverport: " + serverport);
        Log.d(TAG,"-------enabled: " + enabled);

    }
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean enabled = preferences.getBoolean("checkbox_enabled",false);
        if (enabled==true)
        {
            //Intent startIntent = new Intent(context,BitlbeeService.class);
            startService(new Intent(this,BitlDroidService.class));
            Log.d(TAG, "Starting service");
        } else {
            stopService(new Intent(this,BitlDroidService.class));
            Log.d(TAG, "Stopping service");
        }
        Log.d(TAG, "SharedPreferenceChange " + key);
        if (key=="edittext_serverip"){
            addPreferencesFromResource(R.xml.preferences);
            Log.d(TAG,"   reload prefs");
            recreate();
        }

        try{
            //Log.d(TAG, "    " + preferences.getString(key,"null"));
            //Log.d(TAG, "    " + preferences.getString(key,"null"));
        } catch (Exception e){
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            String s = writer.toString();
            Log.d(TAG,"onSharedPreferenceChanged " + e.getMessage() +s);

        }

    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    /*
    public static String getBroadcast() throws SocketException {
        System.setProperty("java.net.preferIPv4Stack", "true");
        for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements();) {
            NetworkInterface ni = niEnum.nextElement();
            if (!ni.isLoopback()) {
                for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                    return interfaceAddress.getBroadcast().toString().substring(1);
                }
            }
        }
        return null;
    }
    */

  private InetAddress getBroadcastAddress() throws IOException {
    DhcpInfo dhcp = mWifi.getDhcpInfo();
    if (dhcp == null) {
      Log.d(TAG, "Could not get dhcp info");
      return null;
    }

    int broadcast = (dhcp.ipAddress & dhcp.netmask) | ~dhcp.netmask;
    byte[] quads = new byte[4];
    for (int k = 0; k < 4; k++)
      quads[k] = (byte) ((broadcast >> k * 8) & 0xFF);
    return InetAddress.getByAddress(quads);
  }
}
