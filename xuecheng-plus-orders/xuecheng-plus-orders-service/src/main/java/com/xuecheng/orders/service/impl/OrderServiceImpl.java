package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.logging.stdout.StdOutImpl;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    XcOrdersMapper xcOrdersMapper;
    @Autowired
    XcOrdersGoodsMapper ordersGoodsMapper;
    @Autowired
    XcPayRecordMapper xcPayRecordMapper;

    @Autowired
    OrderServiceImpl currentProxy;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    MqMessageService mqMessageService;
    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        XcOrders xcOrders = saveXcOrders(userId, addOrderDto);
        XcPayRecord payRecord = createPayRecord(xcOrders);
        Long payNo = payRecord.getPayNo();
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
       /* String url= String.format(qrcodeurl, payNo);*/
        String qrCode =null;
        try {
            qrCode =qrCodeUtil.createQRCode("",200,200);
        } catch (IOException e) {
            XueChengPlusException.cast("generating QR code failed");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord,payRecordDto);
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        XcPayRecord xcPayRecord = xcPayRecordMapper.selectOne(new LambdaQueryWrapper<XcPayRecord>().eq(XcPayRecord::getPayNo, payNo));
        if(xcPayRecord==null){
            XueChengPlusException.cast("PayRecord no exists");
        }
        return xcPayRecord;

    }

    @Override
    public PayRecordDto queryPayResult(String payNo) {
        PayStatusDto payStatusDto = queryPayResultFromAlipay(payNo);

        currentProxy.saveAliPayStatus(payStatusDto);
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecordByPayno,payRecordDto);
        return payRecordDto;
    }

    @Override
    public void notifyPayResult(MqMessage message) {
        String jsonString = JSON.toJSONString(message);
        Message message1 = MessageBuilder.withBody(jsonString.getBytes(StandardCharsets.UTF_8)).setDeliveryMode(MessageDeliveryMode.PERSISTENT).build();
        Long id = message.getId();
        CorrelationData correlationData = new CorrelationData(id.toString());
        correlationData.getFuture().addCallback(result->{
            if(result.isAck()){
            log.debug("message sent:{}",jsonString);
            mqMessageService.completed(id);
            }else{
                log.debug("message sent failed:{}",jsonString);
            }
        },ex->{
            log.debug("message sent failed:{}",jsonString);
        });
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT,"",message1,correlationData);
    }

    public PayStatusDto queryPayResultFromAlipay(String payNo){
        return null;
    }
    public void saveAliPayStatus(PayStatusDto payStatusDto){
        String payNo = payStatusDto.getOut_trade_no();
        XcPayRecord payRecordByPayno = getPayRecordByPayno(payNo);
        if(payRecordByPayno==null){
            XueChengPlusException.cast("no correspond payment record");
        }
        Long orderId =payRecordByPayno.getOrderId();
        XcOrders xcOrders = xcOrdersMapper.selectById(orderId);
        if(xcOrders==null){
            XueChengPlusException.cast("no relative order");
        }
        String status = payRecordByPayno.getStatus();
        if("601002".equals(status)){
            return;
        }
        String tredeStatus= payStatusDto.getTrade_status();
        if(tredeStatus.equals("TRADE_SUCCESS")){
            payRecordByPayno.setStatus("601002");
            payRecordByPayno.setOutPayNo(payStatusDto.getTrade_no());
            payRecordByPayno.setOutPayChannel("Alipay");
            payRecordByPayno.setPaySuccessTime(LocalDateTime.now());
            xcPayRecordMapper.updateById(payRecordByPayno);
            xcOrders.setStatus("600002");
            xcOrdersMapper.updateById(xcOrders);

            MqMessage mqMessage = mqMessageService.addMessage("payresult.notify", xcOrders.getOutBusinessId(), xcOrders.getOrderType(), null);
            notifyPayResult(mqMessage);
        }


    }

    public XcPayRecord createPayRecord(XcOrders orders){
        Long id = orders.getId();
        XcOrders xcOrders = xcOrdersMapper.selectById(id);
        if(xcOrders==null){
            XueChengPlusException.cast("order no exists");
        }
        String status = xcOrders.getStatus();
        if(status.equals("601002")){
           XueChengPlusException.cast("order paid already");
        }
        XcPayRecord xcPayRecord = new XcPayRecord();
        xcPayRecord.setPayNo(IdWorkerUtils.getInstance().nextId());
        xcPayRecord.setOrderName(xcOrders.getOrderName());
        xcPayRecord.setOrderId(id);
        xcPayRecord.setTotalPrice(xcOrders.getTotalPrice());
        xcPayRecord.setCurrency("USD");
        xcPayRecord.setCreateDate(LocalDateTime.now());
        xcPayRecord.setStatus("601001");
        xcPayRecord.setUserId(xcOrders.getUserId());
        int insert = xcPayRecordMapper.insert(xcPayRecord);
        if(insert<=0){
            XueChengPlusException.cast("inserting order record failed");
        }
        return xcPayRecord;
    }
    public XcOrders saveXcOrders(String userId,AddOrderDto addOrderDto){
        XcOrders xcOrders = getOrderByBusinessId(addOrderDto.getOutBusinessId());
        if(xcOrders!=null){
            return xcOrders;
        }
        xcOrders = new XcOrders();
        xcOrders.setId(IdWorkerUtils.getInstance().nextId());
        xcOrders.setTotalPrice(addOrderDto.getTotalPrice());
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("600001");
        xcOrders.setUserId(userId);
        xcOrders.setOrderType("60201");
        xcOrders.setOrderName(addOrderDto.getOrderName());
        xcOrders.setOrderDescrip(addOrderDto.getOrderDescrip());
        xcOrders.setOrderDetail(addOrderDto.getOrderDetail());
        xcOrders.setOutBusinessId(addOrderDto.getOutBusinessId());
        int insert = xcOrdersMapper.insert(xcOrders);
        if(insert<=0){
            XueChengPlusException.cast("adding order failed");
        }
        Long orderId = xcOrders.getId();
        String orderDetailJson = addOrderDto.getOrderDetail();
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        xcOrdersGoods.forEach(i->{
            i.setOrderId(orderId);
            ordersGoodsMapper.insert(i);
        });
        return xcOrders;
    }
    public XcOrders getOrderByBusinessId(String businessId){
        XcOrders xcOrders = xcOrdersMapper.selectOne(new LambdaQueryWrapper<XcOrders>().eq(XcOrders::getOutBusinessId, businessId));
        return xcOrders;
    }
}
