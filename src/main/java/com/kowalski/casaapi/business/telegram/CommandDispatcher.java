package com.kowalski.casaapi.business.telegram;

import com.kowalski.casaapi.business.service.ListaCompraService;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import static com.kowalski.casaapi.config.telegram.MenuStatic.BTN_VOLTAR;

@Component
public class CommandDispatcher {

    private final BotStateManager stateManager;
    private final MainMenuHandler mainMenuHandler;
    private final ListaCompraMenuHandler shoppingMenuHandler;

    public CommandDispatcher(ListaCompraService listaCompraService) {
        this.stateManager = new BotStateManager();
        this.mainMenuHandler = new MainMenuHandler(stateManager);
        this.shoppingMenuHandler = new ListaCompraMenuHandler(stateManager, listaCompraService);
    }

    public SendMessage dispatch(Update update) {
        try {
            if (!update.hasMessage() || !update.getMessage().hasText()) {
                return MainMenuHandler.createMainMenu(update.getMessage().getChatId());
            }

            String messageText = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (messageText.equals(BTN_VOLTAR)) {
                return handleBackCommand(chatId);
            }

            BotStateManager.BotState currentState = stateManager.getCurrentState(chatId);

            return switch (currentState) {
                case MAIN_MENU -> mainMenuHandler.handleMainMenu(chatId, messageText);
                case SHOPPING_MENU -> shoppingMenuHandler.handleShoppingMenu(chatId, messageText);
                case AWAITING_ITEM -> shoppingMenuHandler.handleNewItem(chatId, messageText);
                case AWAITING_REMOVAL -> shoppingMenuHandler.handleItemRemoval(chatId, messageText);
                case AWAITING_ALL_REMOVAL -> shoppingMenuHandler.handleItemAllRemoval(chatId);
            };
        } catch (Exception e) {
            return MainMenuHandler.createMainMenu(update.getMessage().getChatId(), "❌ Ocorreu um erro. Escolha uma opção:");
        }
    }

    private SendMessage handleBackCommand(Long chatId) {
        stateManager.resetToMainMenu(chatId);
        return MainMenuHandler.createMainMenu(chatId);
    }
}