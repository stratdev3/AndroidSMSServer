package github.umer0586.smsserver.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.io.Serializable;
import java.net.UnknownHostException;

import github.umer0586.smsserver.R;
import github.umer0586.smsserver.SMSServer;
import github.umer0586.smsserver.activities.MainActivity;
import github.umer0586.smsserver.broadcastreceiver.MessageReceiver;
import github.umer0586.smsserver.util.IpUtil;

/*
* This is a Foreground Service which runs HTTP server (SMSServer) thread and notifies
* its events by broadcasting intents and receives broadcasts from fragment/Activity via MessageListener
* interface provided by MessageReceiver (BroadCastReceiver)
* */

public class SMSService extends Service implements MessageReceiver.MessageListener {

    private static final String TAG = SMSService.class.getSimpleName();
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    // Http server
    private SMSServer smsServer;

    // Broadcast intent action (published by other app's component) to stop server thread
    public static final String ACTION_STOP_SERVER = "ACTION_STOP_SERVER";

    // Intents actions which are broadcast by this service
    public static final String ACTION_EVENT_SERVER_STARTED = "ACTION_EVENT_SERVER_STARTED";
    public static final String ACTION_EVENT_SERVER_STOPPED = "ACTION_EVENT_SERVER_STOPPED";
    public static final String ACTION_EVENT_SERVER_ERROR = "ACTION_EVENT_SERVER_ERROR";
    public static final String ACTION_EVENT_SERVER_ALREADY_RUNNING = "ACTION_EVENT_SERVER_ALREADY_RUNNING";
    public static final String ACTION_EVENT_FAILED_TO_OBTAIN_IP = "ACTION_EVENT_FAILED_TO_OBTAIN_IP";

    //Broadcast intent action (published by other app's component) to check state of http server thread
    public static final String ACTION_REQUEST_IS_SERVER_RUNNING = "ACTION_REQUEST_IS_SERVER_RUNNING";

    public static final String HOST_IP = "HOST_IP";
    public static final String HOST_PORT= "HOST_PORT";
    public static final String HOST_SECURE = "HOST_SECURE";

    //Intents broadcast by Fragment/Activity are received by this service via MessageReceiver (BroadCastReceiver)
    private MessageReceiver messageReceiver;


