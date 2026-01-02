/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.scan.datastore;

import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
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
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceTestHelper.APPLICATION_ID;
import static com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceTestHelper.SCAN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@RunWith(Parameterized.class)
public class S3ScanPersistenceServiceTest
    extends AbstractScanPersistenceServiceTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-scan-bucket";

  private static final String REGION = "us-east-2";

  @ClassRule
  public static LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.S3);

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
    s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(BUCKET_NAME).build())
        .contents()
        .forEach(obj -> s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME).key(obj.key()).build()));
  }

  @Inject
  private S3Client s3Client;

  @Inject
  private S3AsyncClient s3AsyncClient;

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

  public S3ScanPersistenceServiceTest(String configuredPrefix, String expectedPrefix) {
    this.prefix = configuredPrefix;
    this.expectedPrefix = expectedPrefix;
  }

  @Override
  protected void customizeConfig(InsightConfig insightConfig) {
    var storageConfig = insightConfig.getStorage();
    var s3Config = new S3DataStoreConfig();
    s3Config.setBucketName(BUCKET_NAME);
    s3Config.setRegion(REGION);
    s3Config.setObjectKeyPrefix(prefix);
    s3Config.setEndpoint(localstack.getEndpoint());
    storageConfig.setS3Config(s3Config);
    storageConfig.setType(DataStoreType.S3);
  }

  @Inject
  private InsightConfig insightConfig;

  @Before
  public void setup() {
    var helper = new S3ScanPersistenceServiceTestHelper(insightConfig, s3Client, () -> expectedPrefix);
    setup(helper);

    service = lookup(ScanPersistenceService.class);
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
  @Override
  public void testCorrectImplClass() {
    assertThat(service).isInstanceOf(S3ScanPersistenceService.class);
  }

  @Test
  public void testGetScanLocation() {
    assertThat(service.getScan("app1", "scan1").getLocation())
        .isEqualTo("s3://" + BUCKET_NAME + "/" + expectedPrefix + "scan/app1/scan-scan1.xml.gz");
  }

  @Test
  @Override
  public void testScanEntity_getLocation() {
    var scanEntity = service.getScan(APPLICATION_ID, SCAN_ID);
    String expectedLocation =
        "s3://" + BUCKET_NAME + "/" + expectedPrefix + "scan/" + APPLICATION_ID + "/scan-" + SCAN_ID + ".xml.gz";
    assertThat(scanEntity.getLocation()).isEqualTo(expectedLocation);
  }

  @Test
  @Override
  public void testExceptionHandlingAndCleanup() throws Exception {
    var spyS3Client = spy(this.s3Client);
    var spyS3AsyncClient = spy(this.s3AsyncClient);

    var scanEntity =
        S3ScanEntity.forScan(spyS3Client, spyS3AsyncClient, BUCKET_NAME, expectedPrefix, null, APPLICATION_ID,
            SCAN_ID);

    helper.saveMockScan();
    assertThat(scanEntity.exists()).isTrue();

    // Mock deleteObject to throw an exception
    doThrow(S3Exception.builder().message("S3 deletion failed").build())
        .when(spyS3Client).deleteObject(org.mockito.ArgumentMatchers.any(DeleteObjectRequest.class));

    // Deletion should return false when S3 operation fails
    boolean deleted = scanEntity.delete();
    assertThat(deleted).isFalse();
  }
}
