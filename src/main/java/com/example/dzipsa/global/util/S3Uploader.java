package com.example.dzipsa.global.util;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Component
public class S3Uploader {

  private final AmazonS3Client amazonS3Client;

  @Value("${cloud.aws.s3.bucket}")
  private String bucket;

  public String upload(MultipartFile multipartFile, String dirName) throws IOException {
    // 확장자 체크 로직 추가
    String contentType = multipartFile.getContentType();
    if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png") && !contentType.equals("image/webp"))) {
      throw new IllegalArgumentException("허용되지 않는 파일 형식입니다.");
    }

    // 파일 이름 생성
    String fileName = dirName + "/" + UUID.randomUUID() + "_" + multipartFile.getOriginalFilename();

    // 메타데이터 설정
    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(multipartFile.getSize());
    metadata.setContentType(multipartFile.getContentType());

    // S3에 업로드
    amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, multipartFile.getInputStream(), metadata));

    // URL 반환
    return amazonS3Client.getUrl(bucket, fileName).toString();
  }

  public void deleteFile(String fileUrl) {
    // 1. URL에서 S3 Key(폴더명 포함 전체 경로)를 안전하게 추출
    // 예: https://bucket.s3.amazonaws.com/todo/uuid_file.jpg -> todo/uuid_file.jpg
    String key = fileUrl.split(".com/")[1];

    try {
      amazonS3Client.deleteObject(bucket, key);
    } catch (Exception e) {
      log.error("S3 파일 삭제 실패: {}", e.getMessage());
      throw new RuntimeException("이미지 삭제 중 오류가 발생했습니다.");
    }
  }
}