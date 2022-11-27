package github.umer0586.smsserver.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.net.UnknownHostException;

import github.umer0586.smsserver.R;
import github.umer0586.smsserver.activities.MainActivity;
import github.umer0586.smsserver.broadcastreceiver.MessageReceiver;
import github.umer0586.smsserver.httpserver.SMSServer;
import github.umer0586.smsserver.util.IpUtil;


public class SMSService extends Service implements MessageReceiver.MessageListener {

    private static final String TAG = SMSService.class.getSimpleName();
    public static final String CHANNEL_ID = "ForegroundServiceChannel";

    // Http server
    private SMSServer smsServer;

    // Binder given to clients
    private final IBinder binder = new LocalBinder();

    // Broadcast intent action (published by other app's component) to stop server thread
    public static final String ACTION_STOP_SERVER = "ACTION_STOP_SERVER_"+SMSService.class.getName();

    //Intents broadcast by Fragment/Activity are received by this service via MessageReceiver (BroadCastReceiver)
    private MessageReceiver messageReceiver;


    //callbacks
    ServerStatesListener serverStatesListener;


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

        if( intent.getAction().equals(ACTION_STOP_SERVER) )
        {
            if(smsServer != null && smsServer.isAlive())
            {
                smsServer.stop();
                stopForeground(true);
            }
        }

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "onStartCommand() called with: intent = [" + intent + "], flags = [" + flags + "], startId = [" + startId + "]");
        handleAndroid8andAbove();

        String hostIP = IpUtil.getWifiIpAddress(getApplicationContext());
        int portNo = sharedPreferences.getInt(getString(R.string.pref_key_port_no), 8080);

        if (hostIP == null)
        {
            Log.i(TAG, "hostIP = null");

            if(serverStatesListener != null)
                serverStatesListener.onServerError(new UnknownHostException());

            stopForeground(true);

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

        smsServer.setOnStartedListener((ip, port, isSecure) -> {

            if(serverStatesListener != null)
                serverStatesListener.onServerStarted(ip,port,isSecure);

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

        });

        smsServer.setOnStoppedListener(() -> {

            if(serverStatesListener != null)
                serverStatesListener.onServerStopped();

            stopForeground(true);
        });


        try
        {

            smsServer.start();


        } catch (IOException e)
        {
            if(serverStatesListener != null)
                serverStatesListener.onServerError(e);

            stopForeground(true);

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

    public void isServerRunning()
    {
        if(smsServer != null && smsServer.isAlive())
        {
            if(serverStatesListener != null)
                serverStatesListener.onServerAlreadyRunning(smsServer.getHostname(), smsServer.getListeningPort(),smsServer.isSecure());
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
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
            stopForeground(true);


        }
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {

        public SMSService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SMSService.this;
        }

    }

    public void setServerStatesListener(ServerStatesListener serverStatesListener)
    {
        this.serverStatesListener = serverStatesListener;
    }

    public interface ServerStatesListener {

        void onServerStarted(String IP, int port, boolean isSecure);
        void onServerAlreadyRunning(String IP, int port, boolean isSecure);
        void onServerStopped();
        void onServerError(Throwable throwable);
    }

}