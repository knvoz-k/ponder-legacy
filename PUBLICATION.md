# Ponder Legacy 发布状态

- 作者与维护者：**yudream**；原作者和固定上游提交见 [README](README.md) / [NOTICE](NOTICE.md)。
- 当前正式版：**`1.4.0`**，发行标签 `v1.4.0`，包含 Revo UI 可选适配。
- 历史预发行版：**`1.4.0-cleanroom-alpha.1`**。
- GitHub：[发行入口](https://github.com/mcyudream/ponder-legacy/releases/tag/v1.4.0)、[CI 记录](https://github.com/mcyudream/ponder-legacy/actions/workflows/ci.yml)。标签 CI 构建、校验成功后创建正式发行版；实际发布状态以这两个页面为准。
- CurseForge / Modrinth：尚未创建项目或上传版本。
- Loader ID：`ponder-legacy`；资源空间：`ponder`。安装时替换旧版本，CraftTweaker 4.1.20.698 必须独立安装。

## 当前提交与验收产物

本次提交包含 Revo UI 可选适配、README 与正式基线的运行证据。
Minecraft 1.12.2 / Java 25 环境下，Cleanroom **0.4.0 正式版（无 `-alpha`）**已完成 Revo UI 安装/未安装的客户端及专服验收、编辑器与场景播放回归、MMCE 2.3.2 场景回归；277 项单元测试通过。

生产 jar SHA-256：`4c02d70c6f38ddf5305fa4be47a5e410e693c57f2cd8576ed5f78f8b7f32f14b`。
CI 标签发布时强制核对该值，上传生产 jar、API、源码和 `SHA256SUMS`；测试 harness 不作为发行资产。
验收覆盖范围和限制见 [安装包验收](LEGACY-VERIFICATION.md) / [Revo UI 适配](docs/REVO-UI.md)。

## 历史快照

预发行 `v1.4.0-cleanroom-alpha.1` 对应 `8dd66a5`，包 SHA-256 为 `9be55d18dff404d74b886521395c47cc6d2c5ec1ff3d22d73fecb1553f9ee4ab`。该包的两套加载器 MMCE 证据见 [历史验收](docs/CLEANROOM-VERIFICATION.md)。

之前已验收源码提交为 `1cd51270dd0473eff32f8590b72470d61ff9fcb4`，其 jar SHA-256 为
`82798859f97bf3837a2fb7e07e23a4cc23457f1076f604343e6b50a33ecebc99`，当时尚未包含本轮 MMCE 适配。
旧证据保留原始包名和校验值，仅验证其对应快照；当前结果来自新包的独立运行验收。
