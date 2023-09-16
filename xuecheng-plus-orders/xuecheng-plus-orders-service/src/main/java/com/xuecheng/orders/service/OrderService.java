package com.xuecheng.orders.service;

import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.po.XcPayRecord;

public interface OrderService {
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto);
    public XcPayRecord getPayRecordByPayno(String payNo);
    public PayRecordDto queryPayResult(String payNo);
    public void notifyPayResult(MqMessage message);
}
