# 本地 Cleanroom 验证记录

## 2026-11-03：指引滚动、分类与移除默认示例

| 项目 | 实际结果 |
| --- | --- |
| 安装环境 | Minecraft 1.12.2；分别加载精确 **Cleanroom 0.4.0-alpha** 和 **0.4.0** 的运行库 |
| Java / CraftTweaker | Temurin 25.0.5+8 / 4.1.20.698 |
| 构建 / 单元测试 | PASS，260 项，0 失败、错误或跳过 |
| 真实安装 jar 客户端 | 两个加载器版本各 PASS，116 项检查；无 MCP / userdev 重映射层 |
| 目录压力数据 | 242 个物品入口、42 个测试主题；同物品 12 场景去重 |
| 滚动与点击 | 物品、主题、主题内容、左侧分类独立滚动；末项可达、滚动条拖动、点击进入正确播放器通过 |
| 分类与搜索 | 按组件所属模组、组件及场景主题、未分类、名称与 ID 搜索、空结果、返回保留查询通过 |
| 视口 | 两种窗口尺寸下只绘制和点击可见行；截图帧缓冲确认条目不覆盖固定底部栏 |
| 默认示例 | 启动前迁移 8 个旧默认脚本及编辑器示例，原目录不再加载；默认场景及主题不进入注册表 |
| 数据保护 | 单元测试覆盖修改过的示例、自定义脚本、符号链接保留，以及备份冲突不覆盖 |
| 分辨率 / 语言 | 1280×720、854×480；中文、英文；截图检查通过 |
| 发布包 | refmap 只有一份，没有任何 Smoke / harness 类；最低声明为 0.4.0-alpha |
| 专用服务器 | 两版均在加载器模组发现阶段失败；无 Mod 对照同样失败，尚未通过专服兼容验收 |

最终安装 jar SHA-256：

```text
6c8800d2042bfddbe13bbc6a5465c8dd3a24e8b6eb5f9669581cb0f3afc39a6d
```

### 证据

- 0.4.0-alpha：[客户端报告](verification/catalog-cleanroom-0.4.0-alpha/client-report.json)、[实际运行库](verification/catalog-cleanroom-0.4.0-alpha/client-runtime.json)、[专服结果](verification/catalog-cleanroom-0.4.0-alpha/server-result.json)
- 0.4.0：[客户端报告](verification/catalog-cleanroom-0.4.0/client-report.json)、[实际运行库](verification/catalog-cleanroom-0.4.0/client-runtime.json)、[专服结果](verification/catalog-cleanroom-0.4.0/server-result.json)
- [单元测试](verification/catalog-cleanroom-0.4.0/unit-tests.json)、[所有构建包 SHA-256](verification/catalog-cleanroom-0.4.0/SHA256SUMS)
- [主题筛选](verification/catalog-cleanroom-0.4.0/catalog-themes-zh.png)、[模组分类](verification/catalog-cleanroom-0.4.0/catalog-mods-zh.png)、[小窗口末页](verification/catalog-cleanroom-0.4.0/catalog-small.png)
- [主题卡片目录](verification/catalog-cleanroom-0.4.0/catalog-theme-index.png)、[主题内容末页](verification/catalog-cleanroom-0.4.0/catalog-theme-items.png)、[搜索与多场景](verification/catalog-cleanroom-0.4.0/catalog-search.png)
- [0.4.0 示例迁移日志](verification/catalog-cleanroom-0.4.0/removed-examples.log)、[0.4.0-alpha 示例迁移日志](verification/catalog-cleanroom-0.4.0-alpha/removed-examples.log)

两版专服均在 `Loader.identifyMods(Loader.java:379)` 尝试加载客户端 `EntityPlayerSP`，
触发 `NoClassDefFoundError`，发生在 Ponder 常规模组初始化之前。
两版无 Mod 对照仍复现，证据包含各自 `server-crash.txt`、`empty-server-crash.txt` 和 `runtime.json`。
未修改示例备份在游戏目录的 `config/ponder/removed-examples/`，不在 CraftTweaker / JSON 的扫描目录内。
有修改的示例保留，若希望一并移除，可手动移走对应自定义脚本或 pack 后重启。

