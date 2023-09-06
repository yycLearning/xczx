package com.xuecheng.content.api;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class FreemarkerController {
    @GetMapping("/testfreemarker")
    public ModelAndView test(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.addObject("name","Stella");
        modelAndView.setViewName("test");
        return modelAndView;
    }
}
