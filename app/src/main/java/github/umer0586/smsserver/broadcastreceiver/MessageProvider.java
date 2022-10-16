package github.umer0586.smsserver.broadcastreceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import androidx.annotation.NonNull;

import github.umer0586.smsserver.services.SMSService;

public class MessageProvider extends BroadcastReceiver {

    private static final String TAG = MessageProvider.class.getSimpleName();

    private MessageListener messageListener;

    private Context context;

    private boolean isRegistered = false;

    public static final int MESSAGE_IS_SERVER_RUNNING = 0;
    public static final int MESSAGE_STOP_SERVER = 1;

    public MessageProvider(@NonNull Context context)
    {
        this.context = context;
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.d(TAG, "onReceive() called with: context = [" + context + "], intent = [" + intent + "]");

        if(intent.getAction().equals(SMSService.ACTION_REQUEST_IS_SERVER_RUNNING))
        {
            if(messageListener != null)
                messageListener.onMessage(MESSAGE_IS_SERVER_RUNNING);
        }

        if(intent.getAction().equals(SMSService.ACTION_STOP_SERVER))
        {
            if(messageListener != null)
                messageListener.onMessage(MESSAGE_STOP_SERVER);
        }
    }

    public void setMessageListener(MessageListener messageListener)
    {
        this.messageListener = messageListener;
    }

    public void registerEvents()
    {
        Log.d(TAG, "registerEvents() called");

        IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(SMSService.ACTION_REQUEST_IS_SERVER_RUNNING);
        intentFilter.addAction(SMSService.ACTION_STOP_SERVER);

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


    public interface MessageListener {
        public void onMessage(int message);
    }
}
