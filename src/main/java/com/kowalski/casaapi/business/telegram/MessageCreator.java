package com.kowalski.casaapi.business.telegram;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class MessageCreator {

    public static SendMessage createMessage(Long chatId, String text, boolean withKeyboard) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);

        if (withKeyboard) {
            message.setReplyMarkup(MainMenuHandler.createMainMenuKeyboard());
        }

        return message;
    }
}