package com.trade.algotrade.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;

@Data
@AllArgsConstructor
public class OrderMessage {
    private String status;
    private String orderId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderMessage)) return false;
        OrderMessage that = (OrderMessage) o;
        return Objects.equals(getStatus(), that.getStatus()) && Objects.equals(getOrderId(), that.getOrderId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getStatus(), getOrderId());
    }
}
