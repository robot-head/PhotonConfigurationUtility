package com.heroicrobotics.photonconfigurationutility.app;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.heroicrobot.dropbit.devices.pixelpusher.PixelPusher;
import com.heroicrobot.dropbit.devices.pixelpusher.PusherCommand;
import com.heroicrobotics.photonconfigurationutility.app.RegistryService.LocalBinder;

public class PhotonConfigActivity extends ActionBarActivity {

    public static final String PIXEL_PUSHER_MAC_ADDR_KEY = "pixel_pusher_mac_addr";
    private String mPusherMac;
    protected RegistryService myService;
    protected boolean isBound;
    private PixelPusher pusher;
    private SharedPreferences sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photon_config);
        startService(new Intent(this, RegistryService.class));
        bindService(new Intent(this, RegistryService.class), myConnection, BIND_AUTO_CREATE);
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.config_photon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch(item.getItemId()) {
            case R.id.action_reboot:
                reboot();
                return true;
            case R.id.action_save:
                save();
                return true;
            case R.id.action_apply_wifi:
                applyWifi();
                return true;
            case R.id.action_test:
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_photon_config,
                    container, false);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
        }
    }

    private ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            myService = binder.getService();
            isBound = true;
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                mPusherMac = extras.getString(PIXEL_PUSHER_MAC_ADDR_KEY);
                if (mPusherMac != null) {
                    pusher = myService.getRegistry().getPusherMap().get(mPusherMac);
                    runOnUiThread(new Runnable() {
                        public void run() {
                            ((EditText) findViewById(R.id.groupNumberEditText)).setText("" + pusher.getGroupOrdinal());
                            ((EditText) findViewById(R.id.pusherNumberEditText)).setText("" + pusher.getControllerOrdinal());
                        }
                    });

                }
            }
        }

        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }

    };

    public void save() {

    }

    void applyWifi() {
        if(pusher == null) {
           return;
        }
        String ssid = sharedPref.getString(WifiSettingsActivity.PREF_SSID_KEY, "");
        String pass = sharedPref.getString(WifiSettingsActivity.PREF_WIFI_PASS_KEY, "");
        String protection = sharedPref.getString(WifiSettingsActivity.PREF_WIFI_PROTECTION_KEY, "");
        if(ssid.equals("") || pass.equals("") || protection.equals("")) {
            Toast.makeText(this, "Invalid wifi configuration", Toast.LENGTH_LONG).show();
        }
        Log.d("TESTING", "Setting wifi config " + ssid + " : " + pass + " : " + protection);
        pusher.sendCommand(new PusherCommand((byte) 3, ssid, pass, protection));
        // TODO: Join same wifi settings
        myService.getRegistry().expireDevice(pusher.getMacAddress());

        startActivity(new Intent(this, DetectPhotonActivity.class));
    }

    void reboot() {
        pusher.sendCommand(new PusherCommand((byte) 1));
        myService.getRegistry().expireDevice(pusher.getMacAddress());
        startActivity(new Intent(this, DetectPhotonActivity.class));
    }
}
