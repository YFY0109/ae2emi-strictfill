package com.yfy0109.ae2emistrictfill.client;

import com.yfy0109.ae2emistrictfill.Ae2EmiStrictFill;
import com.yfy0109.ae2emistrictfill.StrictFillConfig;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * 客户端初始化：注册“选项 → 模组”里的配置界面，并加载配置文件。
 */
@Mod.EventBusSubscriber(modid = Ae2EmiStrictFill.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientSetup {
    private ClientSetup() {
    }

    public static void registerConfigScreen() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new StrictFillConfigScreen(parent)
                )
        );
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        StrictFillConfig.load();
    }
}
