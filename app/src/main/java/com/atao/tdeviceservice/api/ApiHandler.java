package com.atao.tdeviceservice.api;

import android.content.Context;

import com.google.gson.JsonObject;

import fi.iki.elonen.NanoHTTPD;

/**
 * API处理器基类
 */
public abstract class ApiHandler {

    protected Context context;

    public ApiHandler(Context context) {
        this.context = context;
    }

    /**
     * 处理API请求
     * @param uri 请求URI
     * @param session HTTP会话
     * @return JSON响应字符串
     */
    public abstract String handleRequest(String uri, NanoHTTPD.IHTTPSession session);

    /**
     * 创建成功响应
     */
    protected String createSuccessResponse(Object data) {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.addProperty("data", data.toString());
        return response.toString();
    }

    /**
     * 创建错误响应
     */
    protected String createErrorResponse(String error) {
        JsonObject response = new JsonObject();
        response.addProperty("success", false);
        response.addProperty("error", error);
        return response.toString();
    }
}