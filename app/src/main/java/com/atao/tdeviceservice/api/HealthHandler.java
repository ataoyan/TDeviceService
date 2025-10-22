package com.atao.tdeviceservice.api;

import android.content.Context;

import com.google.gson.JsonObject;

import fi.iki.elonen.NanoHTTPD;
import android.util.Log;

/**
 * 健康检查API处理器
 */
public class HealthHandler extends ApiHandler {

    private static final String TAG = "HealthHandler";

    public HealthHandler(Context context) {
        super(context);
    }

    @Override
    public String handleRequest(String uri, NanoHTTPD.IHTTPSession session) {
        Log.d(TAG, "HealthHandler handling request: " + uri);

        try {
            if ("/api/health".equals(uri)) {
                return getHealthStatus();
            } else {
                return createErrorResponse("Unknown health endpoint");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting health status", e);
            return createErrorResponse("Failed to get health status: " + e.getMessage());
        }
    }

    /**
     * 获取服务健康状态
     */
    private String getHealthStatus() {
        try {
            JsonObject health = new JsonObject();
            health.addProperty("status", "healthy");
            health.addProperty("service", "TDeviceService");
            health.addProperty("version", "1.0.0");
            health.addProperty("uptime", System.currentTimeMillis());
            health.addProperty("timestamp", System.currentTimeMillis());

            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.add("data", health);
            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Error getting health status", e);
            return createErrorResponse("Failed to get health status: " + e.getMessage());
        }
    }
}