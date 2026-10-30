package com.yfy0109.ae2emistrictfill.mixin;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * 只在安装了 ExtendedAE（expatternprovider）时应用扩展终端相关 Mixin；
 * JEI 传输的 Mixin 还要求 JEI 存在。
 */
public class StrictFillMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("JemiRecipeHandlerMixin")) {
            return isLoaded("expatternprovider");
        }
        if (mixinClassName.endsWith("ExCraftingTransferHandlerMixin")) {
            return isLoaded("expatternprovider") && isLoaded("jei");
        }
        return true;
    }

    private static boolean isLoaded(String modId) {
        try {
            LoadingModList list = LoadingModList.get();
            if (list != null && list.getModFileById(modId) != null) {
                return true;
            }
        } catch (Throwable ignored) {
        }

        // LoadingModList 尚未就绪时的兜底
        try {
            if ("expatternprovider".equals(modId)) {
                Class.forName("com.glodblock.github.extendedae.container.ContainerExCraftingTerminal",
                        false, StrictFillMixinPlugin.class.getClassLoader());
                return true;
            }
            if ("jei".equals(modId)) {
                Class.forName("mezz.jei.api.IModPlugin",
                        false, StrictFillMixinPlugin.class.getClassLoader());
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
