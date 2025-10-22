package com.atao.tdeviceservice.api;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

/**
 * 应用信息API处理器
 */
public class AppInfoHandler extends ApiHandler {

    private static final String TAG = "AppInfoHandler";

    public AppInfoHandler(Context context) {
        super(context);
    }

    @Override
    public String handleRequest(String uri, NanoHTTPD.IHTTPSession session) {
        Log.d(TAG, "AppInfoHandler handling request: " + uri);

        try {
            // 获取URL参数
            Map<String, String> params = session.getParms();
            String packageName = params.get("package");

            switch (uri) {
                case "/api/apps/list":
                    return getAppsList();
                case "/api/apps/name":
                    if (packageName == null || packageName.isEmpty()) {
                        return createErrorResponse("Missing package parameter");
                    }
                    return getAppName(packageName);
                case "/api/apps/isSystem":
                    if (packageName == null || packageName.isEmpty()) {
                        return createErrorResponse("Missing package parameter");
                    }
                    return isSystemApp(packageName);
                case "/api/apps/isLauncher":
                    if (packageName == null || packageName.isEmpty()) {
                        return createErrorResponse("Missing package parameter");
                    }
                    return isLauncherApp(packageName);
                case "/api/apps/launcherActivity":
                    if (packageName == null || packageName.isEmpty()) {
                        return createErrorResponse("Missing package parameter");
                    }
                    return getLauncherActivity(packageName);
                case "/api/apps/version":
                    if (packageName == null || packageName.isEmpty()) {
                        return createErrorResponse("Missing package parameter");
                    }
                    return getAppVersion(packageName);
                case "/api/apps/icon":
                    if (packageName == null || packageName.isEmpty()) {
                        return createErrorResponse("Missing package parameter");
                    }
                    return getAppIcon(packageName);
                default:
                    return createErrorResponse("Unknown app info endpoint");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting app info for " + uri, e);
            return createErrorResponse("Failed to get app info: " + e.getMessage());
        }
    }

    /**
     * 获取所有应用包名列表
     */
    private String getAppsList() {
        try {
            PackageManager pm = context.getPackageManager();
            // 使用 PackageManager.GET_META_DATA 标志来获取所有应用（包括禁用的）
            int flags = PackageManager.GET_META_DATA;
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);

            JsonArray appsArray = new JsonArray();
            for (ApplicationInfo app : apps) {
                appsArray.add(app.packageName);
            }

            Log.d(TAG, "获取到 " + appsArray.size() + " 个应用包名");
            
            JsonObject response = new JsonObject();
            response.addProperty("success", true);
            response.add("data", appsArray);
            return response.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error getting apps list", e);
            return createErrorResponse("Failed to get apps list: " + e.getMessage());
        }
    }

