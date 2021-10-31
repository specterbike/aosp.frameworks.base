/*
 * Copyright (C) 2021 Niels Avonds <niels@codebits.be>
 */

#define LOG_TAG "GpioServiceJNI"
#include "utils/Log.h"

#include "jni.h"
#include <nativehelper/JNIHelp.h>
#include "android_runtime/AndroidRuntime.h"

#include <sys/types.h>
#include <sys/stat.h>
#include <fcntl.h>

#define BUFFER_SIZE (256)

namespace android
{

static struct parcel_file_descriptor_offsets_t
{
    jclass mClass;
    jmethodID mConstructor;
} gParcelFileDescriptorOffsets;

static jobject android_server_GpioService_open(JNIEnv *env, jobject /* thiz */, jint gpio, jstring direction)
{
    char buf[BUFFER_SIZE];
    const char *directionStr;
 
    // Export GPIO if not exported yet

    int fd = open("/sys/class/gpio/export", O_WRONLY);
    if(fd < 0){
        ALOGE("%s", "Error opening export file in write mode");
        return NULL;
    }

    memset(buf,0,sizeof(buf));
    sprintf(buf, "%d", gpio); 
    write(fd, buf, strlen(buf));
    close(fd);

    memset(buf,0,sizeof(buf));
    sprintf(buf, "/sys/class/gpio/gpio%d/direction", gpio);

    fd = open(buf, O_WRONLY);
    if(fd < 0){
        ALOGE("%s", "Error opening direction file in write mode");
        return NULL;
    }

    directionStr = env->GetStringUTFChars(direction, NULL);

    memset(buf,0,sizeof(buf));
    sprintf(buf, "%s", directionStr); 
    write(fd, buf, strlen(buf));

    close(fd);

    env->ReleaseStringUTFChars(direction, directionStr);

    memset(buf,0,sizeof(buf));
    sprintf(buf, "/sys/class/gpio/gpio%d/value", gpio);

    // Wrap in a ParcelFileDescriptor
    jobject fileDescriptor = jniCreateFileDescriptor(env, fd);
    if (fileDescriptor == NULL) {
        return NULL;
    }
    return env->NewObject(gParcelFileDescriptorOffsets.mClass,
        gParcelFileDescriptorOffsets.mConstructor, fileDescriptor);
}


static const JNINativeMethod method_table[] = {
    { "native_open",                "(ILjava/lang/String;)Landroid/os/ParcelFileDescriptor;",
                                    (void*)android_server_GpioService_open },
};

int register_android_server_GpioService(JNIEnv *env)
{
    jclass clazz = env->FindClass("com/android/server/GpioService");
    if (clazz == NULL) {
        ALOGE("Can't find com/android/server/GpioService");
        return -1;
    }

    clazz = env->FindClass("android/os/ParcelFileDescriptor");
    LOG_FATAL_IF(clazz == NULL, "Unable to find class android.os.ParcelFileDescriptor");
    gParcelFileDescriptorOffsets.mClass = (jclass) env->NewGlobalRef(clazz);
    gParcelFileDescriptorOffsets.mConstructor = env->GetMethodID(clazz, "<init>", "(Ljava/io/FileDescriptor;)V");
    LOG_FATAL_IF(gParcelFileDescriptorOffsets.mConstructor == NULL,
                 "Unable to find constructor for android.os.ParcelFileDescriptor");

    return jniRegisterNativeMethods(env, "com/android/server/GpioService",
            method_table, NELEM(method_table));
}

};
