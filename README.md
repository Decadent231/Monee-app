# MoneyCodex Android

这是根据当前 Web 前端改写的 Android 版本（Jetpack Compose）。

## 目录

- `android/` Android 应用模块
- `settings.gradle.kts` / `build.gradle.kts` Gradle 项目配置

## 功能

- 概览：收入、支出、结余、预算摘要、最近记录、支出分类
- 记账：记录列表 + 删除
- 统计：收支汇总、分类统计
- 分类：分类列表
- 预算：预算、已支出、剩余、每日可用 + 设置预算
- 全局浮动按钮：记一笔（弹窗提交）

## 后端地址

当前默认接口地址在 `android/src/main/java/com/money/codex/data/Api.kt`：

- `http://10.0.2.2:8080/api/`（Android 模拟器访问本机后端）

如果你用真机调试，请改成电脑局域网 IP，例如：

- `http://192.168.1.100:8080/api/`

## 说明

- 已开启 `usesCleartextTraffic=true`，支持 HTTP。
- 接口不可用时会回退到内置 mock 数据，方便先看页面。
