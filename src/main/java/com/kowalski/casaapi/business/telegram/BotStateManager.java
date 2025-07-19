package com.kowalski.casaapi.business.telegram;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BotStateManager {
    private final Map<Long, BotState> userStates = new ConcurrentHashMap<>();

    public enum BotState {
        MAIN_MENU,
        SHOPPING_MENU,
        AWAITING_ITEM,
        AWAITING_REMOVAL,
        AWAITING_ALL_REMOVAL
    }

    public BotState getCurrentState(Long chatId) {
        return userStates.getOrDefault(chatId, BotState.MAIN_MENU);
    }

    public void setState(Long chatId, BotState state) {
        userStates.put(chatId, state);
    }

    public void resetToMainMenu(Long chatId) {
        userStates.put(chatId, BotState.MAIN_MENU);
    }
}