/*
 * Copyright (C) 2015 Preetam D'Souza
 *
 * Inspired by android_view_SurfaceSession.cpp
 *
 */

#define LOG_TAG "PerspectiveManagerJNI"

#include "JNIHelp.h"

#include <android_runtime/AndroidRuntime.h>
#include <utils/Log.h>
#include <utils/RefBase.h>

#include <binder/IServiceManager.h>
#include <perspective/IPerspectiveService.h>

namespace android {

static struct {
    jfieldID mNativeClient;
} gPerspectiveManagerClassInfo;


/**
 * Wrapper for IPerspectiveService Binder proxy.
 *
 * In order for us to preserve the Binder proxy between JNI calls
 * we wrap up the proxy in this wrapper, allocate it on the heap,
 * and store a pointer within the corresponding Java class that is
 * passed as an arg to all subsequent calls.
 *
 * Error handling: if the remote does not exist, we always return false.
 *
 * *I think the sp<> handle adds a reference so that the object is
 * not destroyed, i.e. we don't need to manually incStrong().
 */
class PerspectiveClient {
public:
    PerspectiveClient(sp<IPerspectiveService> proxy)
        : mProxy(proxy) {
        // listen for remote death
        mDeathRecipient = new MDeathRecipient(*const_cast<PerspectiveClient*>(this));
        mProxy->asBinder()->linkToDeath(mDeathRecipient);
    }

    void remoteDied() {
        mProxy = NULL;
        mDeathRecipient = NULL;
    }

    bool start() {
        return mProxy != NULL && mProxy->start();
    }

    bool stop() {
        return mProxy != NULL && mProxy->stop();
    }

    bool isRunning() {
        return mProxy != NULL && mProxy->isRunning();
    }

private:
    sp<IPerspectiveService> mProxy;

    class MDeathRecipient : public IBinder::DeathRecipient {
        PerspectiveClient& mClient;
        virtual void binderDied(const wp<IBinder>& who) {
            ALOGW("PerspectiveService remote died [%p]",
                  who.unsafe_get());
            mClient.remoteDied();
        }
    public:
        MDeathRecipient(PerspectiveClient& client) : mClient(client) { }
    };
    sp<MDeathRecipient> mDeathRecipient;
};

static jlong nativeCreateClient(JNIEnv* env, jclass clazz) {
    sp<IPerspectiveService> client;
    getService(String16("PerspectiveService"), &client);
    if (client == NULL) {
        ALOGE("Failed to get a handle to PerspectiveService from ServiceManager!");
        // we go ahead and wrap up the null ptr anyway...our wrapper
        // will always return false
    }
    PerspectiveClient *wrapper = new PerspectiveClient(client);
    return reinterpret_cast<jlong>(wrapper);
}

static jboolean nativeStart(JNIEnv *env, jclass clazz, jlong ptr) {
    PerspectiveClient *client = reinterpret_cast<PerspectiveClient*>(ptr);
    return client->start();
}

static jboolean nativeStop(JNIEnv *env, jclass clazz, jlong ptr) {
    PerspectiveClient *client = reinterpret_cast<PerspectiveClient*>(ptr);
    return client->stop();
}

static jboolean nativeIsRunning(JNIEnv *env, jclass clazz, jlong ptr) {
    PerspectiveClient *client = reinterpret_cast<PerspectiveClient*>(ptr);
    return client->isRunning();
}

static JNINativeMethod gMethods[] = {
    /* name, signature, funcPtr */
    { "nativeCreateClient", "()J",
            (void *)nativeCreateClient },
    { "nativeStart", "(J)Z",
            (void *)nativeStart },
    { "nativeStop", "(J)Z",
            (void *)nativeStop },
    { "nativeIsRunning", "(J)Z",
            (void *)nativeIsRunning }
};

int register_android_mperspective_PerspectiveManager(JNIEnv* env) {
    int res = jniRegisterNativeMethods(env, "android/mperspective/PerspectiveManager",
            gMethods, NELEM(gMethods));
    LOG_ALWAYS_FATAL_IF(res < 0, "Unable to register native methods.");

    jclass clazz = env->FindClass("android/mperspective/PerspectiveManager");
    gPerspectiveManagerClassInfo.mNativeClient = env->GetFieldID(clazz, "mNativeClient", "J");

    return res;
}

} // namespace android
