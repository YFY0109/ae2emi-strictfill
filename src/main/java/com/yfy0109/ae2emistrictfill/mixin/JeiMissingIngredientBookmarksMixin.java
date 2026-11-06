package com.yfy0109.ae2emistrictfill.mixin;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.runtime.EmiFavorites;
import net.minecraftforge.fml.ModList;
import org.gtlcore.gtlcore.integration.jei.JeiMissingIngredientBookmarks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;

/**
 * 在 GTLCore 的“收藏缺失”逻辑之后，把缺失材料同时加入 EMI 书签。
 * <p>
 * GTLCore 原逻辑只加 JEI 书签；这里不改变 JEI 行为，只在 EMI 存在时追加。
 */
@Mixin(value = JeiMissingIngredientBookmarks.class, remap = false)
public class JeiMissingIngredientBookmarksMixin {

    @Inject(method = "add", at = @At("RETURN"), cancellable = true, remap = false)
    private static void ae2emi$addToEmi(Collection<AEKey> keys,
                                        CallbackInfoReturnable<JeiMissingIngredientBookmarks.AddResult> cir) {
        if (!ModList.get().isLoaded("emi")) {
            return;
        }
        int added = 0;
        for (AEKey key : keys) {
            EmiStack stack = ae2emi$toEmi(key);
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            EmiFavorites.addFavorite(stack);
            added++;
        }
        if (added <= 0) {
            return;
        }
        JeiMissingIngredientBookmarks.AddResult original = cir.getReturnValue();
        if (original == null || original.status() != JeiMissingIngredientBookmarks.AddStatus.ADDED) {
            cir.setReturnValue(new JeiMissingIngredientBookmarks.AddResult(
                    JeiMissingIngredientBookmarks.AddStatus.ADDED, added));
        }
    }

    private static EmiStack ae2emi$toEmi(AEKey key) {
        if (key instanceof AEItemKey item) {
            return EmiStack.of(item.toStack());
        }
        if (key instanceof AEFluidKey fluid) {
            return EmiStack.of(fluid.getFluid());
        }
        return null;
    }
}
