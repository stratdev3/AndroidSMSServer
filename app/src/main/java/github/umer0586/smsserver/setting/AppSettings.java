package github.umer0586.smsserver.setting;

import android.content.Context;
import android.content.SharedPreferences;

import github.umer0586.smsserver.R;

public class AppSettings {

    private Context context;
    private SharedPreferences sharedPreferences;

    public AppSettings(Context context)
    {
        this.context = context;
        sharedPreferences = context.getApplicationContext().getSharedPreferences(context.getString(R.string.shared_pref_file), context.getApplicationContext().MODE_PRIVATE);
    }

    public int getPortNo()
    {
        return sharedPreferences.getInt(context.getString(R.string.pref_key_port_no), 8080);
    }

    public boolean isSecureConnectionEnable()
    {
        return sharedPreferences.getBoolean(context.getString(R.string.pref_key_secure_connection), false);
    }

    public boolean isPasswordEnable()
    {
        return sharedPreferences.getBoolean(context.getString(R.string.pref_key_password_switch), false);
    }

    public String getPassword()
    {
        return sharedPreferences.getString(context.getString(R.string.pref_key_password), null);
    }

    public void savePassword(String password)
    {
        sharedPreferences.edit()
                .putString(context.getString(R.string.pref_key_password),password)
                .commit();
    }

    public void savePortNo(int portNo)
    {
        sharedPreferences.edit()
                .putInt(context.getString(R.string.pref_key_port_no),portNo)
                .commit();
    }

    public void secureConnection(boolean state)
    {
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_key_secure_connection),state)
                .commit();
    }

    public void enablePassword(boolean state)
    {
        sharedPreferences.edit()
                .putBoolean(context.getString(R.string.pref_key_password_switch), state)
                .commit();
    }

}
