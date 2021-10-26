/*
 * Copyright (C) 2021 Niels Avonds <niels@codebits.be>
 */

package com.android.server;

import android.content.Context;
import android.hardware.IGpioManager;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.util.ArrayList;

public class GpioService extends IGpioManager.Stub {

    private final Context mContext;

    public GpioService(Context context) {
        mContext = context;
    }

    public ParcelFileDescriptor openGpioPort(int gpio, String direction) {
        mContext.enforceCallingOrSelfPermission(android.Manifest.permission.GPIO, null);
        return native_open(gpio, direction);
    }

    private native ParcelFileDescriptor native_open(int gpio, String direction);
}
