package com.xuecheng.content.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseCategoryMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.model.dto.AddCourseDto;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.EditCourseDTO;
import com.xuecheng.content.model.dto.QueryCourseParamsDto;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseCategory;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.service.CourseBaseInfoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class CourseBaseInfoServiceImpl implements CourseBaseInfoService {
    @Autowired
    private CourseBaseMapper courseBaseMapper;
    @Autowired
    private CourseMarketMapper courseMarketMapper;
    @Autowired
    private CourseCategoryMapper courseCategoryMapper;


    @Override
    public PageResult<CourseBase> queryCourseBaseList(PageParams pageParams, QueryCourseParamsDto courseParamsDto) {

        LambdaQueryWrapper<CourseBase> queryWrapper = new LambdaQueryWrapper<>();
        //Fuzzy query based on name, splicing in sql:course_base.name like '%value%'
        queryWrapper.like(StringUtils.isNotEmpty(courseParamsDto.getCourseName()),CourseBase::getName,courseParamsDto.getCourseName());
        //Query based on auditStatus of course: course_base.audit_status=?
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getAuditStatus()),CourseBase::getAuditStatus,courseParamsDto.getAuditStatus());
        //Query based on coursePostingStatus
        queryWrapper.eq(StringUtils.isNotEmpty(courseParamsDto.getPublishStatus()),CourseBase::getStatus,courseParamsDto.getPublishStatus());
        //the object of pagination parameters


        //creating object of PAGE pagination parameters, parameters:currentPage, PageSize
        Page<CourseBase> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        Page<CourseBase> PageResult = courseBaseMapper.selectPage(page, queryWrapper);
        long total = PageResult.getTotal();
        List<CourseBase> records = PageResult.getRecords();
        PageResult<CourseBase> result = new PageResult<CourseBase>(records,total, pageParams.getPageNo(), pageParams.getPageSize());

        return result;
    }
    @Transactional
    @Override
    public CourseBaseInfoDto createCourseBase(Long companyID, AddCourseDto dto) {
        //Validity Check Of Parameters
        /*if (StringUtils.isBlank(dto.getName())) {
            throw new XueChengPlusException("课程名称为空");
        }

        if (StringUtils.isBlank(dto.getMt())) {
            throw new RuntimeException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getSt())) {
            throw new RuntimeException("课程分类为空");
        }

        if (StringUtils.isBlank(dto.getGrade())) {
            throw new RuntimeException("课程等级为空");
        }

        if (StringUtils.isBlank(dto.getTeachmode())) {
            throw new RuntimeException("教育模式为空");
        }

        if (StringUtils.isBlank(dto.getUsers())) {
            throw new RuntimeException("适应人群为空");
        }

        if (StringUtils.isBlank(dto.getCharge())) {
            throw new RuntimeException("收费规则为空");
        }*/


        CourseBase courseBaseNew = new CourseBase();
        BeanUtils.copyProperties(dto,courseBaseNew);
        courseBaseNew.setCompanyId(companyID);
        courseBaseNew.setCreateDate(LocalDateTime.now());
        courseBaseNew.setAuditStatus("202002");
        courseBaseNew.setStatus("203001");
        int insert = courseBaseMapper.insert(courseBaseNew);
        if(insert<=0){
            throw new RuntimeException("failed to add course");
        }
        CourseMarket courseMarket = new CourseMarket();
        BeanUtils.copyProperties(dto,courseMarket);
        courseMarket.setId(courseBaseNew.getId());
        int result = saveCourseMarket(courseMarket);
        CourseBaseInfoDto courseBaseInfo =getCourseBaseInfoByID(courseBaseNew.getId());
        return courseBaseInfo;
    }



    public CourseBaseInfoDto getCourseBaseInfoByID(Long courseID){
        CourseBaseInfoDto courseBaseInfoDto = new CourseBaseInfoDto();
        CourseBase courseBase = courseBaseMapper.selectById(courseID);
        if(courseBase==null){
            return null;
        }
        CourseMarket courseMarket = courseMarketMapper.selectById(courseID);
        if(courseMarket==null){
           return  null;
        }
        BeanUtils.copyProperties(courseBase,courseBaseInfoDto);
        BeanUtils.copyProperties(courseMarket,courseBaseInfoDto);

        CourseCategory courseCategory = courseCategoryMapper.selectById(courseBase.getMt());
        String MtName = courseCategory.getName();
        courseBaseInfoDto.setMt(MtName);

        CourseCategory courseCategory2 = courseCategoryMapper.selectById(courseBase.getSt());
        String stName = courseCategory2.getName();
        courseBaseInfoDto.setSt(stName);

        return courseBaseInfoDto;
    }

    @Override
    public CourseBaseInfoDto updateCourseBase(Long companyID, EditCourseDTO editCourseDTO) {
        Long courseId = editCourseDTO.getId();
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        if(courseBase==null){
            XueChengPlusException.cast("course not exist");
        }

        if(!companyID.equals(courseBase.getCompanyId())){
            XueChengPlusException.cast("just can modify yourself course");
        }
        BeanUtils.copyProperties(editCourseDTO,courseBase);
        courseBase.setChangeDate(LocalDateTime.now());

        int result = courseBaseMapper.updateById(courseBase);
        if(result<=0){
            XueChengPlusException.cast("failed to modify course");
        }

        CourseBaseInfoDto courseBaseInfoByID = getCourseBaseInfoByID(courseId);
        return courseBaseInfoByID;
    }

    private int saveCourseMarket(CourseMarket courseMarket){
        String charge = courseMarket.getCharge();
        if(StringUtils.isEmpty(charge)){
            throw new RuntimeException("priceType can not be empty");
        }
        if(charge.equals("201001")){
            if(courseMarket.getPrice()==null||courseMarket.getPrice().floatValue()<=0){
                //throw  new RuntimeException("Illegal Price");
                XueChengPlusException.cast("course price can not be empty when charge status is not free");
            }
        }
        Long id = courseMarket.getId();
        CourseMarket courseMarket1 = courseMarketMapper.selectById(id);
        if(courseMarket1==null){
            int result = courseMarketMapper.insert(courseMarket);
            return result;
        }else{
            BeanUtils.copyProperties(courseMarket,courseMarket1);
            courseMarket1.setId(courseMarket.getId());
            int result = courseMarketMapper.updateById(courseMarket1);
            return result;

        }
    }

}
