package com.trade.algotrade.service.impl;

import com.trade.algotrade.constants.NotificationConstants;
import com.trade.algotrade.notification.TelegramClient;
import com.trade.algotrade.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    public TelegramClient telegramClient;

    @Override
    public void sendTelegramNotification(String telegramChatId, String message) {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put(NotificationConstants.CHAT_ID, telegramChatId);
        parameters.put(NotificationConstants.TEXT_MESSAGE, message);
        telegramClient.sendMessage(parameters);
    }
}
