package com.xuecheng.content.api;

import com.alibaba.fastjson.JSON;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDTO;
import com.xuecheng.content.model.dto.TeachPlanDTO;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.util.SecurityUtil;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@Controller
public class CoursePublishController {
    @Autowired
    CoursePublishService coursePublishService;

    @ApiOperation("get course publish info")
    @ResponseBody
    @GetMapping("/course/whole/{CourseId}")
    public CoursePreviewDTO getCoursePublish(@PathVariable("CourseId") Long courseId){
        CoursePreviewDTO coursePreviewDTO = new CoursePreviewDTO();
        CoursePublish coursePublish = coursePublishService.getCoursepublish(courseId);
        if(coursePublish==null){
            return coursePreviewDTO;
        }
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        BeanUtils.copyProperties(coursePublish,courseBaseInfoDto);
        String teachplan = coursePublish.getTeachplan();
        List<TeachPlanDTO> teachPlanDTOS = JSON.parseArray(teachplan, TeachPlanDTO.class);
        coursePreviewDTO.setCourseBase(courseBaseInfoDto);
        coursePreviewDTO.setTeachPlans(teachPlanDTOS);
        return coursePreviewDTO;
    }

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
        String companyId = SecurityUtil.getUser().getId();
        long company = Long.parseLong(companyId);
        coursePublishService.commitAudit(company,courdId);
    }
    @ApiOperation("course publish")
    @ResponseBody
    @PostMapping("/coursepublish/{courseId}")
    public void  coursePublish(@PathVariable("courseId") Long courseId){
        String companyId = SecurityUtil.getUser().getId();
        long company = Long.parseLong(companyId);
        coursePublishService.coursePublish(company,courseId);
    }

    @ApiOperation("querying course publish info")
    @ResponseBody
    @GetMapping("/r/coursepublish/{courseId}")
    public CoursePublish getCoursepublish(@PathVariable("courseId") Long courseId){
        CoursePublish coursePublish= coursePublishService.getCoursepublish(courseId);
        return coursePublish;
    }
}
