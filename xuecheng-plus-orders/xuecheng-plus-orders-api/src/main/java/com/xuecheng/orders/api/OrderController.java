package com.xuecheng.orders.api;

import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.orders.config.AlipayConfig;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import com.xuecheng.orders.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Api(value = "order payment interface",tags = "order payment")
@Slf4j
@Controller
public class OrderController {

    @Autowired
    OrderService orderService;


    @ApiOperation("generate paymode SQ")
    @PostMapping("/generatepaycode")
    @ResponseBody
    public PayRecordDto generatePayCode(@RequestBody AddOrderDto addOrderDto){
        String userId = SecurityUtil.getUser().getId();
        PayRecordDto order = orderService.createOrder(userId, addOrderDto);
        return order;
    }

    @ApiOperation("Scan order interface")
    @GetMapping("/requestpay")
    public void requestpay(String payNo, HttpServletResponse httpResponse) throws IOException{
        XcPayRecord payRecordByPayno = orderService.getPayRecordByPayno(payNo);
        if(payRecordByPayno==null){
            XueChengPlusException.cast("PayRecord no exists");
        }
        String status = payRecordByPayno.getStatus();
        if(status.equals("601002")){
            XueChengPlusException.cast("Already Paid");
        }

    }

    @ApiOperation("query payment result")
    @GetMapping("/payresult")
    @ResponseBody
    public PayRecordDto payresult(String payNo) throws IOException{
        PayRecordDto payRecordDto = orderService.queryPayResult(payNo);
        return payRecordDto;
    }
}
