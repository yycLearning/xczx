package com.xuecheng.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.mapper.MediaFilesMapper;
import com.xuecheng.media.mapper.MediaProcessMapper;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamDTO;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import com.xuecheng.media.model.po.MediaProcess;
import com.xuecheng.media.service.MediaFileService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.*;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mr.M
 * @version 1.0
 * @description TODO
 * @date 2022/9/10 8:58
 */
@Service
@Slf4j
public class MediaFileServiceImpl implements MediaFileService {

    @Autowired
    MediaFilesMapper mediaFilesMapper;
    @Autowired
    MinioClient minioClient;
    @Autowired
    MediaFileService currentProxy;
    @Autowired
    MediaProcessMapper mediaProcessMapper;
    @Value("${minio.bucket.files}")
    private String bucket_mediafiles;
    @Value("${minio.bucket.videofiles}")
    private String bucket_video ;

    @Override
    public PageResult<MediaFiles> queryMediaFiels(Long companyId, PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto) {

        //构建查询条件对象
        LambdaQueryWrapper<MediaFiles> queryWrapper = new LambdaQueryWrapper<>();

        //分页对象
        Page<MediaFiles> page = new Page<>(pageParams.getPageNo(), pageParams.getPageSize());
        // 查询数据内容获得结果
        Page<MediaFiles> pageResult = mediaFilesMapper.selectPage(page, queryWrapper);
        // 获取数据列表
        List<MediaFiles> list = pageResult.getRecords();
        // 获取数据总数
        long total = pageResult.getTotal();
        // 构建结果集
        PageResult<MediaFiles> mediaListResult = new PageResult<>(list, total, pageParams.getPageNo(), pageParams.getPageSize());
        return mediaListResult;

    }

