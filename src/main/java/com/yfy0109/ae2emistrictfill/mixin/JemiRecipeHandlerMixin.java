package com.yfy0109.ae2emistrictfill.mixin;

import appeng.integration.modules.emi.EmiUseCraftingRecipeHandler;
import appeng.menu.me.items.CraftingTermMenu;
import com.glodblock.github.extendedae.container.ContainerExCraftingTerminal;
import com.yfy0109.ae2emistrictfill.StrictFillConfig;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * EMI 的 JEMI 兼容层会把 ExtendedAE 的 JEI 传输处理器包成 JemiRecipeHandler。
 * 这里在 EMI 这一层区分 CRAFTABLE（左键收藏）和 FILL_BUTTON（配方页 +）：
 * - CRAFTABLE：启用时始终严格，材料不齐不填；
 * - FILL_BUTTON：只有 allowPartialInRecipeScreen=true 时才允许部分填充。
 *
 * 不再修改 findMissingIngredients 的返回值，因此预览只高亮真正缺失的槽位，
 * 不会出现“只有一个槽缺失却全部标红”的问题。
 */
@Mixin(targets = "dev.emi.emi.jemi.JemiRecipeHandler", remap = false)
public abstract class JemiRecipeHandlerMixin {
    @Inject(method = "canCraft", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2emi$strictCanCraft(EmiRecipe recipe, EmiCraftContext<?> context,
                                       CallbackInfoReturnable<Boolean> cir) {
        if (!StrictFillConfig.enabled) {
            return;
        }
        if (!(context.getScreenHandler() instanceof ContainerExCraftingTerminal menu)) {
            return;
        }

        CraftingTermMenu.MissingIngredientSlots missing = ae2emi$missing(menu, recipe);
        if (missing == null || !missing.anyMissingOrCraftable()) {
            return;
        }

        if (context.getType() == EmiCraftContext.Type.CRAFTABLE
                || !StrictFillConfig.allowPartialInRecipeScreen) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "craft", at = @At("HEAD"), cancellable = true, remap = false)
    private void ae2emi$strictCraft(EmiRecipe recipe, EmiCraftContext<?> context,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (!StrictFillConfig.enabled) {
            return;
        }
        if (context.getType() != EmiCraftContext.Type.CRAFTABLE) {
            return;
        }
        if (!(context.getScreenHandler() instanceof ContainerExCraftingTerminal menu)) {
            return;
        }

        CraftingTermMenu.MissingIngredientSlots missing = ae2emi$missing(menu, recipe);
        if (missing != null && missing.anyMissingOrCraftable()) {
            // 左键收藏始终严格：材料不齐直接失败，让 EMI 回落到配方页
            cir.setReturnValue(false);
        }
    }

    @Unique
    private static CraftingTermMenu.MissingIngredientSlots ae2emi$missing(
            ContainerExCraftingTerminal menu, EmiRecipe emiRecipe) {
        Recipe<?> recipe = emiRecipe.getBackingRecipe();
        if (recipe == null && emiRecipe.getId() != null) {
            recipe = menu.getPlayer().level().getRecipeManager().byKey(emiRecipe.getId()).orElse(null);
        }
        if (recipe == null) {
            return null;
        }
        return menu.findMissingIngredients(EmiUseCraftingRecipeHandler.getGuiSlotToIngredientMap(recipe));
    }
}
