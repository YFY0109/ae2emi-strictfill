# AE2 EMI 严格填充（ae2emi_strictfill）

一个很小的 Forge 1.20.1 Mixin 模组：控制 AE2 合成终端在使用 EMI 配方时，是否允许“部分填充”；并在 EMI 里补上 GTLCore 的“ME 网络库存”显示。

## 配置项（两个开关）

游戏内：**选项 → 模组 → AE2 EMI Strict Fill → 配置**

1. **启用模组**（`enabled`）
   - 关闭后完全恢复 AE2 原行为，两个入口都允许部分填充。
2. **详细配方页允许不完整填充**（`allowPartialInRecipeScreen`，仅在启用时生效）
   - 开启：配方页 `+` 按钮仍可把现有材料填进合成格（AE2 原行为，Ctrl+点击可排自动合成）；
   - 关闭：`+` 按钮在材料不齐时不可点。

左键收藏/侧栏配方的行为**始终**由“启用模组”决定：启用时只在材料齐全时填充，材料不齐时展开配方页。

### 行为矩阵

| enabled | allowPartialInRecipeScreen | 左键收藏（材料不齐） | 配方页 `+`（材料不齐） |
|---|---|---|---|
| 开 | 关（默认） | 展开配方页 | 红色/不可点 |
| 开 | 开 | 展开配方页 | 可点，部分填充 |
| 关 | 任意 | AE2 原行为 | AE2 原行为 |

配置文件：`config/ae2emi-strictfill.json`

```json
{
  "enabled": true,
  "allowPartialInRecipeScreen": false
}
```

旧版单开关 `{"strictFill": true}` 会自动迁移为 `enabled`。

## 配置界面

- 两个绿色“开启”/红色“关闭”开关，各带 `?` 悬停说明；
- 修改立即写入 `StrictFillConfig`，Mixin 每次点击读取，**无需重启**；
- 关闭“启用模组”时，第二个开关变灰（值保留，重新启用后继续生效）。

## 构建

```powershell
$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
& "C:\Users\YFY0109\AppData\Local\Temp\opencode\gradle-8.8\bin\gradle.bat" -p "C:\Users\YFY0109\Documents\默认项目\AE2EMI严格填充" build --no-daemon
```

产物：`build/libs/ae2emi-strictfill-1.0.1.jar`（`dist/` 下有副本）。

依赖（编译期，来自整合包，放进 `libs/`）：
- `libs/appliedenergistics2-forge-15.4.10.jar`
- `libs/emi-1.1.22+1.20.1+forge.jar`
- `libs/ExtendedAE-1.20-1.4.19-forge.jar`
- `libs/Glodium-1.20-1.5-forge.jar`
- `libs/jei-1.20.1-forge-15.49.0.188.jar`
- `libs/gtlcore-1.2.3.1-fix9.jar`（仅“ME 网络库存”功能需要）

## 安装

1. 关闭游戏；
2. 把 `ae2emi-strictfill-1.0.1.jar` 放进：
   `C:\Minecraft\.minecraft\versions\GregTech Leisure\mods\`
3. 重启游戏；
4. 打开 **选项 → 模组**，确认能看到 `AE2 EMI Strict Fill`，点“配置”。

仅客户端需要安装；服务端不需要。

## 原理

两个 Mixin：

1. `AbstractRecipeHandlerMixin`：在 `AbstractRecipeHandler#transferRecipe(EmiRecipe, EmiCraftContext, boolean)` 入口记录 EMI 上下文类型（`FILL_BUTTON` / `CRAFTABLE`），出口清除。
2. `EmiUseCraftingRecipeHandlerMixin`：在 `EmiUseCraftingRecipeHandler#transferRecipe(...)` 的 `HEAD`：
   - 读取 `enabled`；
   - 若类型是 `FILL_BUTTON` 且 `allowPartialInRecipeScreen=true`，直接放行原逻辑；
   - 否则用真实 `Recipe`（没有 backing recipe 时反射 `createFakeRecipe`）调用
     `CraftingTermMenu#findMissingIngredients`；
   - 只要 `anyMissingOrCraftable()` 为真，就用反射构造
     `AbstractRecipeHandler.Result.createFailed(...)` 并取消原方法。

