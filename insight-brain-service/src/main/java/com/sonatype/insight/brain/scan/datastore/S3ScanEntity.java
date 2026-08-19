/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.zip.GZIPOutputStream;

import com.sonatype.insight.brain.aws.s3.S3ExceptionUtil;
import com.sonatype.insight.brain.aws.s3.S3OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import static java.util.Objects.requireNonNull;

/**
 * Stores scan data as objects in S3 following the key pattern: {objectKeyPrefix}scan/{appId}/scan-{scanId}.xml.gz
 */
public record S3ScanEntity(
    S3Client s3Client,
    S3AsyncClient s3AsyncClient,
    String bucketName,
    String objectKey,
    String serverSideEncryption,
    String appId,
    String scanName)
    implements ScanEntity
{

  private static final Logger log = LoggerFactory.getLogger(S3ScanEntity.class);

  public S3ScanEntity {
    requireNonNull(s3Client, "s3Client cannot be null");
    requireNonNull(bucketName, "bucketName cannot be null");
    requireNonNull(objectKey, "objectKey cannot be null");
    requireNonNull(appId, "appId cannot be null");
    requireNonNull(scanName, "scanName cannot be null");
  }

  public static S3ScanEntity forScan(
      S3Client s3Client,
      S3AsyncClient s3AsyncClient,
      String bucketName,
      String objectKeyPrefix,
      String serverSideEncryption,
      String appId,
      String scanId)
  {
    String scanName = "scan-" + scanId + ".xml.gz";
    return forScanName(s3Client, s3AsyncClient, bucketName, objectKeyPrefix, serverSideEncryption, appId, scanName);
  }

  public static S3ScanEntity forTempScan(
      S3Client s3Client,
      S3AsyncClient s3AsyncClient,
      String bucketName,
      String objectKeyPrefix,
      String serverSideEncryption,
      String appId,
      String tempId)
  {
    String scanName = "temp-" + tempId + ".xml.gz";
    return forScanName(s3Client, s3AsyncClient, bucketName, objectKeyPrefix, serverSideEncryption, appId, scanName);
  }

  /**
   * Create S3ScanEntity with custom scan name.
   */
  public static S3ScanEntity forScanName(
      S3Client s3Client,
      S3AsyncClient s3AsyncClient,
      String bucketName,
      String objectKeyPrefix,
      String serverSideEncryption,
      String appId,
      String scanName)
  {
    String objectKey = objectKeyPrefix + "scan/" + appId + "/" + scanName;
    return new S3ScanEntity(s3Client, s3AsyncClient, bucketName, objectKey, serverSideEncryption, appId, scanName);
  }

  @Override
  public Writer getWriter() throws IOException {
    OutputStream outputStream = getOutputStream();
    return new OutputStreamWriter(
        new GZIPOutputStream(new BufferedOutputStream(outputStream, 32768)),
        StandardCharsets.UTF_8);
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return new S3OutputStream(s3AsyncClient, objectKey, bucketName, serverSideEncryption);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    GetObjectRequest request = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(objectKey)
        .build();

    return S3ExceptionUtil.wrapS3Exception(() -> s3Client.getObject(request));
  }

  @Override
  public String getLocation() {
    return "s3://" + bucketName + "/" + objectKey;
  }

  @Override
  public boolean exists() {
    try {
      HeadObjectRequest request = HeadObjectRequest.builder()
          .bucket(bucketName)
          .key(objectKey)
          .build();

      s3Client.headObject(request);
      return true;
    }
    catch (NoSuchKeyException e) {
      return false;
    }
    catch (Exception e) {
      log.warn("Error checking if S3 object exists: {}", getLocation(), e);
      return false;
    }
  }

  @Override
  public String getName() {
    return scanName;
  }

  @Override
  public long getLastModifiedTime() throws IOException {
    HeadObjectRequest request = HeadObjectRequest.builder()
        .bucket(bucketName)
        .key(objectKey)
        .build();

    Instant lastModified = S3ExceptionUtil.wrapS3Exception(() -> s3Client.headObject(request).lastModified());
    return lastModified.toEpochMilli();
  }

  @Override
  public boolean delete() {
    try {
      DeleteObjectRequest request = DeleteObjectRequest.builder()
          .bucket(bucketName)
          .key(objectKey)
          .build();

      s3Client.deleteObject(request);
      return true;
    }
    catch (Exception e) {
      log.warn("Error deleting S3 object: {}", getLocation(), e);
      return false;
    }
  }

  @Override
  public String getAppId() {
    return appId;
  }

  @Override
  public Class<? extends ScanPersistenceService> getScanPersistenceServiceClass() {
    return S3ScanPersistenceService.class;
  }

  @Override
  public String toString() {
    return getLocation();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof S3ScanEntity other) {
      return bucketName.equals(other.bucketName) && objectKey.equals(other.objectKey);
    }
    return false;
  }
}
