package com.xuecheng.learning.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcCourseTables;

public interface MyCourseTablesService {
    public XcChooseCourseDto addChooseCourse(String userId,Long courseId);
    public XcCourseTablesDto getLearningStatus(String userId,Long courseId);
    public boolean saveChooseCourseSuccess(String chooseCourseId);
    public PageResult<XcCourseTables> myCourseTables(MyCourseTableParams params);
}
