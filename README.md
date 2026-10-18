# 物久

「物久」是一个完全离线的 Android 大件物品账本。记录手机、电脑、相机等高价物品的购买价格与日期，自动计算已经使用的天数和平均每天的成本。

## 功能

- 记录物品名称、分类、购买价格、购买日期、备注和可选照片
- 购买当天自动算作第 1 天，使用天数按本地日期自动累计
- 首页汇总总投入、今天的使用成本和每件物品的日均成本
- 支持内置分类、自定义分类和分类筛选
- 支持编辑物品、标记停用 / 已出售、恢复使用和删除
- 停用后使用天数冻结，仍计入总投入，但不计入今天的使用成本
- 浅色 / 深色主题，所有数据只保存在本机

## 安装

1. 在 Android 手机上下载 [物久 v0.1.0 APK](https://github.com/Yiding-Meng/wujiu/releases/download/v0.1.0/wujiu-v0.1.0.apk)。
2. 在文件管理器中打开下载的 APK。
3. 如果系统提示，允许当前文件管理器「安装未知应用」。
4. 安装后打开「物久」即可使用。

系统要求：Android 8.0（API 26）或更高版本。

这是使用 debug 签名构建的可安装测试版 APK，适合个人安装体验。正式上架应用商店前，应使用发布密钥重新签名。

安装包 SHA-256：`d797eb8b7c6c048863af8a0eb8881d2288b3983ee3d2dd77fcc75d0b2459b5f9`

## 数据与隐私

- 应用不申请网络权限，不包含账号、广告、统计或云端同步。
- 物品数据保存在应用本地的 Room 数据库中，照片复制到应用私有目录。
- 不启用系统云备份或设备迁移备份。
- 卸载应用会删除数据库和照片，删除前请自行确认是否需要保留记录。

## 计算规则

- 使用中物品：`使用天数 = max(1, 今天 - 购买日期 + 1)`
- 已停用物品：`使用天数 = max(1, 停用日期 - 购买日期 + 1)`
- 日均成本：`购买价格 / 使用天数`
- 今天的使用成本：所有使用中物品当天日均成本之和
- 总投入：所有物品购买价格之和，包含已停用物品

## 本地构建

需要 JDK 17 和 Android SDK（compile SDK 37）。

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export GRADLE_USER_HOME="$PWD/work/gradle-user-home"
./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console=plain
```

构建产物位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 项目结构

- `app/src/main/java/com/mengyiding/wujiu/data/`：Room 数据库、仓库和本地图片
- `domain/calculation/`：使用天数和日均成本计算
- `ui/home/`：首页汇总、筛选和物品列表
- `ui/detail/`：物品详情、停用 / 恢复和删除
- `ui/editor/`：新增、编辑、日期选择和图片
- `ui/categories/`：分类管理
- `docs/superpowers/specs/2026-10-18-wujiu-design.md`：产品设计文档

## 已知版本范围

当前版本 `0.1.0` 聚焦单机记账核心流程，不包含云同步、数据导入导出、保修提醒、发票扫描、多币种和桌面小组件。
