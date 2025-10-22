package com.atao.tdeviceservice.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.atao.tdeviceservice.R;
import com.atao.tdeviceservice.server.ApiServer;
import com.karumi.dexter.BuildConfig;

/**
 * 设备信息服务 - 前台服务，确保稳定运行
 */
public class DeviceService extends Service {

    private static final String TAG = "DeviceService";
    // 移除通知相关常量
    private static final int SERVER_PORT = 8080;
    private static final int SERVICE_STOP_TIMEOUT = 5000; // 5秒超时

    private ApiServer apiServer;
    private BroadcastReceiver restartReceiver;
    private Handler keepAliveHandler;
    private Runnable keepAliveRunnable;
    private Handler stopHandler;
    private volatile boolean isServiceStopping = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "DeviceService onCreate");

        // 初始化停止处理器
        stopHandler = new Handler(Looper.getMainLooper());

        // 注册服务重启监听器
        registerRestartReceiver();

        // 检查电池优化设置
        checkBatteryOptimization();

        // 启动API服务器
        startApiServer();
        
        // 启动保活机制
        startKeepAliveMechanism();
    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "DeviceService onStartCommand - flags: " + flags + ", startId: " + startId);

        // 检查是否有停止信号
        if (intent != null && "stop".equals(intent.getStringExtra("action"))) {
            Log.d(TAG, "Received stop signal, stopping service safely");
            stopServiceSafely();
            return START_NOT_STICKY;
        }

        // 如果服务正在停止，不再启动前台服务
        if (isServiceStopping) {
            Log.d(TAG, "Service is stopping, ignoring start command");
            return START_NOT_STICKY;
        }

        try {
            // 启动后台服务，不创建通知
            Log.d(TAG, "Starting background service");

            // 使用START_STICKY确保服务被杀死后自动重启
            // 使用START_REDELIVER_INTENT确保Intent被重新传递
            return START_STICKY | START_REDELIVER_INTENT;
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground service", e);
            return START_NOT_STICKY;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "DeviceService onDestroy");

        try {
            // 立即停止前台服务状态，避免超时异常
            stopForeground(true);
            Log.d(TAG, "Foreground service stopped");

            // 清理Handler
            if (stopHandler != null) {
                stopHandler.removeCallbacksAndMessages(null);
                stopHandler = null;
            }

            // 注销重启监听器
            unregisterRestartReceiver();

            // 停止保活机制
            stopKeepAliveMechanism();
            
            // 停止API服务器
            if (apiServer != null) {
                apiServer.stop();
                Log.d(TAG, "API Server stopped");
            }
            
            Log.d(TAG, "DeviceService destroyed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during service destruction", e);
        }
    }

    // 移除所有通知相关方法

    /**
     * 检查电池优化设置
     */
    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Log.w(TAG, "App is not in battery optimization whitelist");
                // 可以在这里提示用户添加到白名单
            } else {
                Log.d(TAG, "App is in battery optimization whitelist");
            }
        }
    }

    /**
     * 注册服务重启监听器
     */
    private void registerRestartReceiver() {
        restartReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d(TAG, "Service restart detected, action: " + action + ", restarting API server");
                
                // 重新启动API服务器
                if (apiServer != null) {
                    apiServer.stop();
                }
                startApiServer();
                
                // 确保后台服务状态
                Log.d(TAG, "Service restarted successfully");
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(Intent.ACTION_MY_PACKAGE_REPLACED);
        filter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        registerReceiver(restartReceiver, filter);
    }

    /**
     * 注销服务重启监听器
     */
    private void unregisterRestartReceiver() {
        if (restartReceiver != null) {
            try {
                unregisterReceiver(restartReceiver);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Receiver was not registered", e);
            }
        }
    }

    /**
     * 启动API服务器
     */
    private void startApiServer() {
        try {
            // 如果已有服务器实例，先停止
            if (apiServer != null) {
                Log.d(TAG, "Stopping existing API Server before restart");
                apiServer.stop();
                apiServer = null;
                Thread.sleep(1000); // 等待端口释放
            }
            
            // 检查端口是否可用
            if (!isPortAvailable(SERVER_PORT)) {
                Log.w(TAG, "Port " + SERVER_PORT + " is not available");
                
                // 尝试强制清理端口
                forceCleanupPort(SERVER_PORT);
                Thread.sleep(2000); // 等待清理完成
                
                // 再次检查端口是否可用
                if (isPortAvailable(SERVER_PORT)) {
                    Log.d(TAG, "Port " + SERVER_PORT + " is now available after cleanup");
                    apiServer = new ApiServer(SERVER_PORT, this);
                } else {
                    Log.w(TAG, "Port still not available, trying to find alternative port");
                    int alternativePort = findAvailablePort(SERVER_PORT);
                    if (alternativePort != -1) {
                        Log.d(TAG, "Using alternative port: " + alternativePort);
                        apiServer = new ApiServer(alternativePort, this);
                    } else {
                        Log.e(TAG, "No available port found");
                        return;
                    }
                }
            } else {
                apiServer = new ApiServer(SERVER_PORT, this);
            }
            
            apiServer.start();
            Log.d(TAG, "API Server started on port " + apiServer.getListeningPort());
        } catch (Exception e) {
            Log.e(TAG, "Failed to start API Server", e);
            // 尝试使用备用端口
            tryAlternativePort();
        }
    }
    
    /**
     * 启动保活机制
     */
    private void startKeepAliveMechanism() {
        keepAliveHandler = new Handler(Looper.getMainLooper());
        keepAliveRunnable = new Runnable() {
            @Override
            public void run() {
                // 如果服务正在停止，不再执行保活操作
                if (isServiceStopping) {
                    Log.d(TAG, "Service is stopping, keep alive mechanism disabled");
                    return;
                }
                
                try {
                // 定期检查服务状态，保持服务活跃
                Log.d(TAG, "Keep alive check - Service running normally");
                    if (apiServer == null || !isApiServerRunning()) {
                        Log.w(TAG, "API Server not running, restarting...");
                        startApiServer();
                    }
                    
                    // 每30秒执行一次保活检查
                    keepAliveHandler.postDelayed(this, 30000);
                } catch (Exception e) {
                    Log.e(TAG, "Error in keep alive mechanism", e);
                }
            }
        };
        
        // 延迟5秒后开始保活检查
        keepAliveHandler.postDelayed(keepAliveRunnable, 5000);
        Log.d(TAG, "Keep alive mechanism started");
    }
    
    /**
     * 停止保活机制
     */
    private void stopKeepAliveMechanism() {
        if (keepAliveHandler != null && keepAliveRunnable != null) {
            keepAliveHandler.removeCallbacks(keepAliveRunnable);
            keepAliveHandler = null;
            keepAliveRunnable = null;
            Log.d(TAG, "Keep alive mechanism stopped");
        }
    }
    
    /**
     * 检查API服务器是否正在运行
     */
    private boolean isApiServerRunning() {
        if (apiServer == null) {
            return false;
        }
        
        try {
            int port = apiServer.getListeningPort();
            // 尝试通过HTTP请求检查服务器状态
            java.net.URL url = new java.net.URL("http://127.0.0.1:" + port + "/api/health");
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(2000);
            connection.setReadTimeout(2000);
            
            int responseCode = connection.getResponseCode();
            connection.disconnect();
            
            return responseCode == 200;
        } catch (Exception e) {
            Log.d(TAG, "API Server health check failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 安全停止服务
     */
    public void stopServiceSafely() {
        if (isServiceStopping) {
            Log.w(TAG, "Service is already stopping");
            return;
        }
        
        isServiceStopping = true;
        Log.d(TAG, "Stopping service safely");
        
        // 停止后台服务
        try {
            Log.d(TAG, "Background service stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping foreground service", e);
        }
        
        // 使用Handler异步停止其他组件
        if (stopHandler != null) {
            stopHandler.post(() -> {
                try {
                    // 停止保活机制
                    stopKeepAliveMechanism();
                    
                    // 停止API服务器
                    if (apiServer != null) {
                        apiServer.stop();
                        Log.d(TAG, "API Server stopped");
                    }
                    
                    // 注销重启监听器
                    unregisterRestartReceiver();
                    
                    Log.d(TAG, "All components stopped successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Error stopping components", e);
                } finally {
                    // 最终停止服务
                    stopSelf();
                }
            });
        } else {
            // 如果没有Handler，直接停止服务
            stopSelf();
        }
    }
    
    /**
     * 检查服务是否正在停止
     */
    public boolean isStopping() {
        return isServiceStopping;
    }
    
    /**
     * 检查端口是否可用
     */
    private boolean isPortAvailable(int port) {
        try {
            java.net.ServerSocket serverSocket = new java.net.ServerSocket(port);
            serverSocket.close();
            return true;
        } catch (Exception e) {
            Log.d(TAG, "Port " + port + " is not available: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 查找可用端口
     */
    private int findAvailablePort(int startPort) {
        for (int port = startPort; port <= startPort + 100; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        return -1;
    }
    
    /**
     * 尝试使用备用端口启动API服务器
     */
    private void tryAlternativePort() {
        try {
            Log.d(TAG, "Trying to start API Server on alternative port");
            int alternativePort = findAvailablePort(8081);
            if (alternativePort != -1) {
                apiServer = new ApiServer(alternativePort, this);
                apiServer.start();
                Log.d(TAG, "API Server started on alternative port " + alternativePort);
            } else {
                Log.e(TAG, "Failed to find any available port for API Server");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start API Server on alternative port", e);
        }
    }
    
    /**
     * 强制清理端口占用（通过ADB命令）
     */
    private void forceCleanupPort(int port) {
        try {
            Log.d(TAG, "Attempting to cleanup port " + port + " using ADB");
            ProcessBuilder pb = new ProcessBuilder(
                "adb", "shell", "netstat", "-tulpn", "|", "grep", ":" + port
            );
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                Log.d(TAG, "Found process using port " + port + ", attempting to kill");
                // 这里可以添加杀死进程的逻辑，但需要root权限
                Log.w(TAG, "Port cleanup requires root access");
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to cleanup port " + port + ": " + e.getMessage());
        }
    }
}