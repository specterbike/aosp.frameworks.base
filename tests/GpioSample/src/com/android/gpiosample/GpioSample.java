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
import java.util.ArrayList;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class GpioSample extends Activity implements Runnable {

    private static final String TAG = "GpioSample";

    private static final int ASCII_ONE = 49;

    private static final int[] GPIO_BUTTONS = new int[]{12, 34, 46, 93};
    private static final int POLLING_TIMEOUT_MS = 10000;

    private GpioManager mGpioManager;

    private ArrayList<ParcelFileDescriptor> mParcelFileDescriptors = new ArrayList<>();
    private ArrayList<FileDescriptor> mFileDescriptors = new ArrayList<>();
    private ArrayList<FileInputStream> mInputStreams = new ArrayList<>();

    private void readGpio(int index) {
        Log.d(TAG, "READING GPIOS");

        try {
            mInputStreams.get(index).skip(-1);
        } catch (IOException e) {
            // The first skip will yield an error because we're already at the start of the file. Ignore it.
            Log.d(TAG,"ERROR SKIPPING (expected on the first run)");
        }

        try {
            int result = mInputStreams.get(index).read();
            Log.d(TAG, "READ GPIO " + GPIO_BUTTONS[index] + ": " + (result == ASCII_ONE));
        } catch (IOException e) {
            Log.e(TAG,"ERROR READING", e);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mGpioManager = (GpioManager)getSystemService(Context.GPIO_SERVICE);

            for (int gpio : GPIO_BUTTONS) {
                ParcelFileDescriptor parcelFileDescriptor = mGpioManager.openGpioPort(gpio, "in");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                FileInputStream inputStream = new FileInputStream(fileDescriptor);

                mParcelFileDescriptors.add(parcelFileDescriptor);
                mFileDescriptors.add(fileDescriptor);
                mInputStreams.add(inputStream);
            }

        } catch (IOException e) {
            Log.d(TAG, "ERROR: " + e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        for (int i = 0; i < mFileDescriptors.size(); i++) {
            readGpio(i);
        }

        new Thread(this).start();
    }

    public void run() {
        Log.d(TAG, "run");

        int ret = 0;

        // Set up the polling descriptors
        StructPollfd[] descriptors = new StructPollfd[mFileDescriptors.size()];

        for (int i = 0; i < mFileDescriptors.size(); i++) {
            StructPollfd descriptor = new StructPollfd();
            descriptor.fd = mFileDescriptors.get(i);
            descriptor.events = (short) OsConstants.POLLPRI;
            descriptors[i] = descriptor;
        }

        while (ret >= 0) {
            try {
                Log.d(TAG, "polling");

                ret = Os.poll(descriptors, POLLING_TIMEOUT_MS);

                if (ret > 0 ) {

                    // An event was returned. Check which descriptor fired an event

                    for (int i = 0; i < descriptors.length; i++) {

                        if ((descriptors[i].revents & OsConstants.POLLPRI) != 0) {
                            readGpio(i);
                        }
                    }
                }

            } catch (ErrnoException e) {
                Log.e(TAG, "poll failed", e);
                break;
            }

        }
        Log.d(TAG, "thread out");
    }
}


