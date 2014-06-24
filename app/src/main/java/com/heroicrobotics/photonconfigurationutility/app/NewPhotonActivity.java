package com.heroicrobotics.photonconfigurationutility.app;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.lang.reflect.Method;


public class NewPhotonActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_photon);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.new_photon, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
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
            View rootView = inflater.inflate(R.layout.fragment_new_photon, container, false);
            return rootView;
        }
    }

    public void skipButtonPressed(View v) {
        Intent intent = new Intent(this, DetectPhotonActivity.class);
        startActivity(intent);
    }

    public void continueButtonPressed(View v) {
        turnWifiAPOn(wifi, this);
    }

    private static int constant = 0;

    private static final int WIFI_AP_STATE_UNKNOWN = -1;
    private static int WIFI_AP_STATE_DISABLING = 0;
    private static int WIFI_AP_STATE_DISABLED = 1;
    public int WIFI_AP_STATE_ENABLING = 2;
    public int WIFI_AP_STATE_ENABLED = 3;
    private static int WIFI_AP_STATE_FAILED = 4;

    private final String[] WIFI_STATE_TEXTSTATE = new String[]{"DISABLING",
            "DISABLED", "ENABLING", "ENABLED", "FAILED"};

    private WifiManager wifi;
    private String TAG = "WifiAP";

    private int stateWifiWasIn = -1;

    private boolean alwaysEnableWifi = true; // set to false if you want to try
    // and set wifi state back to
    // what it was before wifi ap
    // enabling, true will result in
    // the wifi always being enabled
    // after wifi ap is disabled

    /**
     * Toggle the WiFi AP state
     *
     * @param wifihandler
     * @author http://stackoverflow.com/a/7049074/1233435
     */
    public void toggleWiFiAP(WifiManager wifihandler, Context context) {
        if (wifi == null) {
            wifi = wifihandler;
        }

        boolean wifiApIsOn = getWifiAPState() == WIFI_AP_STATE_ENABLED
                || getWifiAPState() == WIFI_AP_STATE_ENABLING;
        new SetWifiAPTask(!wifiApIsOn, false, context).execute();
    }

    public void turnWifiAPOn(WifiManager wifihandler, Context context) {
        if (wifi == null) {
            wifi = wifihandler;
        }
        new SetWifiAPTask(true, false, context).execute();
    }

    /**
     * Enable/disable wifi
     *
     * @return WifiAP state
     * @author http://stackoverflow.com/a/7049074/1233435
     */
    private int setWifiApEnabled(boolean enabled) {
        Log.d(TAG, "*** setWifiApEnabled CALLED **** " + enabled);

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = "HeroicRobotics";
        config.preSharedKey = "PixelPusher";
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);

        // remember wirelesses current state
        if (enabled && stateWifiWasIn == -1) {
            stateWifiWasIn = wifi.getWifiState();
        }

        // disable wireless
        if (enabled && wifi.getConnectionInfo() != null) {
            Log.d(TAG, "disable wifi: calling");
            wifi.setWifiEnabled(false);
            int loopMax = 10;
            while (loopMax > 0
                    && wifi.getWifiState() != WifiManager.WIFI_STATE_DISABLED) {
                Log.d(TAG, "disable wifi: waiting, pass: " + (10 - loopMax));
                try {
                    Thread.sleep(500);
                    loopMax--;
                } catch (Exception e) {

                }
            }
            Log.d(TAG, "disable wifi: done, pass: " + (10 - loopMax));
        }

        // enable/disable wifi ap
        int state = WIFI_AP_STATE_UNKNOWN;
        try {
            Log.d(TAG, (enabled ? "enabling" : "disabling")
                    + " wifi ap: calling");
            wifi.setWifiEnabled(false);
            Method method1 = wifi.getClass().getMethod("setWifiApEnabled",
                    WifiConfiguration.class, boolean.class);
            // method1.invoke(wifi, null, enabled); // true
            method1.invoke(wifi, config, enabled); // true
            Method method2 = wifi.getClass().getMethod("getWifiApState");
            state = (Integer) method2.invoke(wifi);
        } catch (Exception e) {
            Log.e(WIFI_SERVICE, e.getMessage());
            // toastText += "ERROR " + e.getMessage();
        }

        // hold thread up while processing occurs
        if (!enabled) {
            int loopMax = 10;
            while (loopMax > 0
                    && (getWifiAPState() == WIFI_AP_STATE_DISABLING
                    || getWifiAPState() == WIFI_AP_STATE_ENABLED || getWifiAPState() == WIFI_AP_STATE_FAILED)) {
                Log.d(TAG, (enabled ? "enabling" : "disabling")
                        + " wifi ap: waiting, pass: " + (10 - loopMax));
                try {
                    Thread.sleep(500);
                    loopMax--;
                } catch (Exception e) {

                }
            }
            Log.d(TAG, (enabled ? "enabling" : "disabling")
                    + " wifi ap: done, pass: " + (10 - loopMax));

            // enable wifi if it was enabled beforehand
            // this is somewhat unreliable and app gets confused and doesn't
            // turn it back on sometimes so added toggle to always enable if you
            // desire
            if (stateWifiWasIn == WifiManager.WIFI_STATE_ENABLED
                    || stateWifiWasIn == WifiManager.WIFI_STATE_ENABLING
                    || stateWifiWasIn == WifiManager.WIFI_STATE_UNKNOWN
                    || alwaysEnableWifi) {
                Log.d(TAG, "enable wifi: calling");
                wifi.setWifiEnabled(true);
                // don't hold things up and wait for it to get enabled
            }

            stateWifiWasIn = -1;
        } else if (enabled) {
            int loopMax = 10;
            while (loopMax > 0
                    && (getWifiAPState() == WIFI_AP_STATE_ENABLING
                    || getWifiAPState() == WIFI_AP_STATE_DISABLED || getWifiAPState() == WIFI_AP_STATE_FAILED)) {
                Log.d(TAG, (enabled ? "enabling" : "disabling")
                        + " wifi ap: waiting, pass: " + (10 - loopMax));
                try {
                    Thread.sleep(500);
                    loopMax--;
                } catch (Exception e) {

                }
            }
            Log.d(TAG, (enabled ? "enabling" : "disabling")
                    + " wifi ap: done, pass: " + (10 - loopMax));
        }
        return state;
    }

    /**
     * Get the wifi AP state
     *
     * @return WifiAP state
     * @author http://stackoverflow.com/a/7049074/1233435
     */
    public int getWifiAPState() {
        int state = WIFI_AP_STATE_UNKNOWN;
        try {
            Method method2 = wifi.getClass().getMethod("getWifiApState");
            state = (Integer) method2.invoke(wifi);
        } catch (Exception e) {

        }

        if (state >= 10) {
            // using Android 4.0+ (or maybe 3+, haven't had a 3 device to test
            // it on) so use states that are +10
            constant = 10;
        }

        // reset these in case was newer device
        WIFI_AP_STATE_DISABLING = 0 + constant;
        WIFI_AP_STATE_DISABLED = 1 + constant;
        WIFI_AP_STATE_ENABLING = 2 + constant;
        WIFI_AP_STATE_ENABLED = 3 + constant;
        WIFI_AP_STATE_FAILED = 4 + constant;

        Log.d(TAG, "getWifiAPState.state "
                + (state == -1 ? "UNKNOWN" : WIFI_STATE_TEXTSTATE[state
                - constant]));
        return state;
    }

    /**
     * the AsyncTask to enable/disable the wifi ap
     *
     * @author http://stackoverflow.com/a/7049074/1233435
     */
    class SetWifiAPTask extends AsyncTask<Void, Void, Void> {
        boolean mMode; // enable or disable wifi AP
        boolean mFinish; // finalize or not (e.g. on exit)
        ProgressDialog d;

        /**
         * enable/disable the wifi ap
         *
         * @param mode    enable or disable wifi AP
         * @param finish  finalize or not (e.g. on exit)
         * @param context the context of the calling activity
         * @author http://stackoverflow.com/a/7049074/1233435
         */
        public SetWifiAPTask(boolean mode, boolean finish, Context context) {
            mMode = mode;
            mFinish = finish;
            d = new ProgressDialog(context);
        }

        /**
         * do before background task runs
         *
         * @author http://stackoverflow.com/a/7049074/1233435
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            d.setTitle("Turning WiFi AP " + (mMode ? "on" : "off") + "...");
            d.setMessage("...please wait a moment.");
            d.show();
        }

        /**
         * do after background task runs
         *
         * @param aVoid
         * @author http://stackoverflow.com/a/7049074/1233435
         */
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            try {
                d.dismiss();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Intent intent = new Intent(NewPhotonActivity.this, DetectPhotonActivity.class);
                        startActivity(intent);
                    }
                });
            } catch (IllegalArgumentException e) {

            }
            ;
            if (mFinish) {
                finish();
            }
        }

        /**
         * the background task to run
         *
         * @param params
         * @author http://stackoverflow.com/a/7049074/1233435
         */
        @Override
        protected Void doInBackground(Void... params) {
            setWifiApEnabled(mMode);
            return null;
        }
    }
}
