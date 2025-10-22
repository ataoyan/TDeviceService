# TDeviceService

<div align="center">

![Android](https://img.shields.io/badge/Android-12%2B-green?style=for-the-badge&logo=android)
![API](https://img.shields.io/badge/API-31%2B-blue?style=for-the-badge&logo=android)
![License](https://img.shields.io/badge/License-MIT-yellow?style=for-the-badge)

**轻量级Android设备信息服务**  
通过HTTP API提供设备信息查询功能

[📖 文档](#api文档) • [🚀 快速开始](#快速开始) • [📋 功能特性](#功能特性) • [🤝 贡献](#贡献)

</div>

---

## ✨ 功能特性

### 🔋 电池信息
- 电量百分比、充电状态、健康状态
- 温度、电流、电压监测
- 充电方式识别

### 📱 应用信息  
- 应用列表、名称、版本查询
- 系统应用识别、桌面应用检测
- 启动Activity、应用图标获取

### 🏥 健康检查
- 服务状态监控
- 运行时间统计

## 🚀 快速开始

### 安装

```bash
# 克隆项目
git clone https://github.com/yourusername/TDeviceService.git
cd TDeviceService

# 编译安装
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 启动服务

```bash
# 启动前台服务
adb shell am start-foreground-service -n com.atao.tdeviceservice/.service.DeviceService
```

### 端口转发

在电脑上访问设备API需要先进行端口转发：

```bash
# 将设备8080端口转发到电脑8080端口
adb forward tcp:8080 tcp:8080
```

### 测试API

```bash
# 检查服务状态
curl http://127.0.0.1:8080/api/health

# 获取电池电量
curl http://127.0.0.1:8080/api/battery/level
```

## 📖 API文档

### 电池信息 API

| 端点 | 描述 | 示例 |
|------|------|------|
| `GET /api/battery/level` | 获取电池电量 | `85.5` |
| `GET /api/battery/charging` | 获取充电状态 | `"Charging"` |
| `GET /api/battery/health` | 获取电池健康状态 | `"Good"` |
| `GET /api/battery/temperature` | 获取电池温度 | `25.3` |
| `GET /api/battery/isCharging` | 判断是否在充电 | `true` |
| `GET /api/battery/chargeType` | 获取充电方式 | `"USB"` |
| `GET /api/battery/current` | 获取电池电流 | `1500` |
| `GET /api/battery/voltage` | 获取电池电压 | `3.850` |

### 应用信息 API

| 端点 | 描述 | 参数 |
|------|------|------|
| `GET /api/apps/list` | 获取所有应用包名列表 | - |
| `GET /api/apps/name` | 获取应用名称 | `package` |
| `GET /api/apps/isSystem` | 判断是否为系统应用 | `package` |
| `GET /api/apps/isLauncher` | 判断是否为桌面应用 | `package` |
| `GET /api/apps/launcherActivity` | 获取启动Activity | `package` |
| `GET /api/apps/version` | 获取应用版本号 | `package` |
| `GET /api/apps/icon` | 获取应用图标(Base64) | `package` |

### 健康检查 API

| 端点 | 描述 | 响应 |
|------|------|------|
| `GET /api/health` | 获取服务健康状态 | `{"status": "healthy"}` |

## 📋 响应格式

### 成功响应
```json
{
  "success": true,
  "data": "响应数据"
}
```

### 错误响应
```json
{
  "success": false,
  "error": "错误信息"
}
```

## 🛠️ 技术栈

- **语言**: Java 11
- **框架**: Android SDK
- **服务器**: NanoHTTPD
- **JSON**: Gson

## 📁 项目结构

```
app/src/main/java/com/atao/tdeviceservice/
├── api/                    # API处理器
│   ├── ApiHandler.java     # 基类
│   ├── BatteryInfoHandler.java
│   ├── AppInfoHandler.java
│   └── HealthHandler.java
├── server/
│   └── ApiServer.java      # HTTP服务器
├── service/
│   └── DeviceService.java  # 后台服务
└── TDeviceServiceApplication.java
```

## ⚙️ 配置

### 端口配置
- **默认端口**: 8080
- **备用端口**: 8081-8180 (自动检测)
- **访问地址**: `http://127.0.0.1:8080`
- **端口转发**: 需要 `adb forward tcp:8080 tcp:8080` 才能在电脑上访问

### 权限要求
- `INTERNET` - 网络访问
- `READ_PHONE_STATE` - 设备信息
- `BATTERY_STATS` - 电池信息
- `QUERY_ALL_PACKAGES` - 应用信息

## 🔧 开发

### 添加新API

1. 在对应Handler中添加处理方法
2. 在`ApiServer.java`中注册路径
3. 测试API功能

### 构建

```bash
# Debug版本
./gradlew assembleDebug

# Release版本
./gradlew assembleRelease
```

## ⚠️ 注意事项

- 服务运行在设备本地，仅限本机访问
- 部分API需要特定权限
- 建议添加到电池优化白名单
- 服务不会自动启动，需手动启动

## 🤝 贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## ⭐️ 支持

如果这个项目对你有帮助，请给它一个 ⭐️！

---

<div align="center">

**Made with ❤️ for Android developers**

</div>