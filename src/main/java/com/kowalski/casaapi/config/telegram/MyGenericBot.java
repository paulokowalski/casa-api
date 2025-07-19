package com.kowalski.casaapi.config.telegram;

import com.kowalski.casaapi.business.telegram.CommandDispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
public class MyGenericBot extends TelegramLongPollingBot {
    
    private final CommandDispatcher commandDispatcher;

    @Value("${telegram.user}")
    private String user;

    @Value("${telegram.token}")
    private String token;
    
    public MyGenericBot(CommandDispatcher commandDispatcher) {
        this.commandDispatcher = commandDispatcher;
    }
    
    @Override
    public String getBotUsername() {
        return user;
    }
    
    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            try {
                execute(commandDispatcher.dispatch(update));
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}