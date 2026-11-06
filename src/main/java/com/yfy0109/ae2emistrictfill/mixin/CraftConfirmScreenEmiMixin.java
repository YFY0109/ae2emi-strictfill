package com.yfy0109.ae2emistrictfill.mixin;

import appeng.api.stacks.AEKey;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.crafting.CraftConfirmScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.lowdragmc.lowdraglib.LDLib;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.fml.ModList;
import org.gtlcore.gtlcore.integration.ae2.common.IConfirmStartMenu;
import org.gtlcore.gtlcore.integration.jei.JeiMissingIngredientBookmarks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * 只有 EMI、没有 JEI 时，GTLCore 不会添加“收藏缺失”按钮。
 * <p>
 * 必须复用 GTLCore 的 style id {@code favoriteMissing}：
 * 它在 {@code assets/ae2/screens/modify_craft_confirm.json} 里有布局。
 * 用自定义 id 会导致 {@code WidgetContainer.add} 里 {@code style.getWidget(id)} 返回 null，
 * 进而在 CraftConfirmScreen 构造时抛 NPE，界面打不开。
 */
@Mixin(value = CraftConfirmScreen.class, remap = false)
public abstract class CraftConfirmScreenEmiMixin extends AEBaseScreen<CraftConfirmMenu> {

    @Unique
    private Button ae2emi$favoriteMissing;

    protected CraftConfirmScreenEmiMixin(CraftConfirmMenu menu, Inventory playerInventory, Component title,
                                        ScreenStyle style) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void ae2emi$addButton(CraftConfirmMenu menu, Inventory playerInventory, Component title, ScreenStyle style,
                                  CallbackInfo ci) {
        // GTLCore 用 LDLib.isJeiLoaded() 判断：EMI 接管时为 false，它不会加按钮
        if (LDLib.isJeiLoaded()) {
            return;
        }
        if (!ModList.get().isLoaded("emi")) {
            return;
        }
        this.ae2emi$favoriteMissing = this.widgets.addButton(
                "favoriteMissing",
                Component.translatable("gui.gtlcore.ae.favorite_missing"),
                this::ae2emi$click);
        this.ae2emi$favoriteMissing.active = false;
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"), remap = false)
    private void ae2emi$update(CallbackInfo ci) {
        if (this.ae2emi$favoriteMissing == null) {
            return;
        }
        this.ae2emi$favoriteMissing.active = !ae2emi$missing().isEmpty();
    }

    @Unique
    private List<AEKey> ae2emi$missing() {
        AbstractContainerMenu menu = this.getMenu();
        if (menu instanceof IConfirmStartMenu confirmMenu) {
            List<AEKey> list = confirmMenu.gtlcore$getMissingNow();
            return list == null ? List.of() : list;
        }
        return List.of();
    }

    @Unique
    private void ae2emi$click(Button button) {
        JeiMissingIngredientBookmarks.add(ae2emi$missing());
    }
}
