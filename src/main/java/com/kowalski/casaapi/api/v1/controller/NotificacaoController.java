package com.kowalski.casaapi.api.v1.controller;

import com.kowalski.casaapi.business.telegram.MessageCreator;
import com.kowalski.casaapi.config.telegram.MyGenericBot;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/v1/notificacao")
public class NotificacaoController {

    private final MyGenericBot myGenericBot;

    public NotificacaoController(MyGenericBot myGenericBot){
        this.myGenericBot = myGenericBot;
    }

    @Value("${telegram.chatid}")
    private String chatId;

    @PostMapping
    public void notificar(@RequestParam String notificacao){
        try {
            myGenericBot.execute(MessageCreator.createMessage(Long.parseLong(chatId), notificacao, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
