/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.io.IOException;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Random;
import java.util.stream.Stream;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.aws.s3.S3Utils;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;

import static com.sonatype.insight.brain.aws.s3.S3ExceptionUtil.wrapS3Exception;
import static java.util.Objects.requireNonNull;

@Named
@Singleton
public class S3ScanPersistenceService
    extends ScanPersistenceService
{
  private static final Logger log = LoggerFactory.getLogger(S3ScanPersistenceService.class);

  private static final String SCAN_KEY_FORMAT = "scan/%s/";

  private final S3Client s3Client;

  private final S3AsyncClient s3AsyncClient;

  private final S3DataStoreConfig s3DataStoreConfig;

  @Inject
  public S3ScanPersistenceService(
      @Nullable final S3Client s3Client,
      @Nullable final S3AsyncClient s3AsyncClient,
      final InsightConfig config)
  {
    this.s3Client = s3Client;
    this.s3AsyncClient = s3AsyncClient;
    this.s3DataStoreConfig = config.getStorage().getS3Config();
    if (s3DataStoreConfig != null) {
      requireNonNull(s3Client);
      requireNonNull(s3DataStoreConfig.getBucketName());
      requireNonNull(s3DataStoreConfig.getObjectKeyPrefix());
    }
  }

  @Override
  protected ScanEntity doGetScan(String appId, String scanId) {
    return S3ScanEntity.forScan(
        s3Client,
        s3AsyncClient,
        s3DataStoreConfig.getBucketName(),
        s3DataStoreConfig.getObjectKeyPrefix(),
        s3DataStoreConfig.getServerSideEncryption(),
        appId,
        scanId
    );
  }

  @Override
  public ScanEntity createTempScan(String appId) {
    String tempId = generateTempId();
    return S3ScanEntity.forTempScan(
        s3Client,
        s3AsyncClient,
        s3DataStoreConfig.getBucketName(),
        s3DataStoreConfig.getObjectKeyPrefix(),
        s3DataStoreConfig.getServerSideEncryption(),
        appId,
        tempId
    );
  }

  @Override
  public void moveTempScan(ScanEntity tempScanEntity, String appId, String scanId) throws IOException {
    S3ScanEntity tempS3Entity = (S3ScanEntity) tempScanEntity;
    S3ScanEntity targetEntity = S3ScanEntity.forScan(
        s3Client,
        s3AsyncClient,
        s3DataStoreConfig.getBucketName(),
        s3DataStoreConfig.getObjectKeyPrefix(),
        s3DataStoreConfig.getServerSideEncryption(),
        appId,
        scanId
    );

    CopyObjectRequest copyRequest = CopyObjectRequest.builder()
        .sourceBucket(s3DataStoreConfig.getBucketName())
        .sourceKey(tempS3Entity.objectKey())
        .destinationBucket(s3DataStoreConfig.getBucketName())
        .destinationKey(targetEntity.objectKey())
        .serverSideEncryption(s3DataStoreConfig.getServerSideEncryption())
        .build();

    wrapS3Exception(() -> s3Client.copyObject(copyRequest));

    tempS3Entity.delete();
  }

  @Override
  public ScanEntity getScanByName(String appId, String name) {
    return S3ScanEntity.forScanName(
        s3Client,
        s3AsyncClient,
        s3DataStoreConfig.getBucketName(),
        s3DataStoreConfig.getObjectKeyPrefix(),
        s3DataStoreConfig.getServerSideEncryption(),
        appId,
        name
    );
  }

  @Override
  public void copyScanFile(ScanEntity source, ScanEntity destination) throws IOException {
    S3ScanEntity sourceS3Entity = (S3ScanEntity) source;
    S3ScanEntity destS3Entity = (S3ScanEntity) destination;

    CopyObjectRequest copyRequest = CopyObjectRequest.builder()
        .sourceBucket(s3DataStoreConfig.getBucketName())
        .sourceKey(sourceS3Entity.objectKey())
        .destinationBucket(s3DataStoreConfig.getBucketName())
        .destinationKey(destS3Entity.objectKey())
        .serverSideEncryption(s3DataStoreConfig.getServerSideEncryption())
        .build();

    wrapS3Exception(() -> s3Client.copyObject(copyRequest));
  }

  @Override
  public void deleteScansFor(String appId) throws IOException {
    String scanPrefix = s3DataStoreConfig.getObjectKeyPrefix() + String.format(SCAN_KEY_FORMAT, appId);
    S3Utils.deleteAllWithPrefix(s3Client, s3DataStoreConfig.getBucketName(), scanPrefix);
  }

  @Override
  public Stream<ScanEntity> allScanFilesFor(String appId) {
    String scanPrefix = s3DataStoreConfig.getObjectKeyPrefix() + String.format(SCAN_KEY_FORMAT, appId);

    try {
      return S3Utils.getS3Objects(s3Client, s3DataStoreConfig.getBucketName(), scanPrefix)
          .map(s3Object -> {
            // Extract scan name from the full key
            String fullKey = s3Object.key();
            String scanName = fullKey.substring(fullKey.lastIndexOf('/') + 1);
            return S3ScanEntity.forScanName(
                s3Client,
                s3AsyncClient,
                s3DataStoreConfig.getBucketName(),
                s3DataStoreConfig.getObjectKeyPrefix(),
                s3DataStoreConfig.getServerSideEncryption(),
                appId,
                scanName
            );
          });
    }
    catch (IOException e) {
      log.warn("Error listing scan files for application ID {}: {}", appId, e.getMessage(), e);
      return Stream.empty();
    }
  }

  @Override
  public void deleteScan(String appId, String scanId) throws IOException {
    ScanEntity scanEntity = doGetScan(appId, scanId);
    scanEntity.delete();
  }

  @Override
  public Class<? extends ScanEntity> getScanEntityClass() {
    return S3ScanEntity.class;
  }

  private String generateTempId() {
    DecimalFormat fmt = new DecimalFormat("#####");
    SecureRandom secureRandom = new SecureRandom();
    long secureInitializer = secureRandom.nextLong();
    Random rand = new Random(secureInitializer + Runtime.getRuntime().freeMemory());
    return fmt.format(Math.abs(rand.nextInt()));
  }
}
