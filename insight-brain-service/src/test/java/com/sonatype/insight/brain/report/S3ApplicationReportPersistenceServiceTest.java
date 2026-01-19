/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.inject.Inject;

import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import com.sonatype.insight.test.ContainerRule;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentMatcher;
import org.testcontainers.localstack.LocalStackContainer;
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
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceTestHelper.APPLICATION_ID;
import static com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceTestHelper.SCAN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(Parameterized.class)
public class S3ApplicationReportPersistenceServiceTest
    extends AbstractApplicationReportPersistenceServiceTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-bucket";

  private static final String REGION = "us-east-2";

  @ClassRule
  public static ContainerRule<LocalStackContainer> localstack =
      new ContainerRule<>(new LocalStackContainer(LOCALSTACK_IMAGE).withServices("s3"));

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
    s3Config.setEndpoint(localstack.getContainer().getEndpoint());
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

  @Test
  public void testGetMetadata_WithCachedMetadataSource() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("test.txt", "content");
    S3Client spyS3Client = spy(s3Client);
    S3ApplicationReportPersistenceService spyService =
        new S3ApplicationReportPersistenceService(spyS3Client, s3AsyncClient, insightConfig);
    ReportEntity entity = spyService.getReportEntity(APPLICATION_ID, SCAN_ID, "test.txt");

    // First call to getMetadata with MetadataSource.CACHED - should call headObject to populate cache
    Optional<Metadata> metadata1 =
        entity.getMetadata(MetadataSource.CACHED, MetadataAttribute.LAST_MODIFIED_EPOCH_TIME);
    assertThat(metadata1).isPresent();
    assertThat(metadata1.get().lastModifiedEpochTime()).isGreaterThan(0);

    // Verify headObject was called once (from getReportEntity -> doGetReportEntity -> entity.exists())
    verify(spyS3Client, times(1)).headObject(any(HeadObjectRequest.class));

    // Second call to getMetadata with MetadataSource.CACHED - should use cached metadata without calling S3
    Optional<Metadata> metadata2 =
        entity.getMetadata(MetadataSource.CACHED, MetadataAttribute.LAST_MODIFIED_EPOCH_TIME);
    assertThat(metadata2).isPresent();
    assertThat(metadata2.get().lastModifiedEpochTime()).isEqualTo(metadata1.get().lastModifiedEpochTime());

    // Verify headObject was still only called once (cached metadata was used)
    verify(spyS3Client, times(1)).headObject(any(HeadObjectRequest.class));

    // Third call with different attributes - should still use cache
    Optional<Metadata> metadata3 = entity.getMetadata(MetadataSource.CACHED,
        MetadataAttribute.LAST_MODIFIED_EPOCH_TIME,
        MetadataAttribute.SIZE_IN_BYTES
    );
    assertThat(metadata3).isPresent();
    assertThat(metadata3.get().lastModifiedEpochTime()).isEqualTo(metadata1.get().lastModifiedEpochTime());
    assertThat(metadata3.get().sizeInBytes()).isGreaterThan(0);

    // Still only one headObject call
    verify(spyS3Client, times(1)).headObject(any(HeadObjectRequest.class));
  }

  @Test
  public void testGetMetadata_MultipleEntities_IndependentCaches() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("file1.txt", "content1");
    helper.writeAdditionalFile("file2.txt", "content2");
    S3Client spyS3Client = spy(s3Client);
    S3ApplicationReportPersistenceService spyService =
        new S3ApplicationReportPersistenceService(spyS3Client, s3AsyncClient, insightConfig);

    // Get two different entities
    ReportEntity entity1 = spyService.getReportEntity(APPLICATION_ID, SCAN_ID, "file1.txt");
    ReportEntity entity2 = spyService.getReportEntity(APPLICATION_ID, SCAN_ID, "file2.txt");

    // Each entity.exists() call will make one headObject call
    verify(spyS3Client, times(2)).headObject(any(HeadObjectRequest.class));

    // Get metadata for entity1 - should use cached metadata from exists() call
    Optional<Metadata> m1 = entity1.getMetadata(MetadataSource.CACHED);
    assertThat(m1).isPresent();

    // Get metadata for entity2 - should use cached metadata from exists() call
    Optional<Metadata> m2 = entity2.getMetadata(MetadataSource.CACHED);
    assertThat(m2).isPresent();

    // Still only 2 headObject calls total
    verify(spyS3Client, times(2)).headObject(any(HeadObjectRequest.class));

    // Call getMetadata again on both entities with MetadataSource.CACHED
    entity1.getMetadata(MetadataSource.CACHED);
    entity2.getMetadata(MetadataSource.CACHED);

    // Still only 2 headObject calls - both used cached metadata
    verify(spyS3Client, times(2)).headObject(any(HeadObjectRequest.class));
  }

  @Test
  public void testGetMetadata_ComparingMetadataSources() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("test.txt", "content");
    S3Client spyS3Client = spy(s3Client);
    S3ApplicationReportPersistenceService spyService =
        new S3ApplicationReportPersistenceService(spyS3Client, s3AsyncClient, insightConfig);

    ReportEntity entity = spyService.getReportEntity(APPLICATION_ID, SCAN_ID, "test.txt");

    // exists() made 1 headObject call
    verify(spyS3Client, times(1)).headObject(any(HeadObjectRequest.class));

    // Call getMetadata with MetadataSource.FETCH - should make a NEW S3 call each time
    entity.getMetadata();
    verify(spyS3Client, times(2)).headObject(any(HeadObjectRequest.class));

    Optional<Metadata> m2 = entity.getMetadata(MetadataSource.FETCH);
    verify(spyS3Client, times(3)).headObject(any(HeadObjectRequest.class));

    // But getMetadata with MetadataSource.CACHED should NOT make additional calls
    Optional<Metadata> m3 = entity.getMetadata(MetadataSource.CACHED);
    verify(spyS3Client, times(3)).headObject(any(HeadObjectRequest.class));

    Optional<Metadata> m4 = entity.getMetadata(MetadataSource.CACHED);
    verify(spyS3Client, times(3)).headObject(any(HeadObjectRequest.class));

    assertThat(m3.get().lastModifiedEpochTime()).isEqualTo(m2.get().lastModifiedEpochTime());
    assertThat(m4.get().lastModifiedEpochTime()).isEqualTo(m2.get().lastModifiedEpochTime());
  }

  @Test
  public void testGetMetadata_MixedFetchAndCachedCalls() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("test.txt", "original content");
    S3Client spyS3Client = spy(s3Client);
    S3ApplicationReportPersistenceService spyService =
        new S3ApplicationReportPersistenceService(spyS3Client, s3AsyncClient, insightConfig);

    ReportEntity entity = spyService.getReportEntity(APPLICATION_ID, SCAN_ID, "test.txt");

    // exists() made 1 headObject call during getReportEntity
    verify(spyS3Client, times(1)).headObject(any(HeadObjectRequest.class));

    // Call with CACHED - should use the cached metadata from getReportEntity
    Optional<Metadata> cachedMeta1 = entity.getMetadata(MetadataSource.CACHED);
    assertThat(cachedMeta1).isPresent();
    verify(spyS3Client, times(1)).headObject(any(HeadObjectRequest.class)); // Still only 1 call

    // Call with FETCH - should make a NEW S3 call
    Optional<Metadata> fetchedMeta1 = entity.getMetadata(MetadataSource.FETCH);
    assertThat(fetchedMeta1).isPresent();
    verify(spyS3Client, times(2)).headObject(any(HeadObjectRequest.class)); // Now 2 calls

    // Call with CACHED again - should use the updated cache from the FETCH call
    Optional<Metadata> cachedMeta2 = entity.getMetadata(MetadataSource.CACHED);
    assertThat(cachedMeta2).isPresent();
    verify(spyS3Client, times(2)).headObject(any(HeadObjectRequest.class)); // Still 2 calls

    // Call with FETCH again - should make another NEW S3 call
    Optional<Metadata> fetchedMeta2 = entity.getMetadata(MetadataSource.FETCH);
    assertThat(fetchedMeta2).isPresent();
    verify(spyS3Client, times(3)).headObject(any(HeadObjectRequest.class)); // Now 3 calls

    // All metadata should be consistent since the file hasn't changed
    assertThat(cachedMeta1.get().lastModifiedEpochTime()).isEqualTo(fetchedMeta1.get().lastModifiedEpochTime());
    assertThat(cachedMeta2.get().lastModifiedEpochTime()).isEqualTo(fetchedMeta1.get().lastModifiedEpochTime());
    assertThat(fetchedMeta2.get().lastModifiedEpochTime()).isEqualTo(fetchedMeta1.get().lastModifiedEpochTime());

    // Verify the same pattern works for exists()
    verify(spyS3Client, times(3)).headObject(any(HeadObjectRequest.class));

    boolean existsCached = entity.exists(MetadataSource.CACHED);
    assertThat(existsCached).isTrue();
    verify(spyS3Client, times(3)).headObject(any(HeadObjectRequest.class)); // No new call

    boolean existsFetch = entity.exists(MetadataSource.FETCH);
    assertThat(existsFetch).isTrue();
    verify(spyS3Client, times(4)).headObject(any(HeadObjectRequest.class)); // New call

    boolean existsCached2 = entity.exists(MetadataSource.CACHED);
    assertThat(existsCached2).isTrue();
    verify(spyS3Client, times(4)).headObject(any(HeadObjectRequest.class)); // No new call
  }

  @Test
  public void testGetTime_WithMetadataSource() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("test.txt", "content");
    S3Client spyS3Client = spy(s3Client);
    S3ApplicationReportPersistenceService spyService =
        new S3ApplicationReportPersistenceService(spyS3Client, s3AsyncClient, insightConfig);

    ReportEntity entity = spyService.getReportEntity(APPLICATION_ID, SCAN_ID, "test.txt");

    verify(spyS3Client, times(1)).headObject(any(HeadObjectRequest.class));
    // CACHED - should use cache
    long timeCached = entity.getTime(MetadataSource.CACHED);
    assertThat(timeCached).isGreaterThan(0);
    verify(spyS3Client, times(1)).headObject(any(HeadObjectRequest.class));
    // FETCH - should make new S3 call
    long timeFetch = entity.getTime(MetadataSource.FETCH);
    assertThat(timeFetch).isEqualTo(timeCached);
    verify(spyS3Client, times(2)).headObject(any(HeadObjectRequest.class));
  }

  @Test
  public void testLength_WithMetadataSource() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("test.txt", "content");
    S3Client spyS3Client = spy(s3Client);
    S3ApplicationReportPersistenceService spyService =
        new S3ApplicationReportPersistenceService(spyS3Client, s3AsyncClient, insightConfig);

    ReportEntity entity = spyService.getReportEntity(APPLICATION_ID, SCAN_ID, "test.txt");

    verify(spyS3Client, times(1)).headObject(any(HeadObjectRequest.class));
    // CACHED - should use cache
    long lengthCached = entity.length(MetadataSource.CACHED);
    assertThat(lengthCached).isEqualTo("content".length());
    verify(spyS3Client, times(1)).headObject(any(HeadObjectRequest.class));
    // FETCH - should make new S3 call
    long lengthFetch = entity.length(MetadataSource.FETCH);
    assertThat(lengthFetch).isEqualTo(lengthCached);
    verify(spyS3Client, times(2)).headObject(any(HeadObjectRequest.class));
  }

  @Test
  public void testGetTime_OnNonExistentFile_ThrowsException() throws Exception {
    helper.saveEmptyMockReport();
    ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "nonexistent.txt");

    assertThat(entity.exists(MetadataSource.CACHED)).isFalse();

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> entity.getTime(MetadataSource.CACHED))
        .withMessage("File does not exist");
    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> entity.getTime(MetadataSource.FETCH))
        .withMessage("File does not exist");
  }

  @Test
  public void testLength_OnNonExistentFile_ThrowsException() throws Exception {
    helper.saveEmptyMockReport();
    ReportEntity entity = service.getReportEntity(APPLICATION_ID, SCAN_ID, "nonexistent.txt");

    assertThat(entity.exists(MetadataSource.CACHED)).isFalse();

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> entity.length(MetadataSource.CACHED))
        .withMessage("File does not exist");
    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> entity.length(MetadataSource.FETCH))
        .withMessage("File does not exist");
  }

  @Test
  public void testGetAllReportEntities_PrefetchesMetadata() throws Exception {
    helper.saveEmptyMockReport();
    helper.writeAdditionalFile("file.txt", "content");

    S3Client spyS3Client = spy(s3Client);
    S3ApplicationReportPersistenceService spyService =
        new S3ApplicationReportPersistenceService(spyS3Client, s3AsyncClient, insightConfig);

    try (Stream<ReportEntity> entities = spyService.getAllReportEntities(APPLICATION_ID, SCAN_ID)) {
      List<ReportEntity> entityList = entities.collect(Collectors.toList());
      assertThat(entityList).isNotEmpty();

      // Access metadata on entities - should NOT make headObject calls because metadata was prefetched
      for (ReportEntity entity : entityList) {
        assertThat(entity.getMetadata(MetadataSource.CACHED)).isPresent();
        assertThat(entity.exists(MetadataSource.CACHED)).isTrue();
        assertThat(entity.getTime(MetadataSource.CACHED)).isGreaterThan(0);
        assertThat(entity.length(MetadataSource.CACHED)).isGreaterThan(0);
      }

      // Verify no headObject calls were made (all metadata came from ListObjectsV2)
      verify(spyS3Client, times(0)).headObject(any(HeadObjectRequest.class));
    }
  }

  @Test
  public void testGetOriginalReportEntities_PrefetchesMetadata() throws Exception {
    helper.saveEmptyMockReport();

    S3Client spyS3Client = spy(s3Client);
    S3ApplicationReportPersistenceService spyService =
        new S3ApplicationReportPersistenceService(spyS3Client, s3AsyncClient, insightConfig);

    try (Stream<ReportEntity> entities = spyService.getOriginalReportEntities(APPLICATION_ID, SCAN_ID)) {
      List<ReportEntity> entityList = entities.collect(Collectors.toList());
      assertThat(entityList).isNotEmpty();

      // Access metadata on entities - should NOT make headObject calls
      for (ReportEntity entity : entityList) {
        assertThat(entity.getMetadata(MetadataSource.CACHED)).isPresent();
        assertThat(entity.exists(MetadataSource.CACHED)).isTrue();
        assertThat(entity.getTime(MetadataSource.CACHED)).isGreaterThan(0);
        assertThat(entity.length(MetadataSource.CACHED)).isGreaterThan(0);
      }

      // Verify no headObject calls were made
      verify(spyS3Client, times(0)).headObject(any(HeadObjectRequest.class));
    }
  }
}
