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
import java.nio.ByteBuffer;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

public class GpioSample extends Activity {

    private static final String TAG = "GpioSample";

    private GpioManager mGpioManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            mGpioManager = (GpioManager)getSystemService(Context.GPIO_SERVICE);
            ParcelFileDescriptor descriptor = mGpioManager.openGpioPort(110, "in");
            FileDescriptor file = descriptor.getFileDescriptor();
            FileInputStream is = new FileInputStream(file);
            Log.d(TAG, "READING GPIO 110");
            int result = is.read();
            Log.d(TAG, "READ GPIO: " + result);
        } catch (IOException e) {
            Log.d(TAG, "ERROR: " + e);
        }
    }
}


