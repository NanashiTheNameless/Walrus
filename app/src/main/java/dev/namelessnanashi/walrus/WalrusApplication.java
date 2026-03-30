/*
 * Copyright 2018 Daniel Underhay & Matthew Daley.
 *
 * This file is part of Walrus.
 *
 * Walrus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Walrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Walrus.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.namelessnanashi.walrus;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;
import android.widget.Toast;

import dev.namelessnanashi.walrus.card.carddata.HIDCardData;
import dev.namelessnanashi.walrus.device.CardDevice;
import dev.namelessnanashi.walrus.device.CardDeviceManager;
import dev.namelessnanashi.walrus.device.UsbCardDevice;
import dev.namelessnanashi.walrus.util.AppFontManager;
import dev.namelessnanashi.walrus.util.GeoUtils;

import java.util.List;

public class WalrusApplication extends Application {

    @SuppressLint("StaticFieldLeak")
    private static Context context;

    private static final long LOCATION_UPDATE_INTERVAL_MS = 2000L;

    private static Location currentBestLocation;
    private static boolean locationUpdatesStarted;

    private static final LocationListener LOCATION_LISTENER = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateBestLocation(location);
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    public static Context getContext() {
        return context;
    }

    public static Location getCurrentBestLocation() {
        return currentBestLocation != null ? new Location(currentBestLocation) : null;
    }

    private static void updateBestLocation(Location location) {
        if (location != null && (currentBestLocation == null
                || GeoUtils.isBetterLocation(location, currentBestLocation))) {
            currentBestLocation = location;
        }
    }

    public static synchronized void startLocationUpdates() {
        if (locationUpdatesStarted) {
            return;
        }

        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return;
        }

        List<String> providers = locationManager.getProviders(true);
        // CHECKSTYLE:OFF EmptyCatchBlock
        try {
            for (String provider : providers) {
                updateBestLocation(locationManager.getLastKnownLocation(provider));
                locationManager.requestLocationUpdates(provider, LOCATION_UPDATE_INTERVAL_MS, 0f,
                        LOCATION_LISTENER);
            }
            locationUpdatesStarted = !providers.isEmpty();
        } catch (SecurityException ignored) {
        }
        // CHECKSTYLE:ON EmptyCatchBlock
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            try {
                Class<?> leakCanaryClass = Class.forName("com.squareup.leakcanary.LeakCanary");
                boolean isInAnalyzerProcess = (Boolean) leakCanaryClass
                        .getMethod("isInAnalyzerProcess", Context.class)
                        .invoke(null, this);
                if (isInAnalyzerProcess) {
                    return;
                }

                leakCanaryClass.getMethod("install", Application.class).invoke(null, this);
            } catch (ClassNotFoundException ignored) {
            } catch (ReflectiveOperationException exception) {
                throw new RuntimeException("Unable to initialize LeakCanary", exception);
            }
        }

        context = getApplicationContext();
        AppFontManager.install(this);

        PreferenceManager.setDefaultValues(this, R.xml.preferences_chameleon_mini_rev_g, false);
        PreferenceManager.setDefaultValues(this, R.xml.preferences_chameleon_mini_rev_e_rebooted, false);

        HIDCardData.setup(context);

        LocalBroadcastManager.getInstance(this).registerReceiver(
                new DeviceChangedBroadcastHandler(),
                new IntentFilter(CardDeviceManager.ACTION_UPDATE));

        if (BuildConfig.DEBUG) {
            CardDeviceManager.INSTANCE.addDebugDevice(this);
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                CardDeviceManager.INSTANCE.scanForDevices(WalrusApplication.this);
            }
        }).start();
    }

    public class DeviceChangedBroadcastHandler extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String toast;
            long[] timings;
            int[] amplitudes;

            if (intent.getBooleanExtra(CardDeviceManager.EXTRA_DEVICE_WAS_ADDED, false)) {
                CardDevice cardDevice = CardDeviceManager.INSTANCE.getCardDevices().get(
                        intent.getIntExtra(CardDeviceManager.EXTRA_DEVICE_ID, -1));
                if (cardDevice == null) {
                    return;
                }

                toast = getString(R.string.device_connected,
                        cardDevice.getClass().getAnnotation(UsbCardDevice.Metadata.class).name());

                timings = new long[]{200, 200, 200, 200, 200};
                amplitudes = new int[]{255, 0, 255, 0, 255};
            } else {
                toast = getString(R.string.device_disconnected,
                        intent.getStringExtra(CardDeviceManager.EXTRA_DEVICE_NAME));

                timings = new long[]{500, 200, 500, 200, 500};
                amplitudes = new int[]{255, 0, 255, 0, 255};
            }

            Toast.makeText(context, toast, Toast.LENGTH_LONG).show();

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
            if (sharedPref.getBoolean("pref_key_on_device_connected_vibrate", true)) {
                Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
                if (vibrator != null) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(
                            timings, amplitudes, -1));
                }
            }
        }
    }
}
