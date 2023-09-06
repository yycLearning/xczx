package com.xuecheng.media;

import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SpringBootTest
public class BigFileTest {
    @Autowired
    MinioClient minioClient;
    @Test
    public void testChunk() throws IOException {
        File sourceFile = new File("");
        String chunkFilePath = "";
        int chunkSize = 1024*1024*5;
        int chunkNum = (int) Math.ceil(sourceFile.length()*1.0/chunkSize);
        RandomAccessFile raf_r = new RandomAccessFile(sourceFile, "r");
        byte[] bytes = new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
            File chunkFile = new File(chunkFilePath + i);
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while((len=raf_r.read(bytes))!=-1){
                raf_rw.write(bytes,0,len);
                if(chunkFile.length()>=chunkSize){
                    break;
                }
            }
            raf_rw.close();
        }
        raf_r.close();
    }
    @Test
    public void testMerge() throws IOException {
        File chunkFolder = new File("");
        File sourceFile = new File("");
        File mergeFile = new File("");
        File[] files = chunkFolder.listFiles();
        List<File> filesList = Arrays.asList(files);
        Collections.sort(filesList,(o1,o2)->Integer.parseInt(o1.getName())-Integer.parseInt(o2.getName()));
        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        byte[] bytes = new byte[1024];
        for (File file : filesList) {
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len = -1;
            while((len=raf_r.read(bytes))!=-1){
                raf_rw.write(bytes,0,len);
            }
            raf_r.close();
        }
        raf_rw.close();
        FileInputStream fileInputStream_merge = new FileInputStream(mergeFile);
        FileInputStream fileInputStream_origin = new FileInputStream(sourceFile);
        String md5Hex_merge = DigestUtils.md5Hex(fileInputStream_merge);
        String md5Hex_origin = DigestUtils.md5Hex(fileInputStream_origin);
        if(md5Hex_origin.equals(md5Hex_merge)){
            System.out.println("success to merge files");
        }

    }


    public void uploadChunk() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        for (int i = 0; i < 30; i++) {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder().bucket("testbucket").
                    filename("").object("chunk/"+i).build();
            minioClient.uploadObject(uploadObjectArgs);
            System.out.println("Load chunk"+i+"successfully");
        }
    }
    @Test
    public void testMerge2(){

    }
}
