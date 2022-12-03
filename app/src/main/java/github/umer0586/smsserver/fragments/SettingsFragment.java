package github.umer0586.smsserver.fragments;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.snackbar.Snackbar;
import com.tbruyelle.rxpermissions3.RxPermissions;

import github.umer0586.smsserver.R;
import github.umer0586.smsserver.setting.AppSettings;

// TODO : #9 (Add hotspot feature)
public class SettingsFragment extends PreferenceFragmentCompat {

        private static final String TAG = SettingsFragment.class.getSimpleName();

        private RxPermissions rxPermissions;
        private Preference smsPermissionPref;
        private AppSettings appSettings;


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
        {
            setPreferencesFromResource(R.xml.settings, rootKey);

            appSettings = new AppSettings(getContext());
            rxPermissions = new RxPermissions(this);

            handlePortPref();
            handleSMSPermission();
            handleSecureConnectionPref();
            handlePasswordPref();


        }

        private void handlePasswordPref()
        {
            SwitchPreferenceCompat passwordSwitchPref = findPreference(getString(R.string.pref_key_password_switch));
            passwordSwitchPref.setOnPreferenceChangeListener(((preference, newValue) -> {

                boolean switchState = (boolean)newValue;
                appSettings.enablePassword(switchState);

                return true;
            }));


            EditTextPreference passwordPref = findPreference(getString(R.string.pref_key_password));

            passwordPref.setSummary(getAsterisks(passwordPref.getText().length()));

            passwordPref.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            });

            passwordPref.setOnPreferenceChangeListener((preference, newValue) -> {
                String password = (String)newValue;

                if(password.length() >= 4)
                {

                    preference.setSummary(getAsterisks(password.length()));

                    appSettings.savePassword(password);

                    return true;
                }

                showAlertDialog("Password","Failed to set Password.\nAtleast 4 characters required");
                return false;


            });
        }

        private String getAsterisks(int count)
        {
            String asteric = "";

            for (int i = 0; i < count; i++)
                asteric += "*";

            return asteric;
        }

        private void handleSMSPermission()
        {
            smsPermissionPref = findPreference(getString(R.string.pref_key_send_sms_permission));

            smsPermissionPref.setOnPreferenceClickListener(preference -> {

                rxPermissions
                        .request(Manifest.permission.SEND_SMS)
                        .subscribe(granted -> {
                            if (granted)  // Always true pre-M
                                Snackbar.make(requireView(),"Permission granted",Snackbar.LENGTH_SHORT).show();
                             else
                                Snackbar.make(requireView(),"Permission not granted",Snackbar.LENGTH_SHORT).show();

                        });

                return true;
            });

        }


        private void handlePortPref()
        {
            EditTextPreference portPref = findPreference(getString(R.string.pref_key_port_no));

            portPref.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);

            });

            portPref.setOnPreferenceChangeListener((preference, newValue) -> {

                try {

                    int portNo = Integer.parseInt(newValue.toString());

                    if (portNo >= 1024 && portNo <= 49151)
                    {
                        appSettings.savePortNo(portNo);

                        return true;
                    }
                    else {
                        showAlertDialog("Invalid Input","Please enter valid port No");
                        return false;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    showAlertDialog("Invalid Input","Please enter valid port No");
                    return false;
                }



            });
        }

        private void handleSecureConnectionPref()
        {
            SwitchPreferenceCompat secureConnectionPref = findPreference(getString(R.string.pref_key_secure_connection));
            secureConnectionPref.setOnPreferenceChangeListener(((preference, newValue) -> {

                boolean switchState = (boolean)newValue;
                appSettings.secureConnection(switchState);

                return true;

            }));
        }


        private void showAlertDialog(CharSequence title, CharSequence message)
        {

            new AlertDialog.Builder(getContext())
                    .setTitle(title)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton("Okay", (dialog, id) -> {
                        dialog.cancel();
                    })
                    .create()
                    .show();

        }

    }


