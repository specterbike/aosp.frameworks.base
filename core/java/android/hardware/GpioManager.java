/*
 * Copyright (C) 2021 Niels Avonds <niels@codebits.be>
 */

package android.hardware;

import android.annotation.SystemService;
import android.annotation.UnsupportedAppUsage;
import android.content.Context;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.IOException;

/**
 * @hide
 */
@SystemService(Context.GPIO_SERVICE)
public class GpioManager {
    private static final String TAG = "GpioManager";

    private final Context mContext;
    private final IGpioManager mService;

    /**
     * {@hide}
     */
    public GpioManager(Context context, IGpioManager service) {
        mContext = context;
        mService = service;
    }

    /**
     * Opens and returns the {@link android.hardware.GpioPort} for the given GPIO pin and direction.
     * The direction of the Gpio port must be one of: "in" or "out"
     *
     * @param gpio The pin number
     * @param direction The direction to use the GPIO in
     * @return the Gpio port
     */
    @UnsupportedAppUsage
    public ParcelFileDescriptor openGpioPort(int gpio, String direction) throws IOException {
        try {
            ParcelFileDescriptor pfd = mService.openGpioPort(gpio, direction);
            if (pfd != null) {
                return pfd;
            } else {
                throw new IOException("Could not open Gpio port " + gpio);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
