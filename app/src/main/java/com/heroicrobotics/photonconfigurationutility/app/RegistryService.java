package com.heroicrobotics.photonconfigurationutility.app;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

import com.heroicrobot.dropbit.devices.pixelpusher.PixelPusher;
import com.heroicrobot.dropbit.registry.DeviceRegistry;

import java.util.Observable;
import java.util.Observer;

public class RegistryService extends Service {

    class PixelPusherObserver implements Observer {

        @Override
        public void update(Observable registry, Object updatedDevice) {
            if (updatedDevice != null) {
                ((DeviceRegistry) registry).startPushing();
            }

        }

    }

    public static DeviceRegistry registry;
    private static PixelPusherObserver observer;

    // final Messenger mMessenger = new Messenger(new IncomingHandler());

    @Override
    public IBinder onBind(Intent arg0) {
        // return mMessenger.getBinder();
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (registry == null) {
            return;
        }
        registry.stopPushing();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (registry == null) {
            registry = new DeviceRegistry();
            observer = new PixelPusherObserver();
            registry.addObserver(observer);
        }
    }

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        RegistryService getService() {
            return RegistryService.this;
        }
    }

    public DeviceRegistry getRegistry() {
        return registry;
    }

}
