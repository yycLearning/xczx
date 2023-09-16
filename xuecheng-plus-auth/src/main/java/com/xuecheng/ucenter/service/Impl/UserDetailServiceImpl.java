package com.xuecheng.ucenter.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.ucenter.mapper.XcMenuMapper;
import com.xuecheng.ucenter.mapper.XcUserMapper;
import com.xuecheng.ucenter.model.dto.AuthParamsDto;
import com.xuecheng.ucenter.model.dto.XcUserExt;
import com.xuecheng.ucenter.model.po.XcMenu;
import com.xuecheng.ucenter.model.po.XcUser;
import com.xuecheng.ucenter.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class UserDetailServiceImpl implements UserDetailsService {
    @Autowired
    XcUserMapper xcUserMapper;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    XcMenuMapper xcMenuMapper;
    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        AuthParamsDto authParamsDto = null;
        try {
            authParamsDto= JSON.parseObject(s,AuthParamsDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Request params is illegal");
        }
        String authType = authParamsDto.getAuthType();
        String beanName = authType+"_authservice";
        AuthService bean = applicationContext.getBean(beanName, AuthService.class);
        XcUserExt execute = bean.execute(authParamsDto);

        UserDetails userDetails = getUserDetails(execute);

        return userDetails;
    }
    public UserDetails getUserDetails(XcUserExt xcUser){
        String password = xcUser.getPassword();
        List<XcMenu> xcMenus = xcMenuMapper.selectPermissionByUserId(xcUser.getId());
        String[] authorities = {"test"};
        if(xcMenus.size()>0){
            List<String> permissions = new ArrayList<>();
            xcMenus.forEach(m->{
                permissions.add(m.getCode());
            });
            authorities = permissions.toArray(new String[0]);
        }
        xcUser.setPassword(null);
        String jsonString = JSON.toJSONString(xcUser);
        UserDetails userDetails = User.withUsername(jsonString).password(password).authorities(authorities).build();
        return userDetails;
    }
}
