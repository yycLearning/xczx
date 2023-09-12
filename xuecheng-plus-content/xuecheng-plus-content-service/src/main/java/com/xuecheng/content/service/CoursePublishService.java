package com.xuecheng.content.service;

import com.xuecheng.content.model.dto.CoursePreviewDTO;
import org.springframework.web.bind.annotation.PathVariable;

import java.io.File;

public interface CoursePublishService {
    public CoursePreviewDTO getCoursePreviewInfo(Long courseId);
    public void commitAudit(Long company,Long courseId);
    public void coursePublish(Long companyId,Long courseId);

    public File generateCourseHtml(Long courseId);
    public void uploadCourseHtml(Long courseId,File file);
}
