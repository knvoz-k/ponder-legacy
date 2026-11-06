# Ponder Legacy 发布状态

- 作者与维护者 **yudream**，原作者和来源见 [README](README.md) / [NOTICE](NOTICE.md)。
- 当前版本 **1.5.1**，标签 `v1.5.1`：修复与新版 FTB Quests 同装时的 FML 排序循环。
- [正式发行入口](https://github.com/mcyudream/ponder-legacy/releases/tag/v1.5.1) · [CI](https://github.com/mcyudream/ponder-legacy/actions/workflows/ci.yml)。标签 CI 通过哈希门槛后创建正式版。
- Minecraft 1.12.2 / Java 25 / 最低 **Cleanroom 0.4.0 正式版（无 -alpha）**。
- 只需替换旧 Ponder jar；AE2S Ponder Legacy 0.2.0 可继续使用。FTB Quests、Media API、CraftTweaker、MMCE、Revo UI 均为可选联动。
- 305 项单元测试，生产包的联合安装与未安装可选联动两端验收见 [安装包验收](LEGACY-VERIFICATION.md)。
- 生产 SHA-256：`c452c409358f2facf87b2a5f402492be6930365dc4fb7eb82912835d02469456`；CI 强制匹配已验收产物，不发布测试 harness。
- CurseForge / Modrinth 尚未上传。历史版本的证据保留原哈希及组合，不代替新版验收。
