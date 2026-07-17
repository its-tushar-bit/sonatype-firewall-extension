/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.aws.s3.S3OutputStream;
import com.sonatype.insight.brain.aws.s3.S3Utils;
import com.sonatype.insight.brain.report.S3ObjectKey;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

import static com.sonatype.insight.brain.aws.s3.S3ExceptionUtil.wrapS3Exception;
import static java.util.Objects.requireNonNull;

@Named
@Singleton
public class S3SbomPersistenceService
    extends SbomPersistenceService
{
  private static final Logger log = LoggerFactory.getLogger(S3SbomPersistenceService.class);

  private static final String PERMANENT_SBOM_FORMAT = "sboms/%s/%s%s";

  private static final String TEMPORARY_SBOM_FORMAT = "sboms/temp/persistent/%s%s%s";

  private static final String TRANSIENT_SBOM_FORMAT = "sboms/temp/transient/%s%s%s";

  private final S3Client s3Client;

  private final S3AsyncClient s3AsyncClient;

  private final S3DataStoreConfig s3DataStoreConfig;

  @Inject
  public S3SbomPersistenceService(
      @Nullable final S3Client s3Client,
      @Nullable final S3AsyncClient s3AsyncClient,
      final InsightConfig insightConfig)
  {
    this.s3Client = s3Client;
    this.s3AsyncClient = s3AsyncClient;
    this.s3DataStoreConfig = insightConfig.getStorage().getS3Config();
    if (s3DataStoreConfig != null) {
      requireNonNull(s3Client);
      requireNonNull(s3DataStoreConfig.getBucketName());
      requireNonNull(s3DataStoreConfig.getObjectKeyPrefix());
    }
  }

  @Override
  @WithSpan
  public SbomEntity doGetSbom(final String appId, final String fileName) {
    S3ObjectKey key = getPermanentSbomKey(appId, fileName);
    return new S3SbomEntity(key, s3Client, s3AsyncClient, s3DataStoreConfig, appId, fileName);
  }

  @Override
  @WithSpan
  public SbomEntity getTemporarySbom(final String fileName, final String prefix) {
    S3ObjectKey key = getTemporarySbomKey(fileName, prefix);
    return new S3SbomEntity(key, s3Client, s3AsyncClient, s3DataStoreConfig, null, fileName);
  }

  @Override
  @WithSpan
  public SbomEntity getTransientSbom(final String fileName) {
    String uniqueFileName = generateTempFileName(fileName);
    S3ObjectKey key = getTransientSbomKey(uniqueFileName);

    log.debug("Created transient SBOM entity in S3: {}", key);
    return new S3SbomEntity(key, s3Client, s3AsyncClient, s3DataStoreConfig, null, uniqueFileName);
  }

  @Override
  @WithSpan
  public SbomEntity saveTemporarySbom(
      final SbomEntity sbomEntity,
      final String fileName,
      @Nullable final String prefix) throws IOException
  {
    S3ObjectKey targetKey = getTemporarySbomKey(fileName, prefix);

    try (InputStream inputStream = sbomEntity.getInputStream();
        OutputStream outputStream = new S3OutputStream(s3AsyncClient, targetKey.toString(),
            s3DataStoreConfig.getBucketName(), s3DataStoreConfig.getServerSideEncryption()))
    {
      inputStream.transferTo(outputStream);
    }

    log.debug("Saved temporary SBOM from {} to {}", sbomEntity.getLocation(), targetKey);
    return new S3SbomEntity(targetKey, s3Client, s3AsyncClient, s3DataStoreConfig, sbomEntity.getAppId(), fileName);
  }

  @Override
  @WithSpan
  public void deleteSbom(final SbomEntity sbomEntity) throws IOException {
    if (sbomEntity instanceof S3SbomEntity s3SbomEntity) {
      deleteByKey(s3SbomEntity.key());
    }
    else {
      throw new IllegalArgumentException("Cannot delete non-S3 SBOM entity with S3 service: " + sbomEntity.getClass());
    }
  }

  @Override
  @WithSpan
  public void deleteSbom(final String appId, final String fileName) throws IOException {
    S3ObjectKey key = getPermanentSbomKey(appId, fileName);
    deleteByKey(key);
  }

  @Override
  @WithSpan
  public void deleteSbomsFor(final String appId) throws IOException {
    log.debug("Deleting all SBOMs in S3 for applicationId '{}'", appId);
    String keyPrefix = String.format(PERMANENT_SBOM_FORMAT, appId, "", "");
    S3Utils.deleteAllWithPrefix(s3Client, s3DataStoreConfig.getBucketName(),
        s3DataStoreConfig.getObjectKeyPrefix() + keyPrefix);
  }

  @Override
  @WithSpan
  public void deleteTransientSbomsOlderThan(final Instant instant) throws IOException {
    log.debug("Deleting transient SBOMs older than {}", instant);
    String keyPrefix = s3DataStoreConfig.getObjectKeyPrefix() + "sboms/temp/transient/";

    try (Stream<S3Object> objects = S3Utils.getS3Objects(s3Client, s3DataStoreConfig.getBucketName(), keyPrefix)) {
      Set<ObjectIdentifier> keysToDelete = objects
          .filter(s3Object -> s3Object.lastModified().compareTo(instant) <= 0)
          .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
          .collect(Collectors.toSet());

      if (!keysToDelete.isEmpty()) {
        DeleteObjectsRequest request = DeleteObjectsRequest.builder()
            .bucket(s3DataStoreConfig.getBucketName())
            .delete(delete -> delete.objects(keysToDelete))
            .build();

        wrapS3Exception(() -> s3Client.deleteObjects(request));
        log.debug("Deleted {} transient SBOMs older than {}", keysToDelete.size(), instant);
      }
    }
  }

  @Override
  public void moveSbomEntity(final SbomEntity from, final SbomEntity to) throws IOException {
    S3SbomEntity fromS3 = (S3SbomEntity) from;
    S3SbomEntity toS3 = (S3SbomEntity) to;

    CopyObjectRequest copyRequest = CopyObjectRequest.builder()
        .sourceBucket(s3DataStoreConfig.getBucketName())
        .sourceKey(fromS3.key().toString())
        .destinationBucket(s3DataStoreConfig.getBucketName())
        .destinationKey(toS3.key().toString())
        .serverSideEncryption(s3DataStoreConfig.getServerSideEncryption())
        .build();

    wrapS3Exception(() -> s3Client.copyObject(copyRequest));

    deleteByKey(fromS3.key());
  }

  private String generateTempFileName(final String fileName) {
    String extension = StringUtils.substringAfterLast(fileName, ".");

    long timestamp = System.currentTimeMillis();
    int random = (int) (Math.random() * 1000000);
    String uniquePart = timestamp + "-" + random;

    if (StringUtils.isBlank(extension) || extension.equals(fileName)) {
      return "sbom-" + uniquePart;
    }
    else {
      return "sbom-" + uniquePart + "." + extension;
    }
  }

  /**
   * permanent SBOM: sboms/{appId}/{fileName}
   */
  private S3ObjectKey getPermanentSbomKey(final String appId, final String fileName) {
    return new S3ObjectKey(PERMANENT_SBOM_FORMAT, appId, "", fileName, s3DataStoreConfig.getObjectKeyPrefix());
  }

  /**
   * temporary SBOM: sboms/temp/persistent/{prefix}/{fileName}
   */
  private S3ObjectKey getTemporarySbomKey(final String fileName, final String prefix) {
    String pathPrefix = prefix != null ? prefix + "/" : "";
    return new S3ObjectKey(TEMPORARY_SBOM_FORMAT, pathPrefix, "", fileName,
        s3DataStoreConfig.getObjectKeyPrefix());
  }

  /**
   * transient SBOM: sboms/temp/transient/{fileName}
   */
  private S3ObjectKey getTransientSbomKey(final String fileName) {
    return new S3ObjectKey(TRANSIENT_SBOM_FORMAT, "", "", fileName, s3DataStoreConfig.getObjectKeyPrefix());
  }

  private void deleteByKey(final S3ObjectKey key) throws IOException {
    DeleteObjectRequest request = DeleteObjectRequest.builder()
        .bucket(s3DataStoreConfig.getBucketName())
        .key(key.toString())
        .build();

    try {
      wrapS3Exception(() -> s3Client.deleteObject(request));
      log.debug("Deleted SBOM at S3 key '{}'", key);
    }
    catch (IOException e) {
      if (e.getCause() instanceof NoSuchKeyException) {
        log.debug("SBOM not found for deletion at S3 key '{}'", key);
      }
      else {
        throw e;
      }
    }
  }
}
