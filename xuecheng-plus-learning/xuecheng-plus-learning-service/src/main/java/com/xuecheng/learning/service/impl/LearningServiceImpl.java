package com.xuecheng.learning.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.base.utils.StringUtil;
import com.xuecheng.content.model.po.CoursePublish;
import com.xuecheng.content.model.po.Teachplan;
import com.xuecheng.learning.feignclient.ContentServiceClient;
import com.xuecheng.learning.feignclient.MediaServiceClient;
import com.xuecheng.learning.mapper.XcCourseTablesMapper;
import com.xuecheng.learning.model.dto.XcCourseTablesDto;
import com.xuecheng.learning.service.LearningService;
import com.xuecheng.learning.service.MyCourseTablesService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class LearningServiceImpl implements LearningService {
    @Autowired
    MyCourseTablesService courseTablesService;
    @Autowired
    ContentServiceClient contentServiceClient;
    @Autowired
    MediaServiceClient mediaServiceClient;
    @Override
    public RestResponse<String> getvideo(String userId, Long courseId, Long teachplanId, String mediaID) {
        CoursePublish coursepublish = contentServiceClient.getCoursepublish(courseId);
        if(coursepublish==null){
            return RestResponse.validfail("Course No Exists");
        }
        String teachplan = coursepublish.getTeachplan();
        Map<String,String> map = JSON.parseObject(teachplan, Map.class);
        String isPreview = map.get("isPreview");
        if(isPreview.equals("1")){
            RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaID);
            return playUrlByMediaId;
        }

        if(StringUtils.isNotEmpty(userId)){
            XcCourseTablesDto learningStatus = courseTablesService.getLearningStatus(userId, courseId);
            String learnStatus = learningStatus.getLearnStatus();
            if(!learnStatus.equals("702001")){
                return RestResponse.validfail("Not choose course or complete payment yet");
            }else{
                RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaID);
                return playUrlByMediaId;
            }

        }
        String charge = coursepublish.getCharge();
        if ("201000".equals(charge)) {
            RestResponse<String> playUrlByMediaId = mediaServiceClient.getPlayUrlByMediaId(mediaID);
            return playUrlByMediaId;
        }

        return RestResponse.validfail("Please complete payment for course");
    }
}
