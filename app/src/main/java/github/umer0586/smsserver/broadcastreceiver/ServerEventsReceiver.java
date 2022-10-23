package github.umer0586.smsserver.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import github.umer0586.smsserver.services.SMSService;

public class ServerEventsReceiver extends BroadcastReceiver {

    private static final String TAG = ServerEventsListener.class.getSimpleName();

    private ServerEventsListener serverEventsListener;
    private Context context;


    private boolean isRegistered = false;

    public ServerEventsReceiver(@NonNull Context context)
    {
        this.context = context;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(TAG, "onReceive() called with: context = [" + context + "], intent = [" + intent + "]");

        if(intent.getAction().equals(SMSService.ACTION_EVENT_SERVER_STARTED))
        {
            String IP = intent.getStringExtra(SMSService.HOST_IP);
            int port = intent.getIntExtra(SMSService.HOST_PORT,8080);
            boolean isSecure = intent.getBooleanExtra(SMSService.HOST_SECURE,false);

            if(serverEventsListener != null)
                serverEventsListener.onServerStarted(IP,port,isSecure);
        }

        if(intent.getAction().equals(SMSService.ACTION_EVENT_SERVER_STOPPED))
        {
            if(serverEventsListener != null)
                serverEventsListener.onServerStopped();
        }

        if(intent.getAction().equals(SMSService.ACTION_EVENT_SERVER_ERROR) || intent.getAction().equals(SMSService.ACTION_EVENT_FAILED_TO_OBTAIN_IP))
        {
            Bundle extras = intent.getExtras();
            Throwable exception = (Throwable) extras.getSerializable("EXCEPTION");
            Log.i(TAG, "onReceive: exception : " + exception.getMessage());

            if(serverEventsListener != null)
                serverEventsListener.onError(exception);
        }


        if(intent.getAction().equals(SMSService.ACTION_EVENT_SERVER_ALREADY_RUNNING))
        {
            String IP = intent.getStringExtra(SMSService.HOST_IP);
            int port = intent.getIntExtra(SMSService.HOST_PORT,8080);
            boolean isSecure = intent.getBooleanExtra(SMSService.HOST_SECURE,false);

            if(serverEventsListener != null)
                serverEventsListener.onServerAlreadyRunning(IP,port,isSecure);
        }


    }

    public void checkIfServerIsRunning()
    {
        Log.d(TAG, "checkIfServerIsRunning() called");
        Intent intent = new Intent();
        intent.setAction(SMSService.ACTION_REQUEST_IS_SERVER_RUNNING);
        context.sendBroadcast(intent);
    }

    public void registerEvents()
    {
        Log.d(TAG, "registerEvents() called");

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(SMSService.ACTION_EVENT_SERVER_STARTED);
        intentFilter.addAction(SMSService.ACTION_EVENT_SERVER_STOPPED);
        intentFilter.addAction(SMSService.ACTION_EVENT_SERVER_ERROR);
        intentFilter.addAction(SMSService.ACTION_EVENT_SERVER_ALREADY_RUNNING);
        intentFilter.addAction(SMSService.ACTION_EVENT_FAILED_TO_OBTAIN_IP);

        try
        {
            if(!isRegistered)
                this.context.registerReceiver(this, intentFilter);

            isRegistered = true;
        }catch(IllegalArgumentException e)
        {
            isRegistered = false;
            e.printStackTrace();
        }
    }

    public void unregisterEvents()
    {
        Log.d(TAG, "unregister() called");
       try
       {
           if (isRegistered)
               this.context.unregisterReceiver(this);

           isRegistered = false;
       }catch(IllegalArgumentException e)
       {
           isRegistered = false;
           e.printStackTrace();
       }
    }

    public void setServerEventsListener(ServerEventsListener serverEventsListener)
    {
        this.serverEventsListener = serverEventsListener;
    }

    public interface ServerEventsListener {
        void onServerStarted(String IP, int port, boolean isSecure);
        void onServerStopped();
        void onError(Throwable throwable);
        void onServerAlreadyRunning(String IP, int port, boolean isSecure);
    }

}
