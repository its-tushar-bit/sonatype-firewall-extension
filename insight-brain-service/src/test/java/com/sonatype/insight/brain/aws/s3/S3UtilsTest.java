/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.aws.s3;

import java.io.IOException;

import com.sonatype.insight.test.ContainerRule;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import static org.assertj.core.api.Assertions.assertThat;

public class S3UtilsTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-bucket";

  private static final String REGION = "us-east-2";

  private static final String TEST_PREFIX = "test-prefix/";

  @ClassRule
  public static ContainerRule<LocalStackContainer> localstack =
      new ContainerRule<>(new LocalStackContainer(LOCALSTACK_IMAGE).withServices("s3"));

  private static S3Client s3Client;

  @BeforeClass
  public static void setup() {
    s3Client = createS3Client();
    s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
  }

  private static S3Client createS3Client() {
    return S3Client.builder()
        .endpointOverride(localstack.getContainer().getEndpoint())
        .region(Region.of(REGION))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(
                localstack.getContainer().getAccessKey(),
                localstack.getContainer().getSecretKey())))
        .build();
  }

  @Test
  public void testDeleteAllWithPrefix_SmallBatch() throws IOException {
    // Create 10 objects
    String prefix = TEST_PREFIX + "small/";
    createTestObjects(prefix, 10);

    // Verify objects exist
    assertThat(countObjectsWithPrefix(prefix)).isEqualTo(10);

    // Delete all objects
    S3Utils.deleteAllWithPrefix(s3Client, BUCKET_NAME, prefix);

    // Verify all objects deleted
    assertThat(countObjectsWithPrefix(prefix)).isEqualTo(0);
  }

  @Test
  public void testDeleteAllWithPrefix_ExactlyThousand() throws IOException {
    // Create exactly 1000 objects
    String prefix = TEST_PREFIX + "thousand/";
    createTestObjects(prefix, 1000);

    // Verify objects exist
    assertThat(countObjectsWithPrefix(prefix)).isEqualTo(1000);

    // Delete all objects
    S3Utils.deleteAllWithPrefix(s3Client, BUCKET_NAME, prefix);

    // Verify all objects deleted
    assertThat(countObjectsWithPrefix(prefix)).isEqualTo(0);
  }

  @Test
  public void testDeleteAllWithPrefix_OverThousand() throws IOException {
    // Create 1500 objects to test batching behavior
    String prefix = TEST_PREFIX + "large/";
    int objectCount = 1500;
    createTestObjects(prefix, objectCount);

    // Verify objects exist
    assertThat(countObjectsWithPrefix(prefix)).isEqualTo(objectCount);

    // Delete all objects - this should succeed with batching
    S3Utils.deleteAllWithPrefix(s3Client, BUCKET_NAME, prefix);

    // Verify all objects deleted
    assertThat(countObjectsWithPrefix(prefix)).isEqualTo(0);
  }

  @Test
  public void testDeleteAllWithPrefix_OverTwoThousand() throws IOException {
    // Create 2500 objects to test multiple batches
    String prefix = TEST_PREFIX + "extralarge/";
    int objectCount = 2500;
    createTestObjects(prefix, objectCount);

    // Verify objects exist
    assertThat(countObjectsWithPrefix(prefix)).isEqualTo(objectCount);

    // Delete all objects - this should succeed with multiple batches
    S3Utils.deleteAllWithPrefix(s3Client, BUCKET_NAME, prefix);

    // Verify all objects deleted
    assertThat(countObjectsWithPrefix(prefix)).isEqualTo(0);
  }

  @Test
  public void testDeleteAllWithPrefix_EmptyPrefix() throws IOException {
    // Test with prefix that has no objects
    String prefix = TEST_PREFIX + "nonexistent/";

    // Verify no objects exist
    assertThat(countObjectsWithPrefix(prefix)).isEqualTo(0);

    // Should not fail when deleting non-existent prefix
    S3Utils.deleteAllWithPrefix(s3Client, BUCKET_NAME, prefix);

    // Should still be zero
    assertThat(countObjectsWithPrefix(prefix)).isEqualTo(0);
  }

  private void createTestObjects(String prefix, int count) {
    for (int i = 0; i < count; i++) {
      String key = prefix + "object-" + i + ".txt";
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(BUCKET_NAME)
              .key(key)
              .build(),
          RequestBody.fromString("test content " + i));
    }
  }

  private int countObjectsWithPrefix(String prefix) {
    int count = 0;
    ListObjectsV2Request request = ListObjectsV2Request.builder()
        .bucket(BUCKET_NAME)
        .prefix(prefix)
        .build();

    ListObjectsV2Response response;
    do {
      response = s3Client.listObjectsV2(request);
      count += response.contents().size();

      request = request.toBuilder()
          .continuationToken(response.nextContinuationToken())
          .build();
    }
    while (response.isTruncated());

    return count;
  }
}
