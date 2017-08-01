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

//sendHistory
import android.content.ContentUris;
import android.provider.Telephony.Threads;
import android.provider.Telephony;
import android.provider.BaseColumns;

import android.provider.Contacts;

public class BitlDroidService extends Service{
    private static final String LOGTAG = "bitldroid-service";
    private static final String LOGTAGsms = "bitldroid-service-viewsms";

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
                                        if (line.startsWith("H"))
                                        {
                                            sendHistory(line);
                                        } else
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
                //String key = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                //Log.d("smskey: ",key + " " + name);
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

    public void sendHistory(String line)
    {
        String[] PROJECTION = new String[] {
            Telephony.MmsSms.TYPE_DISCRIMINATOR_COLUMN,
                BaseColumns._ID,
                Telephony.Sms.Conversations.THREAD_ID,
                // For SMS
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE,
                Telephony.Sms.DATE_SENT,
                Telephony.Sms.READ,
                Telephony.Sms.TYPE,
                Telephony.Sms.STATUS,
                Telephony.Sms.LOCKED,
                Telephony.Sms.ERROR_CODE,
                // For MMS
                Telephony.Mms.SUBJECT,
                Telephony.Mms.SUBJECT_CHARSET,
                Telephony.Mms.DATE,
                Telephony.Mms.DATE_SENT,
                Telephony.Mms.READ,
                Telephony.Mms.MESSAGE_TYPE,
                Telephony.Mms.MESSAGE_BOX,
                Telephony.Mms.DELIVERY_REPORT,
                Telephony.Mms.READ_REPORT,
                Telephony.MmsSms.PendingMessages.ERROR_TYPE,
                Telephony.Mms.LOCKED,
                Telephony.Mms.STATUS,
                Telephony.Mms.TEXT_ONLY
        };

        String number;
        final String[] separated = line.split(":");
        number=separated[1];
        Log.d(LOGTAGsms, "N" + number);

        //Get the thread_id first:
        Cursor pCur = getContentResolver().query(
                Uri.parse("content://mms-sms/canonical-addresses"), 
                new String[]{"_id"},
                "address" + " = \"" + number + "\"",
                null, null);

        String thread_id = null;

        if (pCur != null) {
            if (pCur.getCount() != 0) {
                pCur.moveToNext();
                thread_id = pCur.getString(pCur.getColumnIndex("_id"));
            }
            pCur.close();
        }
        Log.d(LOGTAGsms, "T " + thread_id);

        //The easy way didn't work, so try the slow way
        if (thread_id==null)
        {
            Log.d(LOGTAGsms, "SLOW SEARCH==============");
            final String[] projection = new String[]{"_id", "ct_t","thread_id", "address","body"};
            Uri smsUri = Uri.parse("content://mms-sms/conversations/");
            Cursor cursor = getContentResolver().query(smsUri, projection, null, null, null);

            cursor.moveToFirst();

            while(cursor.moveToNext())
            {
                String newNumber;
                newNumber=cursor.getString(3);
                if (newNumber!=null){
                    //Compare the stripped phone numbers
                    if (formatNumber(newNumber).indexOf(formatNumber(number))!=-1)
                    {
                        thread_id=cursor.getString(2);
                        Log.d(LOGTAGsms,"FOUND THREAD ID: " + thread_id);
                    }
                }
            }

            if (thread_id==null)
            {
                Log.d(LOGTAGsms, "No thread ID found. SHAMEFUL DISPLAY");
                return;
            }
        }

        //Use the thread_id to load the conversation into a cursor
        Uri smsUri;
        Cursor cursor;
        smsUri = Uri.parse("content://mms-sms/conversations/"+thread_id);
        cursor = getContentResolver().query(smsUri, PROJECTION, null, null, null);

        int messageCount;
        messageCount=10; //how many messages from the history to send

        if(cursor.getCount()<messageCount)
            messageCount=cursor.getCount();

        try{
            ByteBuffer intbuf = ByteBuffer.allocate(4);
            ByteBuffer b = ByteBuffer.allocate(4);
            ByteBuffer onebuf = ByteBuffer.allocate(1);

            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();

            //Write a short header:
            //  phone number character count
            //  phone number
            //  message count
            byte[] addressBytes;
            addressBytes=number.getBytes(StandardCharsets.UTF_8); //phone number count
            intbuf.putInt(0,addressBytes.length);
            byteStream.write(intbuf.array());

            byteStream.write(addressBytes); //phone number
            intbuf.putInt(0,messageCount);
            byteStream.write(intbuf.array()); //message count

            //Add each message TLV to the frame: (int) type; (int) message length; (char*) message
            cursor.move(-messageCount);
            while(cursor.moveToNext())
            {
                String sms_mms;
                String body;
                byte[] bodyBytes=null;
                byte messageType=0;

                sms_mms=cursor.getString(0);
                body=cursor.getString(4);

                if (body!=null)
                    bodyBytes=body.getBytes(StandardCharsets.UTF_8);

                //Set the type: 0= received sms ; 1= sent sms ; 2= received mms;
                //              3= sent mms ... or something like that
                if (cursor.getInt(8)==1)
                    if (sms_mms.equals("sms"))
                        messageType=0;
                    else
                        messageType=1;
                else
                    if (sms_mms.equals("sms"))
                        messageType=2;
                    else
                        messageType=3;

                byteStream.write(messageType); //Type
                if (body!=null)
                    intbuf.putInt(0,bodyBytes.length);
                else
                    intbuf.putInt(0,0);
                byteStream.write(intbuf.array()); //Length
                if (body!=null)
                    byteStream.write(bodyBytes); //Value


                Log.d(LOGTAGsms, "TEST : |" + cursor.getString(0)+"| |"+number);
                Log.d(LOGTAGsms, "L   " + messageType + " "  + cursor.getString(4));
            }

            byte[] byteStreamArray;
            byteStreamArray=byteStream.toByteArray();
            intbuf.putInt(0,byteStreamArray.length);

            //Send the message history out through a TCP socket
            Socket sck;
            sck = new Socket(SERVERIP, PORT);
            Log.d(LOGTAG,"-------open");

            OutputStream out;
            out=sck.getOutputStream();

            out.write(0x01); //ASCII header start
            out.write('H'); //Message history identifier
            out.write(intbuf.array()); //Total frame size
            out.write(0x02);
            out.write(byteStreamArray); //Message history payload
            out.write(0x04);

            Log.i(LOGTAG, "sending message history");
            out.flush();
            sck.close();
            Log.d(LOGTAG,"-------close");
        } catch (Exception e) {
            Writer writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter(writer);
            e.printStackTrace(printWriter);
            String s = writer.toString();
            Log.e(LOGTAG,"sendHistory exception: " + e +s);
        }
    }

    private String formatNumber(String number)
    {
        if (number==null)
            return null;
        String output;

        output=number.replaceAll("[^\\d]", "");
        if (output.startsWith("1"))
        {
            output=output.substring(1);
        }
        return output;
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

