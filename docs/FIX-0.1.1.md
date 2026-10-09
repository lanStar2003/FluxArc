# FluxArc 0.1.1 启动依赖修复

0.1.0 把 Maven / 发布文件版本 `5.09.51.482` 错误地作为 Forge Mod ID `gregtech` 的版本要求。即便安装了正确的 GTNH 2.8.4，Forge 也会把它报告为缺少所需版本。这是 FluxArc 的依赖声明错误，不是用户需要更换整合包。

直接检查官方 `gregtech-5.09.51.482.jar` 的字节码与 `mcmod.info` 后确认：

| 入口类 | Forge Mod ID | Forge 实际版本 |
|---|---|---|
| `gregtech.GTMod` | `gregtech` | `MC1710` |
| `gregtech.GTNHMod` | `gregtech_nh` | `5.09.51.482` |

官方生产 JAR SHA256：`4ab7ce174a8f6fb7a90d8d11d56056aab2de577c36c6084b37ce890d7b1d67bf`。
虽然 `mcmod.info` 给 `gregtech` 写的是 `5.09.51.482`，Forge 的 `FMLModContainer.bindMetadata` 优先使用 `@Mod.version`，其值为 `MC1710`。因此只看文件名或 `mcmod.info` 会漏掉这次问题。
来源：<https://github.com/GTNewHorizons/GT5-Unofficial/releases/tag/5.09.51.482>。

0.1.1 要求 `gregtech` 存在且先加载，并把精确构建版本约束放到 `gregtech_nh@[5.09.51.482]`。保留 `Forge@[10.13.4.1614,)`。未删除兼容性检查，也未扩大支持的 GT 构建版本。

## 替换方法

1. 关闭游戏和相关服务器。
2. 在原测试实例的 `mods` 目录移走旧 `FluxArc-0.1.0.jar`，只保留新版 `FluxArc-0.1.1.jar`。若服务器也装了本 Mod，客户端和服务器一起替换。
3. 保留整合包原有 GregTech 和其他 Mod，不要为了此错误更换 GTNH，也不要安装 `-dev.jar`。
4. 再次启动。此修复不修改方块、物品、注册 ID、存档格式或配方。

## 回归验证范围

`ForgeDependencyLoadingTest` 从校验过 SHA256 的官方生产 GT JAR 读取真实注解与 `mcmod.info`，经过 `FMLModContainer.bindMetadata` 得到运行时版本，调用 Forge 自身的 `Loader.sortModList`，不复制版本比较算法。

测试覆盖旧声明复现 `MissingModsException`、新版声明通过、前后相邻 GT 构建版本仍拒绝、必需 Mod ID 缺失仍拒绝、过旧 Forge 仍拒绝，以及新版注解和 `mcmod.info` 版本一致。Forge 版本取自目标 Forge 的 `ForgeVersion`，与官方 `ForgeModContainer` 使用相同版本常量。

这里隔离执行真实 Forge 的依赖检查阶段；外部 Mod 使用来自真实元数据的容器，未运行 GTNH 全部 Mod 的生命周期。它不能代替完整客户端、专用服务器和六台机器的游戏内验收。

如替换后仍报错，请提供实例目录中的 `logs/fml-client-latest.log`（服务器为 `logs/fml-server-latest.log`）及最新 `crash-reports/*.txt`。这些日志用于区分其他依赖问题和进入生命周期后的异常，不需要先改动整合包。
