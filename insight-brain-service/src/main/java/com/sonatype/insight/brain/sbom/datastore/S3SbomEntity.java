/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import jakarta.annotation.Nullable;

import com.sonatype.insight.brain.aws.s3.S3OutputStream;
import com.sonatype.insight.brain.report.S3ObjectKey;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import static com.sonatype.insight.brain.aws.s3.S3ExceptionUtil.wrapS3Exception;
import static java.util.Objects.requireNonNull;

public record S3SbomEntity(
    S3ObjectKey key,
    S3Client s3Client,
    S3AsyncClient s3AsyncClient,
    S3DataStoreConfig s3DataStoreConfig,
    @Nullable String appId,
    String fileName
)
    implements SbomEntity
{
  public S3SbomEntity {
    requireNonNull(key);
    requireNonNull(s3Client);
    requireNonNull(s3DataStoreConfig);
    requireNonNull(fileName);
  }

  @Override
  public InputStream getInputStream() throws IOException {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder()
        .bucket(s3DataStoreConfig.getBucketName())
        .key(key.toString())
        .build();
    return wrapS3Exception(() -> s3Client.getObject(getObjectRequest));
  }

  @Override
  public OutputStream getOutputStream() throws IOException {
    return new S3OutputStream(
        s3AsyncClient,
        key.toString(),
        s3DataStoreConfig.getBucketName(),
        s3DataStoreConfig.getServerSideEncryption()
    );
  }

  @Override
  public Path getPath() {
    try {
      Path tempFile = Files.createTempFile("s3-sbom-", "-" + fileName);
      tempFile.toFile().deleteOnExit();

      try (InputStream inputStream = getInputStream();
           OutputStream outputStream = Files.newOutputStream(tempFile)) {
        inputStream.transferTo(outputStream);
      }

      return tempFile;
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to create temporary copy of S3 SBOM: " + key, e);
    }
  }

  @Nullable
  @Override
  public String getAppId() {
    return appId;
  }

  @Override
  public String getName() {
    return fileName;
  }

  @Override
  public String getLocation() {
    return "s3://%s/%s".formatted(s3DataStoreConfig.getBucketName(), key.toString());
  }

  @Override
  public boolean exists() {
    try {
      HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
          .bucket(s3DataStoreConfig.getBucketName())
          .key(key.toString())
          .build();
      wrapS3Exception(() -> s3Client.headObject(headObjectRequest));
      return true;
    }
    catch (IOException e) {
      if (e.getCause() instanceof NoSuchKeyException) {
        return false;
      }
      throw new RuntimeException("Failed to check existence of S3 SBOM: " + key, e);
    }
  }

  @Override
  public Class<? extends SbomPersistenceService> getSbomPersistenceServiceClass() {
    return S3SbomPersistenceService.class;
  }

  @Override
  public String toString() {
    return getLocation();
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof S3SbomEntity other) {
      return key.equals(other.key);
    }
    return false;
  }
}
