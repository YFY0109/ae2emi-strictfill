package com.yfy0109.ae2emistrictfill.mixin;

import com.yfy0109.ae2emistrictfill.client.StrictFillState;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在 AbstractRecipeHandler 的统一入口记录 EMI 的上下文类型：
 * - FILL_BUTTON：配方页 + 按钮；
 * - CRAFTABLE：左键收藏/侧栏。
 * 内层 EmiUseCraftingRecipeHandlerMixin 根据这个类型决定是否允许部分填充。
 */
@Mixin(targets = "appeng.integration.modules.emi.AbstractRecipeHandler", remap = false)
public abstract class AbstractRecipeHandlerMixin {
    @Inject(
            method = "transferRecipe(Ldev/emi/emi/api/recipe/EmiRecipe;Ldev/emi/emi/api/recipe/handler/EmiCraftContext;Z)Lappeng/integration/modules/emi/AbstractRecipeHandler$Result;",
            at = @At("HEAD"),
            remap = false
    )
    private void ae2emi$setType(EmiRecipe recipe, EmiCraftContext<?> context, boolean doTransfer,
                                CallbackInfoReturnable<Object> cir) {
        StrictFillState.setType(context.getType());
    }

    @Inject(
            method = "transferRecipe(Ldev/emi/emi/api/recipe/EmiRecipe;Ldev/emi/emi/api/recipe/handler/EmiCraftContext;Z)Lappeng/integration/modules/emi/AbstractRecipeHandler$Result;",
            at = @At("RETURN"),
            remap = false
    )
    private void ae2emi$clearType(EmiRecipe recipe, EmiCraftContext<?> context, boolean doTransfer,
                                  CallbackInfoReturnable<Object> cir) {
        StrictFillState.clear();
    }
}
