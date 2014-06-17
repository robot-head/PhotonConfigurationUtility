package com.heroicrobotics.photonconfigurationutility.app;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
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

import com.heroicrobot.dropbit.devices.pixelpusher.Pixel;
import com.heroicrobot.dropbit.devices.pixelpusher.PixelPusher;
import com.heroicrobot.dropbit.devices.pixelpusher.PusherCommand;
import com.heroicrobot.dropbit.devices.pixelpusher.Strip;
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

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }


    }


    @Override
    protected void onStart() {
        super.onStart();
        startService(new Intent(this, RegistryService.class));
        bindService(new Intent(this, RegistryService.class), myConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(myConnection);
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
        switch (item.getItemId()) {
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
                test();
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

    class TestPatternTask extends AsyncTask<Void, Void, Void> {

        private final ProgressDialog d;


        public TestPatternTask(Context context) {
            d = new ProgressDialog(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (Strip s : pusher.getStrips()) {
                for (int i = 0; i < s.getLength(); i++) {
                    s.setPixel(new Pixel((byte) 80,(byte) 0,(byte) 0), i);

                }
                SystemClock.sleep(500);
                for (int i = 0; i < s.getLength(); i++) {
                    s.setPixel(new Pixel((byte) 0,(byte) 80,(byte) 0), i);

                }
                SystemClock.sleep(500);
                for (int i = 0; i < s.getLength(); i++) {
                    s.setPixel(new Pixel((byte) 0,(byte) 0,(byte) 80), i);

                }
                SystemClock.sleep(500);
                for (int i = 0; i < s.getLength(); i++) {
                    s.setPixel(new Pixel((byte) 0,(byte) 0,(byte) 0), i);
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            d.setTitle("Running test pattern");
            d.setMessage("...please wait a moment.");
            d.show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                d.dismiss();
            } catch (IllegalArgumentException e) {

            }
        }
    }

    public void test() {
        new TestPatternTask(this).execute();
    }

    public void save() {

    }

    void applyWifi() {
        if (pusher == null) {
            return;
        }
        String ssid = sharedPref.getString(WifiSettingsActivity.PREF_SSID_KEY, "");
        String pass = sharedPref.getString(WifiSettingsActivity.PREF_WIFI_PASS_KEY, "");
        String protection = sharedPref.getString(WifiSettingsActivity.PREF_WIFI_PROTECTION_KEY, "");
        if (ssid.equals("") || pass.equals("") || protection.equals("")) {
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
