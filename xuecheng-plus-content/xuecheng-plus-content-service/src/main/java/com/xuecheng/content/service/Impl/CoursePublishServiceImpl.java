package com.xuecheng.content.service.Impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.config.MultipartSupportConfig;
import com.xuecheng.content.feignclient.MediaServiceClient;
import com.xuecheng.content.mapper.CourseBaseMapper;
import com.xuecheng.content.mapper.CourseMarketMapper;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.mapper.CoursePublishPreMapper;
import com.xuecheng.content.model.dto.CourseBaseInfoDto;
import com.xuecheng.content.model.dto.CoursePreviewDTO;
import com.xuecheng.content.model.dto.TeachPlanDTO;
import com.xuecheng.content.model.po.CourseBase;
import com.xuecheng.content.model.po.CourseMarket;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.CoursePublishPre;
import com.xuecheng.content.service.CourseBaseInfoService;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.content.service.TeachplanService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.jws.Oneway;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
public class CoursePublishServiceImpl implements CoursePublishService {

    @Autowired
    CourseBaseInfoService courseBaseInfoService;
    @Autowired
    TeachplanService teachplanService;
    @Autowired
    CourseMarketMapper courseMarketMapper;
    @Autowired
    CoursePublishPreMapper coursePublishPreMapper;
    @Autowired
    CourseBaseMapper courseBaseMapper;
    @Autowired
    CoursePublishMapper coursePublishMapper;
    @Autowired
    MqMessageService mqMessageService;
    @Autowired
    MediaServiceClient mediaServiceClient;


    @Override
    public CoursePreviewDTO getCoursePreviewInfo(Long courseId) {
        CoursePreviewDTO coursePreviewDTO = new CoursePreviewDTO();
        CourseBaseInfoDto courseBaseInfo = courseBaseInfoService.getCourseBaseInfoByID(courseId);
        coursePreviewDTO.setCourseBase(courseBaseInfo);
        List<TeachPlanDTO> teachplanTree = teachplanService.findTeachplanTree(courseId);
        coursePreviewDTO.setTeachPlans(teachplanTree);


        return coursePreviewDTO;
    }

    @Override
    @Transactional
    public void commitAudit(Long company, Long courseId) {
        CourseBaseInfoDto courseBaseInfoByID = courseBaseInfoService.getCourseBaseInfoByID(courseId);
        Long companyId = courseBaseInfoByID.getCompanyId();
        if(courseBaseInfoByID==null){
            XueChengPlusException.cast("course no exists");
        }
        String status = courseBaseInfoByID.getAuditStatus();
        if(status.equals("202003")){
            XueChengPlusException.cast("auditing is processing ");
        }
        String pic = courseBaseInfoByID.getPic();
        if(StringUtils.isEmpty(pic)){
            XueChengPlusException.cast("images can not be empty");
        }
        List<TeachPlanDTO> teachplanTree = teachplanService.findTeachplanTree(courseId);
        if(teachplanTree==null||teachplanTree.size()==0){
            XueChengPlusException.cast("teachPlan can not be blank");
        }
        if(!company.equals(companyId)){
            XueChengPlusException.cast("institution info dismatch");
        }


        CoursePublishPre coursePublishPre = new CoursePublishPre();
        BeanUtils.copyProperties(courseBaseInfoByID,coursePublishPre);
        coursePublishPre.setCompanyId(company);
        CourseMarket courseMarket = courseMarketMapper.selectById(courseId);
        String courseMarketJSON = JSON.toJSONString(courseMarket);
        coursePublishPre.setMarket(courseMarketJSON);
        String teachPlanString = JSON.toJSONString(teachplanTree);
        coursePublishPre.setTeachplan(teachPlanString);
        coursePublishPre.setStatus("202003");
        coursePublishPre.setCreateDate(LocalDateTime.now());

        CoursePublishPre coursePublishPre1 = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre1==null){
            coursePublishPreMapper.insert(coursePublishPre);
        }else{
            coursePublishPreMapper.updateById(coursePublishPre);
        }
        CourseBase courseBase = courseBaseMapper.selectById(courseId);
        courseBase.setStatus("202003");
        courseBaseMapper.updateById(courseBase);

    }

    @Override
    @Transactional
    public void coursePublish(Long companyId, Long courseId) {

        CoursePublishPre coursePublishPre = coursePublishPreMapper.selectById(courseId);
        if(coursePublishPre==null){
            XueChengPlusException.cast("Please take audit process first");
        }
        String status = coursePublishPre.getStatus();
        if(!status.equals("202004")){
            XueChengPlusException.cast("course must pass the audition before publishing");
        }
        CoursePublish coursePublish = new CoursePublish();
        BeanUtils.copyProperties(coursePublishPre,coursePublish);
        CoursePublish coursePublish1 = coursePublishMapper.selectById(courseId);
        if(coursePublish1!=null){
            coursePublishMapper.updateById(coursePublish);
        }else {
            coursePublishMapper.insert(coursePublish);
        }
        saveCoursepublishMessage(courseId);

        coursePublishPreMapper.deleteById(courseId);
    }

    @Override
    public File generateCourseHtml(Long courseId) {
        Configuration configuration = new Configuration(Configuration.getVersion());
        File Htmlfile = null;
        try{
            String classpath = this.getClass().getResource("/").getPath();
            configuration.setDirectoryForTemplateLoading(new File(classpath+"/templates/"));
            configuration.setDefaultEncoding("utf-8");
            Template template = configuration.getTemplate("course_template.ftl");
            CoursePreviewDTO coursePreviewDTO= this.getCoursePreviewInfo(courseId);
            HashMap<String, Object> map = new HashMap<>();
            map.put("model",coursePreviewDTO);
            String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
            InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
            Htmlfile = File.createTempFile("coursepublish",".html");
            FileOutputStream fileOutputStream = new FileOutputStream(Htmlfile);
            IOUtils.copy(inputStream,fileOutputStream);
        }catch (Exception ex){
            log.error("html staticization failed,courseId:{}",courseId,ex);
            ex.printStackTrace();
        }

        return Htmlfile;
    }

    @Override
    public void uploadCourseHtml(Long courseId, File file) {
        try{

            MultipartFile multipartFile = MultipartSupportConfig.getMultipartFile(file);

            String upload = mediaServiceClient.upload(multipartFile,"course/"+courseId+".html");
            if(upload==null){
                log.debug("Result of fallback by remote is null,CourseId{}",courseId);
                XueChengPlusException.cast("Error uploading file");
            }
        }catch (Exception ex){
            ex.printStackTrace();
            XueChengPlusException.cast("Error uploading file");
        }
    }

    private void saveCoursepublishMessage(Long courseId) {
        MqMessage coursePublish2 = mqMessageService.addMessage("course_publish", String.valueOf(courseId), null, null);
        if(coursePublish2==null){
            XueChengPlusException.cast("System Error");
        }
    }
}
