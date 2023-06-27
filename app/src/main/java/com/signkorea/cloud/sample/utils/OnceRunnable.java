package com.signkorea.cloud.sample.utils;

import androidx.annotation.NonNull;

public class OnceRunnable {
    private boolean executed = false;
    private final Runnable runnable;

    public OnceRunnable(@NonNull Runnable runnable) {
        this.runnable = runnable;
    }

    public synchronized void run() {
        if (!executed) {
            runnable.run();
            executed = true;
        }
    }
}