    // cannot be zero
    public static final int ON_GOING_NOTIFICATION_ID = 123;
    private static final int TEMP_NOTIFICATION_ID = 420;

    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "onCreate() called");

        createNotificationChannel();
        sharedPreferences = getApplicationContext().getSharedPreferences(getString(R.string.shared_pref_file), getApplicationContext().MODE_PRIVATE);

        messageReceiver = new MessageReceiver(getApplicationContext());
        messageReceiver.setMessageListener(this);
        messageReceiver.registerEvents();

    }

    @Override
    public void onMessage(Intent intent)
    {
        Log.d(TAG, "onMessage() called with: intent = [" + intent + "]");

        if(intent.getAction().equals(ACTION_REQUEST_IS_SERVER_RUNNING) )
        {
            if (smsServer != null && smsServer.isAlive())
            {
                Intent i = new Intent(ACTION_EVENT_SERVER_ALREADY_RUNNING);

                i.putExtra(HOST_IP, smsServer.getHostname());
                i.putExtra(HOST_PORT, smsServer.getListeningPort());
                i.putExtra(HOST_SECURE, smsServer.isSecure());

                Log.i(TAG, "SMS server already running ");
                Log.i(TAG, "Broadcasting : " + ACTION_EVENT_SERVER_ALREADY_RUNNING);

                sendBroadcast(i);
            } else
                Log.i(TAG, "SMS server not running");
        }

        if( intent.getAction().equals(ACTION_STOP_SERVER) )
        {
            stopForeground(true);
            stopSelf();
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");

        String hostIP = IpUtil.getWifiIpAddress(getApplicationContext());
        int portNo = sharedPreferences.getInt(getString(R.string.pref_key_port_no), 8080);

        if (hostIP == null)
        {
            Log.i(TAG, "hostIP = null");


            Bundle extras = new Bundle();
            extras.putSerializable("EXCEPTION",(Serializable) new UnknownHostException());

            Intent i = new Intent(ACTION_EVENT_FAILED_TO_OBTAIN_IP);
            i.putExtras(extras);

            sendBroadcast(i);

            handleAndroid8andAbove();

            /*
              Here if we don't call startForeground() for android 8 and above the app
              will crash since service.startForeground() must be called with 5 second after call to context.startForegroundService(),
              handleAndroid8andAbove() (called above) should explicitly handle this
            */
            stopForeground(true);
            stopSelf();


            return START_NOT_STICKY;
        }

        smsServer = new SMSServer(getApplicationContext(), hostIP, portNo);
        boolean isSecureConnectionEnable = sharedPreferences.getBoolean(getString(R.string.pref_key_secure_connection), false);
        // If user has enabled "Use secure connection" option
        if (isSecureConnectionEnable)
            smsServer.makeSecure();


        boolean isPasswordEnable = sharedPreferences.getBoolean(getString(R.string.pref_key_password_switch), false);
        //If user has enabled the password option
        if (isPasswordEnable)
        {
            String password = sharedPreferences.getString(getString(R.string.pref_key_password), null);
            smsServer.enablePassword();
            smsServer.setPassword(password);
        }

        smsServer.setOnStartedListener((ip, port) -> {

            Intent i = new Intent(ACTION_EVENT_SERVER_STARTED);
            i.putExtra(HOST_IP, hostIP);
            i.putExtra(HOST_PORT, port);
            i.putExtra(HOST_SECURE, smsServer.isSecure());
            sendBroadcast(i);


        });

        smsServer.setOnStoppedListener(() -> {
            Intent i = new Intent(ACTION_EVENT_SERVER_STOPPED);
            sendBroadcast(i);
        });


        try
        {

            smsServer.start();


            Intent notificationIntent = new Intent(this, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_sms_24)
                    .setContentTitle("SMS Server Running")
                    .setContentText(getAddress())
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    // Set the intent that will fire when the user taps the notification
                    .setContentIntent(pendingIntent)
                    // don't cancel notification when user taps it
                    .setAutoCancel(false);

            Notification notification = notificationBuilder.build();
            startForeground(ON_GOING_NOTIFICATION_ID, notification);

        } catch (IOException e)
        {
            Bundle extras = new Bundle();
            extras.putSerializable("EXCEPTION",(Serializable) e);

            Intent i = new Intent(ACTION_EVENT_SERVER_ERROR);
            i.putExtras(extras);

            sendBroadcast(i);
            e.printStackTrace();

            handleAndroid8andAbove();
            stopForeground(true);
            stopSelf();
        }


        return START_NOT_STICKY;
    }

    private Spanned getAddress()
    {
        Spanned address = null;

        if(smsServer != null && smsServer.isAlive())
        {
            if(smsServer.isSecure())
                address = Html.fromHtml("<b><font color=\"#5c6bc0\">https://</font>" + smsServer.getHostname() + ":" + smsServer.getListeningPort() + "</b>");
            else
                address = Html.fromHtml("<b>http://" + smsServer.getHostname() + ":" + smsServer.getListeningPort() + "</b>");

        }

        return address;
    }


    private void createNotificationChannel()
    {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            Log.d(TAG, "createNotificationChannel() called");

            CharSequence name = "SMS-Server";
            String description = "Notifications from SMS-server";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");

        if (smsServer != null)
            if (smsServer.isAlive())
                smsServer.stop();

        messageReceiver.unregisterEvents();

    }

    @Override
    public IBinder onBind(Intent intent)
    {

        return null;
    }

    /*
    * For Android 8 and above there is a framework restriction which required service.startForeground()
    * method to be called within five seconds after call to Context.startForegroundService()
    * so make sure we call this method even if we are returning from service.onStartCommand() without calling
    * service.startForeground()
    *
    * */
    private void handleAndroid8andAbove()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {

            Notification tempNotification =  new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_baseline_sms_24)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(TEMP_NOTIFICATION_ID, tempNotification);
            //stopForeground(true);


        }
    }
}