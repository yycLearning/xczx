package com.xuecheng.media.service.jobhandler;

import com.xuecheng.base.utils.Mp4VideoUtil;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileProcessService;
import com.xuecheng.media.service.MediaFileService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * XxlJob开发示例（Bean模式）
 * <p>
 * 开发步骤：
 * 1、任务开发：在Spring Bean实例中，开发Job方法；
 * 2、注解配置：为Job方法添加注解 "@XxlJob(value="自定义jobhandler名称", init = "JobHandler初始化方法", destroy = "JobHandler销毁方法")"，注解value值对应的是调度中心新建任务的JobHandler属性的值。
 * 3、执行日志：需要通过 "XxlJobHelper.log" 打印执行日志；
 * 4、任务结果：默认任务结果为 "成功" 状态，不需要主动设置；如有诉求，比如设置任务结果为失败，可以通过 "XxlJobHelper.handleFail/handleSuccess" 自主设置任务结果；
 *
 * @author xuxueli 2019-12-11 21:52:51
 */
@Component
@Slf4j
public class VideoTask {

    @Autowired
    MediaFileProcessService mediaFileProcessService;
    @Autowired
    MediaFileService mediaFileService;

    @Value("${videoprocess.ffmpegpath}")
    private String ffmpegpath;


    @XxlJob("videoJobHandler")
    public void shardingJobHandler() throws Exception {

        // Fragmentation parameters
        int shardIndex = XxlJobHelper.getShardIndex();
        int shardTotal = XxlJobHelper.getShardTotal();

        //determine the number of cores of CPU
        int processors = Runtime.getRuntime().availableProcessors();

        List<MediaProcess> mediaProcessList = mediaFileProcessService.getMediaProcessList(shardIndex, shardTotal, processors);

        int size = mediaProcessList.size();
        log.debug("number of task :"+size);
        if(size<=0){
            return;
        }
        ExecutorService executorService = Executors.newFixedThreadPool(size);
        //Since the method will automatically end after starting the thread
        // , and the business needs to wait for all threads to complete before continuing, the method needs to be delayed
        CountDownLatch countDownLatch = new CountDownLatch(size);
        mediaProcessList.forEach(mediaProcess -> {
            executorService.execute(() -> {
                try {
                    // execute task logic
                    Long processId = mediaProcess.getId();
                    String fileId = mediaProcess.getFileId();
                    String bucket = mediaProcess.getBucket();
                    String objectName = mediaProcess.getFilePath();
                    File targetFile = mediaFileService.downloadFileFromMinio(bucket, objectName);
                    boolean b = mediaFileProcessService.startTask(processId);
                    if (!b) {
                        log.debug("preempt task failed,task id:{}", processId);

                        return;
                    }

                    if (targetFile == null) {
                        log.debug("error loading originVideo,taskId:{},bucket:{},objectName:{}", processId, bucket, objectName);
                        mediaFileProcessService.saveProcessFinishStatus(processId, "3", fileId, null, "loading file to local failed");

                        return;
                    }

                    //path of origin avi video
                    String video_path = targetFile.getAbsolutePath();

                    //name of file after conversion
                    String mp4_name = fileId + ".mp4";

                    File tempMp4File = null;
                    try {
                        tempMp4File = File.createTempFile("minio", ".mp4");
                    } catch (IOException e) {
                        log.error("error creating temporary file,{}", e.getMessage());
                        mediaFileProcessService.saveProcessFinishStatus(processId, "3", fileId, null, e.getMessage());
                        return;
                    }
                    String mp4_path = tempMp4File.getAbsolutePath();
                    //creating object of utilClass
                    Mp4VideoUtil videoUtil = new Mp4VideoUtil(ffmpegpath, video_path, mp4_name, mp4_path);
                    //launch video conversion, return "success" if passed
                    String result = videoUtil.generateMp4();
                    if (!result.equals("success")) {

                        log.error("video transcoding failed,info:{},bucket:{},objectname:{}", result, bucket, objectName);
                        mediaFileProcessService.saveProcessFinishStatus(processId, "3", fileId, null, result);
                        return;

                    }
                    boolean uploadingResult = mediaFileService.addMediaFilesToMinio(tempMp4File.getAbsolutePath(), "video/mp4", bucket, objectName);
                    if (!uploadingResult) {
                        log.debug("uploading to minio failed,taskid:{}", processId);
                        mediaFileProcessService.saveProcessFinishStatus(processId, "3", fileId, null, "uploading to minio failed");
                        return;
                    }
                    String url = getComposePath(fileId, ".mp4");
                    mediaFileProcessService.saveProcessFinishStatus(processId, "2", fileId, url, null);
                }finally {

                    countDownLatch.countDown();
                }
                //counter minus 1
            });


        });
        //blocking,and set maximum awaiting time
        countDownLatch.await(30,TimeUnit.MINUTES);

    }
    private String getComposePath(String fileMd5,String fileExt){
        return fileMd5.substring(0,1)+"/"+ fileMd5.substring(1,2)+"/"+ fileMd5 +"/"+fileMd5+fileExt;
    }

}






