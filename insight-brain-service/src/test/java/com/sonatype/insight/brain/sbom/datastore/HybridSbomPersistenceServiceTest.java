/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.datastore;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.HybridDataStoreConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
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
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@Category(SlowTest.class)
@RunWith(Parameterized.class)
public class HybridSbomPersistenceServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(HybridSbomPersistenceService.class);

  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-scan-bucket";

  private static final String REGION = "us-east-2";

  private static final String APP_ID = "appId";

  private static final String OTHER_APP_ID = "otherAppId";

  private static final String FILE_NAME = "sbom.xml";

  private static final String OTHER_FILE_NAME = "otherSbom.xml";

  @ClassRule
  public static final LocalStackContainer localstack =
      new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.S3);

  @Inject
  private S3Client s3Client;

  @Inject
  private S3SbomPersistenceService s3SbomPersistenceService;

  private S3SbomPersistenceService s3SbomPersistenceServiceSpy;

  @Inject
  private FileSbomPersistenceService fileSbomPersistenceService;

  private FileSbomPersistenceService fileSbomPersistenceServiceSpy;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private HybridSbomPersistenceService hybridSbomPersistenceService;

  @Inject
  private SbomPersistenceServiceProvider sbomPersistenceServiceProvider;

  @Inject
  private ApiConfigurationService apiConfigurationService;

  @Parameters
  public static List<Object[]> dataStoreTypes() {
    return Arrays.asList(new Object[][]{
        {new LinkedHashSet<>(List.of(DataStoreType.S3, DataStoreType.FILE))},
        {new LinkedHashSet<>(List.of(DataStoreType.FILE, DataStoreType.S3))},
        });
  }

  private final LinkedHashSet<DataStoreType> dataStoreTypes;

  public HybridSbomPersistenceServiceTest(final LinkedHashSet<DataStoreType> dataStoreTypes) {
    this.dataStoreTypes = dataStoreTypes;
  }

  @Override
  public void configure(final Binder binder) {
    super.configure(binder);
    AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
        localstack.getAccessKey(),
        localstack.getSecretKey()
    ));
    binder.bind(AwsCredentialsProvider.class).toInstance(awsCredentialsProvider);
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

  @Override
  protected void customizeConfig(final InsightConfig insightConfig) {
    StorageConfig storageConfig = insightConfig.getStorage();
    HybridDataStoreConfig hybridDataStoreConfig = new HybridDataStoreConfig();
    hybridDataStoreConfig.setTypes(dataStoreTypes);
    storageConfig.setHybridConfig(hybridDataStoreConfig);
    storageConfig.setType(DataStoreType.HYBRID);

    // Setup S3
    S3DataStoreConfig s3DataStoreConfig = new S3DataStoreConfig();
    s3DataStoreConfig.setBucketName(BUCKET_NAME);
    s3DataStoreConfig.setRegion(REGION);
    s3DataStoreConfig.setEndpoint(localstack.getEndpoint());
    storageConfig.setS3Config(s3DataStoreConfig);
  }

  @Before
  public void setupSpies() {
    // Create spies of the services
    s3SbomPersistenceServiceSpy = spy(s3SbomPersistenceService);
    fileSbomPersistenceServiceSpy = spy(fileSbomPersistenceService);

    // Create a custom provider that returns the spies
    SbomPersistenceServiceProvider spiedProvider = new SbomPersistenceServiceProvider(
        insightConfig,
        () -> s3SbomPersistenceServiceSpy,
        () -> fileSbomPersistenceServiceSpy,
        () -> hybridSbomPersistenceService
    );

    // Create a new HybridSbomPersistenceService with the spied provider
    hybridSbomPersistenceService = new HybridSbomPersistenceService(
        insightConfig,
        () -> spiedProvider,
        apiConfigurationService
    );
  }

  @BeforeClass
  public static void setup() {
    try (S3Client s3Client = createS3Client()) {
      s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
    }
  }

  @After
  public void cleanup() throws Exception {
    s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(BUCKET_NAME).build())
        .contents()
        .forEach(obj -> s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME).key(obj.key()).build()));
  }

  @Test
  public void testDoGetSbom_DoesNotExist() {
    SbomEntity expectedSbomEntity = getPrimaryPersistenceService().doGetSbom(APP_ID, FILE_NAME);

    SbomEntity sbomEntity = hybridSbomPersistenceService.doGetSbom(APP_ID, FILE_NAME);

    assertThat(sbomEntity).isInstanceOf(expectedSbomEntity.getClass());
    assertThat(sbomEntity.getLocation()).isEqualTo(expectedSbomEntity.getLocation());
    assertThat(sbomEntity.exists()).isFalse();
  }

  @Test
  public void testDoGetSbom_ExistsOnlyInFile() throws Exception {
    SbomEntity fileSbomEntity = createSbom(fileSbomPersistenceService, APP_ID, FILE_NAME);

    SbomEntity sbomEntity = hybridSbomPersistenceService.doGetSbom(APP_ID, FILE_NAME);

    assertThat(sbomEntity).isInstanceOf(FileSbomEntity.class);
    assertThat(sbomEntity.getLocation()).isEqualTo(fileSbomEntity.getLocation());
    assertThat(readSbom(sbomEntity)).isEqualTo(readSbom(fileSbomEntity));
  }

  @Test
  public void testDoGetSbom_ExistsOnlyInS3() throws Exception {
    SbomEntity s3SbomEntity = createSbom(s3SbomPersistenceService, APP_ID, FILE_NAME);

    SbomEntity sbomEntity = hybridSbomPersistenceService.doGetSbom(APP_ID, FILE_NAME);

    assertThat(sbomEntity).isInstanceOf(S3SbomEntity.class);
    assertThat(sbomEntity.getLocation()).isEqualTo(s3SbomEntity.getLocation());
    assertThat(readSbom(sbomEntity)).isEqualTo(readSbom(s3SbomEntity));
  }

  @Test
  public void testDoGetSbom_ExistsInFileAndS3() throws Exception {
    createSbom(s3SbomPersistenceService, APP_ID, FILE_NAME);
    createSbom(fileSbomPersistenceService, APP_ID, FILE_NAME);
    SbomEntity expectedSbomEntity = getPrimaryPersistenceService().doGetSbom(APP_ID, FILE_NAME);

    SbomEntity sbomEntity = hybridSbomPersistenceService.doGetSbom(APP_ID, FILE_NAME);

    assertThat(sbomEntity).isInstanceOf(expectedSbomEntity.getClass());
    assertThat(sbomEntity.getLocation()).isEqualTo(expectedSbomEntity.getLocation());
    assertThat(readSbom(sbomEntity)).isEqualTo(readSbom(expectedSbomEntity));
  }

  @Test
  public void testGetTemporarySbom_DoesNotExist() {
    SbomEntity sbomEntity = hybridSbomPersistenceService.getTemporarySbom(FILE_NAME, null);
    SbomEntity expectedSbomEntity = getPrimaryPersistenceService().getTemporarySbom(FILE_NAME, null);

    assertThat(sbomEntity).isInstanceOf(expectedSbomEntity.getClass());
    assertThat(sbomEntity.exists()).isFalse();
  }

  @Test
  public void testGetTemporarySbom_ExistsOnlyInFile() throws Exception {
    SbomEntity fileSbomEntity = createTemporarySbom(fileSbomPersistenceService, FILE_NAME, null);

    SbomEntity sbomEntity = hybridSbomPersistenceService.getTemporarySbom(FILE_NAME, null);

    assertThat(sbomEntity).isInstanceOf(FileSbomEntity.class);
    assertThat(sbomEntity.getLocation()).isEqualTo(fileSbomEntity.getLocation());
    assertThat(readSbom(sbomEntity)).isEqualTo(readSbom(fileSbomEntity));
  }

  @Test
  public void testGetTemporarySbom_ExistsOnlyInS3() throws Exception {
    SbomEntity s3SbomEntity = createTemporarySbom(s3SbomPersistenceService, FILE_NAME, null);

    SbomEntity sbomEntity = hybridSbomPersistenceService.getTemporarySbom(FILE_NAME, null);

    assertThat(sbomEntity).isInstanceOf(S3SbomEntity.class);
    assertThat(sbomEntity.getLocation()).isEqualTo(s3SbomEntity.getLocation());
    assertThat(readSbom(sbomEntity)).isEqualTo(readSbom(s3SbomEntity));
  }

  @Test
  public void testGetTemporarySbom_ExistsInFileAndS3() throws Exception {
    createTemporarySbom(s3SbomPersistenceService, FILE_NAME, null);
    createTemporarySbom(fileSbomPersistenceService, FILE_NAME, null);
    SbomEntity expectedSbomEntity = getPrimaryPersistenceService().getTemporarySbom(FILE_NAME, null);

    SbomEntity sbomEntity = hybridSbomPersistenceService.getTemporarySbom(FILE_NAME, null);

    assertThat(sbomEntity).isInstanceOf(expectedSbomEntity.getClass());
    assertThat(sbomEntity.getLocation()).isEqualTo(expectedSbomEntity.getLocation());
    assertThat(readSbom(sbomEntity)).isEqualTo(readSbom(expectedSbomEntity));
  }

  @Test
  public void testGetTransientSbom() throws Exception {
    SbomEntity expectedSbomEntity = getPrimaryPersistenceService().getTransientSbom(FILE_NAME);

    SbomEntity sbomEntity = hybridSbomPersistenceService.getTransientSbom(FILE_NAME);

    assertThat(sbomEntity).isInstanceOf(expectedSbomEntity.getClass());
  }

  @Test
  public void testSaveTemporarySbom() throws Exception {
    SbomPersistenceService primaryPersistenceService = getPrimaryPersistenceService();

    SbomEntity sourceSbomEntity = createSbom(primaryPersistenceService, APP_ID, FILE_NAME);
    String content = readSbom(sourceSbomEntity);

    SbomEntity savedSbomEntity =
        hybridSbomPersistenceService.saveTemporarySbom(sourceSbomEntity, OTHER_FILE_NAME, null);

    assertThat(savedSbomEntity)
        .isInstanceOf(primaryPersistenceService.getTemporarySbom(OTHER_FILE_NAME, null).getClass());
    assertThat(readSbom(savedSbomEntity)).isEqualTo(content);
  }

  @Test
  public void testDeleteSbom_Entity_s3First() throws Exception {
    SbomEntity s3SbomEntity = createSbom(s3SbomPersistenceService, APP_ID, FILE_NAME);
    SbomEntity s3SbomEntityOtherAppId = createSbom(s3SbomPersistenceService, OTHER_APP_ID, FILE_NAME);
    SbomEntity s3SbomEntityOtherFileName = createSbom(s3SbomPersistenceService, APP_ID, OTHER_FILE_NAME);
    SbomEntity fileSbomEntity = createSbom(fileSbomPersistenceService, APP_ID, FILE_NAME);
    SbomEntity fileSbomEntityOtherAppId = createSbom(fileSbomPersistenceService, OTHER_APP_ID, FILE_NAME);
    SbomEntity fileSbomEntityOtherFileName = createSbom(fileSbomPersistenceService, APP_ID, OTHER_FILE_NAME);

    hybridSbomPersistenceService.deleteSbom(s3SbomEntity);

    assertThat(s3SbomEntity.exists()).isFalse();
    assertThat(s3SbomEntityOtherAppId.exists()).isTrue();
    assertThat(s3SbomEntityOtherFileName.exists()).isTrue();
    assertThat(fileSbomEntity.exists()).isTrue();
    assertThat(fileSbomEntityOtherAppId.exists()).isTrue();
    assertThat(fileSbomEntityOtherFileName.exists()).isTrue();

    hybridSbomPersistenceService.deleteSbom(fileSbomEntity);

    assertThat(s3SbomEntity.exists()).isFalse();
    assertThat(s3SbomEntityOtherAppId.exists()).isTrue();
    assertThat(s3SbomEntityOtherFileName.exists()).isTrue();
    assertThat(fileSbomEntity.exists()).isFalse();
    assertThat(fileSbomEntityOtherAppId.exists()).isTrue();
    assertThat(fileSbomEntityOtherFileName.exists()).isTrue();
  }

  @Test
  public void testDeleteSbom_Entity_fileFirst() throws Exception {
    SbomEntity s3SbomEntity = createSbom(s3SbomPersistenceService, APP_ID, FILE_NAME);
    SbomEntity s3SbomEntityOtherAppId = createSbom(s3SbomPersistenceService, OTHER_APP_ID, FILE_NAME);
    SbomEntity s3SbomEntityOtherFileName = createSbom(s3SbomPersistenceService, APP_ID, OTHER_FILE_NAME);
    SbomEntity fileSbomEntity = createSbom(fileSbomPersistenceService, APP_ID, FILE_NAME);
    SbomEntity fileSbomEntityOtherAppId = createSbom(fileSbomPersistenceService, OTHER_APP_ID, FILE_NAME);
    SbomEntity fileSbomEntityOtherFileName = createSbom(fileSbomPersistenceService, APP_ID, OTHER_FILE_NAME);

    hybridSbomPersistenceService.deleteSbom(fileSbomEntity);

    assertThat(s3SbomEntity.exists()).isTrue();
    assertThat(s3SbomEntityOtherAppId.exists()).isTrue();
    assertThat(s3SbomEntityOtherFileName.exists()).isTrue();
    assertThat(fileSbomEntity.exists()).isFalse();
    assertThat(fileSbomEntityOtherAppId.exists()).isTrue();
    assertThat(fileSbomEntityOtherFileName.exists()).isTrue();

    hybridSbomPersistenceService.deleteSbom(s3SbomEntity);

    assertThat(s3SbomEntity.exists()).isFalse();
    assertThat(s3SbomEntityOtherAppId.exists()).isTrue();
    assertThat(s3SbomEntityOtherFileName.exists()).isTrue();
    assertThat(fileSbomEntity.exists()).isFalse();
    assertThat(fileSbomEntityOtherAppId.exists()).isTrue();
    assertThat(fileSbomEntityOtherFileName.exists()).isTrue();
  }

  @Test
  public void testDeleteSbom_AppIdAndFileName() throws Exception {
    SbomEntity s3SbomEntity = createSbom(s3SbomPersistenceService, APP_ID, FILE_NAME);
    SbomEntity s3SbomEntityOtherAppId = createSbom(s3SbomPersistenceService, OTHER_APP_ID, FILE_NAME);
    SbomEntity s3SbomEntityOtherFileName = createSbom(s3SbomPersistenceService, APP_ID, OTHER_FILE_NAME);

    SbomEntity fileSbomEntity = createSbom(fileSbomPersistenceService, APP_ID, FILE_NAME);
    SbomEntity fileSbomEntityOtherAppId = createSbom(fileSbomPersistenceService, OTHER_APP_ID, FILE_NAME);
    SbomEntity fileSbomEntityOtherFileName = createSbom(fileSbomPersistenceService, APP_ID, OTHER_FILE_NAME);

    hybridSbomPersistenceService.deleteSbom(APP_ID, FILE_NAME);

    assertThat(s3SbomEntity.exists()).isFalse();
    assertThat(fileSbomEntity.exists()).isFalse();
    assertThat(s3SbomEntityOtherAppId.exists()).isTrue();
    assertThat(s3SbomEntityOtherFileName.exists()).isTrue();
    assertThat(fileSbomEntityOtherAppId.exists()).isTrue();
    assertThat(fileSbomEntityOtherFileName.exists()).isTrue();
  }

  @Test
  public void testDeleteSbomsFor() throws Exception {
    SbomEntity s3SbomEntity = createSbom(s3SbomPersistenceService, APP_ID, FILE_NAME);
    SbomEntity s3SbomEntityOtherAppId = createSbom(s3SbomPersistenceService, OTHER_APP_ID, FILE_NAME);
    SbomEntity s3SbomEntityOtherFileName = createSbom(s3SbomPersistenceService, APP_ID, OTHER_FILE_NAME);
    SbomEntity fileSbomEntity = createSbom(fileSbomPersistenceService, APP_ID, FILE_NAME);
    SbomEntity fileSbomEntityOtherAppId = createSbom(fileSbomPersistenceService, OTHER_APP_ID, FILE_NAME);
    SbomEntity fileSbomEntityOtherFileName = createSbom(fileSbomPersistenceService, APP_ID, OTHER_FILE_NAME);

    hybridSbomPersistenceService.deleteSbomsFor(APP_ID);

    assertThat(s3SbomEntity.exists()).isFalse();
    assertThat(s3SbomEntityOtherAppId.exists()).isTrue();
    assertThat(s3SbomEntityOtherFileName.exists()).isFalse();
    assertThat(fileSbomEntity.exists()).isFalse();
    assertThat(fileSbomEntityOtherAppId.exists()).isTrue();
    assertThat(fileSbomEntityOtherFileName.exists()).isFalse();
  }

  @Test
  public void testDeleteTransientSbomsOlderThan() throws Exception {
    // This test verifies that the method calls through to both underlying services
    Instant instant = Instant.now();

    hybridSbomPersistenceService.deleteTransientSbomsOlderThan(instant);

    // Verify that the method was called on both spied services with the correct parameter
    verify(s3SbomPersistenceServiceSpy).deleteTransientSbomsOlderThan(instant);
    verify(fileSbomPersistenceServiceSpy).deleteTransientSbomsOlderThan(instant);
  }

  @Test
  public void testDoGetSbom_ExistsOnlyInSecondary_WarnEnabled() throws Exception {
    enableWarning();
    assertWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridSbomPersistenceService.doGetSbom(APP_ID, FILE_NAME));
  }

  @Test
  public void testDoGetSbom_ExistsOnlyInSecondary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridSbomPersistenceService.doGetSbom(APP_ID, FILE_NAME));
  }

  @Test
  public void testGetTemporarySbom_ExistsOnlyInSecondary_WarnEnabled() throws Exception {
    enableWarning();
    assertWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridSbomPersistenceService.getTemporarySbom(APP_ID, "prefix"));
  }

  @Test
  public void testGetTemporarySbom_ExistsOnlyInSecondary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridSbomPersistenceService.getTemporarySbom(APP_ID, "prefix"));
  }

  @Test
  public void testDoGetSbom_ExistsOnlyInPrimary_WarnEnabled() throws Exception {
    enableWarning();
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridSbomPersistenceService.doGetSbom(APP_ID, FILE_NAME));
  }

  @Test
  public void testDoGetSbom_ExistsOnlyInPrimary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridSbomPersistenceService.doGetSbom(APP_ID, FILE_NAME));
  }

  @Test
  public void testGetTemporarySbom_ExistsOnlyInPrimary_WarnEnabled() throws Exception {
    enableWarning();
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridSbomPersistenceService.getTemporarySbom(APP_ID, "prefix"));
  }

  @Test
  public void testGetTemporarySbom_ExistsOnlyInPrimary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridSbomPersistenceService.getTemporarySbom(APP_ID, "prefix"));
  }

  private void enableWarning() {
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS, "true");
    hybridSbomPersistenceService.configurationChanged(
        Collections.singleton(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS));
  }

  private void assertWarning(final DataStoreType dataStoreType, final Callable<?> callable) throws Exception {
    SbomPersistenceService service = sbomPersistenceServiceProvider.get(dataStoreType);
    createSbom(service, APP_ID, FILE_NAME);
    createSbom(service.getTemporarySbom(APP_ID, "prefix"));

    callable.call();

    logOutput.assertThat().atWarnLevel().contains("Non-primary storage access");
  }

  private void assertNoWarning(final DataStoreType dataStoreType, final Callable<?> callable) throws Exception {
    SbomPersistenceService service = sbomPersistenceServiceProvider.get(dataStoreType);
    createSbom(service, APP_ID, FILE_NAME);
    createSbom(service.getTemporarySbom(APP_ID, "prefix"));

    callable.call();

    logOutput.assertThat().atWarnLevel().doesNotContain("Non-primary storage access");
  }

  private SbomPersistenceService getPrimaryPersistenceService() {
    DataStoreType primaryDatastoreType = dataStoreTypes.iterator().next();
    return sbomPersistenceServiceProvider.get(primaryDatastoreType);
  }

  private SbomEntity createSbom(
      final SbomPersistenceService sbomPersistenceService,
      final String appId,
      final String fileName) throws Exception
  {
    return createSbom(sbomPersistenceService.doGetSbom(appId, fileName));
  }

  private SbomEntity createTemporarySbom(
      final SbomPersistenceService sbomPersistenceService,
      final String fileName,
      final String prefix) throws Exception
  {
    return createSbom(sbomPersistenceService.getTemporarySbom(fileName, prefix));
  }

  private SbomEntity createSbom(final SbomEntity sbomEntity) throws Exception {
    assertThat(sbomEntity.exists()).isFalse();
    if (sbomEntity instanceof FileSbomEntity fileSbomEntity) {
      Files.createDirectories(fileSbomEntity.getPath().getParent());
    }
    String content = TemporaryEntity.uuid();
    try (OutputStream outputStream = sbomEntity.getOutputStream()) {
      outputStream.write(content.getBytes(StandardCharsets.UTF_8));
    }
    assertThat(sbomEntity.exists()).isTrue();
    assertThat(readSbom(sbomEntity)).isEqualTo(content);
    return sbomEntity;
  }

  private String readSbom(final SbomEntity sbomEntity) throws Exception {
    try (InputStream inputStream = sbomEntity.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
