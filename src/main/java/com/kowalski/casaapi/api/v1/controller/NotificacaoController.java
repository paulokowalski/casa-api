package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.business.telegram.MessageCreator;
import com.kowalski.casaapi.config.telegram.MyGenericBot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/v1/notificacao")
public class NotificacaoController {

    private final MyGenericBot myGenericBot;

    @Value("${telegram.chatid}")
    private String chatId;

    @PostMapping
    public void notificar(@RequestParam String notificacao) throws TelegramApiException {
        myGenericBot.execute(MessageCreator.createMessage(Long.parseLong(chatId), notificacao, false));
    }
}
