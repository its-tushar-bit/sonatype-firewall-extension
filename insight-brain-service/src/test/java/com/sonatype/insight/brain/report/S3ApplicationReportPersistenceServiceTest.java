/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import com.google.inject.Binder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentMatcher;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Category(SlowTest.class)
@RunWith(Parameterized.class)
public class S3ApplicationReportPersistenceServiceTest
    extends AbstractApplicationReportPersistenceServiceTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.5.0");

  private static final String BUCKET_NAME = "test-bucket";

  private static final String REGION = "us-east-2";

  @Rule
  public LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(S3);

  @Inject
  private S3Client s3Client;

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

  public S3ApplicationReportPersistenceServiceTest(String configuredPrefix, String expectedPrefix) {
    this.prefix = configuredPrefix;
    this.expectedPrefix = expectedPrefix;
  }

  @Before
  public void setup() {
    var storageConfig = insightConfig.getStorage();
    var s3Config = new S3DataStoreConfig();
    s3Config.setBucketName(BUCKET_NAME);
    s3Config.setRegion(REGION);
    s3Config.setObjectKeyPrefix(prefix);
    s3Config.setEndpoint(localstack.getEndpoint());
    storageConfig.setS3Config(s3Config);
    storageConfig.setType(DataStoreType.S3);

    s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());

    var helper = new S3ApplicationReportPersistenceServiceTestHelper(insightConfig, s3Client, expectedPrefix);
    setup(helper);

    service = lookup(ApplicationReportPersistenceService.class);
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

  @Test
  @Override
  public void testGetReportLocation() {
    assertThat(service.getReportLocation("app1", "scan1"))
        .isEqualTo("s3://test-bucket/" + expectedPrefix + "report/app1/scan1/");
  }

  @Override
  protected ApplicationReportPersistenceService mockForSaveOriginalReport_cleansUpOnFailure() {
    var s3Client = spy(this.s3Client);

    ArgumentMatcher<Consumer<CreateMultipartUploadRequest.Builder>> indexHtmlMatcher = builderConsumer -> {
      var builder = CreateMultipartUploadRequest.builder();
      builderConsumer.accept(builder);
      var request = builder.build();
      return request.key().endsWith("index.html");
    };

    // lenient because we are only mocking one of several calls to createMultipartUpload
    lenient().doThrow(S3Exception.builder().message("test exception").build())
        .when(s3Client).createMultipartUpload(argThat(indexHtmlMatcher));

    return new S3ApplicationReportPersistenceService(s3Client, insightConfig);
  }
}
