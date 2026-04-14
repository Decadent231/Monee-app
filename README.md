# Monee Android App

`Monee Android App` 是 `Monee` 记账系统的安卓客户端，基于 `Jetpack Compose + Material 3 + Retrofit + Kotlin Coroutines` 开发，对接 `money-cloud` 微服务后端，通过网关统一访问用户和记账接口。

## 功能概览

- 用户注册、邮箱验证码、登录、退出
- 概览页：本月支出、收入、预算剩余、剩余天数、日均预算、最近记录
- 记录管理：新增、编辑、删除、筛选
- 分类管理：新增、编辑、删除分类
- 预算管理：设置月预算、查看预算消耗
- 统计分析：月统计、年统计、分类统计、趋势图
- 登录态持久化：自动携带 JWT Token

## 技术栈

- Kotlin
- Jetpack Compose
- Material 3
- ViewModel
- Retrofit
- OkHttp
- Gson
- Kotlin Coroutines

## 项目结构

```text
app
├─ android
│  ├─ src/main/java/com/money/codex
│  │  ├─ data           # API、仓储、数据模型、登录会话
│  │  ├─ ui/screens     # 主要页面
│  │  ├─ ui/theme       # 主题与配色
│  │  ├─ MainActivity.kt
│  │  ├─ MainViewModel.kt
│  │  └─ ReminderScheduler.kt
│  ├─ src/main/res      # 图标与资源
│  └─ build.gradle.kts
├─ gradle
├─ build.gradle.kts
└─ settings.gradle.kts
```

## 后端接口

当前默认网关地址定义在 [Api.kt](./android/src/main/java/com/money/codex/data/Api.kt)：

```text
http://101.201.235.13:8080/
```

其中：

- 用户接口前缀：`/user/`
- 记账接口前缀：`/monee/`

如果你要切换到本地开发环境，可以修改：

- `GATEWAY_BASE_URL`
- `MONEE_BASE_URL`
- `USER_BASE_URL`

模拟器访问本机网关时通常使用：

```text
http://10.0.2.2:8080/
```

## 本地运行

### 1. 环境要求

- Android Studio Hedgehog 及以上
- JDK 17
- Android SDK
- Gradle Wrapper

### 2. 打开项目

直接使用 Android Studio 打开当前 `app` 目录。

### 3. 运行调试

在 Android Studio 中选择模拟器或真机后运行，或使用命令：

```bash
./gradlew assembleDebug
```

Windows 下：

```bash
gradlew.bat assembleDebug
```

## 登录态说明

- 登录成功后，后端返回 JWT Token。
- 客户端会把 Token 保存到本地会话存储中。
- 后续请求由 `AuthInterceptor` 自动在请求头中追加：

```text
Authorization: Bearer <token>
```

这样后端就能识别当前请求属于哪个用户，而不是其他人。

## 构建产物

如果要打包 Release：

```bash
./gradlew assembleRelease
```

生成文件通常位于：

```text
android/build/outputs/apk/release/
```

仓库中当前还保留了一个历史生成包：

- `android/release/android-release.apk`

如果后续你不想继续跟踪 APK 文件，可以把它从版本库里移除。

## 相关仓库

- 后端：`money-cloud`
- 笔记 Web 前端：`note-font`

## 说明

- 当前项目已开启明文 HTTP 支持，便于直接对接网关。
- 如果后端服务未启动，页面中的数据请求会失败。
- 如需发布到正式环境，建议把接口地址抽到构建配置或环境变量中管理。
