# 流光主题 (LiuGuang Themes)

面向华为/荣耀设备（EMUI / HarmonyOS 4.x 及以下 / MagicOS）的开源主题库客户端。

本应用基于 **MI-KUN 的开源项目** [FanChen-Theme-Library](https://github.com/MI-KUNs/FanChen-Theme-Library)（GPL-3.0）重构而成：

- 更换包名与品牌（`com.liuguang.theme` / 流光主题）
- 更新检测与下载入口改为本仓库（`up/gx.txt`）
- 遵守 GPL-3.0：源码公开、同许可证分发、保留原作者版权声明

原项目作者：MI-KUN / 凡尘主题库 —— 致谢！

## 构建

```powershell
# 需要 JDK 17+ 与 Android SDK（platforms;android-36）
gradlew.bat assembleRelease
# SDK 位置写在本目录 local.properties (C:\Android\Sdk)
```

## 许可

GPL-3.0 —— 详见 [LICENSE](LICENSE)。

## 关于更新协议

应用启动后会请求 `https://1yk8u.github.io/app1/up/gx.txt`，正文包含
`版本号:(数字)` 与 `更新内容:(...)<` 即触发更新提示（数字大于本机版本时提示）。
