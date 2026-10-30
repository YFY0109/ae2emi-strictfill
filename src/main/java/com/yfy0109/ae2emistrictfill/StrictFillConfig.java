package com.yfy0109.ae2emistrictfill;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时配置：
 * 1. enabled：总开关，关闭后完全恢复 AE2 原行为；
 * 2. allowPartialInRecipeScreen：启用时，配方页 + 按钮是否允许不完整填充。
 * 两个值都是 volatile，Mixin 每次点击读取，配置界面修改后无需重启。
 */
public final class StrictFillConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("AE2EMI-StrictFill");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "ae2emi-strictfill.json";
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private static Path configPath;

    /** 总开关：关闭后完全恢复 AE2 原行为。 */
    public static volatile boolean enabled = true;

    /** 启用时：配方页 + 按钮是否允许部分填充；左键收藏始终严格。 */
    public static volatile boolean allowPartialInRecipeScreen = false;

    private StrictFillConfig() {
    }

    public static void load() {
        configPath = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        if (!Files.exists(configPath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonObject object = GSON.fromJson(reader, JsonObject.class);
            if (object != null) {
                if (object.has("enabled")) {
                    enabled = object.get("enabled").getAsBoolean();
                } else if (object.has("strictFill")) {
                    // 兼容旧版单开关配置
                    enabled = object.get("strictFill").getAsBoolean();
                }
                if (object.has("allowPartialInRecipeScreen")) {
                    allowPartialInRecipeScreen = object.get("allowPartialInRecipeScreen").getAsBoolean();
                }
            }
        } catch (Exception e) {
            LOGGER.warn("读取配置失败，使用默认值", e);
        }
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        save();
    }

    public static void setAllowPartialInRecipeScreen(boolean value) {
        allowPartialInRecipeScreen = value;
        save();
    }

    public static void warnOnce(String key, String message, Throwable throwable) {
        if (WARNED.add(key)) {
            LOGGER.warn(message, throwable);
        }
    }

    public static void save() {
        if (configPath == null) {
            configPath = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
        }

        try {
            Files.createDirectories(configPath.getParent());
            JsonObject object = new JsonObject();
            object.addProperty("enabled", enabled);
            object.addProperty("allowPartialInRecipeScreen", allowPartialInRecipeScreen);
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(object, writer);
            }
        } catch (Exception e) {
            LOGGER.warn("保存配置失败", e);
        }
    }
}
