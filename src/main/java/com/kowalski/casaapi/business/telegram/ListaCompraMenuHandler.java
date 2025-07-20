package com.kowalski.casaapi.business.telegram;

import com.kowalski.casaapi.business.model.ListaCompra;
import com.kowalski.casaapi.business.service.ListaCompraService;
import lombok.RequiredArgsConstructor;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

import static com.kowalski.casaapi.config.telegram.MenuStatic.*;

@RequiredArgsConstructor
public class ListaCompraMenuHandler {

    private final BotStateManager stateManager;
    private final ListaCompraService listaCompraService;

    public SendMessage handleShoppingMenu(Long chatId, String messageText) {
        if (messageText.equals(BTN_ADICIONAR_ITEM)) {
            stateManager.setState(chatId, BotStateManager.BotState.AWAITING_ITEM);
            return MessageCreator.createMessage(chatId, "Digite o item que deseja adicionar:", false);
        } else if (messageText.equals(BTN_ADICIONAR_VER_LISTA)) {
            return showShoppingList(chatId);
        } else if (messageText.equals(BTN_REMOVER_ITEM)) {
            stateManager.setState(chatId, BotStateManager.BotState.AWAITING_REMOVAL);
            return MessageCreator.createMessage(chatId, "Digite o item que deseja remover:", false);
        } else if (messageText.equals(BTN_REMOVER_TODOS_ITENS)) {
            return handleItemAllRemoval(chatId);
        }
        return createShoppingMenu(chatId);
    }

    public SendMessage handleNewItem(Long chatId, String item) {
        listaCompraService.salvar(item);
        stateManager.setState(chatId, BotStateManager.BotState.SHOPPING_MENU);
        return MessageCreator.createMessage(chatId, "✅ Item adicionado: " + item, true);
    }

    public SendMessage handleItemRemoval(Long chatId, String item) {
        boolean removed = listaCompraService.remover(item);
        stateManager.setState(chatId, BotStateManager.BotState.SHOPPING_MENU);
        String response = removed ? "❌ Item removido: " + item : "⚠️ Item não encontrado: " + item;
        return MessageCreator.createMessage(chatId, response, true);
    }

    SendMessage handleItemAllRemoval(Long chatId) {
        listaCompraService.removerTodos();
        stateManager.setState(chatId, BotStateManager.BotState.SHOPPING_MENU);
        return MessageCreator.createMessage(chatId, "❌ Items removidos", true);
    }

    private SendMessage showShoppingList(Long chatId) {
        var items = listaCompraService.buscarTodos().stream().map(ListaCompra::getItem).toList();
        if (items.isEmpty()) {
            return MessageCreator.createMessage(chatId, "📝 Sua lista de compras está vazia!", true);
        }

        StringBuilder sb = new StringBuilder("🛒 Sua lista de compras:\n");
        for (int i = 0; i < items.size(); i++) {
            sb.append(i + 1).append(". ").append(items.get(i)).append("\n");
        }

        return MessageCreator.createMessage(chatId, sb.toString(), true);
    }

    public static SendMessage createShoppingMenu(Long chatId) {
        return SendMessage.builder()
                .chatId(chatId.toString())
                .text("Menu de Compras:")
                .replyMarkup(createShoppingMenuKeyboard())
                .build();
    }

    public static ReplyKeyboardMarkup createShoppingMenuKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);

        List<KeyboardRow> rows = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(BTN_ADICIONAR_ITEM);
        row1.add(BTN_ADICIONAR_VER_LISTA);

        KeyboardRow row2 = new KeyboardRow();
        row2.add(BTN_REMOVER_ITEM);
        row2.add(BTN_REMOVER_TODOS_ITENS);

        KeyboardRow row3 = new KeyboardRow();
        row3.add(BTN_VOLTAR);

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        keyboard.setKeyboard(rows);
        return keyboard;
    }
}