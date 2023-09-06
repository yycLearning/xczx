package com.xuecheng.content.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.mapper.CourseTeacherMapper;
import com.xuecheng.content.model.po.CourseTeacher;
import com.xuecheng.content.service.CourseTeacherService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {
    @Autowired
    private CourseTeacherMapper courseTeacherMapper;

    @Override
    public CourseTeacher selectTeacher(Long id) {
        CourseTeacher courseTeacher = courseTeacherMapper.selectById(id);
        if (courseTeacher == null) {
            XueChengPlusException.cast("No such person");
        }
        return courseTeacher;
    }

    @Override
    @Transactional
    public CourseTeacher insertTeacher(Long courseId,CourseTeacher courseTeacher) {
        if(courseId!=courseTeacher.getCourseId()){
            XueChengPlusException.cast("course and teacher must be matched");
        }
        CourseTeacher courseTeacher1 = new CourseTeacher();
        BeanUtils.copyProperties(courseTeacher,courseTeacher1);
        courseTeacher1.setCreateDate(LocalDateTime.now());
        int insert = courseTeacherMapper.insert(courseTeacher1);
        if(insert!=1){
            XueChengPlusException.cast("insertion failed");
        }
        return getTeacher(courseId,courseTeacher1.getTeacherName());
    }

    @Override
    public CourseTeacher updateTeacher(Long courseId, CourseTeacher courseTeacher) {
       if(courseId!=courseTeacher.getCourseId()){
           XueChengPlusException.cast("course and teacher must be matched");
       }
       CourseTeacher courseTeacher1 = new CourseTeacher();
       BeanUtils.copyProperties(courseTeacher,courseTeacher1);
        int i = courseTeacherMapper.updateById(courseTeacher1);
        if(i!=1){
            XueChengPlusException.cast("failed to update");
        }
        CourseTeacher courseTeacher2 = courseTeacherMapper.selectById(courseTeacher.getId());
        return courseTeacher2;
    }

    @Override
    public void deleteTeacher(Long courseId, Long id,Long operateId) {
        if(courseId!=operateId){
            XueChengPlusException.cast("Please do not touch other agency teacher");
        }
        int i = courseTeacherMapper.deleteById(id);
        if(i!=1){
            XueChengPlusException.cast("deletion failed");
        }
    }

    private CourseTeacher getTeacher(Long courseId,String name){
        LambdaQueryWrapper<CourseTeacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper = queryWrapper.eq(CourseTeacher::getCourseId,courseId).eq(CourseTeacher::getTeacherName,name);
        CourseTeacher courseTeacher = courseTeacherMapper.selectOne(queryWrapper);
        if(courseTeacher==null){
            XueChengPlusException.cast("disMatch");
        }
        return courseTeacher;
    }
}
