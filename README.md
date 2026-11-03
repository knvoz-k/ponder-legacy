# Ponder Legacy

![Ponder Legacy](branding/cover.png)

<!-- legacy-overview:start -->
为 Minecraft 1.12.2 整合包和 Mod 附属提供分步骤、可回放的三维玩法演示。玩家可以从物品进入教程；作者可以在游戏内捕获结构、编辑时间轴，再通过 Java、ZenScript 或 JSON 分发场景。

## 当前状态

> **公开仓库目前包含项目介绍、上游差异、图像与 CI 框架。Mod 源码尚未提交，等待对应迁移验收完成。CI 此时只报告 scaffold，没有执行 Mod 编译。**

当前为进行中的迁移或改名后验收阶段。构建通过、接口已导入和历史测试结果均不等于完整移植完成。当前源码提交与产物状态见 [PUBLICATION.md](PUBLICATION.md)；未通过验收的项目暂不提供成品发布。

<!-- legacy-authorship:start -->
## 作者与来源 / Authors and upstream

- 此 Legacy 版本作者与维护者 / Legacy author and maintainer：**yudream**。
- 原作者 / Original author(s)：**The Create Team (original Ponder), MCMGGF (1.12.2 base)**。
- 原仓库 / Original repository：[MCMGGF/ponder-for-1.12.2](https://github.com/MCMGGF/ponder-for-1.12.2)。
- 固定上游提交：[`c494bf7692c7`](https://github.com/MCMGGF/ponder-for-1.12.2/commit/c494bf7692c7b7363ef86ed82954874be6e49074)。
- Ponder 原始项目：[Creators-of-Create/Ponder](https://github.com/Creators-of-Create/Ponder)。
- 本版本独立维护；上游版权、许可证及资源来源声明保留于 LICENSE / NOTICE 文件中。
<!-- legacy-authorship:end -->

## 运行环境与身份

| 项目 | 要求 / 标识 |
| --- | --- |
| Minecraft | 1.12.2 |
| 运行基线 | Cleanroom **0.4.0 正式版（不带 `-alpha`）** |
| Java | 25 |
| 加载器 Mod ID | `ponder-legacy` |
| 资源与原注册命名空间 | `ponder`（保留） |

Mod 的 alpha 版本号与加载器版本无关。开发 SDK 可以较新，兼容结论必须来自上述正式运行基线。
加载器身份改名后，安装时应替换本项目旧 jar；原资源命名空间的保留不构成跨实现的存档转换承诺。

## 功能与迁移范围

- 物品教程入口、主题与模组分类索引，同一物品可关联多个场景。
- 游戏内结构框选、NBT 捕获、局部 section 动画、时间轴拖动、撤销/重做和保存重载。
- 保留 Java 插件、Forge IMC / ServiceLoader、ZenScript 与 JSON 场景入口。

## 与上游的具体差异

| 项目 | 上游基线 | 本 Legacy 版本 |
| --- | --- | --- |
| 运行环境 | 既有 1.12.2 / Forge 回迁实现 | 迁至 Java 25 / Cleanroom；正式 0.4.0 是运行验收基线，开发 userdev 版本单独记录。 |
| 编辑工具 | 原场景与脚本 API | 新增 F7 编辑器、F8 框选、结构保存、局部位移/旋转编排及 JSON pack 编辑。 |
| 教程导航 | 原有教程入口 | 增加模组/主题筛选、搜索、滚动位置保留及多场景下拉切换。 |
| 默认内容 | 上游附带演示脚本 | 默认不向整合包注入示例；仅移走内容未改动的旧默认示例，保留用户改过的文件。 |
| 可选模块 | 上游含 MMCE 可选模块 | 本移植未集成 MMCE 模块，不能据此声明其兼容。 |
| 服务端 | 正式 0.4.0 模组发现会触发 Kirino 客户端入口 | 加入只对该版本物理专服生效的单处发现调用修复，客户端和其他版本不启用。 |

## 尚未实现与兼容边界

- 结构框选捕获方块、状态和方块实体 NBT，不自动捕获实体或掉落物。
- 远程结构捕获需要服务器安装相同版本并授予相应权限；编辑器文件保存在客户端。
- 不承诺与现代官方 Ponder 的全部二进制 API 互换。

## 依赖

- 必需：CraftTweaker 4.1.20.698；Cleanroom 提供 MixinBooter 兼容接口。
- 作为其他 Mod 的教程运行库时独立安装；附属应以可选兼容方式接入。

## 使用方式

1. 悬停已注册教程的物品并按住 W；`/ponder index` 打开目录。
2. 创造模式按 F8 框选，再按 F7 保存结构和编辑场景。
3. 分发 `scripts/ponder/packs/` 的 JSON 与 `scripts/ponder/structures/` 的 NBT；场景按作者自己的命名空间组织。

## 构建与 CI

源码提交后，使用 Java 25，在项目目录执行：

```bash
python3 tools/bootstrap_legacy.py
bash gradlew --no-daemon :build :reobfJar --console=plain
python3 tools/verify_legacy_package.py
```

CI 会准备固定版本的源码依赖，构建完整主源码与测试，并检查发行 jar 的身份、作者和图标。
仅有文档的仓库会明确报告 scaffold，不把跳过编译标为 Mod 构建成功。CI 构建不能替代客户端玩法和专服验收。

## 授权与图像

沿用上游授权；原始 Ponder、1.12.2 回迁与本版本的来源分别记录在 LICENSE / NOTICE.md。

此版本作者为 **yudream**，上游作者与版权保留如上。`branding/icon.png` 和 `branding/cover.png` 为本项目生成的展示图，
以代表物品或概念表现项目；不作为游戏实机截图，也不改变上游游戏资源的授权。生成记录见 `branding/provenance.json`。

<!-- legacy-overview:end -->
