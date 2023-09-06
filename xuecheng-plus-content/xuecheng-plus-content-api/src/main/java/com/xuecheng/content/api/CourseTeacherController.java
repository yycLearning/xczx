package com.xuecheng.content.api;

import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/courseTeacher")
public class CourseTeacherController {
    @Autowired
    private CourseTeacherService courseTeacherService;
    @GetMapping("/list/{id}")
    @ApiOperation("query teacher")
    public CourseTeacher selectTeacher(@PathVariable Long id){
        return courseTeacherService.selectTeacher(id);
    }

    @PostMapping
    @ResponseBody
    public CourseTeacher insertTeacher(@RequestBody CourseTeacher courseTeacher){
        Long courseId = 72L;
        return courseTeacherService.insertTeacher(courseId,courseTeacher);
    }
    @PutMapping
    @ResponseBody
    public CourseTeacher updateTeacher(@RequestBody CourseTeacher courseTeacher){
        Long courseId = 72L;
        return courseTeacherService.updateTeacher(courseId,courseTeacher);
    }
    @DeleteMapping("course/{courseId}/{id}")
    public void deleteTeacher(@PathVariable Long courseId,@PathVariable Long id){
        Long courseId1 = 72L;
        courseTeacherService.deleteTeacher(courseId,id,courseId1);
    }
}
