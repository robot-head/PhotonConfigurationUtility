package com.heroicrobotics.photonconfigurationutility.app;

import android.content.ComponentName;
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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.heroicrobot.dropbit.devices.pixelpusher.PixelPusher;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;


public class DetectPhotonActivity extends ActionBarActivity {

    static ListView listView;
    static ArrayAdapter<PixelPusher> adapter;
    static TextView statusText;

    class UpdateAdapterRunnable implements Runnable {
        @Override
        public void run() {
            adapter.clear();
            if (myService.getRegistry() == null || myService.getRegistry().getPushers() == null) {
                return;
            }
            int numPushers = myService.getRegistry().getPushers().size();
            if (numPushers == 0) {
                statusText.setText(R.string.scan_result_no_photon_label);
                return;
            }
            if (numPushers == 1) {
                // Maybe go directly to configure
            }
            statusText.setText(R.string.scan_result_multiple_photon_label);
            for (PixelPusher pusher : myService.getRegistry()
                    .getPushers()) {
                adapter.add(pusher);

            }

        }
    }

    class AdapterObserver implements Observer {

        @Override
        public void update(Observable registry, Object updatedDevice) {
            runOnUiThread(new UpdateAdapterRunnable());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detect_photon);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, RegistryService.class), myConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(myConnection);
            isBound = false;
        }
    }

    private RegistryService myService;
    private boolean isBound;
    private ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder service) {
            RegistryService.LocalBinder binder = (RegistryService.LocalBinder) service;
            myService = binder.getService();
            isBound = true;
            myService.getRegistry().addObserver(new AdapterObserver());
            runOnUiThread(new UpdateAdapterRunnable());
        }

        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }

    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.detect_photon, menu);
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
            View rootView = inflater.inflate(R.layout.fragment_detect_photon, container, false);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle saveInstanceState) {
            super.onActivityCreated(saveInstanceState);
            listView = (ListView) getView().findViewById(R.id.pusherListView);
            statusText = (TextView) getView().findViewById(R.id.scan_results_textview);
            adapter = new ArrayAdapter<PixelPusher>(getActivity(),
                    android.R.layout.simple_list_item_1, android.R.id.text1,
                    new ArrayList<PixelPusher>()) {

                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    PixelPusher pusher = this.getItem(position);
                    TextView view = new TextView(parent.getContext());
                    view.setTextAppearance(getContext(), android.R.style.TextAppearance_Large);
                    if (pusher.getControllerOrdinal() == 0) {

                        view.setText(pusher.getMacAddress());
                        return view;
                    }

                    view.setText("Group " + pusher.getGroupOrdinal()
                            + " Controller " + pusher.getControllerOrdinal());
                    return view;
                }

            };
            listView.setAdapter(adapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Intent intent = new Intent(getActivity(), PhotonConfigActivity.class);
                    intent.putExtra(
                            PhotonConfigActivity.PIXEL_PUSHER_MAC_ADDR_KEY,
                            ((PixelPusher) parent.getItemAtPosition(position)).getMacAddress());
                    startActivity(intent);
                }

            });
        }
    }
}
