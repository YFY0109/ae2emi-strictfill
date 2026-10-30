package com.yfy0109.ae2emistrictfill.mixin;

import appeng.core.localization.ItemModText;
import appeng.integration.modules.emi.EmiUseCraftingRecipeHandler;
import appeng.menu.me.items.CraftingTermMenu;
import com.yfy0109.ae2emistrictfill.StrictFillConfig;
import com.yfy0109.ae2emistrictfill.client.StrictFillState;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.recipe.handler.EmiCraftContext;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

/**
 * 严格填充：
 * 在 AE2 的 EMI 配方传输入口前重新检查真实配方的每个槽位，
 * 只要存在“完全缺失”或“只能自动合成”的槽位，就直接返回失败。
 * 这样：
 * - 配方页 + 按钮在材料不齐时不可点；
 * - 左键收藏在材料不齐时 performFill 失败，EMI 会回落到配方页。
 */
@Mixin(value = EmiUseCraftingRecipeHandler.class, remap = false)
public abstract class EmiUseCraftingRecipeHandlerMixin {
    @Inject(
            method = "transferRecipe(Lappeng/menu/me/items/CraftingTermMenu;Lnet/minecraft/world/item/crafting/Recipe;Ldev/emi/emi/api/recipe/EmiRecipe;Z)Lappeng/integration/modules/emi/AbstractRecipeHandler$Result;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false
    )
    private void ae2emi$strictFill(CraftingTermMenu menu, Recipe<?> recipeBase, EmiRecipe emiRecipe,
                                   boolean doTransfer, CallbackInfoReturnable<Object> cir) {
        if (!StrictFillConfig.enabled) {
            return;
        }

        // 取出本次调用的 EMI 上下文类型（取完即清，避免线程复用残留）
        EmiCraftContext.Type type = StrictFillState.takeType();
        if (type == EmiCraftContext.Type.FILL_BUTTON && StrictFillConfig.allowPartialInRecipeScreen) {
            // 配方页允许部分填充：保持 AE2 原行为（PartiallyCraftable + Ctrl 自动合成）
            return;
        }

        if (emiRecipe == null || !VanillaEmiRecipeCategories.CRAFTING.equals(emiRecipe.getCategory())) {
            return;
        }

        try {
            Recipe<?> recipe = recipeBase;
            if (recipe == null) {
                recipe = ae2emi$createFakeRecipe(emiRecipe);
                if (recipe == null) {
                    return;
                }
            }

            Map<Integer, Ingredient> slotToIngredient =
                    EmiUseCraftingRecipeHandler.getGuiSlotToIngredientMap(recipe);
            CraftingTermMenu.MissingIngredientSlots missing =
                    menu.findMissingIngredients(slotToIngredient);

            if (!missing.anyMissingOrCraftable()) {
                return;
            }

            Object failure = ae2emi$createFailure(missing.missingSlots());
            if (failure != null) {
                cir.setReturnValue(failure);
            }
        } catch (RuntimeException e) {
            // 某些整合包配方可能让 findMissingIngredients 抛异常；不要让一次点击崩溃
            StrictFillConfig.warnOnce("check", "严格填充检查异常，回退原行为", e);
        }
    }

    /**
     * EmiUseCraftingRecipeHandler#createFakeRecipe 是 private；
     * 只有 EMI 配方没有 backing recipe 时才会用到，走反射并带降级。
     */
    @Unique
    private Recipe<?> ae2emi$createFakeRecipe(EmiRecipe emiRecipe) {
        try {
            Method method = EmiUseCraftingRecipeHandler.class
                    .getDeclaredMethod("createFakeRecipe", EmiRecipe.class);
            if (!method.trySetAccessible()) {
                StrictFillConfig.warnOnce("createFakeRecipe",
                        "无法访问 createFakeRecipe，缺少 backing recipe 的配方保持原行为", null);
                return null;
            }
            return (Recipe<?>) method.invoke(this, emiRecipe);
        } catch (ReflectiveOperationException | RuntimeException e) {
            StrictFillConfig.warnOnce("createFakeRecipe",
                    "调用 createFakeRecipe 失败，缺少 backing recipe 的配方保持原行为", e);
            return null;
        }
    }

    /**
     * AbstractRecipeHandler.Result 是包私有嵌套类，这里用反射构造失败结果。
     */
    @Unique
    private static Object ae2emi$createFailure(Set<Integer> missingSlots) {
        try {
            Class<?> resultClass = Class.forName(
                    "appeng.integration.modules.emi.AbstractRecipeHandler$Result");
            Method method = resultClass.getDeclaredMethod("createFailed", Component.class, Set.class);
            if (!method.trySetAccessible()) {
                StrictFillConfig.warnOnce("createFailure",
                        "无法访问 Result.createFailed，严格填充无法生效", null);
                return null;
            }
            return method.invoke(null, ItemModText.NO_ITEMS.text(), missingSlots);
        } catch (ReflectiveOperationException | RuntimeException e) {
            StrictFillConfig.warnOnce("createFailure", "构造严格填充失败结果时出错", e);
            return null;
        }
    }
}
