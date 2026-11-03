# MMCE 可选适配

Ponder 的 Cleanroom 运行包内置 MMCE 适配代码，MMCE 自身仍需独立安装。
只安装 Ponder 时也能正常使用；不需要额外的 Ponder-MMCE 运行 jar。

目标为 **Modular Machinery: Community Edition 2.3.2**（CurseForge 文件 `7372951`）。
其他 MMCE 版本先检查所需 ABI；通过检查不等于该版本已经完成运行验收。
不把此结论扩展到原版 Modular Machinery、其他分支或 MMCE 附属。

## 已接入内容

- 读取 MMCE 静态机器结构和指定次数、方向的 Dynamic Pattern。
- 归一化负坐标，保留方块状态与 preview NBT；生成带内容指纹的结构 ID。
- 提供 `mmce:all`、`mmce:controller`、`mmce:tag/<标签>` 等结构命名组。
- 动态段分组为 `mmce:dynamic/<模式>/segment/<序号>/<标签>`，末端为 `mmce:dynamic/<模式>/end/<标签>`。
- 按蓝图 NBT 中的机器 ID 选择场景；同一蓝图物品可以对应不同机器，每台机器也可以有多个场景。
- Java / ZenScript 使用相同的 `MMCEStructures` 接口；生成的结构 ID 可用于 JSON pack 和 F7 编辑器。

适配不会自动给机器生成教程或安装默认示例。整合包作者需要编写演示；没有演示的蓝图不会显示可播放入口。
暂不提供材料清单、配方叠加或放置后的控制器入口。

## ZenScript 示例

保存到 `scripts/ponder/scenes/my_machine.zs`，把机器名替换成自己的 MMCE 注册名。
`#modloaded` 让这份整合包脚本在未安装 MMCE 时跳过。

```zenscript
#modloaded modularmachinery
import mods.ponder.SceneRegistry;
import mods.ponder.Selection;
import mods.ponder.mmce.MMCEStructures;

val machine = MMCEStructures.machine("modularmachinery:my_machine");
val scene = SceneRegistry.create(machine.component, "my_pack:assembly",
    "机器搭建", machine.structure);
scene.configureBasePlate(0, 0, machine.basePlateSize);
scene.showBasePlate();
scene.world.showSection(Selection.structureGroup("mmce:all"), "down");
scene.idle(40);
scene.markAsFinished();
scene.register();
```

另一段教程继续使用 `machine.component`，换一个场景 ID，即可使用同物品多场景菜单。
动态结构使用：

```zenscript
val extended = MMCEStructures.dynamic("modularmachinery:my_machine",
    "line", 3, "north", "north");
```

参数依次为机器 ID、动态模式名、重复次数、模式偏移方向和结构朝向。
重复次数须位于 MMCE JSON 定义的范围内。

## Java、JSON 与编辑器

Java 入口：`net.createmod.ponder.mmce.script.MMCEStructures`。
返回的 `MMCEStructureRef` 包含 `component`、`structure`、尺寸、控制器坐标和 `fingerprint`。
调用代码应隔离在自己的可选兼容包内，检测 `modularmachinery` 已加载后、在方块注册完成后使用。
JSON / 编辑器的 `component` 与 `structure` 字段直接使用这些返回值，动画选择器可使用 `structure_group`。

结构 ID 包含指纹；修改 MMCE 机器后，需重新生成引用。旧指纹会报错，防止在错误结构上播放动画。
ZenScript 在下次启动时重新生成；导出的 JSON 结构 ID 需由作者同步更新。
跨服务器分发场景时，客户端与服务器需安装匹配的机器定义。

MMCE 有自己的模型和纹理配置，Ponder 不替作者生成自定义控制器模型。

## 可选依赖边界

- 主包只声明 `after:modularmachinery`，构建使用 `compileOnly`，不嵌入 MMCE 类。
- 未安装 MMCE 时不加载适配实现及脚本类，也不注册 Provider / Resolver。
- ABI 不匹配时记录原因并禁用适配，Ponder 仍可正常启动。
- 若安装旧 `ponder_mmce` 外部附属，内置入口将注册权交给它；旧附属本身的 Cleanroom 兼容性需另行确认。
- 适配核心不依赖客户端类，专用服务器同样提供结构与蓝图解析。

## 验证

```bash
./tools/dev.sh build reobfMmceHarnessJar
PONDER_CLEANROOM_VERSION=0.4.0-alpha python3 tools/verify-cleanroom.py client --mmce present
PONDER_CLEANROOM_VERSION=0.4.0-alpha python3 tools/verify-cleanroom.py server --mmce present
# 同样验证 absent（不安装 MMCE）、incompatible（模拟 ABI 不匹配）。
# PONDER_CLEANROOM_VERSION=0.4.0 可运行正式版对照。
```

测试机器和脚本只显式安装到独立 `run/cleanroom-<版本>-mmce/` 实例，不进入用户游戏目录。
报告记录实际运行库与生产包校验值；`absent` 额外检查 JVM 类加载日志。
结果与截图见 [Cleanroom 验收记录](CLEANROOM-VERIFICATION.md)，历史产物的验证结论不替代当前包。
