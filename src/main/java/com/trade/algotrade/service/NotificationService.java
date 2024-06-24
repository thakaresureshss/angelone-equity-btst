package com.trade.algotrade.service;

import org.springframework.stereotype.Service;


public interface NotificationService {

    public void sendTelegramNotification(String telegramChatId, String message);
}
