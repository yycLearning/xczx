package com.xuecheng.media.api;

import com.xuecheng.base.model.RestResponse;
import com.xuecheng.media.model.dto.UploadFileParamDTO;
import com.xuecheng.media.service.MediaFileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@RestController
@Api(value = "video files upload interface",tags = "11")
public class BigFilesController {

    @Autowired
    MediaFileService mediaFileService;

    @ApiOperation( value = "verify if upload file is exist or not")
    @PostMapping("/upload/checkfile")
    public RestResponse<Boolean> checkfile(@RequestParam("fileMd5") String fileMd5) throws Exception{
        return mediaFileService.checkFile(fileMd5);
    }


    @ApiOperation(value = "verify chunk files are exist or not")
    @PostMapping("/upload/checkchunk")
    public RestResponse<Boolean> checkchunk(@RequestParam("fileMd5") String fileMd5, @RequestParam("chunk") int chunk) throws Exception{
        return mediaFileService.checkChunk(fileMd5, chunk);
    }

    @ApiOperation(value = "upload chunk files")
    @PostMapping("/upload/uploadchunk")
    public RestResponse uploadchunk(@RequestParam("file") MultipartFile file,
                                    @RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("chunk")  int chunk) throws Exception{
        File tempFile = File.createTempFile("minio",".temp");
        file.transferTo(tempFile);
        String fileLocalPath = tempFile.getPath();
        return mediaFileService.uploadChunk(fileMd5,chunk,fileLocalPath);
    }

    @ApiOperation(value = "merge chunk files")
    @PostMapping("/upload/mergechunks")
    public RestResponse mergeChunks(@RequestParam("fileMd5") String fileMd5,
                                    @RequestParam("filename") String filename,
                                    @RequestParam("chunkTotal") int chunkTotal) throws Exception{
        Long companyId = 1232141425L;
        UploadFileParamDTO uploadFileParamDTO = new UploadFileParamDTO();
        uploadFileParamDTO.setFilename(filename);
        uploadFileParamDTO.setTags("video file");
        uploadFileParamDTO.setFileType("001002");
        RestResponse restResponse = mediaFileService.mergeChunks(companyId, fileMd5, chunkTotal, uploadFileParamDTO);
        return restResponse;
    }

}
