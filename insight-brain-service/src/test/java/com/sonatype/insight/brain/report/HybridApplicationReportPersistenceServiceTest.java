/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;
import jakarta.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.report.ApplicationReport.ReportFile;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.HybridDataStoreConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.test.ContainerRule;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.After;
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

@Category(SlowTest.class)
@RunWith(Parameterized.class)
public class HybridApplicationReportPersistenceServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(HybridApplicationReportPersistenceService.class);

  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-scan-bucket";

  private static final String REGION = "us-east-2";

  private static final String APP_ID = "appId";

  private static final String OTHER_APP_ID = "otherAppId";

  private static final String SCAN_ID = "scanId";

  private static final String OTHER_SCAN_ID = "otherScanId";

  private static final String NAME = "someName";

  private static final String OTHER_NAME = "someOtherName";

  @ClassRule
  public static ContainerRule<LocalStackContainer> localstack =
      new ContainerRule<>(new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.S3));

  @Inject
  private S3Client s3Client;

  @Inject
  private S3ApplicationReportPersistenceService s3ApplicationReportPersistenceService;

  @Inject
  private FileApplicationReportPersistenceService fileApplicationReportPersistenceService;

  @Inject
  private HybridApplicationReportPersistenceService hybridApplicationReportPersistenceService;

  @Inject
  private ApplicationReportPersistenceServiceProvider applicationReportPersistenceServiceProvider;

  @Inject
  private InsightWork insightWork;

  @Inject
  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Parameters
  public static List<Object[]> dataStoreTypes() {
    return Arrays.asList(new Object[][]{
        {new LinkedHashSet<>(List.of(DataStoreType.S3, DataStoreType.FILE))},
        {new LinkedHashSet<>(List.of(DataStoreType.FILE, DataStoreType.S3))},
        });
  }

  private final LinkedHashSet<DataStoreType> dataStoreTypes;

  public HybridApplicationReportPersistenceServiceTest(final LinkedHashSet<DataStoreType> dataStoreTypes) {
    this.dataStoreTypes = dataStoreTypes;
  }

  @Override
  public void configure(final Binder binder) {
    super.configure(binder);
    AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
        localstack.getContainer().getAccessKey(),
        localstack.getContainer().getSecretKey()
    ));
    binder.bind(AwsCredentialsProvider.class).toInstance(awsCredentialsProvider);
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
    s3DataStoreConfig.setEndpoint(localstack.getContainer().getEndpoint());
    storageConfig.setS3Config(s3DataStoreConfig);
  }

  @BeforeClass
  public static void createBucket() {
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
  public void testDoGetReportEntity_DoesNotExist() throws Exception {
    try {
      ReportEntity reportEntity = hybridApplicationReportPersistenceService.doGetReportEntity(APP_ID, SCAN_ID, NAME);

      assertThat(reportEntity).isInstanceOf(
          applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next()).getReportEntityClass());
      assertThat(reportEntity.exists()).isFalse();
    }
    catch (NoSuchFileException e) {
      // If we use FileApplicationReportPersistenceService and try to call doGetReportEntity on a non-existing report,
      // then we get NoSuchFileException when it tries to open the zip
      // this is the expected behavior.
      // Note that this only happens if we've already determined the report entity does not exist on any other storage
      // mechanism and if file-based storage is the preferred storage
    }
  }

  @Test
  public void testDoGetReportEntity_ExistsOnlyInFile() throws Exception {
    ReportEntity expectedReportEntity =
        createReportEntity(fileApplicationReportPersistenceService, APP_ID, SCAN_ID, NAME);

    ReportEntity reportEntity = hybridApplicationReportPersistenceService.doGetReportEntity(APP_ID, SCAN_ID, NAME);

    assertThat(reportEntity).isInstanceOf(expectedReportEntity.getClass());
    assertThat(reportEntity.exists()).isTrue();
    assertThat(readReportEntity(reportEntity)).isEqualTo(readReportEntity(expectedReportEntity));
  }

  @Test
  public void testDoGetReportEntity_ExistsOnlyInS3() throws Exception {
    ReportEntity expectedReportEntity =
        createReportEntity(s3ApplicationReportPersistenceService, APP_ID, SCAN_ID, NAME);

    ReportEntity reportEntity = hybridApplicationReportPersistenceService.doGetReportEntity(APP_ID, SCAN_ID, NAME);

    assertThat(reportEntity).isInstanceOf(expectedReportEntity.getClass());
    assertThat(reportEntity.exists()).isTrue();
    assertThat(readReportEntity(reportEntity)).isEqualTo(readReportEntity(expectedReportEntity));
  }

  @Test
  public void testDoGetReportEntity_ExistsInFileAndS3() throws Exception {
    createReportEntity(fileApplicationReportPersistenceService, APP_ID, SCAN_ID, NAME);
    createReportEntity(s3ApplicationReportPersistenceService, APP_ID, SCAN_ID, NAME);
    ReportEntity expectedReportEntity =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next())
            .doGetReportEntity(APP_ID, SCAN_ID, NAME);

    ReportEntity reportEntity = hybridApplicationReportPersistenceService.doGetReportEntity(APP_ID, SCAN_ID, NAME);

    assertThat(reportEntity).isInstanceOf(expectedReportEntity.getClass());
    assertThat(reportEntity.exists()).isTrue();
    assertThat(readReportEntity(reportEntity)).isEqualTo(readReportEntity(expectedReportEntity));
  }

  @Test
  public void testDoGetReportEntity_ExistsOnlyInSecondary_WarnEnabled() throws Exception {
    enableWarning();
    assertWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridApplicationReportPersistenceService.doGetReportEntity(APP_ID, SCAN_ID, NAME));
  }

  @Test
  public void testDoGetReportEntity_ExistsOnlyInSecondary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridApplicationReportPersistenceService.doGetReportEntity(APP_ID, SCAN_ID, NAME));
  }

  @Test
  public void testGetAllReportEntities_ExistsOnlyInSecondary_WarnEnabled() throws Exception {
    enableWarning();
    assertWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridApplicationReportPersistenceService.getAllReportEntities(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetAllReportEntities_ExistsOnlyInSecondary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridApplicationReportPersistenceService.getAllReportEntities(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetPdfEntity_ExistsOnlyInSecondary_WarnEnabled() throws Exception {
    enableWarning();
    assertWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetPdfEntity_ExistsOnlyInSecondary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetVulnerabilitySignaturesEntity_ExistsOnlyInSecondary_WarnEnabled() throws Exception {
    enableWarning();
    assertWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetVulnerabilitySignaturesEntity_ExistsOnlyInSecondary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID));
  }

  @Test
  public void testDoGetReportEntity_ExistsOnlyInPrimary_WarnEnabled() throws Exception {
    enableWarning();
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridApplicationReportPersistenceService.doGetReportEntity(APP_ID, SCAN_ID, NAME));
  }

  @Test
  public void testDoGetReportEntity_ExistsOnlyInPrimary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridApplicationReportPersistenceService.doGetReportEntity(APP_ID, SCAN_ID, NAME));
  }

  @Test
  public void testGetAllReportEntities_ExistsOnlyInPrimary_WarnEnabled() throws Exception {
    enableWarning();
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridApplicationReportPersistenceService.getAllReportEntities(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetAllReportEntities_ExistsOnlyInPrimary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridApplicationReportPersistenceService.getAllReportEntities(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetPdfEntity_ExistsOnlyInPrimary_WarnEnabled() throws Exception {
    enableWarning();
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetPdfEntity_ExistsOnlyInPrimary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetVulnerabilitySignaturesEntity_ExistsOnlyInPrimary_WarnEnabled() throws Exception {
    enableWarning();
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetVulnerabilitySignaturesEntity_ExistsOnlyInPrimary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID));
  }

  private void enableWarning() {
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS, "true");
    hybridApplicationReportPersistenceService.configurationChanged(
        Collections.singleton(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS));
  }

  private void assertWarning(final DataStoreType dataStoreType, final Callable<?> callable) throws Exception {
    ApplicationReportPersistenceService service = applicationReportPersistenceServiceProvider.get(dataStoreType);
    createReportEntity(service, APP_ID, SCAN_ID, NAME);
    createReportEntity(service.getPdfEntity(APP_ID, SCAN_ID));
    createReportEntity(service.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID));

    callable.call();

    logOutput.assertThat().atWarnLevel().contains("Non-primary storage access");
  }

  private void assertNoWarning(final DataStoreType dataStoreType, final Callable<?> callable) throws Exception {
    ApplicationReportPersistenceService service = applicationReportPersistenceServiceProvider.get(dataStoreType);
    createReportEntity(service, APP_ID, SCAN_ID, NAME);
    createReportEntity(service.getPdfEntity(APP_ID, SCAN_ID));
    createReportEntity(service.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID));

    callable.call();

    logOutput.assertThat().atWarnLevel().doesNotContain("Non-primary storage access");
  }

  @Test
  public void testGetAllReportEntities_DoesNotExist() throws Exception {
    assertThat(hybridApplicationReportPersistenceService.getAllReportEntities(APP_ID, SCAN_ID).toList()).isEmpty();
  }

  @Test
  public void testGetAllReportEntities_ExistsOnlyInFile() throws Exception {
    createReportEntity(fileApplicationReportPersistenceService, APP_ID, SCAN_ID, NAME);
    createReportEntity(fileApplicationReportPersistenceService, APP_ID, SCAN_ID, OTHER_NAME);

    List<ReportEntity> allReportEntities =
        hybridApplicationReportPersistenceService.getAllReportEntities(APP_ID, SCAN_ID).toList();

    assertThat(allReportEntities).hasSize(2);
    assertThat(allReportEntities).extracting(ReportEntity::getName)
        .containsExactlyInAnyOrder(NAME, OTHER_NAME);
  }

  @Test
  public void testGetAllReportEntities_ExistsOnlyInS3() throws Exception {
    createReportEntity(s3ApplicationReportPersistenceService, APP_ID, SCAN_ID, NAME);
    createReportEntity(s3ApplicationReportPersistenceService, APP_ID, SCAN_ID, OTHER_NAME);

    List<ReportEntity> allReportEntities =
        hybridApplicationReportPersistenceService.getAllReportEntities(APP_ID, SCAN_ID).toList();

    assertThat(allReportEntities).hasSize(2);
    assertThat(allReportEntities).extracting(ReportEntity::getName)
        .containsExactlyInAnyOrder(NAME, OTHER_NAME);
  }

  @Test
  public void testGetAllReportEntities_ExistsInFileAndS3() throws Exception {
    createReportEntity(fileApplicationReportPersistenceService, APP_ID, SCAN_ID, NAME);
    createReportEntity(fileApplicationReportPersistenceService, APP_ID, SCAN_ID, OTHER_NAME);
    createReportEntity(s3ApplicationReportPersistenceService, APP_ID, SCAN_ID, NAME);
    createReportEntity(s3ApplicationReportPersistenceService, APP_ID, SCAN_ID, OTHER_NAME);

    List<ReportEntity> allReportEntities =
        hybridApplicationReportPersistenceService.getAllReportEntities(APP_ID, SCAN_ID).toList();

    assertThat(allReportEntities).hasSize(2);
    Class<? extends ReportEntity> reportEntityClass =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next()).getReportEntityClass();
    assertThat(allReportEntities).allMatch(r -> r.getClass().isAssignableFrom(reportEntityClass));
    assertThat(allReportEntities).extracting(ReportEntity::getName)
        .containsExactlyInAnyOrder(NAME, OTHER_NAME);
  }

  @Test
  public void testSaveOriginalReport() throws Exception {
    Path zip = createEmptyZip();
    Path entry = zip.resolve(ReportFile.BOM_JSON.getName());
    String content = "someContent";
    ReportHelper.addToZip(zip, entry, content);

    try (var zipStream = Files.newInputStream(zip)) {
      hybridApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }

    ApplicationReportPersistenceService applicationReportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next());
    ReportEntity reportEntity =
        applicationReportPersistenceService.getReportEntity(APP_ID, SCAN_ID, entry.getFileName().toString());
    assertThat(reportEntity.exists()).isTrue();
    assertThat(readReportEntity(reportEntity)).isEqualTo(content);
  }

  @Test
  public void testMoveReport() throws Exception {
    Path zip = createEmptyZip();
    Path entry = zip.resolve(ReportFile.BOM_JSON.getName());
    String content = "someContent";
    ReportHelper.addToZip(zip, entry, content);
    try (var zipStream = Files.newInputStream(zip)) {
      hybridApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }

    hybridApplicationReportPersistenceService.moveReport(APP_ID, SCAN_ID, OTHER_SCAN_ID);

    ApplicationReportPersistenceService applicationReportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next());
    ReportEntity reportEntity =
        applicationReportPersistenceService.getReportEntity(APP_ID, OTHER_SCAN_ID, entry.getFileName().toString());
    assertThat(reportEntity.exists()).isTrue();
    assertThat(readReportEntity(reportEntity)).isEqualTo(content);
  }

  @Test
  public void testDoSaveReportFile() throws Exception {
    String entry = ReportFile.BOM_JSON.getName();
    String content = "someContent";

    hybridApplicationReportPersistenceService.doSaveReportFile(
        APP_ID,
        SCAN_ID,
        entry,
        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
    );

    ApplicationReportPersistenceService applicationReportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next());
    ReportEntity reportEntity = applicationReportPersistenceService.getReportEntity(APP_ID, SCAN_ID, entry);
    assertThat(reportEntity.exists()).isTrue();
    assertThat(readReportEntity(reportEntity)).isEqualTo(content);
  }

  @Test
  public void testDoSaveAdditionalReportFile() throws Exception {
    String entry = ReportFile.THIRD_PARTY_LICENSE_JSON.getName();
    String content = "someContent";

    hybridApplicationReportPersistenceService.doSaveAdditionalReportFile(
        APP_ID,
        SCAN_ID,
        entry,
        new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8))
    );

    ApplicationReportPersistenceService applicationReportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next());
    ReportEntity reportEntity = applicationReportPersistenceService.getReportEntity(APP_ID, SCAN_ID, entry);
    assertThat(reportEntity.exists()).isTrue();
    assertThat(readReportEntity(reportEntity)).isEqualTo(content);
  }

  @Test
  public void testGetPdfEntity_DoesNotExist() throws Exception {
    ReportPdfEntity reportPdfEntity = hybridApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID);

    ApplicationReportPersistenceService applicationReportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next());
    assertThat(reportPdfEntity.getApplicationReportPersistenceServiceClass()).isEqualTo(
        applicationReportPersistenceService.getClass());
    assertThat(reportPdfEntity.exists()).isFalse();
  }

  @Test
  public void testGetPdfEntity_ExistsOnlyInFile() throws Exception {
    Files.createDirectories(insightWork.getReportDir(APP_ID, SCAN_ID).toPath());
    ReportPdfEntity expectedReportPdfEntity = fileApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID);
    createReportEntity(expectedReportPdfEntity);

    ReportPdfEntity reportPdfEntity = hybridApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID);

    assertThat(reportPdfEntity.getApplicationReportPersistenceServiceClass()).isEqualTo(
        expectedReportPdfEntity.getApplicationReportPersistenceServiceClass());
    assertThat(reportPdfEntity.exists()).isTrue();
    assertThat(readReportEntity(reportPdfEntity)).isEqualTo(readReportEntity(expectedReportPdfEntity));
  }

  @Test
  public void testGetPdfEntity_ExistsOnlyInS3() throws Exception {
    ReportPdfEntity expectedReportPdfEntity = s3ApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID);
    createReportEntity(expectedReportPdfEntity);

    ReportPdfEntity reportPdfEntity = hybridApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID);

    assertThat(reportPdfEntity.getApplicationReportPersistenceServiceClass()).isEqualTo(
        expectedReportPdfEntity.getApplicationReportPersistenceServiceClass());
    assertThat(reportPdfEntity.exists()).isTrue();
    assertThat(readReportEntity(reportPdfEntity)).isEqualTo(readReportEntity(expectedReportPdfEntity));
  }

  @Test
  public void testGetPdfEntity_ExistsInFileAndS3() throws Exception {
    Files.createDirectories(insightWork.getReportDir(APP_ID, SCAN_ID).toPath());
    createReportEntity(fileApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID));
    createReportEntity(s3ApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID));
    ApplicationReportPersistenceService applicationReportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next());
    ReportPdfEntity expectedReportPdfEntity = applicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID);

    ReportPdfEntity reportPdfEntity = hybridApplicationReportPersistenceService.getPdfEntity(APP_ID, SCAN_ID);

    assertThat(reportPdfEntity.getApplicationReportPersistenceServiceClass()).isEqualTo(
        expectedReportPdfEntity.getApplicationReportPersistenceServiceClass());
    assertThat(reportPdfEntity.exists()).isTrue();
    assertThat(readReportEntity(reportPdfEntity)).isEqualTo(readReportEntity(expectedReportPdfEntity));
  }

  @Test
  public void testGetVulnerabilitySignaturesEntity_DoesNotExist() throws Exception {
    BaseReportEntity reportVulnerabilitySignaturesEntity =
        hybridApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID);

    ApplicationReportPersistenceService applicationReportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next());
    assertThat(reportVulnerabilitySignaturesEntity.getApplicationReportPersistenceServiceClass()).isEqualTo(
        applicationReportPersistenceService.getClass());
    assertThat(reportVulnerabilitySignaturesEntity.exists()).isFalse();
  }

  @Test
  public void testGetVulnerabilitySignaturesEntity_ExistsOnlyInFile() throws Exception {
    Files.createDirectories(insightWork.getReportDir(APP_ID, SCAN_ID).toPath());
    BaseReportEntity expectedReportVulnerabilitySignaturesEntity =
        fileApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID);
    createReportEntity(expectedReportVulnerabilitySignaturesEntity);

    BaseReportEntity reportVulnerabilitySignaturesEntity =
        hybridApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID);

    assertThat(reportVulnerabilitySignaturesEntity.getApplicationReportPersistenceServiceClass()).isEqualTo(
        expectedReportVulnerabilitySignaturesEntity.getApplicationReportPersistenceServiceClass());
    assertThat(reportVulnerabilitySignaturesEntity.exists()).isTrue();
    assertThat(readReportEntity(reportVulnerabilitySignaturesEntity)).isEqualTo(
        readReportEntity(expectedReportVulnerabilitySignaturesEntity));
  }

  @Test
  public void testGetVulnerabilitySignaturesEntity_ExistsOnlyInS3() throws Exception {
    BaseReportEntity expectedReportVulnerabilitySignaturesEntity =
        s3ApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID);
    createReportEntity(expectedReportVulnerabilitySignaturesEntity);

    BaseReportEntity reportVulnerabilitySignaturesEntity =
        hybridApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID);

    assertThat(reportVulnerabilitySignaturesEntity.getApplicationReportPersistenceServiceClass()).isEqualTo(
        expectedReportVulnerabilitySignaturesEntity.getApplicationReportPersistenceServiceClass());
    assertThat(reportVulnerabilitySignaturesEntity.exists()).isTrue();
    assertThat(readReportEntity(reportVulnerabilitySignaturesEntity)).isEqualTo(
        readReportEntity(expectedReportVulnerabilitySignaturesEntity));
  }

  @Test
  public void testGetVulnerabilitySignaturesEntity_ExistsInFileAndS3() throws Exception {
    Files.createDirectories(insightWork.getReportDir(APP_ID, SCAN_ID).toPath());
    createReportEntity(fileApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID));
    createReportEntity(s3ApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID));
    ApplicationReportPersistenceService applicationReportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next());
    BaseReportEntity expectedReportVulnerabilitySignaturesEntity =
        applicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID);

    BaseReportEntity reportVulnerabilitySignaturesEntity =
        hybridApplicationReportPersistenceService.getVulnerabilitySignaturesEntity(APP_ID, SCAN_ID);

    assertThat(reportVulnerabilitySignaturesEntity.getApplicationReportPersistenceServiceClass()).isEqualTo(
        expectedReportVulnerabilitySignaturesEntity.getApplicationReportPersistenceServiceClass());
    assertThat(reportVulnerabilitySignaturesEntity.exists()).isTrue();
    assertThat(readReportEntity(reportVulnerabilitySignaturesEntity)).isEqualTo(
        readReportEntity(expectedReportVulnerabilitySignaturesEntity));
  }

  @Test
  public void testGetReportLocation_DoesNotExist() {
    String reportLocation = hybridApplicationReportPersistenceService.getReportLocation(APP_ID, SCAN_ID);

    ApplicationReportPersistenceService applicationReportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next());
    assertThat(reportLocation).isEqualTo(applicationReportPersistenceService.getReportLocation(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetReportLocation_ExistsOnlyInFile() throws Exception {
    Path zip = createEmptyZip();
    Path entry = zip.resolve(ReportFile.BOM_JSON.getName());
    String content = "someContent";
    ReportHelper.addToZip(zip, entry, content);
    try (var zipStream = Files.newInputStream(zip)) {
      fileApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }

    String reportLocation = hybridApplicationReportPersistenceService.getReportLocation(APP_ID, SCAN_ID);

    assertThat(reportLocation).isEqualTo(fileApplicationReportPersistenceService.getReportLocation(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetReportLocation_ExistsOnlyInS3() throws Exception {
    Path zip = createEmptyZip();
    Path entry = zip.resolve(ReportFile.BOM_JSON.getName());
    String content = "someContent";
    ReportHelper.addToZip(zip, entry, content);
    try (var zipStream = Files.newInputStream(zip)) {
      s3ApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }

    String reportLocation = hybridApplicationReportPersistenceService.getReportLocation(APP_ID, SCAN_ID);

    assertThat(reportLocation).isEqualTo(s3ApplicationReportPersistenceService.getReportLocation(APP_ID, SCAN_ID));
  }

  @Test
  public void testGetReportLocation_ExistsInFileAndS3() throws Exception {
    Path zip = createEmptyZip();
    Path entry = zip.resolve(ReportFile.BOM_JSON.getName());
    String content = "someContent";
    ReportHelper.addToZip(zip, entry, content);
    try (var zipStream = Files.newInputStream(zip)) {
      fileApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      s3ApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }

    String reportLocation = hybridApplicationReportPersistenceService.getReportLocation(APP_ID, SCAN_ID);

    ApplicationReportPersistenceService applicationReportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.iterator().next());
    assertThat(reportLocation).isEqualTo(applicationReportPersistenceService.getReportLocation(APP_ID, SCAN_ID));
  }

  @Test
  public void testReportExists_DoesNotExist() throws Exception {
    assertThat(hybridApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isFalse();
  }

  @Test
  public void testReportExists_ExistsOnlyInFile() throws Exception {
    Path zip = createEmptyZip();
    Path entry = zip.resolve(ReportFile.BOM_JSON.getName());
    String content = "someContent";
    ReportHelper.addToZip(zip, entry, content);
    try (var zipStream = Files.newInputStream(zip)) {
      fileApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }

    assertThat(hybridApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isTrue();
  }

  @Test
  public void testReportExists_ExistsOnlyInS3() throws Exception {
    Path zip = createEmptyZip();
    Path entry = zip.resolve(ReportFile.BOM_JSON.getName());
    String content = "someContent";
    ReportHelper.addToZip(zip, entry, content);
    try (var zipStream = Files.newInputStream(zip)) {
      s3ApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }

    assertThat(hybridApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isTrue();
  }

  @Test
  public void testReportExists_ExistsInFileAndS3() throws Exception {
    Path zip = createEmptyZip();
    Path entry = zip.resolve(ReportFile.BOM_JSON.getName());
    String content = "someContent";
    ReportHelper.addToZip(zip, entry, content);
    try (var zipStream = Files.newInputStream(zip)) {
      fileApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      s3ApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }

    assertThat(hybridApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isTrue();
  }

  @Test
  public void testDeleteReport() throws Exception {
    Path zip = createEmptyZip();
    Path entry = zip.resolve(ReportFile.BOM_JSON.getName());
    String content = "someContent";
    ReportHelper.addToZip(zip, entry, content);
    try (var zipStream = Files.newInputStream(zip)) {
      fileApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      fileApplicationReportPersistenceService.saveOriginalReport(OTHER_APP_ID, SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      fileApplicationReportPersistenceService.saveOriginalReport(APP_ID, OTHER_SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      s3ApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      s3ApplicationReportPersistenceService.saveOriginalReport(OTHER_APP_ID, SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      s3ApplicationReportPersistenceService.saveOriginalReport(APP_ID, OTHER_SCAN_ID, zipStream);
    }
    assertThat(fileApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isTrue();
    assertThat(s3ApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isTrue();
    assertThat(fileApplicationReportPersistenceService.reportExists(APP_ID, OTHER_SCAN_ID)).isTrue();
    assertThat(fileApplicationReportPersistenceService.reportExists(OTHER_APP_ID, SCAN_ID)).isTrue();
    assertThat(s3ApplicationReportPersistenceService.reportExists(APP_ID, OTHER_SCAN_ID)).isTrue();
    assertThat(s3ApplicationReportPersistenceService.reportExists(OTHER_APP_ID, SCAN_ID)).isTrue();

    hybridApplicationReportPersistenceService.deleteReport(APP_ID, SCAN_ID);

    assertThat(fileApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isFalse();
    assertThat(s3ApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isFalse();
    assertThat(fileApplicationReportPersistenceService.reportExists(APP_ID, OTHER_SCAN_ID)).isTrue();
    assertThat(fileApplicationReportPersistenceService.reportExists(OTHER_APP_ID, SCAN_ID)).isTrue();
    assertThat(s3ApplicationReportPersistenceService.reportExists(APP_ID, OTHER_SCAN_ID)).isTrue();
    assertThat(s3ApplicationReportPersistenceService.reportExists(OTHER_APP_ID, SCAN_ID)).isTrue();
  }

  @Test
  public void testDeleteReports() throws Exception {
    Path zip = createEmptyZip();
    Path entry = zip.resolve(ReportFile.BOM_JSON.getName());
    String content = "someContent";
    ReportHelper.addToZip(zip, entry, content);
    try (var zipStream = Files.newInputStream(zip)) {
      fileApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      fileApplicationReportPersistenceService.saveOriginalReport(APP_ID, OTHER_SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      fileApplicationReportPersistenceService.saveOriginalReport(OTHER_APP_ID, SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      fileApplicationReportPersistenceService.saveOriginalReport(OTHER_APP_ID, OTHER_SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      s3ApplicationReportPersistenceService.saveOriginalReport(APP_ID, SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      s3ApplicationReportPersistenceService.saveOriginalReport(APP_ID, OTHER_SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      s3ApplicationReportPersistenceService.saveOriginalReport(OTHER_APP_ID, SCAN_ID, zipStream);
    }
    try (var zipStream = Files.newInputStream(zip)) {
      s3ApplicationReportPersistenceService.saveOriginalReport(OTHER_APP_ID, OTHER_SCAN_ID, zipStream);
    }
    assertThat(fileApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isTrue();
    assertThat(fileApplicationReportPersistenceService.reportExists(APP_ID, OTHER_SCAN_ID)).isTrue();
    assertThat(fileApplicationReportPersistenceService.reportExists(OTHER_APP_ID, SCAN_ID)).isTrue();
    assertThat(fileApplicationReportPersistenceService.reportExists(OTHER_APP_ID, OTHER_SCAN_ID)).isTrue();
    assertThat(s3ApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isTrue();
    assertThat(s3ApplicationReportPersistenceService.reportExists(APP_ID, OTHER_SCAN_ID)).isTrue();
    assertThat(s3ApplicationReportPersistenceService.reportExists(OTHER_APP_ID, SCAN_ID)).isTrue();
    assertThat(s3ApplicationReportPersistenceService.reportExists(OTHER_APP_ID, OTHER_SCAN_ID)).isTrue();

    hybridApplicationReportPersistenceService.deleteReports(APP_ID);

    assertThat(fileApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isFalse();
    assertThat(fileApplicationReportPersistenceService.reportExists(APP_ID, OTHER_SCAN_ID)).isFalse();
    assertThat(fileApplicationReportPersistenceService.reportExists(OTHER_APP_ID, SCAN_ID)).isTrue();
    assertThat(fileApplicationReportPersistenceService.reportExists(OTHER_APP_ID, OTHER_SCAN_ID)).isTrue();
    assertThat(s3ApplicationReportPersistenceService.reportExists(APP_ID, SCAN_ID)).isFalse();
    assertThat(s3ApplicationReportPersistenceService.reportExists(APP_ID, OTHER_SCAN_ID)).isFalse();
    assertThat(s3ApplicationReportPersistenceService.reportExists(OTHER_APP_ID, SCAN_ID)).isTrue();
    assertThat(s3ApplicationReportPersistenceService.reportExists(OTHER_APP_ID, OTHER_SCAN_ID)).isTrue();
  }

  private ReportEntity createReportEntity(
      final ApplicationReportPersistenceService applicationReportPersistenceService,
      final String appId,
      final String scanId,
      final String name) throws Exception
  {
    if (applicationReportPersistenceService instanceof FileApplicationReportPersistenceService) {
      Path reportDir = insightWork.getReportDir(appId, scanId).toPath();
      Path zipPath = reportDir.resolve("report.zip");
      ReportHelper.createEmptyZip(zipPath);
    }
    return createReportEntity(applicationReportPersistenceService.getReportEntity(appId, scanId, name));
  }

  private <T extends BaseReportEntity> T createReportEntity(final T reportEntity) throws Exception {
    assertThat(reportEntity.exists()).isFalse();
    String content = TemporaryEntity.uuid();
    try (OutputStream outputStream = reportEntity.getOutputStream()) {
      outputStream.write(content.getBytes(StandardCharsets.UTF_8));
    }
    assertThat(reportEntity.exists()).isTrue();
    assertThat(readReportEntity(reportEntity)).isEqualTo(content);
    return reportEntity;
  }

  private String readReportEntity(final BaseReportEntity reportEntity) throws Exception {
    try (InputStream inputStream = reportEntity.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private Path createEmptyZip() throws Exception {
    Path path = tempDir.newFile().toPath();
    Files.delete(path);
    ReportHelper.createEmptyZip(path);
    return path;
  }
}
