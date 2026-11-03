# Ponder Legacy — 安装包验收

## 当前：1.4.0-cleanroom-alpha.1（含 MMCE 软适配）

- Minecraft 1.12.2；Java 25.0.5；CraftTweaker 4.1.20.698。
- **Cleanroom 0.4.0-alpha** 与 **0.4.0 正式版（无 -alpha）**分别验收，两个版本不视为相同版本。
- 安装包：`ponder-legacy-1.4.0-cleanroom-alpha.1.jar`。
- SHA-256：`9be55d18dff404d74b886521395c47cc6d2c5ec1ff3d22d73fecb1553f9ee4ab`。
- 277 项单元测试通过，两版分别完成 MMCE 2.3.2 已安装、未安装和模拟 ABI 不兼容的客户端及物理专服验证，各 85 项运行检查通过。
- 包内不含 MMCE 本体、旧 `ponder_mmce` 附属入口或开发 harness；不自动安装示例或生成机器教程。

MMCE 覆盖静态/动态结构、preview NBT、命名组、指纹、蓝图入口、Java / ZenScript / JSON 与编辑器编译及播放。
精确 `0.4.0-alpha` / `0.4.0` 专服的 Kirino 发现调用修复仅在对应物理专服启用；不修改加载器 jar。
完整报告、运行库指纹、截图及限制见 [MMCE 验收记录](docs/CLEANROOM-VERIFICATION.md)。CI 发行必须匹配上述 SHA-256，重新构建不能自动继承运行验收。

## 历史：改名、作者与图标更新后的正式基线验收

本记录对应改名、作者与图标更新后的安装包。验收使用 Minecraft 1.12.2、**Cleanroom 0.4.0 正式版（无 -alpha）**、Java 25.0.5；编译 SDK 为 0.7.14-alpha，不作为运行兼容证据。

- 安装包：`ponder-legacy-1.4.0-cleanroom-alpha.1.jar`。
- SHA-256：`82798859f97bf3837a2fb7e07e23a4cc23457f1076f604343e6b50a33ecebc99`。
- 必需运行库：CraftTweaker 4.1.20.698；校验值见各 runtime.json。
- 验收日期：2026-11-03。

| 组合 | 结果 | 检查项 |
| --- | --- | --- |
| client | PASS | 116 |
| server | PASS | startup + scene registration |

客户端覆盖正常 FML 安装包发现、无默认示例、结构及 NBT 捕获、动画编辑/预览/保存重载、Java/JSON 场景注册、教程导航、目录分类/搜索/滚动、中文/英文与小窗口渲染。专服覆盖启动及服务端场景插件注册。

正式 0.4.0 专服的 Kirino 模组发现兼容修复仅在该物理端和该版本启用；不修改官方加载器 jar。渲染验收使用 Linux llvmpipe 软件渲染，未覆盖全部硬件/整合包组合。

报告、运行库指纹与截图见 [客户端证据](docs/verification/legacy-identity/cleanroom-0.4.0/client/report.json) 和 [专服证据](docs/verification/legacy-identity/cleanroom-0.4.0/server/report.json)。开发验证模组未包含在发行包中。
