# Revo UI 可选适配

Ponder Legacy 从 `1.4.0` 起支持 [Revo UI / 焕新 UI](https://www.mcmod.cn/class/30040.html) 的现代物品提示框。安装对应 Mod 后自动启用，无需额外附属包或配置开关。

## 修复内容

Revo UI 会重新安排标题图标、稀有度、模型预览、换行和正文行距。Ponder 现在读取 Revo UI 实际绘制的保留布局，将按住 W 的进度条放入其预留行，并按文字区宽度绘制。进度条不会延伸到左侧模型预览区。

关闭 Revo UI 的现代提示框时使用原版 Forge 提示框布局。未安装 Revo UI 时也可正常使用 Ponder；它与 NFR、ModularUI 均不会被打包进 Ponder，也不会成为 Ponder 的必需前置。

1.6.1 起，Ponder 页面自行处理背景，跳过 Revo UI 的背景模糊与渐变过渡。这样 Markdown 图片／视频面板、文件选择器、JSON 编辑器及场景预览不受额外后处理影响，透明投影工具仍能看清世界。进入时清空旧过渡，离开时不会遗留模糊动画；其他模组与原版页面保持 Revo UI 的设置。实机调试发现官方 Revo UI 0.8.7 的模糊状态恢复会在当前软件 OpenGL 环境报告 `glPopAttrib / GL_COLOR_ATTACHMENT0` 错误；该适配只避开 Ponder 自己页面上的冗余效果，不修改依赖 jar 或全局配置。

适配代码只在物理客户端且发现 `neofontrender_ui_enhancements` 时加入可选 Mixin；专服不加入该客户端 Mixin。教程仍由作者通过 JSON、ZenScript 或 Java 注册，不会自动为其他 Mod 生成演示。

## 验收组合

使用官方未修改的生产 jar：

| 组件 | 版本 |
| --- | --- |
| Minecraft | 1.12.2 |
| Cleanroom | **0.4.0 正式版，无 `-alpha`** |
| Java | 25.0.5 |
| CraftTweaker | 4.1.20.698 |
| Revo UI | 0.8.7 |
| Neo Font Render | 0.8.0，`full` 包 |
| ModularUI | 3.2.0-nfr.2，Revo UI 官方配套版本 |
| 测试物品 | AE2S / Supergiant 1.1.0 灰色 ME 智能线缆、原版指南针 |

Revo UI、NFR 与配套 ModularUI 从 [官方发布页](https://github.com/AndreaFrederica/NeoFontRender/releases) 独立安装。适配针对上述版本的行布局；其他版本与其他提示框替换 Mod 的组合需要单独验证。

验收内容及对应包哈希见 [安装包验收](../LEGACY-VERIFICATION.md)。渲染环境为 Linux llvmpipe 软件 OpenGL，不能据此推导所有显卡或整合包的兼容性。

## 复现

```bash
./tools/dev.sh --no-daemon clean build reobfJar reobfRevoHarnessJar reobfDevHarnessJar reobfServerHarnessJar --console=plain
python3 tools/verify-cleanroom.py client --revo present --revo-dir /path/to/revo-jars --ae2s /path/to/AppliedEnergistics2-Supergiant-1.1.0.jar
python3 tools/verify-cleanroom.py server --revo present --revo-dir /path/to/revo-jars
python3 tools/verify-cleanroom.py client --revo absent
python3 tools/verify-cleanroom.py server --revo absent
```

`--revo-dir` 包含 `neofontrender-0.8.0-full.jar`、`neofontrender-ui-enhancements-0.8.7.jar`、`modularui-3.2.0-nfr.2.jar`。`--ae2s` 可选；提供时会核实并使用实际 AE2S 线缆。

启动器在 `run/cleanroom-0.4.0-revo/` 创建隔离实例，加载实际重混淆 Ponder 安装包与独立验收 Mod。客户端报告及原始截图在 `ponder-revo-smoke/` 下，运行库与安装包指纹在 `runtime.json`。开发验收 Mod 和测试场景不会进入发行包。

NFR 0.8.0 首次生成的默认 TOML 存在两次 `[compat]` 声明，官方包会在解析该文件时退出。启动器仅为隔离验收实例预置一个合法的最小 NFR 配置，其余选项由 NFR 自己使用运行默认值；没有修改依赖 jar 或用户实例。该配置见 [`src/revoHarness/fixtures/neofontrender.toml`](../src/revoHarness/fixtures/neofontrender.toml)。
