package com.xuecheng.content.api;

import com.xuecheng.content.model.dto.CoursePreviewDTO;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CoursePublishService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CoursePublishController {
    @Autowired
    CoursePublishService coursePublishService;

    @GetMapping("/coursepreview/{courseId}")
    public ModelAndView preview(@PathVariable("courseId") Long courseId){
        ModelAndView modelAndView = new ModelAndView();
        CoursePreviewDTO coursePreviewDTO= coursePublishService.getCoursePreviewInfo(courseId);
        modelAndView.addObject("model",coursePreviewDTO);
        modelAndView.setViewName("course_template");
        return modelAndView;
    }
    @ResponseBody
    @PostMapping("/courseaudit/commit/{courseId}")
    public void commitAudit(@PathVariable("courseId") Long courdId){
        Long company = 1232141425L;
        coursePublishService.commitAudit(company,courdId);
    }
    @ApiOperation("course publish")
    @ResponseBody
    @PostMapping("/coursepublish/{courseId}")
    public void  coursePublish(@PathVariable("courseId") Long courseId){
        Long companyId = 1232141425L;
        coursePublishService.coursePublish(companyId,courseId);
    }
}