    @Override
    public UploadFileResultDto uploadFile(Long companyId, UploadFileParamDTO uploadFileParamDTO, String localFilePath,String objectname) {

        String filename = uploadFileParamDTO.getFilename();
        String extensionString = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(extensionString);

        String defaultFolderPath = getDefaultFolderPath();
        String fileMD5 = getFileMD5(new File(localFilePath));
        if(StringUtils.isEmpty(objectname)) {
            objectname = defaultFolderPath + fileMD5 + extensionString;
        }


        boolean result = addMediaFilesToMinio(localFilePath, mimeType, bucket_mediafiles, objectname);

        if(!result){
            XueChengPlusException.cast("failed to upload file");
        }
        MediaFiles mediaFiles = currentProxy.addMediaFilesToDb(companyId, bucket_mediafiles, uploadFileParamDTO, fileMD5, objectname);
        if(mediaFiles==null){
            XueChengPlusException.cast("file upload failure");
        }

        UploadFileResultDto resultDTO = new UploadFileResultDto();
        BeanUtils.copyProperties(mediaFiles,resultDTO);

        return resultDTO;
    }
    @Transactional
    public MediaFiles addMediaFilesToDb(Long companyId,String bucket, UploadFileParamDTO uploadFileParamDTO, String fileMD5,String objectname) {
        MediaFiles id = mediaFilesMapper.selectById(fileMD5);
        if(id==null){
            id = new MediaFiles();
            BeanUtils.copyProperties(uploadFileParamDTO,id);
            id.setId(fileMD5);
            id.setCompanyId(companyId);
            id.setBucket(bucket);
            id.setFilePath(objectname);
            id.setFileId(fileMD5);
            id.setUrl("/"+bucket+"/"+objectname);
            id.setCreateDate(LocalDateTime.now());
            id.setStatus("1");
            id.setAuditStatus("002003");
            int result = mediaFilesMapper.insert(id);
            if(result!=1){
                log.debug("upload file failure,objectname:{},bucket:{}",objectname,bucket);
                return null;
            }

            addWaitingTask(id);
            return id;
        }
        return id;
    }
    private void addWaitingTask(MediaFiles id){
        String filename = id.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));
        String mimeType = getMimeType(extension);
        if(mimeType.equals("video/x-msvideo")){
            MediaProcess mediaProcess = new MediaProcess();
            BeanUtils.copyProperties(id,mediaProcess,"url");
            mediaProcess.setStatus("1");
            mediaProcess.setCreateDate(LocalDateTime.now());
            mediaProcess.setFailCount(0);
            mediaProcess.setUrl(null);

            mediaProcessMapper.insert(mediaProcess);
        }
    }

    @Override
    public RestResponse<Boolean> checkFile(String fileMd5) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(fileMd5);
        if(mediaFiles!=null){
            String bucket = mediaFiles.getBucket();
            String filePath = mediaFiles.getFilePath();
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(filePath)
                    .build();
            try {
                FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
                if(inputStream!=null) {
                    return RestResponse.success(true);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        }

        return RestResponse.success(false);

    }

    @Override
    public RestResponse<Boolean> checkChunk(String fileMd5, int chunkIndex) {
         String chunkPath  =  getChunkPath(fileMd5);


        GetObjectArgs getObjectArgs = GetObjectArgs.builder().bucket(bucket_video).object(chunkPath+chunkIndex).build();

        try {
            FilterInputStream inputStream = minioClient.getObject(getObjectArgs);
            if(inputStream!=null){
                return RestResponse.success(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return RestResponse.success(false);
    }

    @Override
    public RestResponse uploadChunk(String fileMd5, int chunk, String localChunkFilePath) {
        String mimeType = getMimeType(null);
        String chunkFilePath = getChunkPath(fileMd5)+chunk;
        boolean b = addMediaFilesToMinio(localChunkFilePath, mimeType, bucket_video, chunkFilePath);
        if(!b){
            return RestResponse.validfail(false,"chunk uploading failure");
        }
        return  RestResponse.success(true,"chunk uploading success");

    }

    @Override
    public RestResponse mergeChunks(Long companyId, String fileMd5, int chunkTotal, UploadFileParamDTO uploadFileParamDTO) {
        String chunkPath = getChunkPath(fileMd5);
        String filename = uploadFileParamDTO.getFilename();
        String extension = filename.substring(filename.lastIndexOf("."));
        String composePath = getComposePath(fileMd5, extension);
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(chunkTotal)
                .map(i -> ComposeSource.builder().bucket(bucket_video).object(chunkPath + i).build())
                .collect(Collectors.toList());

        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket(bucket_video)
                .object(composePath)
                .sources(sources)
                .build();
        try {
            minioClient.composeObject(composeObjectArgs);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error merging files,bucket:{},object:{},ERROR MESSAGE:{}",bucket_video,composePath,e.getMessage());
            return RestResponse.validfail(false,"Error merging files");
        }
        File mergedFile = downloadFileFromMinio(bucket_video,composePath);
        try(FileInputStream mergedStream = new FileInputStream(mergedFile)){
            String mergeFile_md5 = DigestUtils.md5Hex(mergedStream);
            if(!fileMd5.equals(mergeFile_md5)){
                log.error("mergedFile dismatch with originFile,originFile:{},mergedFile:{} ",fileMd5, mergeFile_md5);
                return RestResponse.validfail(false,"file verifying failure");
            }
            uploadFileParamDTO.setFileSize(mergedFile.length());
        }catch (Exception e){
            return RestResponse.validfail(false,"file verifying failure");
        }
        MediaFiles mediaFiles =currentProxy.addMediaFilesToDb(companyId, bucket_video, uploadFileParamDTO, fileMd5, composePath);
        if(mediaFiles==null){

            return RestResponse.validfail(false,"File Storage failed");
        }
        clearChunkFiles(chunkPath,chunkTotal);

        return RestResponse.success(true);
    }

    private String getChunkPath(String fileMd5) {
        return  fileMd5.substring(0,1)+"/"+ fileMd5.substring(1,2)+"/"+ fileMd5 +"/"+"chunk"+"/";
    }
    private String getComposePath(String fileMd5,String fileExt){
        return fileMd5.substring(0,1)+"/"+ fileMd5.substring(1,2)+"/"+ fileMd5 +"/"+fileMd5+fileExt;
    }

    public  boolean addMediaFilesToMinio(String localFilePath,String mimeType,String bucket,String objectname) {
        try {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder().bucket(bucket).filename(localFilePath).object(objectname).contentType(mimeType).build();
            minioClient.uploadObject(uploadObjectArgs);
            log.debug("success to upload,bucket{},objectname{}",bucket,objectname);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("error upload file,bucket{},objectname{},error message:{}",bucket,objectname,e.getMessage());
        }
        return false;
    }

    @Override
    public MediaFiles getFileById(String mediaId) {
        MediaFiles mediaFiles = mediaFilesMapper.selectById(mediaId);
        return mediaFiles;
    }

    private  String getMimeType(String extension) {
        if(extension==null){
            extension="";
        }
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(extension);
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if(extensionMatch!=null){
            mimeType = extensionMatch.getMimeType();
        }
        return mimeType;
    }
    private String getDefaultFolderPath(){
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String folder= sdf.format(new Date()).replace("-","/")+"/";
        return folder;
    }
    private String getFileMD5(File file){
       try (FileInputStream fileInputStream = new FileInputStream(file)){
           String fileMd5 = DigestUtils.md5Hex(fileInputStream);
           return fileMd5;
       } catch (Exception e){
           e.printStackTrace();
           return null;
       }
    }
    public File downloadFileFromMinio(String bucket,String objectname){
        File minioFile = null;
        FileOutputStream fileOutputStream = null;
        try {
            InputStream stream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectname).build());
            minioFile=File.createTempFile("minio",".merge");
            fileOutputStream = new FileOutputStream(minioFile);
            IOUtils.copy(stream,fileOutputStream);
        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            if(fileOutputStream!=null){
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
    private void clearChunkFiles(String chunkFileFolderPath,int chunkTotal){
        Iterable<DeleteObject> objects = Stream.iterate(0,i->++i).limit(chunkTotal).map(i->new DeleteObject(chunkFileFolderPath+i)).collect(Collectors.toList());
        RemoveObjectsArgs removeObjectsArgs = RemoveObjectsArgs.builder().bucket(bucket_video).objects(objects).build();

        Iterable<Result<DeleteError>> results = minioClient.removeObjects(removeObjectsArgs);
        results.forEach(f->{
            try {
                DeleteError deleteError =f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }


}
