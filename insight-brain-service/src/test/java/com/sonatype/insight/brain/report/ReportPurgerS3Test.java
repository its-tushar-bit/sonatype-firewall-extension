/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Category(SlowTest.class)
public class ReportPurgerS3Test
    extends AbstractReportPurgerTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.5.0");

  private static final String BUCKET_NAME = "test-bucket";

  private static final String REGION = "us-east-2";

  @Rule
  public LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(S3);

  @Inject
  private S3Client s3Client;

  @Inject
  private ApplicationReportPersistenceService applicationReportPersistenceService;

  @Override
  protected void customizeConfig(InsightConfig insightConfig) {
    var storageConfig = insightConfig.getStorage();
    var s3Config = new S3DataStoreConfig();
    s3Config.setBucketName(BUCKET_NAME);
    s3Config.setRegion(REGION);
    s3Config.setEndpoint(localstack.getEndpoint());
    storageConfig.setS3Config(s3Config);
    storageConfig.setType(DataStoreType.S3);
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);

    var s3Client = S3Client.builder()
        .endpointOverride(localstack.getEndpoint())
        .region(Region.of(REGION))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(
                localstack.getAccessKey(),
                localstack.getSecretKey()
            )))
        .build();

    binder.bind(S3Client.class).toInstance(s3Client);
  }

  @Before
  public void before() {
    s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
  }

  @Override
  void mockReport(PolicyEvaluation evaluation) throws Exception {
    try (var zipStream = getClass().getClassLoader().getResourceAsStream("AbstractReportPurgerTest/small-report.zip")) {
      applicationReportPersistenceService.saveOriginalReport(evaluation.getApplicationId(), evaluation.getScanId(),
          zipStream);
    }
  }
}
