package com.kowalski.casaapi.business.telegram;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static com.kowalski.casaapi.config.telegram.MenuStatic.BTN_AJUDA;
import static com.kowalski.casaapi.config.telegram.MenuStatic.BTN_COMPRAS;

public class MainMenuHandler {
    private final BotStateManager stateManager;

    public MainMenuHandler(BotStateManager stateManager) {
        this.stateManager = stateManager;
    }

    public SendMessage handleMainMenu(Long chatId, String messageText) {
        if (messageText.equals(BTN_COMPRAS)) {
            stateManager.setState(chatId, BotStateManager.BotState.SHOPPING_MENU);
            return ListaCompraMenuHandler.createShoppingMenu(chatId);
        } else if (messageText.equals(BTN_AJUDA)) {
            return createMainMenu(chatId, """
                    📚 Ajuda:
                    - 🛒 Compras: Gerencia lista de compras
                    """);
        }
        return createMainMenu(chatId);
    }

    public static SendMessage createMainMenu(Long chatId) {
        return createMainMenu(chatId, "Menu Principal:");
    }

    public static SendMessage createMainMenu(Long chatId, String text) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text(text)
                .replyMarkup(createMainMenuKeyboard())
                .build();
    }

    public static ReplyKeyboardMarkup createMainMenuKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(BTN_COMPRAS);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(BTN_AJUDA);

        rows.add(row1);
        rows.add(row2);

        keyboard.setKeyboard(rows);
        return keyboard;
    }
}