package com.yfy0109.ae2emistrictfill.client;

import dev.emi.emi.api.recipe.handler.EmiCraftContext;

/**
 * 记录当前 transferRecipe 调用来自哪条 EMI 路径：
 * FILL_BUTTON（配方页 + 按钮）或 CRAFTABLE（左键收藏/侧栏）。
 * 只在客户端线程使用。
 */
public final class StrictFillState {
    private static final ThreadLocal<EmiCraftContext.Type> CURRENT_TYPE = new ThreadLocal<>();

    private StrictFillState() {
    }

    public static void setType(EmiCraftContext.Type type) {
        CURRENT_TYPE.set(type);
    }

    /** 取值并立即清除，避免线程复用时残留。 */
    public static EmiCraftContext.Type takeType() {
        EmiCraftContext.Type type = CURRENT_TYPE.get();
        CURRENT_TYPE.remove();
        return type;
    }

    public static void clear() {
        CURRENT_TYPE.remove();
    }
}
