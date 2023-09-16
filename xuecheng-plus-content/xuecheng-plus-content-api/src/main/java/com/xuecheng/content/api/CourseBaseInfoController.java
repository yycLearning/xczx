package com.xuecheng.content.api;

import com.xuecheng.base.exception.ValidationGroup;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDTO;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.util.SecurityUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2023/2/11 15:44
 */
@Api(value = "课程信息管理接口",tags = "课程信息管理接口")
@RestController
public class CourseBaseInfoController {
    @Autowired
    private CourseBaseInfoService courseBaseInfoService;

    @ApiOperation("课程查询接口")
    @PostMapping("/course/list")
    @PreAuthorize("hasAuthority('xc_teachmanager_course_list')")
    public PageResult<CourseBase> list(PageParams pageParams, @RequestBody(required=false) QueryCourseParamsDto queryCourseParamsDto) {


        String companyId1 = SecurityUtil.getUser().getCompanyId();
        long companyId = Long.parseLong(companyId1);
        return courseBaseInfoService.queryCourseBaseList(companyId,pageParams,queryCourseParamsDto);

    }
    @ApiOperation("Adding new course")
    @PostMapping("/course")
    public CourseBaseInfoDto createCourseBase(@RequestBody @Validated({ValidationGroup.Insert.class}) AddCourseDto dto){
        Long companyId = 1232141425L;
        CourseBaseInfoDto courseBase = courseBaseInfoService.createCourseBase(companyId, dto);
        return courseBase;
    }
    @ApiOperation("Query course by courseId")
    @GetMapping("/course/{courseid}")
    public CourseBaseInfoDto getCourseById(@PathVariable Long courseid){
        /*//get current client info:
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        System.out.println(principal);*/
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        CourseBaseInfoDto courseBaseInfoByID = courseBaseInfoService.getCourseBaseInfoByID(courseid);
        return courseBaseInfoByID;
    }

    @ApiOperation("update course")
    @PutMapping("/course")
    public CourseBaseInfoDto modifyCourseBase(@RequestBody @Validated({ValidationGroup.Insert.class}) EditCourseDTO editCourseDTO){
        SecurityUtil.XcUser user = SecurityUtil.getUser();
        String companyId = user.getCompanyId();
        long companyId2 = Long.parseLong(companyId);
        CourseBaseInfoDto courseBaseInfoDto = courseBaseInfoService.updateCourseBase(companyId2, editCourseDTO);
        return courseBaseInfoDto;

    }


}