生产测试使用独立重混淆 harness，Renamer 的依赖包含主项目 MCP jar，以解析 Ponder 类上继承的 Minecraft 方法。
验证启动器提取 native 库时比较内容并原子替换，避免截断其他进程已映射的共享库。
测试数据只在开发 harness / 验证目录中生成，不随发布包安装。

### 复现

```bash
./tools/dev.sh build reobfDevHarnessJar reobfServerHarnessJar
python3 tools/verify-cleanroom.py client
python3 tools/verify-cleanroom.py server
python3 tools/verify-cleanroom.py server --empty
PONDER_CLEANROOM_VERSION=0.4.0-alpha python3 tools/verify-cleanroom.py client
PONDER_CLEANROOM_VERSION=0.4.0-alpha python3 tools/verify-cleanroom.py server
PONDER_CLEANROOM_VERSION=0.4.0-alpha python3 tools/verify-cleanroom.py server --empty
```

## 2026-11-03：图标、多场景悬浮菜单、提示框进度条

验证时间：2026-11-03T15:02:57-05:00。

| 项目 | 实际结果 |
| --- | --- |
| Minecraft / Cleanroom | 1.12.2 / **0.4.0（不带 -alpha）** |
| Java / CraftTweaker | Temurin 25.0.5+8 / 4.1.20.698 |
| 构建 / 单元测试 | PASS，253 项，0 失败、错误或跳过 |
| 真实安装 jar 客户端 | PASS，62 项检查；无 MCP / userdev 重映射层 |
| 客户端核心流程 | 世界选区、NBT 捕获、编辑动画、保存重载、播放与 JSON 源编辑入口通过 |
| 同一物品多个场景 | 指南针绑定 12 个 JSON 场景；鼠标与键盘直接切换、滚动与首尾边界通过 |
| 菜单输入 | 悬停、移入、固定展开、Tab、Esc、外部关闭通过；点击与滚轮不穿透到相机 |
| 图标 | 直接检查最终帧缓冲，播放与下一场景图标均可见 |
| 提示框进度条 | 普通与真实自动换行提示框均铺满文字区宽度，60% 填充像素比例通过 |
| 分辨率 / 语言 | 1280×720、854×480；中英菜单，截图已检查 |
| 发布包内容 | refmap 只有一份，未包含客户端或服务器测试类 |
| 专用服务器 | **FAIL：加载器在模组发现前崩溃**；不安装任何 Mod 的对照服务器复现相同错误 |

本次安装 jar SHA-256：

```text
1a663b4fcb9c9851d42a4d64801cb536047757fee0c644b3479b1751a439ad9b
```

运行库来自 `cleanroom-0.4.0-installer.jar` 的 `version.json`，精确加载器 jar 的 SHA-1 为
`2b697f6538c4d893ddbd876eb02d9ea55743a4f8`。
官方安装器 SHA-256 为 `882435d851a51e14b802e0a9384e4a05cfbb3890fca435c40c494af75e5a166f`。
这是 Maven 发布的 **0.4.0**，没有把历史 `0.4.0-alpha` 的验证当作此次基线结果。

### 证据与复现

- [客户端原始报告](verification/scene-menu-cleanroom-0.4.0/client-report.json)
- [运行库与安装文件清单](verification/scene-menu-cleanroom-0.4.0/client-runtime.json)
- [单元测试结果](verification/scene-menu-cleanroom-0.4.0/unit-tests.json)、[发布 jar 校验值](verification/scene-menu-cleanroom-0.4.0/SHA256SUMS)
- [工具栏图标](verification/scene-menu-cleanroom-0.4.0/viewer-toolbar.png)
- [中文悬浮菜单](verification/scene-menu-cleanroom-0.4.0/scene-menu-zh.png)、[英文菜单](verification/scene-menu-cleanroom-0.4.0/scene-menu-en.png)
- [长列表末尾](verification/scene-menu-cleanroom-0.4.0/scene-menu-last.png)、[小窗口](verification/scene-menu-cleanroom-0.4.0/scene-menu-small.png)
- [提示框进度条](verification/scene-menu-cleanroom-0.4.0/tooltip-progress.png)、[真实标题换行](verification/scene-menu-cleanroom-0.4.0/tooltip-progress-wrapped.png)
- [专用服务器失败记录](verification/scene-menu-cleanroom-0.4.0/server-result.json)
- [安装 Ponder 的崩溃记录](verification/scene-menu-cleanroom-0.4.0/server-crash.txt)、[无 Mod 对照的崩溃记录](verification/scene-menu-cleanroom-0.4.0/empty-server-crash.txt)

