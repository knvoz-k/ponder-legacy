# Ponder Legacy 发布状态

- 作者与维护者：**yudream**；原作者和固定上游提交见 [README](README.md) / [NOTICE](NOTICE.md)。
- 本次版本：**`1.4.0-cleanroom-alpha.1`**，发行标签 `v1.4.0-cleanroom-alpha.1`，保持 Alpha 预发行状态。
- GitHub：[发行入口](https://github.com/mcyudream/ponder-legacy/releases/tag/v1.4.0-cleanroom-alpha.1)、[CI 记录](https://github.com/mcyudream/ponder-legacy/actions/workflows/ci.yml)。标签 CI 构建、校验成功后创建发行版；实际发布状态以这两个页面为准。
- CurseForge / Modrinth：尚未创建项目或上传版本。
- Loader ID：`ponder-legacy`；资源空间：`ponder`。安装时替换旧版本，CraftTweaker 4.1.20.698 必须独立安装。

## 当前提交与验收产物

本次提交包含 MMCE 软适配、说明文档及两套加载器的独立运行证据。
Minecraft 1.12.2 / Java 25 环境下，Cleanroom **0.4.0-alpha** 与 **0.4.0 正式版**均已完成客户端和专服验收。
每套覆盖 MMCE 2.3.2 已安装、未安装及模拟 ABI 不兼容三种情况，各 85 项运行检查通过；277 项单元测试通过。

生产 jar SHA-256：`9be55d18dff404d74b886521395c47cc6d2c5ec1ff3d22d73fecb1553f9ee4ab`。
CI 标签发布时强制核对该值，上传生产 jar、API、源码和 `SHA256SUMS`；测试 harness 不作为发行资产。
验收覆盖范围和限制见 [安装包验收](LEGACY-VERIFICATION.md) / [MMCE 详细证据](docs/CLEANROOM-VERIFICATION.md)。

## 历史快照

之前已验收源码提交为 `1cd51270dd0473eff32f8590b72470d61ff9fcb4`，其 jar SHA-256 为
`82798859f97bf3837a2fb7e07e23a4cc23457f1076f604343e6b50a33ecebc99`，当时尚未包含本轮 MMCE 适配。
旧证据保留原始包名和校验值，仅验证其对应快照；当前结果来自新包的独立运行验收。
