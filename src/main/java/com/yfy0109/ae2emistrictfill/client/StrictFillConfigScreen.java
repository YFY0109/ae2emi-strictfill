package com.yfy0109.ae2emistrictfill.client;

import com.yfy0109.ae2emistrictfill.StrictFillConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * 仿 ModernFix 风格的双开关配置页：
 * 1. 是否启用模组；
 * 2. 启用时，详细配方页是否允许不完整填充。
 * 修改后直接写入 StrictFillConfig，Mixin 下一次点击就会读到新值。
 */
public class StrictFillConfigScreen extends Screen {
    private final Screen parent;

    private Button enabledButton;
    private Button allowPartialButton;

    public StrictFillConfigScreen(Screen parent) {
        super(Component.translatable("ae2emi_strictfill.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;

        this.enabledButton = Button.builder(this.toggleText(StrictFillConfig.enabled), button -> {
            StrictFillConfig.setEnabled(!StrictFillConfig.enabled);
            button.setMessage(this.toggleText(StrictFillConfig.enabled));
            this.updateAllowPartialState();
        }).bounds(centerX - 60, 70, 120, 20).build();
        this.addRenderableWidget(this.enabledButton);
        this.addHelpButton(centerX + 65, 70, "ae2emi_strictfill.config.enabled.tooltip");

        this.allowPartialButton = Button.builder(
                this.toggleText(StrictFillConfig.allowPartialInRecipeScreen), button -> {
                    StrictFillConfig.setAllowPartialInRecipeScreen(!StrictFillConfig.allowPartialInRecipeScreen);
                    button.setMessage(this.toggleText(StrictFillConfig.allowPartialInRecipeScreen));
                }).bounds(centerX - 60, 120, 120, 20).build();
        this.addRenderableWidget(this.allowPartialButton);
        this.addHelpButton(centerX + 65, 120, "ae2emi_strictfill.config.allow_partial.tooltip");

        this.updateAllowPartialState();

        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds(centerX - 100, this.height - 30, 200, 20).build());
    }

    private void addHelpButton(int x, int y, String tooltipKey) {
        Button helpButton = Button.builder(Component.literal("?"), button -> {
        }).bounds(x, y, 20, 20).build();
        helpButton.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        this.addRenderableWidget(helpButton);
    }

    private void updateAllowPartialState() {
        if (this.allowPartialButton != null) {
            this.allowPartialButton.active = StrictFillConfig.enabled;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        graphics.drawCenteredString(this.font, this.title, centerX, 20, 0xFFFFFF);

        graphics.drawCenteredString(this.font,
                Component.literal("ae2emi_strictfill.enabled").withStyle(ChatFormatting.DARK_GRAY),
                centerX, 38, 0x808080);
        graphics.drawCenteredString(this.font,
                Component.translatable("ae2emi_strictfill.config.enabled"),
                centerX, 50, 0xFFFFFF);

        graphics.drawCenteredString(this.font,
                Component.literal("ae2emi_strictfill.allow_partial").withStyle(ChatFormatting.DARK_GRAY),
                centerX, 88, 0x808080);
        graphics.drawCenteredString(this.font,
                Component.translatable("ae2emi_strictfill.config.allow_partial"),
                centerX, 100, 0xFFFFFF);

        graphics.drawCenteredString(this.font,
                Component.translatable(StrictFillConfig.enabled
                        ? "ae2emi_strictfill.config.hint.enabled"
                        : "ae2emi_strictfill.config.hint.disabled"),
                centerX, 150, 0xA0A0A0);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component toggleText(boolean value) {
        return Component.translatable(value ? "ae2emi_strictfill.config.on" : "ae2emi_strictfill.config.off")
                .withStyle(value ? ChatFormatting.GREEN : ChatFormatting.RED);
    }
}
