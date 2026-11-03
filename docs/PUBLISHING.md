# CurseForge / Modrinth 发布准备

核查日期：2026-11-03。本文件是发布操作说明；目前未在这两个平台创建项目或上传版本。

## 本批项目的统一信息

| 字段 | 内容 |
| --- | --- |
| 维护者 | yudream |
| 项目名 / slug | 各项目 `legacy-identity.json` 中的名称 / `modid` |
| 游戏与运行要求 | Minecraft 1.12.2；Cleanroom **0.4.0 正式版（无 `-alpha`）**；Java 25 |
| 源码 / 问题反馈 | 对应 `https://github.com/mcyudream/<modid>` / `issues` |
| 图标 | 项目 `branding/icon.png`，1254 × 1254 PNG |
| 封面 | 项目 `branding/cover.png`；概念展示图，标注为展示插画 |
| 实机截图 | 通过验收后的 `docs/verification/` 中截图 |
| 版本主文件 | `build/libs/` 下经过 reobf 与身份校验的生产 jar |
| 功能和上游差异 | README 的功能范围及差异表；另见 `docs/UPSTREAM-DIFFERENCES.md` |

正式发布的文件应对应 `LEGACY-VERIFICATION.md` 中的 SHA-256。CI 重新构建出的文件若哈希不同，应先核实差异并完成对应运行验收。`sources`、`api`、`mcp`、开发验证模组和测试世界不作为主安装文件。

原作者、原仓库、固定版本及授权须同时保留；维护者字段为 yudream 不会转移上游版权。Ars Nouveau / Data Energistics 的非代码资源另有授权限制，不能因为代码开放或封面已生成就直接发布其游戏资源。

## CurseForge

1. 在 [作者后台](https://authors.curseforge.com/) 创建 Minecraft 的 Mods 项目，填写唯一名称、英文摘要、英文详细介绍、合适分类及图标。中文说明可以作为补充；官方创建说明要求标题与描述为英文。[官方创建说明](https://support.curseforge.com/support/solutions/articles/9000197241-creating-and-submitting-a-project)
2. 介绍采用“玩法 → 安装要求 → 上游来源 → 具体改动 → 当前限制”的顺序，放源码与问题反馈链接。图标应为独立设计的正方形 PNG，官方建议至少 400 × 400。[官方提交建议](https://support.curseforge.com/support/solutions/articles/9000199552-project-submission-guide-and-tips)
3. 选择项目实际适用的许可证，保留移植来源和资源声明。平台项目页与源码许可证应一致；若资源使用不同条款，在详细说明中列明。[移植授权说明](https://support.curseforge.com/support/solutions/articles/9000199552-project-submission-guide-and-tips)
4. 在 Files 上传验收对应的 jar，游戏版本选择 1.12.2，填写改动记录、实际依赖和 Alpha / Beta / Release。首次迁移版本建议维持当前测试阶段。不要为获得默认分发而把未稳定版本标为 Release。[文件类型与关系](https://support.curseforge.com/support/solutions/articles/9000197242)
5. 检查文件、图片和依赖，提交平台审核；状态应以后台审核结果为准。[官方流程](https://support.curseforge.com/support/solutions/articles/9000197241-creating-and-submitting-a-project)

### 加载器分类

本批文件仅完成声明组合下的 Cleanroom 验收，不能写成普通 Forge 1.12.2 通用版本。发布当日查看后台是否有 Cleanroom 分类；若没有，先向平台确认所接受的分类方法，并在摘要开头明确 `Requires Cleanroom 0.4.0 and Java 25`。类别标签不会改变实际安装要求。

## Modrinth

1. 创建 `mod` 项目草稿，填写名称、slug、英文简述、详细玩法、许可证、源码与 Issues 链接，再设置图标与图库。项目创建和版本上传分开处理。[项目 API](https://docs.modrinth.com/api/operations/createproject/)
2. 添加版本，设置 Minecraft 1.12.2、实际加载器、发布阶段和环境；上传生产 jar 并设为主文件，填写 changelog 以及 required / optional 依赖。[版本 API](https://docs.modrinth.com/api/operations/createversion/)
3. 将验收范围和上游差异附在描述中；提交项目审核，并核对审核后的公开页面。

**当前分类限制：**2026-11-03 实查 [官方 loader tags](https://api.modrinth.com/v2/tag/loader) 没有 `cleanroom`。因此尚未设置自动上传的 `loaders` 值；发布时须确认平台接受的分类，不能填一个不存在的值，也不能由 Forge 标签推导普通 Forge 兼容。

## 依赖关系填写

| 项目 | 必需关系 | 可选关系 / 说明 |
| --- | --- | --- |
| Ponder Legacy | CraftTweaker 4.1.20.698 | 附属自行依赖 Ponder；不要把开发 API jar 当运行库 |
| Hostile Neural Networks Legacy | Cleanroom / Java 要求写入描述 | Ponder Legacy；JEI 用于配方查询 |
| Powah Legacy | Cleanroom / Java 要求写入描述 | Ponder Legacy；目前没有 energizing JEI 页面 |
| AE2S Ponder Legacy | **AE2S / Supergiant 1.1.0**，实际 loader ID `ae2` | Ponder Legacy；没有 Ponder 时正常启动但不显示教程 |

平台关系必须选择实际的 Legacy / AE2S 项目 ID，并检查其 1.12.2 文件；不能链接现代同名项目、原版 AE2 或 AE2UEL 代替 AE2S。附属安装 Ponder 时，CraftTweaker 由 Ponder 的必需关系传递安装；手工安装时也须提供它。

## GitHub CI 后续自动发布

现有 `ci.yml` 负责源码构建、测试、重混淆、身份/作者/图标检查及构建产物归档。平台发布需另行配置实际项目 ID 和凭据：

| 配置 | 存放位置 |
| --- | --- |
| CurseForge project ID | 各仓库 GitHub Actions variable `CURSEFORGE_PROJECT_ID` |
| Modrinth project ID | 各仓库 variable `MODRINTH_PROJECT_ID` |
| CurseForge 上传凭据 | 仓库或组织 secret `CURSEFORGE_TOKEN` |
| Modrinth 上传凭据 | 仓库或组织 secret `MODRINTH_TOKEN`，只授予需要的项目/版本权限 |
| 依赖的平台 ID | 版本发布配置；不要把 Mod loader ID 当平台 project ID |

正式启用时，使用手动或版本 tag 触发发布，绑定通过验收的提交和文件哈希，先完成依赖项目发布，再上传附属。普通 push 保持构建职责。凭据只通过 Secrets 注入；不会写入源码、README 或日志。

## 可直接使用的英文说明开头

每个项目后接自己的功能、差异和限制，不能仅粘贴这一段充当完整介绍：

> A Legacy edition for Minecraft 1.12.2, maintained by yudream. Requires Cleanroom 0.4.0 (the release without an -alpha suffix) and Java 25. See the documented feature scope, upstream credits, migration differences and packaged runtime verification in the source repository. Install the production jar and its listed dependencies.

AE2S 附属追加：

> This addon targets Applied Energistics 2 Supergiant (AE2S), whose loader ID is ae2. Compatibility with original AE2 or AE2UEL has not been established.

AppliedCompatibility 直接使用 [原项目](https://github.com/FormlessDragon/AppliedCompatibility)；本批已撤回重复的 Legacy 仓库，不为它创建第二个平台项目。
