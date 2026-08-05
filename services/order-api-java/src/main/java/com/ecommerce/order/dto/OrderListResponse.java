package com.ecommerce.order.dto;

import java.util.List;

public class OrderListResponse {
    private List<OrderResponse> orders;
    private long total;
    private int limit;
    private int offset;

    public OrderListResponse() {}

    public OrderListResponse(List<OrderResponse> orders, long total, int limit, int offset) {
        this.orders = orders;
        this.total = total;
        this.limit = limit;
        this.offset = offset;
    }

    public List<OrderResponse> getOrders() { return orders; }
    public void setOrders(List<OrderResponse> orders) { this.orders = orders; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
    public int getOffset() { return offset; }
    public void setOffset(int offset) { this.offset = offset; }
}
