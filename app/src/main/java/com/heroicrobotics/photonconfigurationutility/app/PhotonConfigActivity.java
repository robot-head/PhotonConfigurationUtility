package com.heroicrobotics.photonconfigurationutility.app;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.heroicrobot.dropbit.devices.pixelpusher.PixelPusher;
import com.heroicrobotics.photonconfigurationutility.app.RegistryService.LocalBinder;

public class PhotonConfigActivity extends ActionBarActivity {

    public static final String PIXEL_PUSHER_MAC_ADDR_KEY = "pixel_pusher_mac_addr";
    private String mPusherMac;
    protected RegistryService myService;
    protected boolean isBound;
    private static PixelPusher pusher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photon_config);
        Intent intent = new Intent(this, RegistryService.class);
        bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment()).commit();
        }
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mPusherMac = extras.getString(PIXEL_PUSHER_MAC_ADDR_KEY);
            pusher = myService.getRegistry().getPusherMap().get(mPusherMac);
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.photon_config, menu);
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
            View rootView = inflater.inflate(R.layout.fragment_photon_config,
                    container, false);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            Spinner spinner = (Spinner) getView().findViewById(R.id.wifi_protection_spinner);
            // Create an ArrayAdapter using the string array and a default spinner layout
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(),
                    R.array.wifi_protection_choices, android.R.layout.simple_spinner_item);
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            // Apply the adapter to the spinner
            spinner.setAdapter(adapter);
            if (pusher != null) {
                ((EditText) getView().findViewById(R.id.groupNumberEditText)).setText(pusher.getGroupOrdinal());
                ((EditText) getView().findViewById(R.id.pusherNumberEditText)).setText(pusher.getControllerOrdinal());


            }
        }
    }

    private ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            LocalBinder binder = (LocalBinder) service;
            myService = binder.getService();
            isBound = true;
        }

        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }

    };

}
