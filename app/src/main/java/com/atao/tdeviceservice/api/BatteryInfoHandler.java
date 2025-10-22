package com.atao.tdeviceservice.api;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;

import fi.iki.elonen.NanoHTTPD;
import android.util.Log;

/**
 * 电池信息API处理器
 */
public class BatteryInfoHandler extends ApiHandler {

    private static final String TAG = "BatteryInfoHandler";

    public BatteryInfoHandler(Context context) {
        super(context);
    }

    @Override
    public String handleRequest(String uri, NanoHTTPD.IHTTPSession session) {
        Log.d(TAG, "BatteryInfoHandler handling request: " + uri);

        try {
            IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
            Intent batteryStatus = context.registerReceiver(null, ifilter);

            if (batteryStatus == null) {
                return createErrorResponse("Unable to get battery status");
            }

            switch (uri) {
                case "/api/battery/level":
                    return getBatteryLevel(batteryStatus);
                case "/api/battery/temperature":
                    return getBatteryTemperature(batteryStatus);
                case "/api/battery/health":
                    return getBatteryHealth(batteryStatus);
                case "/api/battery/charging":
                    return getChargingStatus(batteryStatus);
                case "/api/battery/isCharging":
                    return getIsCharging(batteryStatus);
                case "/api/battery/chargeType":
                    return getChargeType(batteryStatus);
                case "/api/battery/current":
                    return getBatteryCurrent(batteryStatus);
                case "/api/battery/voltage":
                    return getBatteryVoltage(batteryStatus);
                default:
                    return createErrorResponse("Unknown battery info endpoint");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting battery info for " + uri, e);
            return createErrorResponse("Failed to get battery info: " + e.getMessage());
        }
    }

    /**
     * 获取电池电量百分比
     */
    @SuppressLint("DefaultLocale")
    private String getBatteryLevel(Intent batteryStatus) {
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level == -1 || scale == -1) {
            return createErrorResponse("Unable to get battery level");
        }

        float batteryPct = level * 100 / (float) scale;
        return createSuccessResponse(String.format("%.1f", batteryPct));
    }

    /**
     * 获取电池温度
     */
    @SuppressLint("DefaultLocale")
    private String getBatteryTemperature(Intent batteryStatus) {
        int temperature = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);

        if (temperature == -1) {
            return createErrorResponse("Unable to get battery temperature");
        }

        // 温度以0.1°C为单位
        float tempCelsius = temperature / 10.0f;
        return createSuccessResponse(String.format("%.1f", tempCelsius));
    }

    /**
     * 获取电池健康状态
     */
    private String getBatteryHealth(Intent batteryStatus) {
        int health = batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, -1);

        if (health == -1) {
            return createErrorResponse("Unable to get battery health");
        }

        String healthStatus;
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_COLD:
                healthStatus = "Cold";
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                healthStatus = "Dead";
                break;
            case BatteryManager.BATTERY_HEALTH_GOOD:
                healthStatus = "Good";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                healthStatus = "Overheat";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                healthStatus = "Over Voltage";
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                healthStatus = "Unspecified Failure";
                break;
            default:
                healthStatus = "Unknown";
                break;
        }

        return createSuccessResponse(healthStatus);
    }

    /**
     * 获取充电状态
     */
    private String getChargingStatus(Intent batteryStatus) {
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

        if (status == -1) {
            return createErrorResponse("Unable to get charging status");
        }

        String chargingStatus;
        switch (status) {
            case BatteryManager.BATTERY_STATUS_CHARGING:
                chargingStatus = "Charging";
                break;
            case BatteryManager.BATTERY_STATUS_DISCHARGING:
                chargingStatus = "Discharging";
                break;
            case BatteryManager.BATTERY_STATUS_FULL:
                chargingStatus = "Full";
                break;
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                chargingStatus = "Not Charging";
                break;
            default:
                chargingStatus = "Unknown";
                break;
        }

        return createSuccessResponse(chargingStatus);
    }

    /**
     * 判断是否在充电
     */
    private String getIsCharging(Intent batteryStatus) {
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        
        if (status == -1) {
            return createErrorResponse("Unable to get charging status");
        }
        
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || 
                           status == BatteryManager.BATTERY_STATUS_FULL;
        
        return createSuccessResponse(String.valueOf(isCharging));
    }

    /**
     * 获取充电方式
     */
    private String getChargeType(Intent batteryStatus) {
        int chargePlug = batteryStatus.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        
        if (chargePlug == -1) {
            return createErrorResponse("Unable to get charge type");
        }
        
        String chargeType;
        switch (chargePlug) {
            case BatteryManager.BATTERY_PLUGGED_AC:
                chargeType = "交流电源";
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                chargeType = "USB";
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                chargeType = "无线充电";
                break;
            default:
                chargeType = "未充电";
                break;
        }
        
        return createSuccessResponse(chargeType);
    }

    /**
     * 获取电池电流（mA）
     */
    @SuppressLint("DefaultLocale")
    private String getBatteryCurrent(Intent batteryStatus) {
        try {
            BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
            if (batteryManager == null) {
                return createErrorResponse("Unable to get BatteryManager");
            }
            
            // 获取电流（单位：µA，转换为 mA 需除以 1000）
            long currentMicroAmp = batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
            int currentMilliAmp = (int) (currentMicroAmp / 1000);
            
            return createSuccessResponse(String.format("%d", currentMilliAmp));
        } catch (Exception e) {
            Log.e(TAG, "Error getting battery current", e);
            return createErrorResponse("Unable to get battery current: " + e.getMessage());
        }
    }

    /**
     * 获取电池电压（V）
     */
    @SuppressLint("DefaultLocale")
    private String getBatteryVoltage(Intent batteryStatus) {
        int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        
        if (voltage == -1) {
            return createErrorResponse("Unable to get battery voltage");
        }
        
        // 电压单位从mV转换为V
        float voltageVolts = voltage / 1000.0f;
        return createSuccessResponse(String.format("%.3f", voltageVolts));
    }
}
