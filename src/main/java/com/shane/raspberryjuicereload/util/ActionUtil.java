package com.shane.raspberryjuicereload.util;

import com.shane.raspberryjuicereload.type.HitClickType;
import org.bukkit.event.block.Action;

public class ActionUtil {
    public static boolean isLeftClick(Action action) {
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
    }

    public static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    public static boolean isLeftClickOnBlock(Action action) {
        return action == Action.LEFT_CLICK_BLOCK;
    }

    public static boolean isRightClickOnBlock(Action action) {
        return action == Action.RIGHT_CLICK_BLOCK;
    }

    public static boolean isAnyClick(Action action) {
        return isLeftClick(action) || isRightClick(action);
    }

    public static boolean isAnyClickOnBlock(Action action) {
        return isLeftClickOnBlock(action) || isRightClickOnBlock(action);
    }

    public static boolean checkAction(Action action, boolean onBlock, HitClickType hitClickType) {
        return switch (hitClickType) {
            case BOTH -> onBlock ? isAnyClickOnBlock(action) : isAnyClick(action);
            case LEFT -> onBlock ? isLeftClickOnBlock(action) : isLeftClick(action);
            case RIGHT -> onBlock ? isRightClickOnBlock(action) : isRightClick(action);
        };
    }
}
