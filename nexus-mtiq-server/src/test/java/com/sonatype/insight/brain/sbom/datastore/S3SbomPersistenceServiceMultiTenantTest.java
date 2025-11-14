/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.sbom.datastore;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.MultiTenantS3DataStoreConfig;
import com.sonatype.insight.brain.service.config.MultiTenantStorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
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

@Category(SlowTest.class)
@RunWith(Parameterized.class)
public class S3SbomPersistenceServiceMultiTenantTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-sbom-bucket";

  private static final String REGION = "us-east-2";

  @ClassRule
  public static LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.S3);

  protected S3SbomPersistenceService service;

  private final String prefix;

  @Parameters
  public static List<String> prefixes() {
    return Arrays.asList(
        null,
        "",
        "valid-prefix/with/path/",
        "valid-prefix/with/path/ends-with-slash/"
    );
  }

  @BeforeClass
  public static void createBucket() {
    try (S3Client s3Client = createS3Client()) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
    }
  }

  private static S3Client createS3Client() {
    return S3Client.builder()
        .endpointOverride(localstack.getEndpoint())
        .region(Region.of(REGION))
        .credentialsProvider(
            StaticCredentialsProvider.create(AwsBasicCredentials.create(
                localstack.getAccessKey(),
                localstack.getSecretKey()
            )))
        .build();
  }

  @After
  public void cleanup() throws Exception {
    S3Client s3Client = lookup(S3Client.class);
    s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(BUCKET_NAME).build())
        .contents()
        .forEach(obj -> s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME).key(obj.key()).build()));
  }

  public S3SbomPersistenceServiceMultiTenantTest(String configuredPrefix) {
    this.prefix = configuredPrefix;
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
        s3Config.setEndpoint(localstack.getEndpoint());

        storageConfig.setS3Config(s3Config);
        storageConfig.setType(DataStoreType.S3);
      }
    };

    startIqTestServer(configurator);

    this.service = lookup(S3SbomPersistenceService.class);
  }

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
        localstack.getAccessKey(),
        localstack.getSecretKey()
    ));
    binder.bind(AwsCredentialsProvider.class).toInstance(awsCredentialsProvider);
  }

  @Test
  public void testCorrectImplClass() {
    assertThat(service).isInstanceOf(S3SbomPersistenceService.class);
  }
}
