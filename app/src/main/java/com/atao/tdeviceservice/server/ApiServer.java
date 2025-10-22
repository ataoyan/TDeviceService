package com.atao.tdeviceservice.server;

import android.content.Context;
import android.util.Log;

import com.atao.tdeviceservice.TDeviceServiceApplication;
import com.atao.tdeviceservice.api.ApiHandler;
import com.atao.tdeviceservice.api.AppInfoHandler;
import com.atao.tdeviceservice.api.BatteryInfoHandler;
import com.atao.tdeviceservice.api.HealthHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * API服务器 - 基于NanoHTTPD的轻量级HTTP服务器
 */
public class ApiServer extends NanoHTTPD {

    private static final String TAG = "ApiServer";
    private Context context;
    private Map<String, ApiHandler> handlers;

    public ApiServer(int port, Context context) {
        super(port);
        this.context = context;
        initHandlers();
    }

    /**
     * 初始化API处理器
     */
    private void initHandlers() {
        handlers = new HashMap<>();

        // 健康检查处理器 - 最基础的服务状态检查
        handlers.put("/api/health", new HealthHandler(context));

        // 电池信息处理器
        handlers.put("/api/battery/level", new BatteryInfoHandler(context));
        handlers.put("/api/battery/charging", new BatteryInfoHandler(context));
        handlers.put("/api/battery/health", new BatteryInfoHandler(context));
        handlers.put("/api/battery/temperature", new BatteryInfoHandler(context));
        handlers.put("/api/battery/isCharging", new BatteryInfoHandler(context));
        handlers.put("/api/battery/chargeType", new BatteryInfoHandler(context));
        handlers.put("/api/battery/current", new BatteryInfoHandler(context));
        handlers.put("/api/battery/voltage", new BatteryInfoHandler(context));

        // 应用信息处理器
        handlers.put("/api/apps/list", new AppInfoHandler(context));
        handlers.put("/api/apps/name", new AppInfoHandler(context));
        handlers.put("/api/apps/isSystem", new AppInfoHandler(context));
        handlers.put("/api/apps/isLauncher", new AppInfoHandler(context));
        handlers.put("/api/apps/launcherActivity", new AppInfoHandler(context));
        handlers.put("/api/apps/version", new AppInfoHandler(context));
        handlers.put("/api/apps/icon", new AppInfoHandler(context));

        Log.d(TAG, "API handlers initialized: " + handlers.size() + " handlers");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        String method = session.getMethod().name();

        Log.d(TAG, "API Request: " + method + " " + uri);

        // 设置CORS头
        Response response = newFixedLengthResponse(Response.Status.OK, "application/json", "");
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        response.addHeader("Access-Control-Allow-Headers", "Content-Type");

        // 处理OPTIONS请求
        if ("OPTIONS".equals(method)) {
            return response;
        }

        // 查找对应的处理器
        ApiHandler handler = handlers.get(uri);
        if (handler != null) {
            try {
                String result = handler.handleRequest(uri, session);
                return newFixedLengthResponse(Response.Status.OK, "application/json", result);
            } catch (Exception e) {
                Log.e(TAG, "Error handling request: " + uri, e);
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json",
                        "{\"error\":\"Internal server error\"}");
            }
        }

        // 404 Not Found
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json",
                "{\"error\":\"API endpoint not found\"}");
    }

    @Override
    public void start() throws IOException {
        super.start();
        Log.d(TAG, "API Server started on port " + getListeningPort());
    }

    @Override
    public void stop() {
        try {
            Log.d(TAG, "Stopping API Server...");
            super.stop();
            Log.d(TAG, "API Server stopped successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping API Server", e);
        }
    }
}
