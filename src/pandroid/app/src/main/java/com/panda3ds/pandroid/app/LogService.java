package com.panda3ds.pandroid.app;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.panda3ds.pandroid.lang.Task;
import com.panda3ds.pandroid.utils.Constants;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class LogService extends Service {
    private Process process;
    private RandomAccessFile logFile;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        try {
            File file = new File(getExternalMediaDirs()[0], "/logcat.txt");
            file.getParentFile().mkdirs();

            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            Runtime.getRuntime().exec("logcat -c").waitFor();

            Log.i(Constants.LOG_TAG, "=======================[ APPLICATION STARTED ]===================");
            Log.i(Constants.LOG_TAG, "Device: "+ Build.DEVICE);
            Log.i(Constants.LOG_TAG, "ABIs: "+ Arrays.toString(Build.SUPPORTED_ABIS));
            Log.i(Constants.LOG_TAG, "Display: "+ getResources().getDisplayMetrics().widthPixels+"x"+getResources().getDisplayMetrics().heightPixels);
            Log.i(Constants.LOG_TAG, "Version: "+ Build.VERSION.SDK_INT);

            logFile = new RandomAccessFile(file, "rw");
            logFile.seek(file.length());

            process = Runtime.getRuntime().exec("logcat");
            new OutputTask(logFile, process.getInputStream()).start();
            new OutputTask(logFile, process.getErrorStream()).start();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        try {
            if (process != null){
                process.destroy();
                process = null;
            }
            Log.i(Constants.LOG_TAG, "Logcat finished!!");
            logFile.close();
        } catch (Exception e){
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        try {
            Log.i(Constants.LOG_TAG, "Application finished!!");
            Thread.sleep(1000);
            stopSelf();
        } catch (Exception e){}
        super.onTaskRemoved(rootIntent);
    }

    private static class OutputTask extends Task {
        public OutputTask(RandomAccessFile logcat, InputStream stream){
            super(() -> {
                try {
                    int value;
                    while ((value = stream.read()) != -1){
                        logcat.write(value);
                    }
                } catch (Exception e){}
            });
        }
    }
}
