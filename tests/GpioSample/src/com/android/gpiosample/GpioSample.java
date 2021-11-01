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
import android.widget.TextView;
import android.os.Handler;
import android.os.Message;

public class GpioSample extends Activity implements Runnable {

    private static final String TAG = "GpioSample";

    private static final int ASCII_PRESSED = 48;
    private static final int ASCII_NOT_PRESSED = 49;

    private static final int[] GPIO_BUTTONS = new int[]{12, 34, 46, 93};
    private static final int POLLING_TIMEOUT_MS = 10000;

    private static final int MESSAGE_LOG = 1;

    private GpioManager mGpioManager;

    private TextView mLog;

    private ArrayList<ParcelFileDescriptor> mParcelFileDescriptors = new ArrayList<>();
    private ArrayList<FileDescriptor> mFileDescriptors = new ArrayList<>();
    private ArrayList<FileInputStream> mInputStreams = new ArrayList<>();

    private void readGpio(int index, boolean firstRead) {
        Log.d(TAG, "READING GPIOS");

        if (!firstRead) {
            try {
                    mInputStreams.get(index).skip(-1);
            } catch (IOException e) {
                // The first skip will yield an error because we're already at the start of the file. Ignore it.
                Log.d(TAG,"ERROR SKIPPING (expected on the first run)");
            }
        }

        try {
            int result = mInputStreams.get(index).read();
            Log.d(TAG, "READ GPIO " + GPIO_BUTTONS[index] + ": " + (result == ASCII_PRESSED));

            if (!firstRead) {
                String action = (result == ASCII_PRESSED) ? "pressed" : "released";

                Message m = Message.obtain(mHandler, MESSAGE_LOG);
                m.obj = "GPIO" + GPIO_BUTTONS[index] + " " + action + "\n";
                mHandler.sendMessage(m);
            }

        } catch (IOException e) {
            Log.e(TAG,"ERROR READING", e);

            Message m = Message.obtain(mHandler, MESSAGE_LOG);
            m.obj = "GPIO" + GPIO_BUTTONS[index] + " error " + e + "\n";
            mHandler.sendMessage(m);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.gpio_sample);
        mLog = (TextView)findViewById(R.id.log);

        mGpioManager = (GpioManager)getSystemService(Context.GPIO_SERVICE);

        try {

            for (int gpio : GPIO_BUTTONS) {
                ParcelFileDescriptor parcelFileDescriptor = mGpioManager.openGpioPort(gpio, "in");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                FileInputStream inputStream = new FileInputStream(fileDescriptor);

                mParcelFileDescriptors.add(parcelFileDescriptor);
                mFileDescriptors.add(fileDescriptor);
                mInputStreams.add(inputStream);
            }

            new Thread(this).start();

        } catch (IOException e) {
            Log.d(TAG, "ERROR: " + e);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        for (ParcelFileDescriptor descriptor : mParcelFileDescriptors) {
            try {
                descriptor.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing", e);
            }
        }

        mParcelFileDescriptors.clear();
        mFileDescriptors.clear();
        mInputStreams.clear();

        super.onDestroy();
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

            // Read the GPIO once to prevent the poll from returning immediately on the first run
            readGpio(i, true);
        }

        while (ret >= 0) {
            try {
                Log.d(TAG, "polling");

                ret = Os.poll(descriptors, POLLING_TIMEOUT_MS);

                if (ret > 0 ) {

                    // An event was returned. Check which descriptor fired an event

                    for (int i = 0; i < descriptors.length; i++) {

                        if ((descriptors[i].revents & OsConstants.POLLPRI) != 0) {
                            readGpio(i, false);
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

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_LOG:
                    mLog.setText((String)msg.obj + mLog.getText());
                    break;
             }
        }
    };
}


