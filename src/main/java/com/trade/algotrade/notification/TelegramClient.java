package com.trade.algotrade.notification;

import feign.QueryMap;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@FeignClient(name = "telegram", url = "${notification.telegram.url}", path = "${notification.telegram.context}")
public interface TelegramClient {

    @GetMapping("/sendMessage?{params}")
    public ResponseEntity<Object> sendMessage(@QueryMap Map<String, String> params);

}