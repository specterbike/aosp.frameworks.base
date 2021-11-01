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

#define MAX_BUF (256)
#define SYSFS_GPIO_DIR "/sys/class/gpio"

namespace android
{

static struct parcel_file_descriptor_offsets_t
{
    jclass mClass;
    jmethodID mConstructor;
} gParcelFileDescriptorOffsets;

static int gpio_export(int gpio)
{
	int fd, len;
	char buf[MAX_BUF];

	fd = open(SYSFS_GPIO_DIR "/export", O_WRONLY);
	if (fd < 0) {
		ALOGE("Failed to export GPIO %d: %d\n", gpio, fd);
		return fd;
	}

	len = snprintf(buf, sizeof(buf), "%d", gpio);
	write(fd, buf, len);
	close(fd);

	return 0;
}

static int gpio_set_dir(int gpio, const char *direction)
{
	int fd, len;
	char buf[MAX_BUF];

	len = snprintf(buf, sizeof(buf), SYSFS_GPIO_DIR  "/gpio%d/direction", gpio);

	fd = open(buf, O_WRONLY);
	if (fd < 0) {
		ALOGE("Failed to set GPIO %d direction to %s: %d\n", gpio, direction, fd);
		return fd;
	}

    write(fd, direction, strlen(direction) + 1);
	close(fd);
	return 0;
}

static int gpio_set_edge(int gpio, const char *edge)
{
	int fd, len;
	char buf[MAX_BUF];

	len = snprintf(buf, sizeof(buf), SYSFS_GPIO_DIR "/gpio%d/edge", gpio);

	fd = open(buf, O_WRONLY);
	if (fd < 0) {
		ALOGE("Failed to set GPIO %d edge to %s: %d\n", gpio, edge, fd);
		return fd;
	}

	write(fd, edge, strlen(edge) + 1);
	close(fd);
	return 0;
}

static int gpio_fd_open(int gpio)
{
	int fd, len;
	char buf[MAX_BUF];

	len = snprintf(buf, sizeof(buf), SYSFS_GPIO_DIR "/gpio%d/value", gpio);

	fd = open(buf, O_RDWR);
	if (fd < 0) {
		ALOGE("Failed to open GPIO %d: %d\n", gpio, fd);
	}
	return fd;
}

static jobject android_server_GpioService_open(JNIEnv *env, jobject /* thiz */, jint gpio, jstring direction)
{
    const char *directionStr;

    // Export GPIO
    if (gpio_export(gpio) < 0) {
        return NULL;
    }

    directionStr = env->GetStringUTFChars(direction, NULL);
    if (gpio_set_dir(gpio, directionStr) < 0) {
        env->ReleaseStringUTFChars(direction, directionStr);
        return NULL;
    }

    env->ReleaseStringUTFChars(direction, directionStr);

    if (gpio_set_edge(gpio, "falling") < 0) {
        return NULL;
    }

    int fd = gpio_fd_open(gpio);
    if (fd < 0) {
        return NULL;
    }

    // Wrap in a ParcelFileDescriptor
    jobject fileDescriptor = jniCreateFileDescriptor(env, fd);
    if (fileDescriptor == NULL) {
        ALOGE("%s", "Error creating JNI file descriptor");
        close(fd);
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
