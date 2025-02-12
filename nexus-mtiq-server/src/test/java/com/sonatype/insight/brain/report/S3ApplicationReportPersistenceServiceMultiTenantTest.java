/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Binder;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.ReportDataStoreConfig;
import com.sonatype.insight.brain.service.config.ReportDataStoreConfig.S3DataStoreConfig;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Ignore // until S3 impl is made tenant aware in CLM-33230
@Category(SlowTest.class)
@RunWith(Parameterized.class)
public class S3ApplicationReportPersistenceServiceMultiTenantTest
    extends AbstractApplicationReportPersistenceServiceMultiTenantTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.5.0");

  private static final String BUCKET_NAME = "test-bucket";

  private static final String REGION = "us-east-2";

  @Rule
  public LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(S3);

  private final String prefix;

  private final String expectedPrefix;

  @Parameters
  public static List<Object[]> prefixes() {
    return Arrays.asList(new Object[][]{
        {null, ""},
        {"", ""},
        {"valid-prefix/with/path", "valid-prefix/with/path/"},
        {"valid-prefix/with/path/ends-with-slash/", "valid-prefix/with/path/ends-with-slash/"}
    });
  }

  public S3ApplicationReportPersistenceServiceMultiTenantTest(String configuredPrefix, String expectedPrefix) {
    this.prefix = configuredPrefix;
    this.expectedPrefix = expectedPrefix;
  }

  @Before
  public void setup() throws Exception {
    var configurator = new MtiqDatabaseConfigurator() {
      @Override
      public void configure(InsightConfig config) {
        super.configure(config);

        var reportDataStoreConfig = config.getReportDataStoreConfig();
        var s3Config = new S3DataStoreConfig();
        s3Config.setBucketName(BUCKET_NAME);
        s3Config.setRegion(REGION);
        s3Config.setObjectKeyPrefix(prefix);
        s3Config.setEndpoint(localstack.getEndpoint());

        reportDataStoreConfig.setS3Config(s3Config);
        reportDataStoreConfig.setType(ReportDataStoreConfig.ReportDataStoreType.S3);
      }
    };

    setup(configurator, () -> {
      var s3Client = lookup(S3Client.class);
      var insightConfig = lookup(InsightConfig.class);

      return new S3ApplicationReportPersistenceServiceTestHelper(insightConfig, s3Client, expectedPrefix);
    });

    lookup(S3Client.class).createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
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

  @Test
  @Override
  public void testCorrectImplClass() {
    assertThat(service).isInstanceOf(S3ApplicationReportPersistenceService.class);
  }
}
