/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;

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
import org.mockito.ArgumentMatcher;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceTestHelper.APPLICATION_ID;
import static com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceTestHelper.SCAN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;

@Category(SlowTest.class)
@RunWith(Parameterized.class)
public class S3ApplicationReportPersistenceServiceTest
    extends AbstractApplicationReportPersistenceServiceTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-bucket";

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

  public S3ApplicationReportPersistenceServiceTest(String configuredPrefix, String expectedPrefix) {
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
    var helper = new S3ApplicationReportPersistenceServiceTestHelper(insightConfig, s3Client, () -> expectedPrefix);
    setup(helper);

    service = lookup(ApplicationReportPersistenceService.class);
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
    var spyS3AsyncClient = spy(s3AsyncClient);

    ArgumentMatcher<PutObjectRequest> indexHtmlMatcher =
        putObjectRequest -> putObjectRequest.key().endsWith("index.html");

    // lenient because we are only mocking one of several calls to createMultipartUpload
    lenient().doThrow(S3Exception.builder().message("test exception").build())
        .when(spyS3AsyncClient).putObject(argThat(indexHtmlMatcher), any(AsyncRequestBody.class));

    return new S3ApplicationReportPersistenceService(s3Client, spyS3AsyncClient, insightConfig);
  }

  @Test
  public void testGetReportEntity_extractsZipOnFirstAccess() throws Exception {
    var helper = (S3ApplicationReportPersistenceServiceTestHelper) this.helper;
    byte[] zipContent = createMockReportZip();
    helper.writeZipFile(APPLICATION_ID, SCAN_ID, zipContent);

    assertThat(helper.zipFileExists(APPLICATION_ID, SCAN_ID)).isTrue();
    assertThat(helper.readFromOriginalFiles("index.html")).isNull();

    var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "index.html");
    assertThat(entity.exists()).isTrue();
    helper.assertEntityContents(entity, "<html></html>");

    assertThat(helper.readFromOriginalFiles("index.html")).isEqualTo("<html></html>");
    assertThat(helper.zipFileExists(APPLICATION_ID, SCAN_ID)).isFalse();

    var dataJsonEntity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "data.json");
    assertThat(dataJsonEntity.exists()).isTrue();
    helper.assertEntityContents(dataJsonEntity, "{\"test\":\"data\"}");
  }

  @Test
  public void testGetReportEntity_handlesExtractedFiles() throws Exception {
    helper.saveEmptyMockReport();

    assertThat(helper.readFromOriginalFiles("index.html")).isEqualTo("<html></html>");

    var entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "index.html");
    assertThat(entity.exists()).isTrue();
    helper.assertEntityContents(entity, "<html></html>");
  }

  @Test
  public void testGetReportEntity_handlesNonExistentFileAfterExtraction() throws Exception {
    var helper = (S3ApplicationReportPersistenceServiceTestHelper) this.helper;
    byte[] zipContent = createMockReportZip();
    helper.writeZipFile(APPLICATION_ID, SCAN_ID, zipContent);

    var existingEntity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "index.html");
    assertThat(existingEntity.exists()).isTrue();
    assertThat(helper.zipFileExists(APPLICATION_ID, SCAN_ID)).isFalse();

    var nonExistentEntity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "nonexistent.html");
    assertThat(nonExistentEntity.exists()).isFalse();
  }

  @Test
  public void testGetReportEntity_copyObjectFailureAfterExtraction_zipDeleted() throws Exception {
    var helper = (S3ApplicationReportPersistenceServiceTestHelper) this.helper;
    byte[] zipContent = createMockReportZip();
    helper.writeZipFile(APPLICATION_ID, SCAN_ID, zipContent);

    assertThat(helper.zipFileExists(APPLICATION_ID, SCAN_ID)).isTrue();

    try {
      service.getReportEntity(APPLICATION_ID, SCAN_ID, "nonexistent.html");
    }
    catch (Exception e) {
      // Expected exception when file not in zip
    }

    assertThat(helper.zipFileExists(APPLICATION_ID, SCAN_ID)).isFalse();
    assertThat(helper.readFromOriginalFiles("index.html")).isNotNull();
    assertThat(helper.readFromOriginalFiles("nonexistent.html")).isNull();
  }

  private byte[] createMockReportZip() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      zos.putNextEntry(new ZipEntry("index.html"));
      zos.write("<html></html>".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();

      zos.putNextEntry(new ZipEntry("data.json"));
      zos.write("{\"test\":\"data\"}".getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();
    }
    return baos.toByteArray();
  }
}
