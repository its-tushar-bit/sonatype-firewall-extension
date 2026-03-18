/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import com.sonatype.insight.test.ContainerRule;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.experimental.categories.Category;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

@Category(SlowTest.class)
public class ReportPurgerS3Test
    extends AbstractReportPurgerTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-bucket";

  private static final String REGION = "us-east-2";

  @ClassRule
  public static ContainerRule<LocalStackContainer> localstack =
      new ContainerRule<>(new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.S3));

  @Inject
  private S3Client s3Client;

  @Inject
  private ApplicationReportPersistenceService applicationReportPersistenceService;

  @BeforeClass
  public static void createBucket() {
    try (S3Client s3Client = createS3Client()) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
    }
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

  @After
  public void cleanup() throws Exception {
    s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(BUCKET_NAME).build())
        .contents()
        .forEach(obj -> s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME)
            .key(obj.key())
            .build()));
  }

  @Override
  protected void customizeConfig(InsightConfig insightConfig) {
    var storageConfig = insightConfig.getStorage();
    var s3Config = new S3DataStoreConfig();
    s3Config.setBucketName(BUCKET_NAME);
    s3Config.setRegion(REGION);
    s3Config.setEndpoint(localstack.getContainer().getEndpoint());
    storageConfig.setS3Config(s3Config);
    storageConfig.setType(DataStoreType.S3);
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
        localstack.getContainer().getAccessKey(),
        localstack.getContainer().getSecretKey()));
    binder.bind(AwsCredentialsProvider.class).toInstance(awsCredentialsProvider);
  }

  @Override
  void mockReport(PolicyEvaluation evaluation) throws Exception {
    try (var zipStream = getClass().getClassLoader().getResourceAsStream("AbstractReportPurgerTest/small-report.zip")) {
      applicationReportPersistenceService.saveOriginalReport(evaluation.getApplicationId(), evaluation.getScanId(),
          zipStream);
    }
  }
}
