/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.IOException;

import com.sonatype.insight.brain.service.config.ReportDataStoreConfig.S3DataStoreConfig;
import com.sonatype.insight.brain.utils.IdValidationUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

public class S3ReportPdf
    extends AbstractS3ReportEntity
    implements ReportPdf
{
  private static final Logger log = LoggerFactory.getLogger(S3ReportPdf.class);

  public S3ReportPdf(
      final S3Client s3Client,
      final S3DataStoreConfig s3Config,
      final String appId,
      final String scanId)
  {
    super(s3Client, s3Config, appId, scanId, REPORT_FILE_NAME);
    IdValidationUtils.validate(appId);
    IdValidationUtils.validate(scanId);
  }

  @Override
  public boolean canCreate() {
    return true;
  }

  @Override
  public long length() {
    HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
        .bucket(bucketName)
        .key(getKey().toString())
        .build();
    HeadObjectResponse headObjectResponse = s3Client.headObject(headObjectRequest);
    return headObjectResponse.contentLength();
  }

  @Override
  public void deleteIfExists() throws IOException {
    HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
        .bucket(bucketName)
        .key(getKey().toString())
        .build();

    try {
      s3Client.headObject(headObjectRequest);
      DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
          .bucket(bucketName)
          .key(getKey().toString())
          .build();
      s3Client.deleteObject(deleteObjectRequest);
    }
    catch (NoSuchKeyException e) {
      log.trace("Failed to delete PDF for appId '{}' scanId '{}'.", appId, scanId, e);
    }
  }
}
