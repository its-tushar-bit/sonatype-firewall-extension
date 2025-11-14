/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadataStatus;
import com.sonatype.insight.brain.report.ApplicationReport.ReportFile;
import com.sonatype.insight.brain.report.ApplicationReport.ReportFileLocationType;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceServiceProvider;
import com.sonatype.insight.brain.report.ReportEntity;
import com.sonatype.insight.brain.report.ReportPdfEntity;
import com.sonatype.insight.brain.sbom.datastore.SbomEntity;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceServiceProvider;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceProvider;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.HybridDataStoreConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.test.LogOutput;

import ch.qos.logback.classic.Level;
import com.google.inject.Binder;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;
import static software.amazon.awssdk.transfer.s3.SizeConstant.MB;

@Category(SlowTest.class)
@RunWith(Parameterized.class)
public class CopyStorageServiceTest
    extends AbstractComponentTest
{
  private static final Logger log = LoggerFactory.getLogger(CopyStorageServiceTest.class);

  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-scan-bucket";

  private static final String REGION = "us-east-2";

  @ClassRule
  public static LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.S3);

  @Rule
  public LogOutput logOutput = new LogOutput(CopyStorageService.class);

  @Inject
  private S3Client s3Client;

  @Inject
  private ScanPersistenceServiceProvider scanPersistenceServiceProvider;

  @Inject
  private ApplicationReportPersistenceServiceProvider applicationReportPersistenceServiceProvider;

  @Inject
  private SbomPersistenceServiceProvider sbomPersistenceServiceProvider;

  @Inject
  private CopyStorageService copyStorageService;

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private ClusterLockManager clusterLockManager;

  @Inject
  private ApiConfigurationService apiConfigurationService;

  @Mock
  private TaskScheduler mockTaskScheduler;

  @Inject
  private PolicyEvaluationDAO policyEvaluationDAO;

  @Inject
  private ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  @Parameters
  public static List<Object[]> dataStoreTypes() {
    return Arrays.asList(new Object[][]{
        {List.of(DataStoreType.S3, DataStoreType.FILE)},
        {List.of(DataStoreType.FILE, DataStoreType.S3)},
        });
  }

  private final List<DataStoreType> dataStoreTypes;

  public CopyStorageServiceTest(final List<DataStoreType> dataStoreTypes) {
    this.dataStoreTypes = dataStoreTypes;
  }

  @Override
  public void configure(final Binder binder) {
    binder.bind(TaskScheduler.class).toInstance(mockTaskScheduler);
    super.configure(binder);
    AwsCredentialsProvider awsCredentialsProvider = StaticCredentialsProvider.create(AwsBasicCredentials.create(
        localstack.getAccessKey(),
        localstack.getSecretKey()
    ));
    binder.bind(AwsCredentialsProvider.class).toInstance(awsCredentialsProvider);
  }

  @Override
  protected void customizeConfig(final InsightConfig insightConfig) {
    StorageConfig storageConfig = insightConfig.getStorage();
    HybridDataStoreConfig hybridDataStoreConfig = new HybridDataStoreConfig();
    hybridDataStoreConfig.setTypes(new LinkedHashSet<>(dataStoreTypes));
    storageConfig.setHybridConfig(hybridDataStoreConfig);
    storageConfig.setType(DataStoreType.HYBRID);

    S3DataStoreConfig s3DataStoreConfig = new S3DataStoreConfig();
    s3DataStoreConfig.setBucketName(BUCKET_NAME);
    s3DataStoreConfig.setRegion(REGION);
    s3DataStoreConfig.setEndpoint(localstack.getEndpoint());
    storageConfig.setS3Config(s3DataStoreConfig);
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
    s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(BUCKET_NAME).build())
        .contents()
        .forEach(obj -> s3Client.deleteObject(DeleteObjectRequest.builder()
            .bucket(BUCKET_NAME).key(obj.key()).build()));
  }

  @Test
  public void testExecute_UnsupportedFrom() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> copyStorageService.execute(DataStoreType.HYBRID, dataStoreTypes.get(0)));
  }

  @Test
  public void testExecute_UnsupportedTo() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> copyStorageService.execute(dataStoreTypes.get(1), DataStoreType.HYBRID));
  }

  @Test
  public void testExecute_PrimaryStorageNotTo() {
    StorageConfig storageConfig = insightConfig.getStorage();
    storageConfig.setType(dataStoreTypes.get(1));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0)))
        .withMessageContaining(("Primary storage type is '%s' but copy is targeting '%s'," +
            " scans, reports, and/or SBOMs written during copy may be missed.").formatted(dataStoreTypes.get(1),
            dataStoreTypes.get(0)));

    storageConfig.setType(DataStoreType.HYBRID);
    storageConfig.getHybridConfig()
        .setTypes(new LinkedHashSet<>(List.of(dataStoreTypes.get(1), dataStoreTypes.get(0))));
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0)))
        .withMessageContaining(("Primary storage type is '%s' but copy is targeting '%s'," +
            " scans, reports, and/or SBOMs written during copy may be missed.").formatted(dataStoreTypes.get(1),
            dataStoreTypes.get(0)));
  }

  @Test
  public void testExecute_FromAndToShouldBeDifferent() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> copyStorageService.execute(dataStoreTypes.get(0), dataStoreTypes.get(0)))
        .withMessageContaining(
            "Not copying from '%s' to '%s', these should be different.".formatted(dataStoreTypes.get(0),
                dataStoreTypes.get(0)));
  }

  @Test
  public void testExecute_NoApps() {
    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    logOutput.assertThat().atInfoLevel().contains(
        String.format("Finished copy of scans, reports, and SBOMs from '%s' to '%s': CopyStorageResult",
            dataStoreTypes.get(1).name(), dataStoreTypes.get(0).name())
    );
  }

  @Test
  public void testExecute_NoData() {
    tempEntity.newApplicationWithParent();

    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    logOutput.assertThat().atInfoLevel().contains(
        String.format("Finished copy of scans, reports, and SBOMs from '%s' to '%s': CopyStorageResult",
            dataStoreTypes.get(1).name(), dataStoreTypes.get(0).name())
    );
  }

  @Test
  public void testExecute_WithData() throws Exception {
    Application app1 = tempEntity.newApplicationWithParent();
    // App 1 first scan, report, sbom
    PolicyEvaluation app1Eval1 =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app1Eval1);
    createReport(app1Eval1);
    ThirdPartySbomMetadata app1Sbom1 = tempEntity.newThirdPartySbomMetadata(app1.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "someFileName1.json.gz");
    createSbom(app1Sbom1);
    // App 1 second scan, report, sbom
    PolicyEvaluation app1Eval2 =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app1Eval2);
    createReport(app1Eval2);
    ThirdPartySbomMetadata app1Sbom2 = tempEntity.newThirdPartySbomMetadata(app1.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "someFileName2.json.gz");
    createSbom(app1Sbom2);

    Application app2 = tempEntity.newApplicationWithParent();
    // App 2 first scan, report, sbom
    PolicyEvaluation app2Eval1 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app2Eval1);
    createReport(app2Eval1);
    ThirdPartySbomMetadata app2Sbom1 = tempEntity.newThirdPartySbomMetadata(app2.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "someFileName3.json.gz");
    createSbom(app2Sbom1);
    // App 2 second scan, report, sbom
    PolicyEvaluation app2Eval2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app2Eval2);
    createReport(app2Eval2);
    ThirdPartySbomMetadata app2Sbom2 = tempEntity.newThirdPartySbomMetadata(app2.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "someFileName4.json.gz");
    createSbom(app2Sbom2);

    // These should be ignored - we should already be using hybrid storage at this point,  
    // so any new SBOMs should already be going to the primary storage backend
    ThirdPartySbomMetadata uploadedSbom = tempEntity.newThirdPartySbomMetadata(app2.getId(),
        ThirdPartySbomMetadataStatus.UPLOADED, "someFileName5.json.gz");
    ThirdPartySbomMetadata pendingSbom = tempEntity.newThirdPartySbomMetadata(app2.getId(),
        ThirdPartySbomMetadataStatus.PENDING, "someFileName6.json.gz");

    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    // App 1 first scan, report, sbom
    assertScanCopied(app1Eval1);
    assertReportCopied(app1Eval1);
    assertSbomCopied(app1Sbom1);
    // App 1 second scan, report, sbom
    assertScanCopied(app1Eval2);
    assertReportCopied(app1Eval2);
    assertSbomCopied(app1Sbom2);
    // App 2 first scan, report, sbom
    assertScanCopied(app2Eval1);
    assertReportCopied(app2Eval1);
    assertSbomCopied(app2Sbom1);
    // App 2 second scan, report, sbom
    assertScanCopied(app2Eval2);
    assertReportCopied(app2Eval2);
    assertSbomCopied(app2Sbom2);

    assertThat(sbomPersistenceServiceProvider.get(dataStoreTypes.get(1))
        .getPermanentSbom(app1.getId(), uploadedSbom.getFilename()).exists()).isFalse();
    assertThat(sbomPersistenceServiceProvider.get(dataStoreTypes.get(1))
        .getPermanentSbom(app1.getId(), pendingSbom.getFilename()).exists()).isFalse();

    logOutput.assertThat().atInfoLevel().contains(
        String.format("Finished copy of scans, reports, and SBOMs from '%s' to '%s': CopyStorageResult",
            dataStoreTypes.get(1).name(), dataStoreTypes.get(0).name()));
  }

  @Test
  public void testExecute_SkipsReportInProgress() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation eval1 =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(eval1);
    createReport(eval1);
    PolicyEvaluation eval2 =
        tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(eval2);
    createReport(eval2);

    // Simulate eval2 is still in progress, which means it should be ignored
    try (ClusterLock clusterLock = clusterLockManager.createForPolicyEvaluation(app, eval2.getScanId())) {
      clusterLock.lock();
      copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));
    }

    assertScanCopied(eval1);
    assertReportCopied(eval1);
    assertScanCopied(eval2);
    ApplicationReportPersistenceService reportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.get(0));
    // Check that the report and copy marker do not exist
    assertThat(reportPersistenceService.reportExists(eval2.getApplicationId(), eval2.getScanId())).isFalse();
    try (Stream<ReportEntity> reportEntities = reportPersistenceService.getAllReportEntities(eval2.getApplicationId(),
        eval2.getScanId())) {
      assertThat(reportEntities.toList()).isEmpty();
    }
  }

  @Test
  public void testExecute_WithConcurrentThreads() throws Exception {
    logOutput.setLogLevel(Level.TRACE);
    apiConfigurationService.setConfigurationNoAuthz(SystemConfigurationProperty.COPY_STORAGE_CONFIG,
        JsonUtils.convertValue(new CopyStorageConfig(1, 2), Map.class));
    copyStorageService.configurationChanged(Set.of(SystemConfigurationProperty.COPY_STORAGE_CONFIG));

    Application app1 = tempEntity.newApplicationWithParent();
    Application app2 = tempEntity.newApplicationWithParent();
    Application app3 = tempEntity.newApplicationWithParent();
    Application app4 = tempEntity.newApplicationWithParent();

    // App 1: 3 scans, reports, and sboms
    PolicyEvaluation app1Eval1 =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app1Eval1);
    createReport(app1Eval1);
    ThirdPartySbomMetadata app1Sbom1 = tempEntity.newThirdPartySbomMetadata(app1.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "app1-sbom1.json.gz");
    createSbom(app1Sbom1);

    PolicyEvaluation app1Eval2 =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app1Eval2);
    createReport(app1Eval2);
    ThirdPartySbomMetadata app1Sbom2 = tempEntity.newThirdPartySbomMetadata(app1.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "app1-sbom2.json.gz");
    createSbom(app1Sbom2);

    PolicyEvaluation app1Eval3 =
        tempEntity.newPolicyEvaluation(app1.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app1Eval3);
    createReport(app1Eval3);
    ThirdPartySbomMetadata app1Sbom3 = tempEntity.newThirdPartySbomMetadata(app1.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "app1-sbom3.json.gz");
    createSbom(app1Sbom3);

    // App 2: 3 scans, reports, and sboms
    PolicyEvaluation app2Eval1 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app2Eval1);
    createReport(app2Eval1);
    ThirdPartySbomMetadata app2Sbom1 = tempEntity.newThirdPartySbomMetadata(app2.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "app2-sbom1.json.gz");
    createSbom(app2Sbom1);

    PolicyEvaluation app2Eval2 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app2Eval2);
    createReport(app2Eval2);
    ThirdPartySbomMetadata app2Sbom2 = tempEntity.newThirdPartySbomMetadata(app2.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "app2-sbom2.json.gz");
    createSbom(app2Sbom2);

    PolicyEvaluation app2Eval3 =
        tempEntity.newPolicyEvaluation(app2.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app2Eval3);
    createReport(app2Eval3);
    ThirdPartySbomMetadata app2Sbom3 = tempEntity.newThirdPartySbomMetadata(app2.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "app2-sbom3.json.gz");
    createSbom(app2Sbom3);

    // App 3: 2 scans, reports, and sboms
    PolicyEvaluation app3Eval1 =
        tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app3Eval1);
    createReport(app3Eval1);
    ThirdPartySbomMetadata app3Sbom1 = tempEntity.newThirdPartySbomMetadata(app3.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "app3-sbom1.json.gz");
    createSbom(app3Sbom1);

    PolicyEvaluation app3Eval2 =
        tempEntity.newPolicyEvaluation(app3.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app3Eval2);
    createReport(app3Eval2);
    ThirdPartySbomMetadata app3Sbom2 = tempEntity.newThirdPartySbomMetadata(app3.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "app3-sbom2.json.gz");
    createSbom(app3Sbom2);

    // App 4: 2 scans, reports, and sboms
    PolicyEvaluation app4Eval1 =
        tempEntity.newPolicyEvaluation(app4.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app4Eval1);
    createReport(app4Eval1);
    ThirdPartySbomMetadata app4Sbom1 = tempEntity.newThirdPartySbomMetadata(app4.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "app4-sbom1.json.gz");
    createSbom(app4Sbom1);

    PolicyEvaluation app4Eval2 =
        tempEntity.newPolicyEvaluation(app4.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(app4Eval2);
    createReport(app4Eval2);
    ThirdPartySbomMetadata app4Sbom2 = tempEntity.newThirdPartySbomMetadata(app4.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "app4-sbom2.json.gz");
    createSbom(app4Sbom2);

    // Execute copy with concurrent threads
    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    // Verify all app1 data was copied correctly
    assertScanCopied(app1Eval1);
    assertReportCopied(app1Eval1);
    assertSbomCopied(app1Sbom1);
    assertScanCopied(app1Eval2);
    assertReportCopied(app1Eval2);
    assertSbomCopied(app1Sbom2);
    assertScanCopied(app1Eval3);
    assertReportCopied(app1Eval3);
    assertSbomCopied(app1Sbom3);

    // Verify all app2 data was copied correctly
    assertScanCopied(app2Eval1);
    assertReportCopied(app2Eval1);
    assertSbomCopied(app2Sbom1);
    assertScanCopied(app2Eval2);
    assertReportCopied(app2Eval2);
    assertSbomCopied(app2Sbom2);
    assertScanCopied(app2Eval3);
    assertReportCopied(app2Eval3);
    assertSbomCopied(app2Sbom3);

    // Verify all app3 data was copied correctly
    assertScanCopied(app3Eval1);
    assertReportCopied(app3Eval1);
    assertSbomCopied(app3Sbom1);
    assertScanCopied(app3Eval2);
    assertReportCopied(app3Eval2);
    assertSbomCopied(app3Sbom2);

    // Verify all app4 data was copied correctly
    assertScanCopied(app4Eval1);
    assertReportCopied(app4Eval1);
    assertSbomCopied(app4Sbom1);
    assertScanCopied(app4Eval2);
    assertReportCopied(app4Eval2);
    assertSbomCopied(app4Sbom2);

    // Verify summary log message
    logOutput.assertThat().atInfoLevel().contains(
        String.format("Finished copy of scans, reports, and SBOMs from '%s' to '%s': CopyStorageResult",
            dataStoreTypes.get(1).name(), dataStoreTypes.get(0).name()));
  }

  @Test
  public void testExecute_AlreadyActive() throws Exception {
    CopyStorageService spyCopyStorageService = spy(copyStorageService);
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch waiting = new CountDownLatch(1);
    doAnswer(invocation -> {
      started.countDown();
      assertThat(waiting.await(5, TimeUnit.SECONDS)).isTrue();
      return null;
    }).when(spyCopyStorageService).doExecute(any(), any());
    new Thread(() -> spyCopyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0))).start();
    assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();

    spyCopyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    waiting.countDown();
    logOutput.assertThat().atInfoLevel().contains(
        String.format("Request for copy of scans, reports, and SBOMs from '%s' to '%s' is already active.",
            dataStoreTypes.get(1).name(), dataStoreTypes.get(0).name()));
  }

  @Test
  public void testCheckSupported_ValidDataStoreTypes() {
    assertThatNoException().isThrownBy(() -> copyStorageService.checkSupported(DataStoreType.FILE));
    assertThatNoException().isThrownBy(() -> copyStorageService.checkSupported(DataStoreType.S3));
  }

  @Test
  public void testCheckSupported_UnsupportedDataStoreType() {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> copyStorageService.checkSupported(DataStoreType.HYBRID))
        .withMessageContaining("Storage 'HYBRID' is unsupported.");
  }

  @Test
  public void testCheckPrimaryStorageIsTarget_ValidTarget() {
    StorageConfig storageConfig = insightConfig.getStorage();
    storageConfig.setType(dataStoreTypes.get(0));

    assertThatNoException().isThrownBy(() -> copyStorageService.checkPrimaryStorageIsTarget(dataStoreTypes.get(0)));
  }

  @Test
  public void testCheckPrimaryStorageIsTarget_HybridPrimaryStorage() {
    StorageConfig storageConfig = insightConfig.getStorage();
    storageConfig.setType(DataStoreType.HYBRID);
    storageConfig.getHybridConfig().setTypes(new LinkedHashSet<>(dataStoreTypes));

    assertThatNoException().isThrownBy(() -> copyStorageService.checkPrimaryStorageIsTarget(dataStoreTypes.get(0)));
  }

  @Test
  public void testCheckPrimaryStorageIsTarget_InvalidTarget() {
    StorageConfig storageConfig = insightConfig.getStorage();
    storageConfig.setType(dataStoreTypes.get(0));

    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> copyStorageService.checkPrimaryStorageIsTarget(dataStoreTypes.get(1)))
        .withMessageContaining(("Primary storage type is '%s' but copy is targeting '%s'," +
            " scans, reports, and/or SBOMs written during copy may be missed.").formatted(dataStoreTypes.get(0),
            dataStoreTypes.get(1)));
  }

  @Test
  public void testCheckFromAndToAreDifferent_Valid() {
    assertThatNoException().isThrownBy(
        () -> copyStorageService.checkFromAndToAreDifferent(dataStoreTypes.get(0), dataStoreTypes.get(1)));
  }

  @Test
  public void testCheckFromAndToAreDifferent_Invalid() {
    assertThatExceptionOfType(BadRequestException.class)
        .isThrownBy(() -> copyStorageService.checkFromAndToAreDifferent(dataStoreTypes.get(0), dataStoreTypes.get(0)))
        .withMessageContaining("Not copying from '%s' to '%s', these should be different.".formatted(
            dataStoreTypes.get(0), dataStoreTypes.get(0)));
  }

  @Test
  public void testConfigurationChanged() {
    CopyStorageConfig newConfig = new CopyStorageConfig(2, 3);

    apiConfigurationService.setConfigurationNoAuthz(SystemConfigurationProperty.COPY_STORAGE_CONFIG,
        JsonUtils.convertValue(newConfig, Map.class));
    copyStorageService.configurationChanged(Set.of(SystemConfigurationProperty.COPY_STORAGE_CONFIG));

    logOutput.assertThat().atDebugLevel().contains("Updated 'copyLimit' to 3.");
  }

  @Test
  public void testConfigurationChanged_UnrelatedProperty() {
    copyStorageService.configurationChanged(Set.of("SOME_OTHER_PROPERTY"));

    logOutput.assertThat().atDebugLevel().doesNotContain("Updated 'appLimit'");
    logOutput.assertThat().atDebugLevel().doesNotContain("Updated 'copyLimits'");
  }

  @Test
  public void testExecute_MissingScan() {
    logOutput.setLogLevel(Level.TRACE);
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    // Don't create scan - it doesn't exist in source storage

    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    // Verify scan was not copied
    ScanPersistenceService scanPersistenceService = scanPersistenceServiceProvider.get(dataStoreTypes.get(0));
    ScanEntity scanEntity = scanPersistenceService.getScan(eval.getApplicationId(), eval.getScanId());
    assertThat(scanEntity.exists()).isFalse();
  }

  @Test
  public void testExecute_ScanAlreadyCopied() throws Exception {
    logOutput.setLogLevel(Level.TRACE);
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createScan(eval);
    // Copy once
    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));
    assertScanCopied(eval);

    // Copy again - should skip
    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    logOutput.assertThat().atTraceLevel().contains(
        String.format("Skipping scan copying for app id '%s' scan id '%s' since it is already done.",
            eval.getApplicationId(), eval.getScanId()));
  }

  @Test
  public void testExecute_MissingReport() throws Exception {
    logOutput.setLogLevel(Level.TRACE);
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    // Don't create report - it doesn't exist in source storage

    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    // Verify report was not copied
    ApplicationReportPersistenceService reportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.get(0));
    assertThat(reportPersistenceService.reportExists(eval.getApplicationId(), eval.getScanId())).isFalse();

    logOutput.assertThat().atTraceLevel().contains(
        String.format("Skipping report copying for app id '%s' scan id '%s' since it does not exist.",
            eval.getApplicationId(), eval.getScanId()));
  }

  @Test
  public void testExecute_ReportAlreadyCopied() throws Exception {
    logOutput.setLogLevel(Level.TRACE);
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    createReport(eval);
    // Copy once
    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));
    assertReportCopied(eval);

    // Copy again - should skip
    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    logOutput.assertThat().atTraceLevel().contains(
        String.format("Skipping report copying for app id '%s' scan id '%s' since it is already done.",
            eval.getApplicationId(), eval.getScanId()));
  }

  @Test
  public void testExecute_ReportWithoutPdf() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(app.getId(), BuildStageType.ID, TemporaryEntity.uuid());
    // Create report without PDF
    String reportZipName = "report.zip";
    Path zipPath = tempDir.getRoot().toPath().resolve(reportZipName);
    ReportHelper.createEmptyZip(zipPath);
    for (ReportFile reportFile : ReportFile.values()) {
      if (reportFile.getLocationTypes().contains(ReportFileLocationType.ORIGINAL)) {
        ReportHelper.addToZip(zipPath, zipPath.resolve(reportFile.getName()), reportFile.getName());
      }
    }
    ApplicationReportPersistenceService service =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.get(1));
    try (InputStream inputStream = new FileInputStream(zipPath.toFile())) {
      service.saveOriginalReport(eval.getApplicationId(), eval.getScanId(), inputStream);
    }
    // Note: NOT creating PDF file

    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    // Verify report was copied but PDF was not
    ApplicationReportPersistenceService reportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.get(0));
    assertThat(reportPersistenceService.reportExists(eval.getApplicationId(), eval.getScanId())).isTrue();
    ReportPdfEntity reportPdfEntity = reportPersistenceService.getPdfEntity(eval.getApplicationId(), eval.getScanId());
    assertThat(reportPdfEntity.exists()).isFalse();
  }

  @Test
  public void testExecute_MissingSbom() {
    logOutput.setLogLevel(Level.TRACE);
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = tempEntity.newThirdPartySbomMetadata(app.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "missing-sbom.json.gz");
    // Don't create SBOM - it doesn't exist in source storage

    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    // Verify SBOM was not copied
    SbomPersistenceService sbomPersistenceService = sbomPersistenceServiceProvider.get(dataStoreTypes.get(0));
    SbomEntity sbomEntity = sbomPersistenceService.getPermanentSbom(sbomMetadata.getApplicationId(),
        sbomMetadata.getFilename());
    assertThat(sbomEntity.exists()).isFalse();

    logOutput.assertThat().atTraceLevel().contains(
        String.format("Skipping sbom copying for app id '%s' file name '%s' since it does not exist.",
            sbomMetadata.getApplicationId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testExecute_SbomAlreadyCopied() throws Exception {
    logOutput.setLogLevel(Level.TRACE);
    Application app = tempEntity.newApplicationWithParent();
    ThirdPartySbomMetadata sbomMetadata = tempEntity.newThirdPartySbomMetadata(app.getId(),
        ThirdPartySbomMetadataStatus.ACTIVE, "already-copied-sbom.json.gz");
    createSbom(sbomMetadata);
    // Copy once
    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));
    assertSbomCopied(sbomMetadata);

    // Copy again - should skip
    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    logOutput.assertThat().atTraceLevel().contains(
        String.format("Skipping sbom copying for app id '%s' file name '%s' since it is already done.",
            sbomMetadata.getApplicationId(), sbomMetadata.getFilename()));
  }

  @Test
  public void testExecute_LargeFile() throws Exception {
    Application app = tempEntity.newApplicationWithParent();
    PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
        app.getId(),
        BuildStageType.ID,
        TemporaryEntity.uuid()
    );
    createScan(eval, (int) (120 * MB));

    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    assertScanCopied(eval);
  }

  @Test
  @Category(SlowTest.class)
  @Ignore // Only for on-demand running
  // Tune down noisy logging by modifying src/test/resources/logback-test.xml
  // e.g.
  // <logger name="com.sonatype" level="WARN" />
  // <logger name="com.sonatype.insight.brain.service.CopyStorageServiceTest" level="DEBUG" />
  // <logger name="org.apache.http.headers" level="WARN" />
  public void testExecute_Stress() throws Exception {
    if (dataStoreTypes.get(0) == DataStoreType.FILE) {
      return;
    }

    apiConfigurationService.setConfigurationNoAuthz(SystemConfigurationProperty.COPY_STORAGE_CONFIG,
        JsonUtils.convertValue(new CopyStorageConfig(1, 100), Map.class));
    copyStorageService.configurationChanged(Set.of(SystemConfigurationProperty.COPY_STORAGE_CONFIG));

    int numApps = 10;
    int reportsPerApp = 60;

    log.info("Creating {} applications with {} reports each for stress test", numApps, reportsPerApp);

    for (int appIdx = 0; appIdx < numApps; appIdx++) {
      Application app = tempEntity.newApplicationWithParent();

      for (int reportIdx = 0; reportIdx < reportsPerApp; reportIdx++) {
        PolicyEvaluation eval = tempEntity.newPolicyEvaluation(
            app.getId(),
            BuildStageType.ID,
            TemporaryEntity.uuid()
        );
        // Create some large files
        createScan(eval, reportIdx % 20 == 0 ? (int) (20 * MB) : 1024);
        createReport(eval, reportIdx % 20 == 0 ? (int) (MB) : 1024);

        ThirdPartySbomMetadata sbom = tempEntity.newThirdPartySbomMetadata(
            app.getId(),
            ThirdPartySbomMetadataStatus.ACTIVE,
            String.format("app%d-sbom%d.json.gz", appIdx, reportIdx)
        );
        createSbom(sbom, reportIdx % 20 == 0 ? (int) (20 * MB) : 1024);
      }
    }

    long maxMemory = Runtime.getRuntime().maxMemory();
    log.info("Max heap size: {} MB", maxMemory / 1024 / 1024);

    int iterations = 3;
    for (int iteration = 0; iteration < iterations; iteration++) {
      long beforeMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      log.info("=== Iteration {} of {} - Memory before: {} MB ===",
          iteration + 1, iterations, beforeMemory / 1024 / 1024);

      log.info("Starting copy...");

      try {
        copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));
        log.info("Copy completed successfully");
      }
      catch (OutOfMemoryError e) {
        log.error("OOM during copy execution at iteration {}", iteration + 1, e);
        log.error("Memory stats - Total: {} MB, Free: {} MB, Used: {} MB",
            Runtime.getRuntime().totalMemory() / 1024 / 1024,
            Runtime.getRuntime().freeMemory() / 1024 / 1024,
            (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024);
        throw e;
      }

      long afterMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      log.info("Memory after copy: {} MB (peak spike: +{} MB)",
          afterMemory / 1024 / 1024, (afterMemory - beforeMemory) / 1024 / 1024);

      long peakUsedPercent = (afterMemory * 100) / maxMemory;
      if (peakUsedPercent > 85) {
        log.warn("WARNING: Peak memory reached {}% of heap! This would cause OOM with more load or less heap.",
            peakUsedPercent);
      }

      for (PolicyEvaluation policyEvaluation : policyEvaluationDAO.getAll()) {
        assertScanCopied(policyEvaluation);
        assertReportCopied(policyEvaluation);
      }

      for (ThirdPartySbomMetadata thirdPartySbomMetadata : thirdPartySbomMetadataDAO.getAll()) {
        assertSbomCopied(thirdPartySbomMetadata);
      }

      log.info("Cleaning up destination storage for next iteration");
      deleteAllFromStorage(dataStoreTypes.get(0));

      // GC to show that memory CAN be reclaimed (not a traditional leak)
      System.gc();

      long afterGcMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
      log.info("Memory after GC: {} MB", afterGcMemory / 1024 / 1024);

      long memoryReclaimed = afterMemory - afterGcMemory;
      log.info("Memory reclaimed by GC: {} MB", memoryReclaimed / 1024 / 1024);
    }
  }

  private void deleteAllFromStorage(DataStoreType dataStoreType) throws Exception {
    if (dataStoreType == DataStoreType.S3) {
      s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder().bucket(BUCKET_NAME).build())
          .contents()
          .forEach(obj -> s3Client.deleteObject(DeleteObjectRequest.builder()
              .bucket(BUCKET_NAME).key(obj.key()).build()));
    }
    else if (dataStoreType == DataStoreType.FILE) {
      log.debug("Skipping file storage cleanup - not implemented for this test");
    }
  }

  private void assertScanCopied(final PolicyEvaluation eval) throws Exception {
    ScanPersistenceService from = scanPersistenceServiceProvider.get(dataStoreTypes.get(1));
    ScanPersistenceService to = scanPersistenceServiceProvider.get(dataStoreTypes.get(0));

    ScanEntity fromScanEntity = from.getScan(eval.getApplicationId(), eval.getScanId());
    assertThat(fromScanEntity.exists()).isTrue();
    ScanEntity toScanEntity = to.getScan(eval.getApplicationId(), eval.getScanId());
    assertThat(toScanEntity.exists()).isTrue();

    try (InputStream fromIs = fromScanEntity.getInputStream(); InputStream toIs = toScanEntity.getInputStream()) {
      assertThat(IOUtils.contentEquals(fromIs, toIs)).isTrue();
    }
  }

  private void assertReportCopied(final PolicyEvaluation eval) throws Exception {
    ApplicationReportPersistenceService from = applicationReportPersistenceServiceProvider.get(dataStoreTypes.get(1));
    ApplicationReportPersistenceService to = applicationReportPersistenceServiceProvider.get(dataStoreTypes.get(0));

    assertReportEntityStreamsEqual(() -> from.getOriginalReportEntities(eval.getApplicationId(), eval.getScanId()),
        () -> to.getOriginalReportEntities(eval.getApplicationId(), eval.getScanId()));
    assertReportEntityStreamsEqual(() -> from.getAllReportEntities(eval.getApplicationId(), eval.getScanId()),
        () -> to.getAllReportEntities(eval.getApplicationId(), eval.getScanId()));

    for (ReportFile reportFile : ReportFile.values()) {
      ReportEntity fromReportEntity =
          from.getReportEntity(eval.getApplicationId(), eval.getScanId(), reportFile.getName());
      assertThat(fromReportEntity.exists()).isTrue();
      ReportEntity toReportEntity =
          to.getReportEntity(eval.getApplicationId(), eval.getScanId(), reportFile.getName());
      assertThat(toReportEntity.exists()).isTrue();
      try (InputStream fromIs = fromReportEntity.getInputStream(); InputStream toIs = toReportEntity.getInputStream()) {
        assertThat(IOUtils.contentEquals(fromIs, toIs)).isTrue();
      }
    }

    ReportPdfEntity fromReportPdfEntity = from.getPdfEntity(eval.getApplicationId(), eval.getScanId());
    assertThat(fromReportPdfEntity.exists()).isTrue();
    ReportPdfEntity toReportPdfEntity = to.getPdfEntity(eval.getApplicationId(), eval.getScanId());
    assertThat(toReportPdfEntity.exists()).isTrue();
    try (InputStream fromIs = fromReportPdfEntity.getInputStream();
         InputStream toIs = toReportPdfEntity.getInputStream()) {
      assertThat(IOUtils.contentEquals(fromIs, toIs)).isTrue();
    }
  }

  private void assertReportEntityStreamsEqual(
      final Callable<Stream<ReportEntity>> fromStreamSupplier,
      final Callable<Stream<ReportEntity>> toStreamSupplier) throws Exception
  {
    try (Stream<ReportEntity> fromStream = fromStreamSupplier.call();
         Stream<ReportEntity> toStream = toStreamSupplier.call()) {
      Map<String, ReportEntity> fromReportEntityByName = fromStream
          .collect(Collectors.toMap(ReportEntity::getName, Function.identity()));
      Map<String, ReportEntity> toReportEntityByName = toStream
          .collect(Collectors.toMap(ReportEntity::getName, Function.identity()));

      assertThat(fromReportEntityByName.keySet()).isEqualTo(toReportEntityByName.keySet());

      for (String key : fromReportEntityByName.keySet()) {
        ReportEntity fromReportEntity = fromReportEntityByName.get(key);
        ReportEntity toReportEntity = toReportEntityByName.get(key);
        try (InputStream fromIs = fromReportEntity.getInputStream();
             InputStream toIs = toReportEntity.getInputStream()) {
          assertThat(IOUtils.contentEquals(fromIs, toIs)).isTrue();
        }
      }
    }
  }

  private void assertSbomCopied(final ThirdPartySbomMetadata sbomMetadata) throws Exception {
    SbomPersistenceService from = sbomPersistenceServiceProvider.get(dataStoreTypes.get(1));
    SbomPersistenceService to = sbomPersistenceServiceProvider.get(dataStoreTypes.get(0));

    SbomEntity fromSbomEntity = from.getPermanentSbom(sbomMetadata.getApplicationId(), sbomMetadata.getFilename());
    assertThat(fromSbomEntity.exists()).isTrue();
    SbomEntity toSbomEntity = to.getPermanentSbom(sbomMetadata.getApplicationId(), sbomMetadata.getFilename());
    assertThat(toSbomEntity.exists()).isTrue();

    try (InputStream fromIs = fromSbomEntity.getInputStream();
         InputStream toIs = toSbomEntity.getInputStream()) {
      assertThat(IOUtils.contentEquals(fromIs, toIs)).isTrue();
    }
  }

  private void createScan(final PolicyEvaluation eval) throws Exception {
    createScan(eval, 1024);
  }

  private void createScan(final PolicyEvaluation eval, final int contentSizeInBytes) throws Exception {
    String scanFileName = "scan-" + eval.getScanId() + ".xml";
    Path zipPath = tempDir.getRoot().toPath().resolve(scanFileName + ".gz");
    ReportHelper.createEmptyZip(zipPath);
    ReportHelper.addToZip(zipPath, zipPath.resolve(scanFileName), createRandomInputStream(contentSizeInBytes));
    ScanPersistenceService service = scanPersistenceServiceProvider.get(dataStoreTypes.get(1));
    ScanEntity scanEntity = service.getScan(eval.getApplicationId(), eval.getScanId());
    try (InputStream inputStream = new FileInputStream(zipPath.toFile());
         OutputStream outputStream = scanEntity.getOutputStream()) {
      inputStream.transferTo(outputStream);
    }
  }

  private void createReport(final PolicyEvaluation eval) throws Exception {
    createReport(eval, 1024);
  }

  private void createReport(final PolicyEvaluation eval, final int contentSizeInBytes)
      throws Exception
  {
    String reportZipName = "report.zip";
    Path zipPath = tempDir.getRoot().toPath().resolve(reportZipName);
    ReportHelper.createEmptyZip(zipPath);
    for (ReportFile reportFile : ReportFile.values()) {
      if (reportFile.getLocationTypes().contains(ReportFileLocationType.ORIGINAL)) {
        ReportHelper.addToZip(zipPath, zipPath.resolve(reportFile.getName()),
            createRandomInputStream(contentSizeInBytes));
      }
    }
    ApplicationReportPersistenceService service =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.get(1));
    // Create report.zip
    try (InputStream inputStream = new FileInputStream(zipPath.toFile())) {
      service.saveOriginalReport(eval.getApplicationId(), eval.getScanId(), inputStream);
    }
    for (ReportFile reportFile : ReportFile.values()) {
      // Create report.cache
      if (reportFile.getLocationTypes().contains(ReportFileLocationType.ORIGINAL) ||
          reportFile.getLocationTypes().contains(ReportFileLocationType.CACHE)) {
        try (InputStream content = createRandomInputStream(contentSizeInBytes)) {
          service.saveReportFile(eval.getApplicationId(), eval.getScanId(), reportFile.getName(), content);
        }
      }
      // Create additional.files
      if (reportFile.getLocationTypes().contains(ReportFileLocationType.ADDITIONAL)) {
        try (InputStream content = createRandomInputStream(contentSizeInBytes)) {
          service.saveAdditionalReportFile(eval.getApplicationId(), eval.getScanId(), reportFile.getName(), content);
        }
      }
    }
    // Create report.pdf
    try (InputStream inputStream = createRandomInputStream(contentSizeInBytes);
         OutputStream outputStream = service.getPdfEntity(eval.getApplicationId(), eval.getScanId())
             .getOutputStream()) {
      inputStream.transferTo(outputStream);
    }
  }

  private void createSbom(final ThirdPartySbomMetadata sbomMetadata) throws Exception {
    createSbom(sbomMetadata, 1024);
  }

  private void createSbom(final ThirdPartySbomMetadata sbomMetadata, final int contentSizeInBytes)
      throws Exception
  {
    String sbomFileName = sbomMetadata.getFilename();
    Path zipPath = tempDir.getRoot().toPath().resolve(sbomFileName);
    ReportHelper.createEmptyZip(zipPath);
    ReportHelper.addToZip(zipPath, zipPath.resolve(sbomFileName.substring(0, sbomFileName.length() - 3)),
        createRandomInputStream(contentSizeInBytes));
    SbomPersistenceService service = sbomPersistenceServiceProvider.get(dataStoreTypes.get(1));
    SbomEntity sbomEntity = service.getPermanentSbom(sbomMetadata.getApplicationId(), sbomFileName);
    try (InputStream inputStream = new FileInputStream(zipPath.toFile());
         OutputStream outputStream = sbomEntity.getOutputStream()) {
      inputStream.transferTo(outputStream);
    }
  }

  private InputStream createRandomInputStream(final int numberOfBytes) {
    return new InputStream()
    {
      private final Random random = new Random();

      private int remaining = numberOfBytes;

      @Override
      public int read() {
        if (remaining <= 0) {
          return -1;
        }
        remaining--;
        return random.nextInt(256);
      }
    };
  }
}
