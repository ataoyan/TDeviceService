# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# 保留必要的类
-keep public class com.atao.tdeviceservice.TDeviceServiceApplication
-keep public class com.atao.tdeviceservice.service.DeviceService
-keep public class com.atao.tdeviceservice.server.ApiServer
-keep public class com.atao.tdeviceservice.receiver.BootReceiver

# 保留API处理器
-keep public class com.atao.tdeviceservice.api.** { *; }

# 保留必要的Android组件
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Application

# 保留NanoHTTPD相关类
-keep class fi.iki.elonen.** { *; }

# 保留Gson相关类
-keep class com.google.gson.** { *; }

# 保留Dexter权限库
-keep class com.karumi.dexter.** { *; }

# 移除调试信息
-dontwarn **
-ignorewarnings

# 优化
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# 移除未使用的资源
-keep class **.R$* {
    public static <fields>;
}

# 保留字符串资源
-keepclassmembers class **.R$string {
    public static <fields>;
}