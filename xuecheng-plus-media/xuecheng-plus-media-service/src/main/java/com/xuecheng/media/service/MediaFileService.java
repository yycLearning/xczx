package com.xuecheng.media.service;

import com.xuecheng.base.model.PageParams;
import com.xuecheng.base.model.PageResult;
import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.QueryMediaParamsDto;
import com.xuecheng.media.model.dto.UploadFileParamDTO;
import com.xuecheng.media.model.dto.UploadFileResultDto;
import com.xuecheng.media.model.po.MediaFiles;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.File;
import java.util.List;

/**
 * @description 媒资文件管理业务类
 * @author Mr.M
 * @date 2022/9/10 8:55
 * @version 1.0
 */
public interface MediaFileService {

 /**
  * @description 媒资文件查询方法
  * @param pageParams 分页参数
  * @param queryMediaParamsDto 查询条件
  * @return com.xuecheng.base.model.PageResult<com.xuecheng.media.model.po.MediaFiles>
  * @author Mr.M
  * @date 2022/9/10 8:57
 */
 public PageResult<MediaFiles> queryMediaFiels(Long companyId,PageParams pageParams, QueryMediaParamsDto queryMediaParamsDto);

 public UploadFileResultDto uploadFile(Long companyId, UploadFileParamDTO uploadFileParamDTO, String localFilePath,String objectName);
 public MediaFiles addMediaFilesToDb(Long companyId,String bucket, UploadFileParamDTO uploadFileParamDTO, String fileMD5,String objectname);

 public RestResponse<Boolean> checkFile(String fileMd5);

 public RestResponse<Boolean> checkChunk(String fileMd5,int chunkIndex);
 public RestResponse uploadChunk(String fileMd5,int chunk,String localChunkFilePath);
 public RestResponse mergeChunks(Long companyId,String fileMd5,int chunkTotal,UploadFileParamDTO uploadFileParamDTO);

 public File downloadFileFromMinio(String bucket, String objectname);
 public  boolean addMediaFilesToMinio(String localFilePath,String mimeType,String bucket,String objectname);

  MediaFiles getFileById(String mediaId);


}
