/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.OutputStream;

import com.sonatype.insight.brain.aws.s3.S3OutputStream;
import com.sonatype.insight.brain.service.config.ReportDataStoreConfig.S3DataStoreConfig;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import static java.util.Objects.requireNonNull;

public class AbstractS3ReportEntity
    implements ReportEntity
{
  private static final String BASE_FORMAT = "sonatype-work/report/%s/%s/";

  private static final String ADDITIONAL_KEY_FORMAT = BASE_FORMAT + "additional.files/%s";

  private static final String KEY_FORMAT = BASE_FORMAT + "report.files/%s";

  protected static final String CACHE_KEY_FORMAT_PREFIX = BASE_FORMAT + "report.cache";

  private static final String CACHE_KEY_FORMAT = CACHE_KEY_FORMAT_PREFIX + "/%s";

  protected final S3Client s3Client;

  protected final String bucketName;

  protected final String appId;

  protected final String scanId;

  protected final String name;

  protected final S3DataStoreConfig s3Config;

  protected final String keyPrefix;

  public AbstractS3ReportEntity(
      final S3Client s3Client,
      final S3DataStoreConfig s3Config,
      final String appId,
      final String scanId,
      final String name)
  {
    this.s3Client = requireNonNull(s3Client);
    this.s3Config = requireNonNull(s3Config);
    this.bucketName = requireNonNull(s3Config.getBucketName());
    this.keyPrefix = s3Config.getObjectKeyPrefix();
    this.appId = requireNonNull(appId);
    this.scanId = requireNonNull(scanId);
    this.name = requireNonNull(name);
  }

  @Override
  public ResponseInputStream<GetObjectResponse> getInputStream() {
    return getGetObjectResponseResponseInputStream(getKey());
  }

  protected ResponseInputStream<GetObjectResponse> getGetObjectResponseResponseInputStream(final S3ObjectKey key) {
    GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(key.toString()).build();
    return s3Client.getObject(getObjectRequest);
  }

  @Override
  public OutputStream getOutputStream() {
    return new S3OutputStream(s3Client, getKey().toString(), bucketName);
  }

  @Override
  public boolean exists() {
    return exists(getKey(name));
  }

  protected boolean exists(final S3ObjectKey key) {
    try {
      s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key.toString()).build());
      return true;
    }
    catch (NoSuchKeyException e) {
      return false;
    }
  }

  protected S3ObjectKey getKey() {
    return getKey(name);
  }

  protected S3ObjectKey getKey(final String objectName) {
    return new S3ObjectKey(KEY_FORMAT, appId, scanId, objectName, keyPrefix);
  }

  protected S3ObjectKey getAdditionalObjectKey(final String objectName) {
    return new S3ObjectKey(ADDITIONAL_KEY_FORMAT, appId, scanId, objectName, keyPrefix);
  }

  protected S3ObjectKey getCacheKey(final String objectName) {
    return new S3ObjectKey(CACHE_KEY_FORMAT, appId, scanId, objectName, keyPrefix);
  }
}
