package com.xuecheng.content;

import com.xuecheng.content.model.dto.CoursePreviewDTO;
import com.xuecheng.content.service.CoursePublishService;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

@SpringBootTest
public class FreeMarkerTest {
    @Autowired
    CoursePublishService coursePublishService;
    @Test
    public void testGenerateHtmlByTempate() throws IOException, TemplateException {
        Configuration configuration = new Configuration(Configuration.getVersion());
        String classpath = this.getClass().getResource("/").getPath();
        configuration.setDirectoryForTemplateLoading(new File(classpath+"/templates/"));
        configuration.setDefaultEncoding("utf-8");
        Template template = configuration.getTemplate("course_template.ftl");
        CoursePreviewDTO coursePreviewDTO= coursePublishService.getCoursePreviewInfo(120L);
        HashMap<String, Object> map = new HashMap<>();
        map.put("model",coursePreviewDTO);
        String html = FreeMarkerTemplateUtils.processTemplateIntoString(template, map);
        InputStream inputStream = IOUtils.toInputStream(html, "utf-8");
        FileOutputStream fileOutputStream = new FileOutputStream(new File(""));
        IOUtils.copy(inputStream,fileOutputStream);
    }
}
