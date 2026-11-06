# Ponder Legacy 发布状态

- 作者与维护者 **yudream**，原作者和来源见 [README](README.md) / [NOTICE](NOTICE.md)。
- 当前版本 **1.6.0**，标签 `v1.6.0`：直接在世界内定位投影，使用透明模型、轮廓及透明工具面板，修复不存在底板导致机器层被删除的问题。
- [正式发行入口](https://github.com/mcyudream/ponder-legacy/releases/tag/v1.6.0) · [CI](https://github.com/mcyudream/ponder-legacy/actions/workflows/ci.yml)。标签 CI 通过哈希门槛后创建正式版。
- Minecraft 1.12.2 / Java 25 / 最低 **Cleanroom 0.4.0 正式版（无 -alpha）**。
- 替换旧 Ponder jar 即可；AE2S Ponder Legacy 0.2.0 可继续使用。保留 FTB Quests 加载顺序修复，外部联动 Mod 保持可选。
- 307 项单元测试，以及核心、AE2S／FTBQ／Media 联合安装、专服与 Revo 回归；[验收记录](LEGACY-VERIFICATION.md)。
- 生产 SHA-256：`a7863b678e2a43f43722690032c89d65c243356f970671b6aefa7b34073cad10`；CI 强制匹配已验收产物，不发布测试 harness。
- CurseForge / Modrinth 尚未上传。
