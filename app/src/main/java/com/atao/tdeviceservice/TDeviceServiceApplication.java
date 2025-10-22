package com.atao.tdeviceservice;

import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.atao.tdeviceservice.service.DeviceService;
import com.karumi.dexter.BuildConfig;

/**
 * 应用程序类 - 无界面后台服务应用
 */
public class TDeviceServiceApplication extends Application {

    private static final String TAG = "TDeviceServiceApp";
    private static TDeviceServiceApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        Log.d(TAG, "TDeviceServiceApplication created - Starting background service");
        
        // 自动启动后台服务
        startBackgroundService();
    }

    /**
     * 启动后台服务
     */
    private void startBackgroundService() {
        try {
            Intent serviceIntent = new Intent(this, DeviceService.class);
            
            // 对于Android 8.0+，使用startForegroundService
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
            
            Log.d(TAG, "Background service started successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start background service", e);
        }
    }

    /**
     * 获取应用实例
     */
    public static TDeviceServiceApplication getInstance() {
        return instance;
    }
}
