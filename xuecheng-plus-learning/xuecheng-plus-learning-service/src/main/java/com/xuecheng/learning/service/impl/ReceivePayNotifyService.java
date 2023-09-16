package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.learning.config.PayNotifyConfig;
import com.xuecheng.learning.service.MyCourseTablesService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.channels.Channel;

@Service
public class ReceivePayNotifyService {
    @Autowired
    MyCourseTablesService myCourseTablesService;
    @RabbitListener(queues = PayNotifyConfig.PAYNOTIFY_QUEUE)
    public void receive(Message message){
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        byte[] body = message.getBody();
        String value = String.valueOf(body);
        String JsonString = new String(body);
        MqMessage mqMessage = JSON.parseObject(JsonString, MqMessage.class);
        String chooseCourseId = mqMessage.getBusinessKey1();
        String OrderType = mqMessage.getBusinessKey2();
        if(OrderType.equals("60201")){

            boolean result = myCourseTablesService.saveChooseCourseSuccess(chooseCourseId);
            if(!result){
                XueChengPlusException.cast("update learingInfo failed");
            }
        }
    }
}
