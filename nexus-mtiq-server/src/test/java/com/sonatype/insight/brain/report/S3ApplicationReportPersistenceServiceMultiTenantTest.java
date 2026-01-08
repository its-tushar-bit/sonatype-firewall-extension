/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.MultiTenantS3DataStoreConfig;
import com.sonatype.insight.brain.service.config.MultiTenantStorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.tenancy.TenantThreadLocal;
import com.sonatype.insight.test.ContainerRule;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
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

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("CLM-38156")
@Category(SlowTest.class)
@RunWith(Parameterized.class)
public class S3ApplicationReportPersistenceServiceMultiTenantTest
    extends AbstractApplicationReportPersistenceServiceMultiTenantTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-bucket";

  private static final String REGION = "us-east-2";

  @ClassRule
  public static ContainerRule<LocalStackContainer> localstack =
      new ContainerRule<>(new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.S3));

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
                localstack.getContainer().getSecretKey()
            )))
        .build();
  }

  @After
  public void cleanup() {
    S3Client s3Client = lookup(S3Client.class);
    s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(BUCKET_NAME).build())
        .contents()
        .forEach(obj -> s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME).key(obj.key()).build()));
  }

  public S3ApplicationReportPersistenceServiceMultiTenantTest(String configuredPrefix, String expectedPrefix) {
    this.prefix = configuredPrefix;
    this.expectedPrefix = expectedPrefix;
  }

  @Before
  public void setup() throws Exception {
    var configurator = new MtiqDatabaseConfigurator()
    {
      @Override
      public void configure(InsightConfig config) {
        super.configure(config);

        MultiTenantStorageConfig storageConfig = (MultiTenantStorageConfig) config.getStorage();
        var s3Config = new MultiTenantS3DataStoreConfig();
        s3Config.setBucketName(BUCKET_NAME);
        s3Config.setRegion(REGION);
        s3Config.setObjectKeyPrefix(prefix);
        s3Config.setEndpoint(localstack.getContainer().getEndpoint());

        storageConfig.setS3Config(s3Config);
        storageConfig.setType(DataStoreType.S3);
      }
    };

    setup(configurator, () -> {
      var s3Client = lookup(S3Client.class);
      var insightConfig = lookup(InsightConfig.class);

      return new S3ApplicationReportPersistenceServiceTestHelper(insightConfig, s3Client,
          () -> expectedPrefix + TenantThreadLocal.getTenant().tenantSlug + "/");
    });
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
        localstack.getContainer().getAccessKey(),
        localstack.getContainer().getSecretKey()
    ));
    binder.bind(AwsCredentialsProvider.class).toInstance(awsCredentialsProvider);
  }

  @Test
  @Override
  public void testCorrectImplClass() {
    assertThat(service).isInstanceOf(S3ApplicationReportPersistenceService.class);
  }
}
