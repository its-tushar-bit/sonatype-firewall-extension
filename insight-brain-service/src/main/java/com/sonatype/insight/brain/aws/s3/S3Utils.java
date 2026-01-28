/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.aws.s3;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import static com.sonatype.insight.brain.aws.s3.S3ExceptionUtil.wrapS3Exception;

public class S3Utils
{
  private static final Logger log = LoggerFactory.getLogger(S3Utils.class);

  /**
   * AWS S3 deleteObjects API has a limit of 1000 objects per request. See:
   * https://docs.aws.amazon.com/AmazonS3/latest/API/API_DeleteObjects.html
   */
  private static final int S3_DELETE_BATCH_SIZE = 1000;

  private S3Utils() {
  }

  public static void deleteAllWithPrefix(S3Client s3Client, String bucketName, String prefix) throws IOException {
    List<ObjectIdentifier> keysToDelete;
    try (Stream<S3Object> objects = getS3Objects(s3Client, bucketName, prefix)) {
      keysToDelete = objects
          .map(s3Object -> ObjectIdentifier.builder().key(s3Object.key()).build())
          .toList();
    }

    if (keysToDelete.isEmpty()) {
      log.info("No objects found with prefix: {}", prefix);
      return;
    }

    // Batch delete operations into chunks of S3_DELETE_BATCH_SIZE to avoid exceeding S3 API limits
    List<List<ObjectIdentifier>> batches = Lists.partition(keysToDelete, S3_DELETE_BATCH_SIZE);
    int totalDeleted = 0;
    for (List<ObjectIdentifier> batch : batches) {
      DeleteObjectsRequest request = DeleteObjectsRequest.builder()
          .bucket(bucketName)
          .delete(delete -> delete.objects(batch))
          .build();

      wrapS3Exception(() -> s3Client.deleteObjects(request));
      totalDeleted += batch.size();
    }
    log.info("Deleted {} objects with prefix: {}", totalDeleted, prefix);
  }

  public static Stream<S3Object> getS3Objects(S3Client s3Client, String bucketName, String prefix) throws IOException {
    return getS3Objects(s3Client, bucketName, prefix, null);
  }

  public static Stream<S3Object> getS3Objects(S3Client s3Client, String bucketName, String prefix, Integer maxKeys)
      throws IOException
  {
    ListObjectsV2Request request = ListObjectsV2Request.builder()
        .bucket(bucketName)
        .prefix(prefix)
        .maxKeys(maxKeys)
        .build();

    return wrapS3Exception(() -> s3Client.listObjectsV2Paginator(request).stream()
        .map(ListObjectsV2Response::contents)
        .flatMap(List::stream));
  }

  public static boolean exists(S3Client s3Client, String bucketName, String key) throws IOException {
    try {
      s3Client.headObject(HeadObjectRequest.builder()
          .bucket(bucketName)
          .key(key)
          .build());
      return true;
    }
    catch (NoSuchKeyException e) {
      return false;
    }
    catch (S3Exception e) {
      throw new IOException(e);
    }
  }
}