这样：
- `FILL_BUTTON`（`+` 按钮）的 `canCraft` 和 `craft` 都受配置控制；
- `CRAFTABLE`（左键收藏）在 `craft` 阶段被严格拦截，进而回落到配方页。

## 已知限制

- AE2 升级如果改动 `transferRecipe` 签名，Mixin 会失效，需要同步更新描述符。
- `createFakeRecipe` / `Result.createFailed` 是反射调用；若被 JPMS 拒绝，会在日志打印 `AE2EMI-StrictFill` 警告并降级为原行为。
- 依赖 AE2 15.4.x / EMI 1.1.22 的类结构。

## ExtendedAE 扩展合成终端（JEI）

ExtendedAE 的 `ContainerExCraftingTerminal` 不是 AE2 的 `CraftingTermMenu`，也没有注册 EMI handler；它的配方传输走 JEI 的 `ExCraftingTransferHandler`。

因此本模组额外处理：

- `JemiRecipeHandlerMixin`：在 EMI/JEMI 层区分 `CRAFTABLE`（左键收藏）和 `FILL_BUTTON`（配方页 `+`）。左键收藏启用时**始终严格**；`+` 只在 `allowPartialInRecipeScreen=true` 时允许部分填充；
- `ExCraftingTransferHandlerMixin`：在 JEI 真正执行 `performTransfer` 前拦截，覆盖直接 JEI 和 JEMI 两条路径；
- 不再修改 `findMissingIngredients` 的返回值，预览只高亮真正缺失的槽位（不会“只缺一个却全红”）。

| enabled | allowPartial | 左键收藏（材料不齐） | ExtendedAE 配方页 `+` |
|---|---|---|---|
| 开 | 关 | 不填，展开配方页 | 阻止部分填充 |
| 开 | 开 | 不填，展开配方页 | 允许部分填充 |
| 关 | 任意 | 原行为 | JEI 原行为 |

注意：ExtendedAE 终端没有 EMI handler，点击 EMI 收藏条目不会在扩展终端里填充（只会打开 EMI 配方页）；你看到的部分填充来自 JEI 的 `+`。如果希望 EMI 收藏在 ExtendedAE 终端里也能填充，需要额外注册一个 EMI handler。

可选依赖：`expatternprovider`、`jei`；未安装时对应 Mixin 由 `StrictFillMixinPlugin` 自动跳过。

## ME 网络库存（EMI，1.0.1 新增）

GTLCore 自带“背包里有无线终端时，JEI/Jade 显示物品在 ME 网络中的数量”，但它的 JEI 部分只挂了 `RenderTooltipEvent.GatherComponents` + `IJeiRuntime`；EMI 自己渲染 tooltip，不触发该事件，所以 EMI 下看不到。

本模组新增 `EmiStackMeInventoryMixin`，注入 `EmiStack#getTooltip` 的 `RETURN`，复用 GTLCore 的 `MeInventoryAmountClient.getTooltip(AEKey)`：

- **JEI 式物品/流体悬停**：EMI 物品列表、书签、配方界面悬停时，tooltip 末尾追加一行；
- **显示文本**：`ME 网络库存：%s`；
- **Jade**：不受影响，GTLCore 的 Jade provider 本来就独立工作。

触发条件由 GTLCore 的 `WirelessTerminalGridResolver` 决定：

1. 背包或 Curios 里有 AE2 `WirelessTerminalItem`（含 AE2WTLib 的无线终端）；
2. 终端已连接网络；
3. 电量 ≥ 50%；
4. 在无线接入点范围内。

未安装 GTLCore 时，Mixin 内捕获 `Throwable` 静默跳过，不影响 EMI 正常 tooltip。

可选依赖：`gtlcore`。