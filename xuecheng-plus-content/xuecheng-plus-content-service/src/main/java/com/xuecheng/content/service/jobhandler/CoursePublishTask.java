package com.xuecheng.content.service.jobhandler;

import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.content.feignclient.CourseIndex;
import com.xuecheng.content.feignclient.SearchServiceClient;
import com.xuecheng.content.mapper.CoursePublishMapper;
import com.xuecheng.content.model.dto.CoursePreviewDTO;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.service.CoursePublishService;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MessageProcessAbstract;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;

@Slf4j
@Component
public class CoursePublishTask extends MessageProcessAbstract {
    @Autowired
    CoursePublishService coursePublishService;
    @Autowired
    SearchServiceClient searchServiceClient;
    @Autowired
    CoursePublishMapper coursePublishMapper;

    @XxlJob("CoursePublishJobHandler")
    public void coursePublishJobHandler() throws Exception{
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        process(shardIndex,shardTotal,"course_publish",30,60);
    }
    @Override

    public boolean execute(MqMessage mqMessage) {
        String businessKey1 = mqMessage.getBusinessKey1();
        Long courseId = Long.parseLong(businessKey1);

        generateCourseHtml(mqMessage,courseId);
        saveCourseIndex(mqMessage,courseId);
        return false;
    }
    private void generateCourseHtml(MqMessage mqMessage,long courseId){
        Long id = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        int stageOne = mqMessageService.getStageOne(id);
        if(stageOne>0){
            log.debug("Course staticization completed");
            return;
        }
        File file= coursePublishService.generateCourseHtml(courseId);
        if(file==null){
            XueChengPlusException.cast("Generating Static html failed");
        }
        coursePublishService.uploadCourseHtml(courseId,file);

        mqMessageService.completedStageOne(id);

    }
    private void saveCourseIndex(MqMessage mqMessage,long courseId){
        Long id = mqMessage.getId();
        MqMessageService mqMessageService = this.getMqMessageService();
        int stageTwo = mqMessageService.getStageTwo(id);
        if(stageTwo>0){
            log.debug("Course index info written");
            return;
        }
        CoursePublish coursePublish = coursePublishMapper.selectById(courseId);
        CourseIndex courseIndex = new CourseIndex();
        BeanUtils.copyProperties(coursePublish,courseIndex);
        Boolean add = searchServiceClient.add(courseIndex);
        if(!add){
            XueChengPlusException.cast("remote call adding index failed");
        }
        mqMessageService.completedStageTwo(id);
    }
}
