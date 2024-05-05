package com.telegramTest.service;

import com.telegramTest.config.TelegramConfig;
import com.telegramTest.entities.User;
import com.telegramTest.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class BotService extends TelegramLongPollingBot {
    private TelegramConfig telegramConfig;
    private UserRepository userRepository;
    public BotService(TelegramConfig telegramConfig, UserRepository userRepository) {
        this.telegramConfig = telegramConfig;
        this.userRepository = userRepository;
        List<BotCommand> menu = new ArrayList<>();
        menu.add(new BotCommand("/start", "Hello world message"));
        menu.add(new BotCommand("/register", "Register yourself"));
        menu.add(new BotCommand("/users", "Get all users"));
        try {
            this.execute(new SetMyCommands(menu, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage()) {
            Message message = update.getMessage();
            Long chatid = message.getChatId();
            switch (message.getText()) {
                case "/start": {
                    ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
                    List<KeyboardRow> rows = new ArrayList<>();
                    KeyboardRow row = new KeyboardRow();
                    row.add("/register");
                    row.add("/users");
                    rows.add(row);
                    keyboardMarkup.setKeyboard(rows);
                    keyboardMarkup.setResizeKeyboard(false);
                    sendMessage(chatid, "Hello, @" + update.getMessage().getChat().getUserName() + "!", keyboardMarkup);
                    break;
                }
                case "/register": {
                    Optional<User> user = userRepository.findByUsername("@" + message.getChat().getUserName());
                    if (user.isPresent()) sendMessage(chatid, "You already registered!");
                    else {
                        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
                        List<InlineKeyboardButton> row = new ArrayList<>();
                        InlineKeyboardButton yes = new InlineKeyboardButton();
                        yes.setText("Yes");
                        yes.setCallbackData("YES_BUTTON");
                        InlineKeyboardButton no = new InlineKeyboardButton();
                        no.setText("No");
                        no.setCallbackData("NO_BUTTON");
                        row.add(yes);
                        row.add(no);
                        rows.add(row);
                        keyboardMarkup.setKeyboard(rows);
                        sendMessage(update.getMessage().getChatId(), "Are you sure?", keyboardMarkup);
                    }
                    break;
                }
                case "/users": {
                    List<User> users = userRepository.findAll();
                    if (users.isEmpty()) sendMessage(chatid, "List of users is clear");
                    else users.forEach(el -> sendMessage(chatid, el.getId() + " - " + el.getUsername()));
                    break;
                }
                default: {
                    sendMessage(update.getMessage().getChatId(), "I don't know such command!");
                    break;
                }
            }
        }
        else if (update.hasCallbackQuery()) {
            String userName = update.getCallbackQuery().getFrom().getUserName();
            Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();
            String callbackData = update.getCallbackQuery().getData();
            switch (callbackData) {
                case "YES_BUTTON": {
                    register(messageId, chatId, userName);
                    break;
                }
                case "NO_BUTTON": {
                    editMessage(messageId, chatId, "Registration canceled!");
                    break;
                }
                default: {
                    log.error("Unknown callback data");
                    break;
                }
            }
        }
    }
    public void register(Integer messageId, Long chatid, String userName) {
        User newUser = new User();
        newUser.setUsername("@" + userName);
        try {
            userRepository.save(newUser);
            editMessage(messageId, chatid, "Successfully registered!");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
    public void sendMessage(Long to, String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(to);
        sendMessage.setText(message);
        try {
            this.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
    public void sendMessage(Long to, String message, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(to);
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(keyboardMarkup);
        try {
            this.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
    public void sendMessage(Long to, String message, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(to);
        sendMessage.setText(message);
        sendMessage.setReplyMarkup(keyboardMarkup);
        try {
            this.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
    public void editMessage (Integer messageId, Long chatId, String newMessage) {
        EditMessageText editMessageText = new EditMessageText();
        editMessageText.setText(newMessage);
        editMessageText.setChatId(chatId);
        editMessageText.setMessageId(messageId);
        try {
            this.execute(editMessageText);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }
    @Override
    public String getBotUsername() {
        return telegramConfig.getName();
    }

    @Override
    public String getBotToken() {
        return telegramConfig.getToken();
    }
}
