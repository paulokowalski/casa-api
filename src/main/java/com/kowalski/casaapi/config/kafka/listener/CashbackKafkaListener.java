package com.kowalski.casaapi.config.kafka.listener;

import com.kowalski.casaapi.config.telegram.MyGenericBot;
import com.kowalski.casaapi.business.telegram.MessageCreator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class CashbackKafkaListener {

    private final MyGenericBot myGenericBot;

    @Value("${telegram.chatid}")
    private Long chatId;

    public CashbackKafkaListener(MyGenericBot myGenericBot) {
        this.myGenericBot = myGenericBot;
    }

    @KafkaListener(topics = "notify-cashback-topic", groupId = "${spring.kafka.groupId}", containerFactory = "kafkaListenerContainerFactoryString")
    public void listen(String message) {
        try {
            myGenericBot.execute(MessageCreator.createMessage(chatId, message, false));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 