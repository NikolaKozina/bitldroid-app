//package com.test.bitlbee.IncomingSms;
package com.test.bitlbee;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsMessage;


import android.content.SharedPreferences;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import android.telephony.SmsManager;
import android.util.Log;
import android.os.Bundle;
import android.widget.Toast;
import java.io.*;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.net.InetAddress;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.SupplicantState;

public class IncomingSms extends BroadcastReceiver
{
    private static final String TAG = "bitlbeereceiver";
    final SmsManager sms=SmsManager.getDefault();
    private Socket bitlbeeSocket = null;
    private String SERVERIP;
    private String SERVERPORT;

    public void onReceive(Context context, Intent intent) 
    {
        final Bundle bundle = intent.getExtras();

        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (mWifi.isConnected()) {
            // Do whatever
            Log.d("SSID"," Connected");
        }else{
            Log.d("SSID"," Not Connected");
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SERVERIP = preferences.getString("edittext_serverip","null");
        SERVERPORT = preferences.getString("edittext_serverport","null");

        Log.i(TAG, "IncomingSMS onReceive" );

        ///*
        String message="nothing";
        byte[] header;
        String body;
        header = new byte[7];
        try{

            if (bundle!=null) {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");


                for (int i = 0 ; i< pdusObj.length ; i++) {
                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte []) pdusObj[i]);
                    String phoneNumber = currentMessage.getDisplayOriginatingAddress();

                    String senderNum = phoneNumber;
                    message =  currentMessage.getDisplayMessageBody();

                    int duration = Toast.LENGTH_LONG;
                    //QQToast toast = Toast.makeText(context,
                    //QQ        "sendernum: " + senderNum + ", message: " + message, duration);
                    //QQtoast.show();
                    Log.i(TAG, "IncomingSMS socketstuff:    " + message );
                    Log.i(TAG, "Connecting to: " );
                    Log.i(TAG, "    SERVERIP: " + SERVERIP );
                    Log.i(TAG, "    SERVERPORT: " + SERVERIP );

                    bitlbeeSocket = new Socket(SERVERIP, Integer.parseInt(SERVERPORT));

                    OutputStream writer = bitlbeeSocket.getOutputStream();

                    byte[] strBytes;
                    ByteBuffer frameLen     = ByteBuffer.allocate(4);

                    body=senderNum + (char)0x1E + message;
                    strBytes=body.getBytes(StandardCharsets.UTF_8);
                    frameLen.putInt(strBytes.length);

                    writer.write(0x01); //ASCII start of header
                    writer.write('M');
                    writer.write(frameLen.array());
                    writer.write(0x02); //ASCII start of text
                    writer.write(strBytes);
                    writer.write(0x04); //ASCII end of transmission




                /*
                    header[0]='x';
                    header[1]=(byte)0xff;
                    header[2]=(byte)0xff;
                    header[3]=(byte)0xff;
                    header[4]=(byte)0xff;
                */

                    //clientOutWriter.println("GGG");
                    //clientOutWriter.flush();



                    Log.i(TAG, "new printwriter" + message );
                    Log.i(TAG, "println " + body );
                    //clientOutWriter.println(body);//senderNum + "::" + message + ";");
                    //clientOutWriter.flush();

                    //header=body.length();
                    //headerWriter.write(header,0,5);
                    //

                    Log.i(TAG, "flush " + message );
                    bitlbeeSocket.close();

                    Log.i(TAG, "Done sending socketstuff:    " + message );
                }
            }

        } catch (Exception e) {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            String s = writer.toString();
            Log.e(TAG, "Exception smsReceiver" + message + "  ||  " + s);
        }
        //*/
    }

}
