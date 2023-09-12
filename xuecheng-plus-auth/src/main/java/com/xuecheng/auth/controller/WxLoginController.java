package com.xuecheng.auth.controller;

import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.Impl.WxAuthServiceImpl;
import com.xuecheng.ucenter.service.WxAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;

@Controller
@Slf4j
public class WxLoginController {
    @Autowired
    WxAuthService wxAuthService;

    @RequestMapping("/WxLogin")
    public String wxLogin(String code, String state) throws IOException {
        log.debug("WeChat code scan callback,code{},state:{}", code, state);
        XcUser xcUser = wxAuthService.wxAuth(code);



        if (xcUser == null) {
            return "redirect:http://www.51xuecheng.cn/error.html";
        }
        String username = xcUser.getUsername();
        return "redirct:http://www.51xuecheng.cn/sign.html?username="+username+"&authType=wx";
    }
}