```bash
./tools/dev.sh build reobfDevHarnessJar reobfServerHarnessJar
python3 tools/verify-cleanroom.py client
python3 tools/verify-cleanroom.py server
python3 tools/verify-cleanroom.py server --empty
```

两种专服均在 `Loader.identifyMods(Loader.java:379)` 调用 `KirinoCore.identifyMods` 时
尝试加载客户端 `EntityPlayerSP`，触发 `NoClassDefFoundError`；尚未进入 Ponder 初始化。
进程自行以 0 退出，但验收脚本仍将缺少 PASS 报告视为失败。
此次完成了专服启动检查，**没有宣称 Cleanroom 0.4.0 专服兼容验收通过**。
客户端使用 Mesa / llvmpipe；尚未覆盖物理 GPU、远程多人整合包及所有第三方方块实体。

## 历史记录：编辑器迁移（0.7.14-alpha userdev）

时间：2026-11-03T09:44:33-05:00。

## 实际通过的验证

| 项目 | 结果 |
| --- | --- |
| Java / Gradle | Temurin 25.0.5+8 / Gradle 9.8.1 |
| Minecraft / Cleanroom | 1.12.2 / 0.7.14-alpha |
| CraftTweaker | 4.1.20.698 |
| `build devClasses` | PASS |
| 单元测试 | 247 项，0 失败，0 错误，0 跳过；包含 14 项场景编辑回归测试 |
| 最终安装 jar 客户端流程 | PASS，25 项检查 |
| 图形环境 | Mesa 26.0.8，OpenGL 4.5 compatibility，llvmpipe (LLVM 21.1.8, 256 bits) |
| 截图检查 | 1280×720 和 854×480，中英界面 |

安装 jar SHA-256：

```text
045f8c45d28f0b3d68b6c65a4880ddd5b89cf1ac6ee00ea0b65d01569551d5e5
```

本次客户端使用 `-PponderSmoke -PpackagedSmoke`：从 classpath 中移除源码输出，
改为加载 `build/libs/` 的 SRG 安装 jar，验证清单中的 coremod、Mixin、资源和完整编辑流程。
Cleanroom 的运行环境仍是 userdev / MCP 环境，其 SRG→MCP 重映射器处理安装 jar。
本记录不把它表述为另一个独立启动器或独立生产服务器的安装认证。

实际执行了内置服务器创造世界、放置方块与箱子、网络捕获选区、NBT 与朝向检查、
快捷动画表单、位移与旋转执行、保存与注册、时间轴倒退/前进、预览方块拾取、
中英文资源切换、两种窗口尺寸、原 Ponder 播放器和 JSON 编辑入口。
截图后的 OpenGL 错误检查均为零。

## 证据

- [客户端原始报告](verification/cleanroom-1.4.0/client-report.json)
- [单元测试计数](verification/cleanroom-1.4.0/unit-tests.json)
- [全部 jar 校验值](verification/cleanroom-1.4.0/SHA256SUMS)
- [中文编辑器](verification/cleanroom-1.4.0/editor-zh.png)
- [英文编辑器](verification/cleanroom-1.4.0/editor-en.png)
- [小窗口](verification/cleanroom-1.4.0/editor-small.png)
- [动作参数表单](verification/cleanroom-1.4.0/animation-form.png)
- [原 Ponder 播放器](verification/cleanroom-1.4.0/ponder-viewer.png)

本机完整日志：`build/reports/cleanroom/build.log` 与 `client.log`。
JUnit HTML 报告：`build/reports/tests/test/index.html`。

## 本次未覆盖

独立生产服务器、其他启动器、多人远程联机、大规模整合包、所有第三方方块实体、
物理 GPU 驱动，以及人工输入的完整交互矩阵。
MMCE 可选模块未在此分支构建中集成。
当前捕获保留方块与方块实体；实体捕获、动画曲线和可拖动的三维变换手柄尚未实现。

开发环境首次启动发现 Cleanroom 异步 splash 的并发类加载问题；
`prepareDevClient` 为新的本地配置关闭此画面后，完整流程正常。
此设置仅位于开发运行目录，不修改发布模组的用户配置。
