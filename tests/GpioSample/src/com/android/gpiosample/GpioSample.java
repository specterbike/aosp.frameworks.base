/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.gpiosample;

import android.app.Activity;
import android.content.Context;
import android.hardware.GpioManager;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.system.Os;
import android.system.StructPollfd;
import android.system.OsConstants;
import android.system.ErrnoException;
import java.nio.ByteBuffer;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class GpioSample extends Activity implements Runnable {

    private static final String TAG = "GpioSample";

    private static final int ASCII_ONE = 49;

    private GpioManager mGpioManager;
    private ParcelFileDescriptor mGpio;
    private FileDescriptor mFileDescriptor;
    private FileInputStream mInputStream;

    private void readGpio() {
        Log.d(TAG, "READING GPIO");

        try {
            mInputStream.skip(-1);
        } catch (IOException e) {
            // The first skip will yield an error because we're already at the start of the file. Ignore it.
            Log.d(TAG,"ERROR SKIPPING (expected on the first run)");
        }

        try {
            int result = mInputStream.read();
            Log.d(TAG, "READ GPIO: " + (result == ASCII_ONE));
        } catch (IOException e) {
            Log.e(TAG,"ERROR READING", e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mGpioManager = (GpioManager)getSystemService(Context.GPIO_SERVICE);
            mGpio = mGpioManager.openGpioPort(12, "in");
            mFileDescriptor = mGpio.getFileDescriptor();
            mInputStream = new FileInputStream(mFileDescriptor);
        } catch (IOException e) {
            Log.d(TAG, "ERROR: " + e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        readGpio();

        new Thread(this).start();
    }

    public void run() {
        Log.d(TAG, "run");

        int ret = 0;
        byte[] buffer = new byte[1024];
        while (ret >= 0) {
            try {
                Log.d(TAG, "polling");

                StructPollfd descriptors = new StructPollfd();
                descriptors.fd = mFileDescriptor;
                descriptors.events = (short) OsConstants.POLLPRI;

                ret = Os.poll(new StructPollfd[]{descriptors}, 10000);

                if (ret > 0 ) {
                    readGpio();
                }

            } catch (ErrnoException e) {
                Log.e(TAG, "poll failed", e);
                break;
            }

        }
        Log.d(TAG, "thread out");
    }
}


