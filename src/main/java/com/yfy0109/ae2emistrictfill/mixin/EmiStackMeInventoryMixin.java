package com.yfy0109.ae2emistrictfill.mixin;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import org.gtlcore.gtlcore.client.ae2.MeInventoryAmountClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 在 EMI 的物品/流体 tooltip 末尾追加“ME 网络库存：N”。
 * <p>
 * GTLCore 自带的同类功能只注册在 JEI 上（JeiMeInventoryTooltip），EMI 接管物品列表后不会触发。
 * 这里直接复用 GTLCore 的客户端查询与缓存（MeInventoryAmountClient.getTooltip）。
 * <p>
 * 触发条件由 GTLCore 的 WirelessTerminalGridResolver 决定：
 * 背包/Curios 里有已连接、电量 ≥ 50%、在范围内的 AE2 无线终端。
 */
@Mixin(value = EmiStack.class, remap = false)
public abstract class EmiStackMeInventoryMixin {

    @Inject(method = "getTooltip", at = @At("RETURN"), remap = false)
    private void ae2emi$appendMeInventory(CallbackInfoReturnable<List<ClientTooltipComponent>> cir) {
        try {
            EmiStack self = (EmiStack) (Object) this;
            AEKey key = ae2emi$toKey(self);
            if (key == null) {
                return;
            }
            MeInventoryAmountClient.getTooltip(key).ifPresent(component ->
                    cir.getReturnValue().add(ClientTooltipComponent.create(component.getVisualOrderText())));
        } catch (Throwable ignored) {
            // 没装 GTLCore 或网络请求异常时静默跳过，不影响 EMI 正常 tooltip
        }
    }

    private static AEKey ae2emi$toKey(EmiStack stack) {
        ItemStack itemStack = stack.getItemStack();
        if (!itemStack.isEmpty()) {
            return AEItemKey.of(itemStack);
        }
        Object key = stack.getKey();
        if (key instanceof Fluid fluid) {
            return AEFluidKey.of(fluid);
        }
        return null;
    }
}
