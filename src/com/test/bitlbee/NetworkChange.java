package com.test.bitlbee;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import android.content.SharedPreferences;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import android.util.Log;
import android.os.Bundle;

import java.io.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetAddress;

public class NetworkChange extends BroadcastReceiver 
{
    private static final String TAG = "bitldroid-network";
    private Socket sck = null;
    private String SERVERIP;
    private String SERVERPORT;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.i(TAG, "Network change" );
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SERVERIP = preferences.getString("edittext_serverip","null");
        SERVERPORT = preferences.getString("edittext_serverport","null");

        String status = NetworkUtil.getConnectivityStatusString(context);
            Log.i(TAG,"STAAAAAAAAAAAAAAAAAATUS" + status);

        try{

        sck = new Socket(SERVERIP, Integer.parseInt(SERVERPORT));
        PrintWriter out;
        out=new PrintWriter(sck.getOutputStream());

        out.println("N/" +status + ";");
        out.flush();
        sck.close();
        } catch (Exception e)
        {
            Log.i(TAG,"EXCEPTION " +e);
        }


        //QQToast.makeText(context, status, Toast.LENGTH_LONG).show();
    }

}
