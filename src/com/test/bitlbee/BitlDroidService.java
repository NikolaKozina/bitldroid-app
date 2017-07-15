package com.test.bitlbee;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;



import android.telephony.SmsMessage;
import android.telephony.SmsManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import android.app.PendingIntent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.content.ContentValues;
import android.content.Context;

import java.io.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.InetAddress;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.net.ServerSocket;

import android.content.ContentValues;
import android.content.Context;
import android.app.Activity;
import android.database.Cursor;

public class BitlDroidService extends Service{
    private static final String LOGTAG = "bitlbeeservice";

    private static final int PORT = 8888;
    private static String SERVERIP;
    private boolean running = false;
    private boolean registered = false;
    private Socket sck = null;
    private ServerSocket server;
    private int MsgID =1000;
    final Context context=this;

    BroadcastReceiver SentMessageReceiver;
    BroadcastReceiver DeliveredMessageReceiver;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }
    @Override
    public void onCreate() {
        //context=this;
        Log.d(LOGTAG, "Oncreate");
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SERVERIP = preferences.getString("edittext_serverip","null");
    }
    @Override
    public void onStart(Intent intent, int startId) {
        Log.d(LOGTAG, "OnStart");
        doit();
    }
    @Override
    public void onDestroy() {
        running=false;
        Log.d(LOGTAG, "OnDestroy");
        if (registered){
            unregisterReceiver(SentMessageReceiver); //qq these two lines used to be commented out
            unregisterReceiver(DeliveredMessageReceiver);
        }
        try {
            server.close();
        }catch (Exception e) {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            String s = writer.toString();
            Log.e(LOGTAG,"Server close exception: " + e + s);
        }
    }

    public void doit()
    {
        if (running==false){
            running=true;
            try
            {
                //setContentView(R.layout.main);

                //sendContacts();

                /*
                   while (true) {
                   }
                   */

                Thread thread = new Thread() {
                    public void run() {
                        try {
                            BufferedReader socketReadStream =   null;
                            PrintWriter socketWrite = null;
                            server = new ServerSocket(PORT);


                            Log.d(LOGTAG,"starting accept loop");
                            while (running) {
                                Log.d(LOGTAG,"waiting to accept");
                                Socket socket;
                                socket = server.accept();

                                Log.d(LOGTAG,"-----accepted");

                                Log.d(LOGTAG,"INETAAAAAAAAAAAAAAAAAAAA" + socket.getInetAddress());

                                socketReadStream    =   new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                socketWrite = new PrintWriter(socket.getOutputStream(),true);

                                socketWrite.println("WWWWRRRRRRIIIIIIIIIIIIITTTTTTEEEEEEE");

                                String line     =   null;

                                //Now read the response..
                                while((line = socketReadStream.readLine()) != null){
                                    Log.d(LOGTAG, "Message: " + line);
                                    if (line.indexOf("::")!=-1)
                                    {
                                        sendSMS(line);
                                    } else {
                                        sendContacts();
                                        //socket.close();
                                        Log.d(LOGTAG, "sendContacts: " );
                                    }
                                    //QQ make 'R' a constant
                                    Log.d(LOGTAG, "line.charAt(0)==" + line.charAt(0));
                                    if(line.charAt(0)=='R')
                                    {
                                        registerClient(socket.getInetAddress());
                                    }
                                }
                                socket.close();
                                Log.d(LOGTAG,"-----closed");

                                //Thread.sleep(10);

                                //writer.write("**************************************Hello********");
                                //writer.flush();
                                //writer.close();
                            }
                            Log.d(LOGTAG,"running=false");
                            server.close();
                        } catch (Exception e) {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            String s = writer.toString();
                            Log.e(LOGTAG,"thread exception: " + e + s);
                        }
                    }
                };
                thread.start();
            } catch (Exception e) {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            String s = writer.toString();
                Log.e(LOGTAG,"exception: " + e + s);
            }
        }
        Log.d(LOGTAG,"DONE");
    }

    public void registerClient(InetAddress clientaddress)
    {
        Log.d(LOGTAG, "REGISTER: " + clientaddress);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor1 = preferences.edit();
        editor1.putString("edittext_serverip",clientaddress.getHostName());
        editor1.commit();
        SERVERIP=clientaddress.getHostName();
        //SERVERIP = preferences.edit().putString("edittext_serverip",clientaddress.getHostName());


    }

    public String readContacts()
    {
        String out=null;
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI,
                null, null, null, null);

        if (cur.getCount() > 0) {
            while (cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                String phone="0";
                if (Integer.parseInt(cur.getString(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    //System.out.println("name : " + name + ", ID : " + id);
                    //System.out.println(LOGTAG + "name : " + name + ", ID : " + id);
                    Log.i(LOGTAG, "name : " + name + ", ID : " + id);
                    //out.println("name : " + name + ", ID : " + id);

                    // get the phone number
                    Cursor pCur = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                            new String[]{id}, null);

                    boolean hasmobile=false;
                    while (pCur.moveToNext()) {
                        int type;
                        type= pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                        if (type==ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                        {
                            phone = pCur.getString(
                                    pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            hasmobile=true;
                        }
                        //System.out.println("phone" + phone);
                        Log.i(LOGTAG,"type : " + pCur.getInt(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)));
                        Log.i(LOGTAG,"phone: " + phone);
                        //out.println("phone" + phone);
                    }
                    pCur.close();
                    Log.i(LOGTAG,"hasmobile: " + hasmobile);
                    Log.i(LOGTAG,"phone    : " + phone);

                    if (hasmobile) {
                        out=out + ("X" + phone + "::" + name + ";\n");
                    }

                    /*
                    Log.i(LOGTAG,  String.format("%d", Activity.RESULT_OK));
                    Log.i(LOGTAG, "Successful");
                    Log.i(LOGTAG, String.format("%d", SmsManager.RESULT_ERROR_GENERIC_FAILURE));
                    Log.i(LOGTAG, "Failed    ");
                    Log.i(LOGTAG, String.format("%d", SmsManager.RESULT_ERROR_RADIO_OFF));
                    Log.i(LOGTAG, "Radio off ");
                    Log.i(LOGTAG, String.format("%d", SmsManager.RESULT_ERROR_NULL_PDU));
                    Log.i(LOGTAG, "No PDU    ");
                    Log.i(LOGTAG, String.format("%d", SmsManager.RESULT_ERROR_NO_SERVICE));
                    Log.i(LOGTAG, "No service");
                    */
                }
            }
            Log.i(LOGTAG, "Sent contacts");
        }
        return out;
    }

    public void sendContacts()
    {
        //sendHistory();
        if (1==1)
            try{
                Socket sck;
                sck = new Socket(SERVERIP, PORT);
                Log.d(LOGTAG,"-------open");

                OutputStream out;
                out=sck.getOutputStream();

                String message="";
                byte[] msgBytes;

                //out.println("CONNECTED");
                message=readContacts();
                Log.i(LOGTAG, message);
                msgBytes=message.getBytes(StandardCharsets.UTF_8);

                ByteBuffer b = ByteBuffer.allocate(4);
                b.putInt(msgBytes.length);

                out.write(0x01);
                out.write('C');
                out.write(b.array());
                out.write(0x02);
                out.write(msgBytes);
                out.write(0x04);

                Log.i(LOGTAG, "sending message");
                out.flush();
                sck.close();
                Log.d(LOGTAG,"-------close");
            } catch (Exception e) {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            String s = writer.toString();
                Log.e(LOGTAG,"sendContacts exception: " + e +s);
            }
    }

    public void sendHistory()
    {
        try{

            Uri mSmsinboxQueryUri = Uri.parse("content://sms/inbox");
            Cursor cursor1 = getContentResolver().query(mSmsinboxQueryUri,new String[] { "_id", "thread_id", "address", "person", "date","body", "type" }, null, null, null);
                //startManagingCursor(cursor1);
            String[] columns = new String[] { "address", "person", "date", "body","type" };
            if (cursor1.getCount() > 0) {
                String count = Integer.toString(cursor1.getCount());
                while (cursor1.moveToNext()){
                    String address = cursor1.getString(cursor1.getColumnIndex(columns[0]));

                    if(address.equalsIgnoreCase("6474495076")){ //put your number here
                        String name = cursor1.getString(cursor1.getColumnIndex(columns[1]));
                        String date = cursor1.getString(cursor1.getColumnIndex(columns[2]));
                        String body = cursor1.getString(cursor1.getColumnIndex(columns[3]));
                        String type = cursor1.getString(cursor1.getColumnIndex(columns[4]));

                        Log.d(LOGTAG, "body="+body);

                    }


                }
            }

        } catch (Exception e) {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            String s = writer.toString();
            Log.e(LOGTAG,"sendHistory exception: " + e + s);
        }


    }

    public void sendSMS(String line)
    {
        String SENT = "sent";
        String DELIVERED = "delivered";
        MsgID=MsgID+1;
        DELIVERED=DELIVERED+MsgID;
        SENT=SENT + MsgID;


        SentMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterReceiver(this);
                String result = "";

                switch (getResultCode()) {

                    case Activity.RESULT_OK:
                        result = "Successful";
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        result = "Failed    ";
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        result = "Radio off ";
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        result = "No PDU    ";
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        result = "No service";
                        break;
                }
                Log.i(LOGTAG, "result: " + result);

                /*
                   Toast.makeText(getApplicationContext(), result,
                   Toast.LENGTH_LONG).show();
                   */
                try{
                    sck = new Socket(SERVERIP, PORT);
                    Log.d(LOGTAG,"-------open:"+sck.getLocalPort());
                    OutputStream out;
                    out=sck.getOutputStream();

                    

                    String body;
                    body=  String.format("S/%5d/%2dX%s",
                            intent.getIntExtra("id",-100),
                            getResultCode(),
                            intent.getStringExtra("msg"));
                    byte[] strBytes;
                    strBytes = body.getBytes(StandardCharsets.UTF_8);

                    ByteBuffer frameLen = ByteBuffer.allocate(4);
                    frameLen.putInt(strBytes.length);

                    out.write(0x01);
                    out.write('S');
                    out.write(frameLen.array());
                    out.write(0x02);
                    out.write(strBytes);
                    out.write(0x04);


                    out.flush();
                    sck.close();
                    Log.d(LOGTAG,"-------close");
                } catch (Exception e) {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            String s = writer.toString();
                    Log.e(LOGTAG,"sent intent exception: " + e + s);
                }
            }
        };
        registerReceiver(SentMessageReceiver, new IntentFilter(SENT));

        DeliveredMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                unregisterReceiver(this);
                try{
                    sck = new Socket(SERVERIP, PORT);
                    Log.d(LOGTAG,"-------open");
                    OutputStream out;
                    out=sck.getOutputStream();

                    String result = "";
                    switch (getResultCode()) {
                        case Activity.RESULT_OK:
                            result = "Successful";
                            break;
                        case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                            result = "Failed    ";
                            break;
                        case SmsManager.RESULT_ERROR_RADIO_OFF:
                            result = "Radio off ";
                            break;
                        case SmsManager.RESULT_ERROR_NULL_PDU:
                            result = "No PDU "   ;
                            break;
                        case SmsManager.RESULT_ERROR_NO_SERVICE:
                            result = "No service";
                            break;
                    }
                    String body;
                    //body=new String;
                    body=String.format("D %10s (%5d)[%2d] X%s",
                                        result,
                                        intent.getIntExtra("id",-100),
                                        getResultCode(),
                                        intent.getStringExtra("msg"));

                    byte[] strBytes;
                    strBytes=body.getBytes(StandardCharsets.UTF_8);

                    ByteBuffer frameLen = ByteBuffer.allocate(4);
                    frameLen.putInt(strBytes.length);

                    out.write(0x01); //ASCII start of header
                    out.write('D');
                    out.write(frameLen.array());
                    out.write(0x02); //ASCII start of text
                    out.write(strBytes);
                    out.write(0x04); //ASCII end of transmission


                    //out.println("D : " + intent.getIntExtra("id",-100));
                    //Log.i("HELLO", "sending message");
                    out.flush();
                    sck.close();
                    Log.d(LOGTAG,"-------close");
                } catch (Exception e) {
                    Writer writer = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(writer);
                    e.printStackTrace(printWriter);
                    String s = writer.toString();
                    Log.e(LOGTAG,"delivered intent exception: " + e + s);
                }
            }
        };
        registerReceiver(DeliveredMessageReceiver, new IntentFilter(DELIVERED));

        registered=true;
        Intent deliveryIntent = new Intent(DELIVERED);
        Intent sentIntent = new Intent(SENT);

        final String[] separated = line.split("::"); //ZZ added final keyword
        //sms.sendTextMessage(separated[0],null,separated[1],null,null);
        SmsManager sms = SmsManager.getDefault();


        //sentIntent.putExtra("msg",separated[1]);
        sentIntent.putExtra("id",MsgID);
        sentIntent.putExtra("msg",line);
        //deliveryIntent.putExtra("msg",separated[1]);
        deliveryIntent.putExtra("id",MsgID);
        deliveryIntent.putExtra("msg",line);

        PendingIntent sentPI = PendingIntent.getBroadcast(
                getApplicationContext(), 0, sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);


        PendingIntent deliverPI = PendingIntent.getBroadcast(
                getApplicationContext(), 0, deliveryIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        sms.sendTextMessage(separated[0],null,separated[1],sentPI,deliverPI);

        //Save to history(sent messages):
        ContentValues values = new ContentValues();
        values.put("address", separated[0]);
        values.put("date", System.currentTimeMillis()+"");
        values.put("read", "1");
        values.put("type", "2");
        values.put("body", separated[1]);

        Uri uri = Uri.parse("content://sms/");
        Uri rowUri = context.getContentResolver().insert(uri,values);


        try{
            sck = new Socket(SERVERIP, PORT);
        }catch (Exception e) {
            Log.d(LOGTAG,"COULDNT OPEN SOCKET");
        }
        Log.d(LOGTAG,"-------open");
        PrintWriter out;
        try{
            out=new PrintWriter(sck.getOutputStream());
            //out.println("C" + separated[0] + "::" + separated[1] + ";"); ZZ
            out.flush();
        }catch (Exception e){
            Log.d(LOGTAG,"Coudlnt create printwriter");
        }
        //Log.i("HELLO", "sending message");
        try{
            sck.close();
        } catch (Exception e){
            Log.d(LOGTAG,"Coudln't close socket");
        }

        Log.d(LOGTAG,"-------close");

        //Intent smsintent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:"+separated[0]));
        //smsintent.putExtra("sms_body",separated[1]);
        //startActivity(smsintent);
        Log.d(LOGTAG, "SPLIT: " + separated[0]);
        Log.d(LOGTAG, "SPLIT: " + separated[1]);
    }
}

