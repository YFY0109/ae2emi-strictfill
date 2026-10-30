package com.yfy0109.ae2emistrictfill;

import com.yfy0109.ae2emistrictfill.client.ClientSetup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;

@Mod(Ae2EmiStrictFill.MODID)
public final class Ae2EmiStrictFill {
    public static final String MODID = "ae2emi_strictfill";

    public Ae2EmiStrictFill() {
        // 仅客户端注册配置界面；DistExecutor 防止服务端提前加载客户端类
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientSetup.registerConfigScreen());
    }
}
