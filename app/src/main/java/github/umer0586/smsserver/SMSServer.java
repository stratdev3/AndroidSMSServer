package github.umer0586.smsserver;


import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.HashMap;

import javax.net.ssl.KeyManagerFactory;

import fi.iki.elonen.NanoHTTPD;

public class SMSServer extends NanoHTTPD {


    private static final String TAG = SMSServer.class.getSimpleName();

    private Context context;

    private boolean isSecure = false;


    private boolean isPasswordEnable = false;
    private String password;


    private onStartedListener onStartedListener;
    private onStoppedListener onStoppedListener;

    public SMSServer(@NonNull Context context,@NonNull String hostname, int port)
    {
        super(hostname, port);

        this.context = context;

    }

    public boolean isSecure()
    {
        return isSecure;
    }

    public void makeSecure()
    {

        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream keystoreStream = this.context.getAssets().open("keystore.bks");

            if (keystoreStream == null)
            {
                this.isSecure = false;
                throw new IOException("Unable to load keystore");

            }

            final String PASS = "12345";
            keystore.load(keystoreStream, PASS.toCharArray());
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keystore, PASS.toCharArray());
            makeSecure(makeSSLSocketFactory(keystore, keyManagerFactory),null);


        } catch (Exception e) {
            this.isSecure = false;
            e.printStackTrace();
            return;
        }

        this.isSecure = true;

    }

    public void enablePassword()
    {
        isPasswordEnable = true;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        Log.i(TAG, "request URI: "+ session.getUri());
        Log.i(TAG, "request headers " + session.getHeaders());


        // check requested method
        if(session.getMethod() != Method.POST)
        {
            return newFixedLengthResponse(
                    Response.Status.METHOD_NOT_ALLOWED,
                    "text/plain",
                    "Method " + session.getMethod() + " not allowed, use POST"
            );
        }


        final String contentType = session.getHeaders().get("content-type");

       if(contentType == null || !contentType.equalsIgnoreCase("application/x-www-form-urlencoded"))
       {
           return newFixedLengthResponse(
                   Response.Status.UNSUPPORTED_MEDIA_TYPE,
                   "text/plain",
                   "un supported Content-Type please use application/x-www-form-urlencoded "
           );
       }



        try {
            /*
                This step is important for reading POST parameters from body
                After calling session.parseBody(map) POST parameters will be available via session.getQueryParameterString()

                HashMap<String,String> map = new HashMap<>();
                session.parseBody(map);
                map.get("postData") contains POST parameters from request body if client has set Content-Type to application/json

                To read POST parameters when client Content-Type is application/x-form-www-urlencoded we must use session.getQueryParameterString()

                Note that session.getParams() and session.getParameters() also contains POST parameters for application/x-form-www-urlencoded
                but they also contains parameters from URL
            */
            session.parseBody(new HashMap<>());

        } catch (IOException | ResponseException e) {

          return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "text/plain",
                    "Exception occurred while parsing : " + e.getMessage()
            );
        }


        if( session.getUri().equalsIgnoreCase("/sendSMS"))
            return handleSMSRequest(session);

        return newFixedLengthResponse(
                Response.Status.NOT_FOUND,
                "text/plain",
                "unknown request path. Use /sendSMS"
        );
    }

    private Response handleSMSRequest(IHTTPSession session)
    {

        Uri uri = Uri.parse(session.getUri()+ "?" +session.getQueryParameterString());


        String phone = uri.getQueryParameter("phone");
        String message = uri.getQueryParameter("message");
        String password = uri.getQueryParameter("password");



        if(phone == null)
        {
            return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "text/plain",
                    "<phone> parameter missing"
            );

        }
        else if( message == null)
        {

            return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST,
                    "text/plain",
                    "<message> parameter missing"
            );
        }

        // don't check for password option when security option is disabled
        if( isPasswordEnable() && isSecure)
        {

            if(password == null)
            {

                return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "text/plain",
                        "<password> parameter required"
                );

            }

            else if(!this.getPassword().equals(password))
            {

                Response httpsResponse = newFixedLengthResponse("invalid Password");
                httpsResponse.addHeader("WWW-Authenticate","Invalid Password");
                httpsResponse.setMimeType("text/plain");
                httpsResponse.setStatus(Response.Status.UNAUTHORIZED);

                return httpsResponse;
            }

        }

        if(!hasPermissionToSendSMS())
        {
            return newFixedLengthResponse(
                    Response.Status.FORBIDDEN,
                    "text/plain",
                    "App has no permission to send sms, please grant permission in settings"
            );

        }

        // send sms when everything is OKAY !
        //blocking call
        final SMSResult result = SMSSender.sendSMS(context,phone,message);

        if(result.getStatus() == SMSResult.STATUS_EXCEPTION_OCCURRED)
            return newFixedLengthResponse(
                    Response.Status.BAD_REQUEST, //because the client has provided invalid address
                    "text/plain",
                    "Exception occurred while sending sms : \n " + result.getReason()
            );

        if(result.getStatus() == SMSResult.STATUS_SENT_FAIL)
            return newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "text/plain",
                    "Unable to send sms " + result.getReason()
            );

        // when successful
        return newFixedLengthResponse(
                Response.Status.OK,
                "text/plain",
                "sms successfully sent to " + phone
        );

    }

    private boolean hasPermissionToSendSMS()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            return this.context.checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
        }

        //prior to android marshmallow dangerous permission are prompt at install time
        return true;
    }

    private boolean isPasswordEnable()
    {
        return isPasswordEnable;
    }

    public void setOnStartedListener(onStartedListener onStartedListener)
    {
        this.onStartedListener = onStartedListener;
    }

    public void setOnStoppedListener(onStoppedListener onStoppedListener)
    {
        this.onStoppedListener = onStoppedListener;
    }

    @Override
    public void start() throws IOException
    {

         super.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
           if(onStartedListener!=null)
               onStartedListener.onStarted(getHostname(),getListeningPort());

    }


    @Override
    public void stop()
    {
        super.stop();
        if(onStoppedListener!=null)
            onStoppedListener.onStopped();
    }


    @FunctionalInterface
    public interface onStartedListener {
        void onStarted(final String host, final int port);
    }
    @FunctionalInterface
    public interface onStoppedListener {
        void onStopped();
    }
}


