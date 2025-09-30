/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.model.Application;
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
import com.sonatype.insight.brain.report.pdf.PdfGenerator;
import com.sonatype.insight.brain.sbom.datastore.SbomEntity;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceServiceProvider;
import com.sonatype.insight.brain.scan.datastore.ScanEntity;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceService;
import com.sonatype.insight.brain.scan.datastore.ScanPersistenceServiceProvider;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.DataStoreType;
import com.sonatype.insight.brain.service.config.StorageConfig.HybridDataStoreConfig;
import com.sonatype.insight.brain.service.config.StorageConfig.S3DataStoreConfig;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.error.exception.BadRequestException;
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
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@Category(SlowTest.class)
@RunWith(Parameterized.class)
public class CopyStorageServiceTest
    extends AbstractComponentTest
{
  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.5.0");

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
    super.configure(binder);
    binder.bind(S3Client.class).toInstance(createS3Client());
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
        String.format("Finished copy of scans, reports, and SBOMs from '%s' to '%s' for 0 app(s)",
            dataStoreTypes.get(1).name(), dataStoreTypes.get(0).name())
    );
  }

  @Test
  public void testExecute_NoData() {
    tempEntity.newApplicationWithParent();

    copyStorageService.execute(dataStoreTypes.get(1), dataStoreTypes.get(0));

    logOutput.assertThat().atInfoLevel().contains(
        String.format("Finished copy of scans, reports, and SBOMs from '%s' to '%s' for 1 app(s)",
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
        String.format("Finished copy of scans, reports, and SBOMs from '%s' to '%s' for 2 app(s)",
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

  private void assertScanCopied(final PolicyEvaluation eval) throws Exception {
    ScanPersistenceService scanPersistenceService = scanPersistenceServiceProvider.get(dataStoreTypes.get(0));
    ScanEntity scanEntity = scanPersistenceService.getScan(eval.getApplicationId(), eval.getScanId());
    assertThat(scanEntity.exists()).isTrue();
    assertThat(ReportHelper.readFromZipStream(scanEntity.getInputStream(), "scan-" + eval.getScanId() + ".xml"))
        .isEqualTo(eval.getScanId());
  }

  private void assertReportCopied(final PolicyEvaluation eval) throws Exception {
    ApplicationReportPersistenceService reportPersistenceService =
        applicationReportPersistenceServiceProvider.get(dataStoreTypes.get(0));
    assertThat(reportPersistenceService.reportExists(eval.getApplicationId(), eval.getScanId())).isTrue();
    for (ReportFile reportFile : ReportFile.values()) {
      ReportEntity reportEntity =
          reportPersistenceService.getReportEntity(eval.getApplicationId(), eval.getScanId(), reportFile.getName());
      assertThat(reportEntity.exists()).isTrue();
      String content;
      try (InputStream inputStream = reportEntity.getInputStream()) {
        content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      }
      assertThat(content).isEqualTo(reportFile.getName());
    }
    ReportPdfEntity reportPdfEntity = reportPersistenceService.getPdfEntity(eval.getApplicationId(), eval.getScanId());
    assertThat(reportPdfEntity.exists()).isTrue();
    String content;
    try (InputStream inputStream = reportPdfEntity.getInputStream()) {
      content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
    assertThat(content).isEqualTo(PdfGenerator.REPORT_FILE_NAME);
  }

  private void assertSbomCopied(final ThirdPartySbomMetadata sbomMetadata) throws Exception {
    SbomPersistenceService sbomPersistenceService = sbomPersistenceServiceProvider.get(dataStoreTypes.get(0));
    SbomEntity sbomEntity =
        sbomPersistenceService.getPermanentSbom(sbomMetadata.getApplicationId(), sbomMetadata.getFilename());
    assertThat(sbomEntity.exists()).isTrue();
    assertThat(ReportHelper.readFromZipStream(sbomEntity.getInputStream(),
        sbomMetadata.getFilename().substring(0, sbomMetadata.getFilename().length() - 3)))
        .isEqualTo(sbomMetadata.getId());
  }

  private void createScan(final PolicyEvaluation eval) throws Exception {
    String scanFileName = "scan-" + eval.getScanId() + ".xml";
    Path zipPath = tempDir.getRoot().toPath().resolve(scanFileName + ".gz");
    ReportHelper.createEmptyZip(zipPath);
    ReportHelper.addToZip(zipPath, zipPath.resolve(scanFileName), eval.getScanId());
    ScanPersistenceService service = scanPersistenceServiceProvider.get(dataStoreTypes.get(1));
    ScanEntity scanEntity = service.getScan(eval.getApplicationId(), eval.getScanId());
    try (InputStream inputStream = new FileInputStream(zipPath.toFile());
         OutputStream outputStream = scanEntity.getOutputStream()) {
      inputStream.transferTo(outputStream);
    }
  }

  private void createReport(final PolicyEvaluation eval)
      throws Exception
  {
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
    // Create report.zip
    try (InputStream inputStream = new FileInputStream(zipPath.toFile())) {
      service.saveOriginalReport(eval.getApplicationId(), eval.getScanId(), inputStream);
    }
    for (ReportFile reportFile : ReportFile.values()) {
      // Create report.cache
      if (reportFile.getLocationTypes().contains(ReportFileLocationType.ORIGINAL) ||
          reportFile.getLocationTypes().contains(ReportFileLocationType.CACHE)) {
        try (InputStream content = new ByteArrayInputStream(reportFile.getName().getBytes(StandardCharsets.UTF_8))) {
          service.saveReportFile(eval.getApplicationId(), eval.getScanId(), reportFile.getName(), content);
        }
      }
      // Create additional.files
      if (reportFile.getLocationTypes().contains(ReportFileLocationType.ADDITIONAL)) {
        try (InputStream content = new ByteArrayInputStream(reportFile.getName().getBytes(StandardCharsets.UTF_8))) {
          service.saveAdditionalReportFile(eval.getApplicationId(), eval.getScanId(), reportFile.getName(), content);
        }
      }
    }
    // Create report.pdf
    try (OutputStream outputStream = service.getPdfEntity(eval.getApplicationId(), eval.getScanId())
        .getOutputStream()) {
      outputStream.write(PdfGenerator.REPORT_FILE_NAME.getBytes(StandardCharsets.UTF_8));
    }
  }

  private void createSbom(final ThirdPartySbomMetadata sbomMetadata)
      throws Exception
  {
    String sbomFileName = sbomMetadata.getFilename();
    Path zipPath = tempDir.getRoot().toPath().resolve(sbomFileName);
    ReportHelper.createEmptyZip(zipPath);
    ReportHelper.addToZip(zipPath, zipPath.resolve(sbomFileName.substring(0, sbomFileName.length() - 3)),
        sbomMetadata.getId());
    SbomPersistenceService service = sbomPersistenceServiceProvider.get(dataStoreTypes.get(1));
    SbomEntity sbomEntity = service.getPermanentSbom(sbomMetadata.getApplicationId(), sbomFileName);
    try (InputStream inputStream = new FileInputStream(zipPath.toFile());
         OutputStream outputStream = sbomEntity.getOutputStream()) {
      inputStream.transferTo(outputStream);
    }
  }
}
