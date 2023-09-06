package com.xuecheng.media;

import io.minio.*;
import io.minio.errors.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.io.IOUtil;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MinioTest {
    MinioClient minioClient = MinioClient.builder().endpoint("http://192.168.101.65:9000")
            .credentials("minioadmin","minioadmin")
            .build();
    @Test
    public void test_upload() throws Exception {
        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket("yycbucket").filename("")
                .object("11").build();
        minioClient.uploadObject(uploadObjectArgs);

    }
    @Test
    public void test_delete() throws Exception {
        UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder()
                .bucket("yycbucket").filename("")
                .object("11").build();
        RemoveObjectArgs yycbucket = RemoveObjectArgs.builder().bucket("yycbucket").object("").build();
        minioClient.removeObject(yycbucket);

    }
    public void test_query() throws Exception{
        GetObjectArgs yycbucket = GetObjectArgs.builder().bucket("yycbucket").object("test/01/11").build();
        FilterInputStream inputStream = minioClient.getObject(yycbucket);
        FileOutputStream fileOutputStream = new FileOutputStream(new File(""));
        IOUtils.copy(inputStream,fileOutputStream);

        String source_md5 = DigestUtils.md5Hex(inputStream);
        String local_md5 =  DigestUtils.md5Hex(new FileInputStream(new File("c:\\")));
        if(source_md5.equals(local_md5)){
            System.out.println("successful");
        }
    }
    public void uploadChunk() throws IOException, ServerException, InsufficientDataException, ErrorResponseException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        for (int i = 0; i < 6; i++) {
            UploadObjectArgs uploadObjectArgs = UploadObjectArgs.builder().bucket("testbucket").
                    filename("").object("chunk/"+i).build();
            minioClient.uploadObject(uploadObjectArgs);
            System.out.println("Load chunk"+i+"successfully");
        }
    }
    @Test
    public void testMerge() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        /*List<ComposeSource> sources = null;
        for (int i = 0; i < 30; i++) {

            ComposeSource composeSource = ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build();
            sources.add(composeSource);
        }*/
        List<ComposeSource> sources = Stream.iterate(0, i -> ++i).limit(6)
                .map(i -> ComposeSource.builder().bucket("testbucket").object("chunk/" + i).build())
                .collect(Collectors.toList());

        ComposeObjectArgs composeObjectArgs = ComposeObjectArgs.builder()
                .bucket("testbucket")
                .object("merge01.mp4")
                .sources(sources)
                .build();
        minioClient.composeObject(composeObjectArgs);
    }
}
