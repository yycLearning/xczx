package com.xuecheng.content.service;

import com.xuecheng.content.model.po.CourseTeacher;

public interface CourseTeacherService {
    public CourseTeacher selectTeacher(Long id);

    public CourseTeacher insertTeacher(Long courseId,CourseTeacher courseTeacher);
    public CourseTeacher updateTeacher(Long courseId,CourseTeacher courseTeacher);
    public void deleteTeacher(Long courseId,Long id,Long operatorID);
}
