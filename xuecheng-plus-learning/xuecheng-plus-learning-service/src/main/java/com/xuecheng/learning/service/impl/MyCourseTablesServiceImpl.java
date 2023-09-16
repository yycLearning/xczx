package com.xuecheng.learning.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.mapper.XcChooseCourseMapper;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.MyCourseTableParams;
import com.xuecheng.learning.model.dto.XcChooseCourseDto;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.model.po.XcChooseCourse;
import com.xuecheng.learning.model.po.XcCourseTables;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class MyCourseTablesServiceImpl implements MyCourseTablesService {
    @Autowired
    XcChooseCourseMapper xcChooseCourseMapper;
    @Autowired
    XcCourseTablesMapper xcCourseTablesMapper;
    @Autowired
    ContentServiceClient contentServiceClient;
    @Override
    @Transactional
    public XcChooseCourseDto addChooseCourse(String userId, Long courseId) {
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if(coursepublish==null){
            XueChengPlusException.cast("Course no exists");
        }
        String charge = coursepublish.getCharge();
        // "201000" means its free course
        XcChooseCourse xcChooseCourse=null;
        if("201000".equals(charge)){
            xcChooseCourse = addFreeCourse(userId, coursepublish);
            XcCourseTables xcCourseTables = addCourseTables(xcChooseCourse);
        }else{
            xcChooseCourse = addChargeCourse(userId, coursepublish);
        }
        XcCourseTablesDto learningStatus = getLearningStatus(userId, courseId);
        XcChooseCourseDto chooseCourseDto = new XcChooseCourseDto();
        BeanUtils.copyProperties(xcChooseCourse,chooseCourseDto);
        chooseCourseDto.setLearnStatus(learningStatus.getLearnStatus());

        return chooseCourseDto;
    }

    @Override
    public XcCourseTablesDto getLearningStatus(String userId, Long courseId) {
        XcCourseTablesDto xcCourseTablesDto = new XcCourseTablesDto();
        XcCourseTables xcCourseTables = getXcCourseTables(userId, courseId);
        if(xcCourseTables==null){
            xcCourseTablesDto.setLearnStatus("702002");
            return xcCourseTablesDto;
        }
        boolean before = xcCourseTables.getValidtimeEnd().isBefore(LocalDateTime.now());
        if(before){

            xcCourseTablesDto.setLearnStatus("702003");
            BeanUtils.copyProperties(xcCourseTables,xcCourseTablesDto);
            return xcCourseTablesDto;
        }else {
            xcCourseTablesDto.setLearnStatus("702001");
            BeanUtils.copyProperties(xcCourseTables, xcCourseTablesDto);
            return xcCourseTablesDto;
        }
    }

    @Override
    public boolean saveChooseCourseSuccess(String chooseCourseId) {
        XcChooseCourse xcChooseCourse = xcChooseCourseMapper.selectById(chooseCourseId);
        if(xcChooseCourse==null){
            log.debug("Error occurred in ChooseCourseTable:{}",xcChooseCourse);
            return false;
        }
        String status = xcChooseCourse.getStatus();
        if(status.equals("701002")){
            xcChooseCourse.setStatus("701001");
            int i = xcChooseCourseMapper.updateById(xcChooseCourse);
            if(i<=0){
                log.debug("updating chooseCourseTable failed:{}",xcChooseCourse);
                XueChengPlusException.cast("update status failed");
            }
            XcCourseTables xcCourseTables = addCourseTables(xcChooseCourse);
            return true;
        }
        return false;
    }

    @Override
    public PageResult<XcCourseTables> myCourseTables(MyCourseTableParams params) {
        int page = params.getPage();
        int size = params.getSize();
        Page<XcCourseTables> xcCourseTablesPage = new Page<>(page,size);
        LambdaQueryWrapper<XcCourseTables> lambdaQueryWrapper = new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, params.getUserId());
        Page<XcCourseTables> xcCourseTablesPage1 = xcCourseTablesMapper.selectPage(xcCourseTablesPage, lambdaQueryWrapper);
        List<XcCourseTables> records = xcCourseTablesPage1.getRecords();
        long total = xcCourseTablesPage1.getTotal();
        PageResult<XcCourseTables> xcCourseTablesPageResult = new PageResult<>(records, total, page, size);
        return xcCourseTablesPageResult;
    }

    public XcChooseCourse addFreeCourse(String userID,CoursePublish coursePublish){
        Long courseId = coursePublish.getId();
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<XcChooseCourse>().eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getUserId, userID)
                .eq(XcChooseCourse::getOrderType, "700001")
                .eq(XcChooseCourse::getStatus, "701001");
        List<XcChooseCourse> Courselist = xcChooseCourseMapper.selectList(queryWrapper);
        if(Courselist.size()>0){
            return Courselist.get(0);
        }
        XcChooseCourse chooseCourse = new XcChooseCourse();
        chooseCourse.setCourseId(courseId);
        chooseCourse.setCourseName(coursePublish.getName());
        chooseCourse.setUserId(userID);
        chooseCourse.setCompanyId(coursePublish.getCompanyId());
        chooseCourse.setOrderType("700001");
        chooseCourse.setCreateDate(LocalDateTime.now());
        chooseCourse.setCoursePrice(coursePublish.getPrice());
        chooseCourse.setValidDays(365);
        chooseCourse.setStatus("701001");
        chooseCourse.setValidtimeStart(LocalDateTime.now());
        chooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        int result = xcChooseCourseMapper.insert(chooseCourse);
        if(result<0){
            XueChengPlusException.cast("adding course failed");
        }
        return chooseCourse;

    }
    public XcChooseCourse addChargeCourse(String userID,CoursePublish coursePublish){
        Long courseId = coursePublish.getId();
        LambdaQueryWrapper<XcChooseCourse> queryWrapper = new LambdaQueryWrapper<XcChooseCourse>().eq(XcChooseCourse::getCourseId, courseId)
                .eq(XcChooseCourse::getUserId, userID)
                .eq(XcChooseCourse::getOrderType, "700002")
                .eq(XcChooseCourse::getStatus, "701002");
        List<XcChooseCourse> Courselist = xcChooseCourseMapper.selectList(queryWrapper);
        if(Courselist.size()>0){
            return Courselist.get(0);
        }
        XcChooseCourse chooseCourse = new XcChooseCourse();
        chooseCourse.setCourseId(courseId);
        chooseCourse.setCourseName(coursePublish.getName());
        chooseCourse.setUserId(userID);
        chooseCourse.setCompanyId(coursePublish.getCompanyId());
        chooseCourse.setOrderType("700002");
        chooseCourse.setCreateDate(LocalDateTime.now());
        chooseCourse.setCoursePrice(coursePublish.getPrice());
        chooseCourse.setValidDays(365);
        chooseCourse.setStatus("701002");
        chooseCourse.setValidtimeStart(LocalDateTime.now());
        chooseCourse.setValidtimeEnd(LocalDateTime.now().plusDays(365));
        int result = xcChooseCourseMapper.insert(chooseCourse);
        if(result<0){
            XueChengPlusException.cast("adding course failed");
        }
        return chooseCourse;
    }
    public XcCourseTables addCourseTables(XcChooseCourse xcChooseCourse){
        String status = xcChooseCourse.getStatus();
        if(!status.equals("701001")){
            XueChengPlusException.cast("can not add to CourseTables");
        }
        XcCourseTables xcCourseTables = getXcCourseTables(xcChooseCourse.getUserId(), xcChooseCourse.getCourseId());
        if(xcCourseTables!=null){
            return xcCourseTables;
        }
        xcCourseTables = new XcCourseTables();
        BeanUtils.copyProperties(xcChooseCourse,xcCourseTables);
        xcCourseTables.setChooseCourseId(xcChooseCourse.getId());
        xcCourseTables.setCourseType(xcChooseCourse.getOrderType());
        xcCourseTables.setUpdateDate(LocalDateTime.now());
        int insert = xcCourseTablesMapper.insert(xcCourseTables);
        if(insert<=0){
            XueChengPlusException.cast("adding courseTables failed");
        }
        return xcCourseTables;

    }
    public XcCourseTables getXcCourseTables(String userId,Long courseId){
        XcCourseTables xcCourseTables = xcCourseTablesMapper.selectOne(new LambdaQueryWrapper<XcCourseTables>().eq(XcCourseTables::getUserId, userId)
                .eq(XcCourseTables::getCourseId, courseId));
        return xcCourseTables;

    }
}