    /**
     * 获取应用名称
     */
    private String getAppName(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo app = pm.getApplicationInfo(packageName, 0);
            String appName = (String) app.loadLabel(pm);
            return createSuccessResponse(appName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + packageName, e);
            return createErrorResponse("Package not found: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error getting app name", e);
            return createErrorResponse("Failed to get app name: " + e.getMessage());
        }
    }

    /**
     * 判断是否为系统应用
     */
    private String isSystemApp(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo app = pm.getApplicationInfo(packageName, 0);
            boolean isSystem = (app.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            return createSuccessResponse(String.valueOf(isSystem));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + packageName, e);
            return createErrorResponse("Package not found: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error checking system app", e);
            return createErrorResponse("Failed to check system app: " + e.getMessage());
        }
    }

    /**
     * 判断是否为桌面应用（会在桌面上显示图标的应用）
     * 即判断应用是否有启动器图标
     */
    private String isLauncherApp(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            
            // 创建一个Intent，用于查询可以在桌面上显示的Activity
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            
            // 获取所有可以在桌面显示的应用
            List<android.content.pm.ResolveInfo> resolveInfoList = 
                pm.queryIntentActivities(intent, 0);
            
            // 检查目标包名是否在列表中
            boolean hasLauncherIcon = false;
            for (android.content.pm.ResolveInfo resolveInfo : resolveInfoList) {
                if (resolveInfo.activityInfo.packageName.equals(packageName)) {
                    hasLauncherIcon = true;
                    break;
                }
            }
            
            return createSuccessResponse(String.valueOf(hasLauncherIcon));
        } catch (Exception e) {
            Log.e(TAG, "Error checking launcher app", e);
            return createErrorResponse("Failed to check launcher app: " + e.getMessage());
        }
    }

    /**
     * 获取启动Activity
     */
    private String getLauncherActivity(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            Intent intent = pm.getLaunchIntentForPackage(packageName);
            
            if (intent != null && intent.getComponent() != null) {
                String launcherActivity = intent.getComponent().getClassName();
                return createSuccessResponse(launcherActivity);
            } else {
                return createSuccessResponse("未知");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting launcher activity", e);
            return createErrorResponse("Failed to get launcher activity: " + e.getMessage());
        }
    }

    /**
     * 获取应用版本号
     */
    private String getAppVersion(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, 0);
            
            // 组合版本名称和版本码
            String versionName = packageInfo.versionName;
            long versionCode;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                versionCode = packageInfo.getLongVersionCode();
            } else {
                versionCode = packageInfo.versionCode;
            }
            
            String version = versionName + " (" + versionCode + ")";
            return createSuccessResponse(version);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + packageName, e);
            return createErrorResponse("Package not found: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error getting app version", e);
            return createErrorResponse("Failed to get app version: " + e.getMessage());
        }
    }

    /**
     * 获取应用图标（Base64编码）
     */
    private String getAppIcon(String packageName) {
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo app = pm.getApplicationInfo(packageName, 0);
            Drawable icon = app.loadIcon(pm);
            
            // 将Drawable转换为Bitmap
            Bitmap bitmap = getBitmapFromDrawable(icon);
            
            // 将Bitmap转换为Base64
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] iconBytes = baos.toByteArray();
            String base64Icon = Base64.encodeToString(iconBytes, Base64.NO_WRAP);
            
            return createSuccessResponse(base64Icon);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Package not found: " + packageName, e);
            return createErrorResponse("Package not found: " + packageName);
        } catch (Exception e) {
            Log.e(TAG, "Error getting app icon", e);
            return createErrorResponse("Failed to get app icon: " + e.getMessage());
        }
    }

    /**
     * 将Drawable转换为Bitmap
     * 修复版本 - 确保总是返回有效的图标
     */
    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable == null) {
            // 如果drawable为null，返回默认图标
            return createDefaultIcon();
        }
        
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            return bitmap != null ? bitmap : createDefaultIcon();
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O 
                   && drawable instanceof AdaptiveIconDrawable) {
            AdaptiveIconDrawable adaptiveIcon = (AdaptiveIconDrawable) drawable;
            Drawable backgroundDr = adaptiveIcon.getBackground();
            Drawable foregroundDr = adaptiveIcon.getForeground();
            
            // 确保有有效的尺寸
            int size = Math.max(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            if (size <= 0) {
                size = 64; // 默认尺寸
            }
            
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            
            // 绘制背景
            if (backgroundDr != null) {
                backgroundDr.setBounds(0, 0, size, size);
                backgroundDr.draw(canvas);
            }
            
            // 绘制前景
            if (foregroundDr != null) {
                foregroundDr.setBounds(0, 0, size, size);
                foregroundDr.draw(canvas);
            }
            
            return bitmap;
        } else {
            // Fallback for other types of drawables
            int width = drawable.getIntrinsicWidth();
            int height = drawable.getIntrinsicHeight();
            
            if (width <= 0 || height <= 0) {
                width = height = 64; // 默认尺寸
            }
            
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            return bitmap;
        }
    }
    
    /**
     * 创建默认图标（当无法获取应用图标时）
     */
    private Bitmap createDefaultIcon() {
        Bitmap bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        
        // 绘制一个简单的默认图标
        Paint paint = new Paint();
        paint.setColor(0xFF666666); // 灰色
        paint.setStyle(Paint.Style.FILL);
        paint.setAntiAlias(true);
        
        // 绘制圆形背景
        canvas.drawCircle(32, 32, 30, paint);
        
        // 绘制字母"A"
        paint.setColor(0xFFFFFFFF); // 白色
        paint.setTextSize(32);
        paint.setTextAlign(Paint.Align.CENTER);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float textY = 32 + (fontMetrics.descent - fontMetrics.ascent) / 2 - fontMetrics.descent;
        canvas.drawText("A", 32, textY, paint);
        
        return bitmap;
    }
}

