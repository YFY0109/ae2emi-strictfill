package com.yfy0109.ae2emistrictfill.mixin;

import com.glodblock.github.extendedae.api.CraftingMode;
import com.glodblock.github.extendedae.container.ContainerExCraftingTerminal;
import com.yfy0109.ae2emistrictfill.StrictFillConfig;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * ExtendedAE 的扩展合成终端只接了 JEI，没有 EMI handler。
 * JEI 的 ExCraftingTransferHandler#handlerRecipe 在 doTransfer=true 时会跳过
 * “全部缺失”检查，直接调用 ExCraftingHelper.performTransfer，所以只改
 * findMissingIngredients 还挡不住实际传输。
 *
 * 这里在真正传输前再检查一次：严格模式下只要存在缺失/仅可自动合成，
 * 就直接返回 null（JEI 视为成功但不执行传输），从而阻止部分填充。
 */
@Mixin(targets = "com.glodblock.github.extendedae.xmod.jei.transfer.ExCraftingTransferHandler", remap = false)
public abstract class ExCraftingTransferHandlerMixin {
    @Inject(
            method = "handlerRecipe",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void ae2emi$blockPartialTransfer(Recipe<?> recipe, CraftingMode mode,
                                             Map<Integer, Ingredient> ingredients,
                                             ContainerExCraftingTerminal menu,
                                             IRecipeSlotsView slotsView, int modeIndex,
                                             boolean doTransfer, boolean craftMissing,
                                             CallbackInfoReturnable<Object> cir) {
        if (!doTransfer || !StrictFillConfig.enabled || StrictFillConfig.allowPartialInRecipeScreen) {
            return;
        }

        var missing = menu.findMissingIngredients(ingredients);
        if (missing != null && missing.anyMissingOrCraftable()) {
            cir.setReturnValue(null);
        }
    }
}
