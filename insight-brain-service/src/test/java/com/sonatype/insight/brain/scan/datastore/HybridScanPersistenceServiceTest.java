/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scan.datastore;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.Callable;
import javax.inject.Inject;

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
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
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
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class HybridScanPersistenceServiceTest
    extends AbstractComponentTest
{
  @Rule
  public LogOutput logOutput = new LogOutput(HybridScanPersistenceService.class);

  private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:4.10.0");

  private static final String BUCKET_NAME = "test-scan-bucket";

  private static final String REGION = "us-east-2";

  private static final String APP_ID = "appId";

  private static final String OTHER_APP_ID = "otherAppId";

  private static final String SCAN_ID = "scanId";

  private static final String OTHER_SCAN_ID = "otherScanId";

  private static final String SCAN_NAME = "scan-" + SCAN_ID + ".xml.gz";

  @ClassRule
  public static LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMAGE).withServices(Service.S3);

  @Inject
  private S3Client s3Client;

  @Inject
  private S3ScanPersistenceService s3ScanPersistenceService;

  @Inject
  private FileScanPersistenceService fileScanPersistenceService;

  @Inject
  private HybridScanPersistenceService hybridScanPersistenceService;

  @Inject
  private ScanPersistenceServiceProvider scanPersistenceServiceProvider;

  @Parameters
  public static List<Object[]> dataStoreTypes() {
    return Arrays.asList(new Object[][]{
        {new LinkedHashSet<>(List.of(DataStoreType.S3, DataStoreType.FILE))},
        {new LinkedHashSet<>(List.of(DataStoreType.FILE, DataStoreType.S3))},
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

  private final LinkedHashSet<DataStoreType> dataStoreTypes;

  public HybridScanPersistenceServiceTest(final LinkedHashSet<DataStoreType> dataStoreTypes) {
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

  @Test
  public void testDoGetScan_DoesNotExist() {
    ScanEntity expectedScanEntity =
        scanPersistenceServiceProvider.get(dataStoreTypes.iterator().next()).doGetScan(APP_ID, SCAN_ID);

    ScanEntity scanEntity = hybridScanPersistenceService.doGetScan(APP_ID, SCAN_ID);

    assertThat(scanEntity).isInstanceOf(expectedScanEntity.getClass());
    assertThat(scanEntity.getLocation()).isEqualTo(expectedScanEntity.getLocation());
    assertThat(scanEntity.exists()).isFalse();
  }

  @Test
  public void testDoGetScan_ExistsOnlyInFile() throws Exception {
    ScanEntity fileScanEntity = createScan(fileScanPersistenceService, APP_ID, SCAN_ID);

    ScanEntity scanEntity = hybridScanPersistenceService.doGetScan(APP_ID, SCAN_ID);

    assertThat(scanEntity).isInstanceOf(FileScanEntity.class);
    assertThat(scanEntity.getLocation()).isEqualTo(fileScanEntity.getLocation());
    assertThat(readScan(scanEntity)).isEqualTo(readScan(fileScanEntity));
  }

  @Test
  public void testDoGetScan_ExistsOnlyInS3() throws Exception {
    ScanEntity s3ScanEntity = createScan(s3ScanPersistenceService, APP_ID, SCAN_ID);

    ScanEntity scanEntity = hybridScanPersistenceService.doGetScan(APP_ID, SCAN_ID);

    assertThat(scanEntity).isInstanceOf(S3ScanEntity.class);
    assertThat(scanEntity.getLocation()).isEqualTo(s3ScanEntity.getLocation());
    assertThat(readScan(scanEntity)).isEqualTo(readScan(s3ScanEntity));
  }

  @Test
  public void testDoGetScan_ExistsInFileAndS3() throws Exception {
    createScan(s3ScanPersistenceService, APP_ID, SCAN_ID);
    createScan(fileScanPersistenceService, APP_ID, SCAN_ID);
    ScanEntity expectedScanEntity =
        scanPersistenceServiceProvider.get(dataStoreTypes.iterator().next()).doGetScan(APP_ID, SCAN_ID);

    ScanEntity scanEntity = hybridScanPersistenceService.doGetScan(APP_ID, SCAN_ID);

    assertThat(scanEntity).isInstanceOf(expectedScanEntity.getClass());
    assertThat(scanEntity.getLocation()).isEqualTo(expectedScanEntity.getLocation());
    assertThat(readScan(scanEntity)).isEqualTo(readScan(expectedScanEntity));
  }

  @Test
  public void testCreateTempScan() throws Exception {
    ScanEntity scanEntity = hybridScanPersistenceService.createTempScan(APP_ID);
    Class<? extends ScanEntity> expectedScanEntityClass =
        scanPersistenceServiceProvider.get(dataStoreTypes.iterator().next()).getScanEntityClass();

    assertThat(scanEntity).isInstanceOf(expectedScanEntityClass);
    assertThat(scanEntity.getAppId()).isEqualTo(APP_ID);
  }

  @Test
  public void testMoveTempScan_S3() throws Exception {
    ScanEntity scanEntity = createScan(s3ScanPersistenceService);
    String content = readScan(scanEntity);
    ScanEntity expectedScanEntity =
        scanPersistenceServiceProvider.get(dataStoreTypes.iterator().next()).getScan(APP_ID, SCAN_ID);

    hybridScanPersistenceService.moveTempScan(scanEntity, APP_ID, SCAN_ID);

    assertThat(scanEntity.exists()).isFalse();
    assertThat(readScan(expectedScanEntity)).isEqualTo(content);
  }

  @Test
  public void testMoveTempScan_File() throws Exception {
    ScanEntity scanEntity = createScan(fileScanPersistenceService);
    String content = readScan(scanEntity);
    ScanEntity expectedScanEntity =
        scanPersistenceServiceProvider.get(dataStoreTypes.iterator().next()).getScan(APP_ID, SCAN_ID);

    hybridScanPersistenceService.moveTempScan(scanEntity, APP_ID, SCAN_ID);

    assertThat(scanEntity.exists()).isFalse();
    assertThat(readScan(expectedScanEntity)).isEqualTo(content);
  }

  @Test
  public void testGetScanByName_DoesNotExist() {
    ScanEntity expectedScanEntity =
        scanPersistenceServiceProvider.get(dataStoreTypes.iterator().next()).getScanByName(APP_ID, SCAN_NAME);

    ScanEntity scanEntity = hybridScanPersistenceService.getScanByName(APP_ID, SCAN_NAME);

    assertThat(scanEntity).isInstanceOf(expectedScanEntity.getClass());
    assertThat(scanEntity.getLocation()).isEqualTo(expectedScanEntity.getLocation());
    assertThat(scanEntity.exists()).isFalse();
  }

  @Test
  public void testGetScanByName_ExistsOnlyInFile() throws Exception {
    ScanEntity fileScanEntity = createScan(fileScanPersistenceService, APP_ID, SCAN_ID);

    ScanEntity scanEntity = hybridScanPersistenceService.getScanByName(APP_ID, SCAN_NAME);

    assertThat(scanEntity).isInstanceOf(FileScanEntity.class);
    assertThat(scanEntity.getLocation()).isEqualTo(fileScanEntity.getLocation());
    assertThat(readScan(scanEntity)).isEqualTo(readScan(fileScanEntity));
  }

  @Test
  public void testGetScanByName_ExistsOnlyInS3() throws Exception {
    ScanEntity s3ScanEntity = createScan(s3ScanPersistenceService, APP_ID, SCAN_ID);

    ScanEntity scanEntity = hybridScanPersistenceService.getScanByName(APP_ID, SCAN_NAME);

    assertThat(scanEntity).isInstanceOf(S3ScanEntity.class);
    assertThat(scanEntity.getLocation()).isEqualTo(s3ScanEntity.getLocation());
    assertThat(readScan(scanEntity)).isEqualTo(readScan(s3ScanEntity));
  }

  @Test
  public void testGetScanByName_ExistsInFileAndS3() throws Exception {
    createScan(s3ScanPersistenceService, APP_ID, SCAN_ID);
    createScan(fileScanPersistenceService, APP_ID, SCAN_ID);
    ScanEntity expectedScanEntity =
        scanPersistenceServiceProvider.get(dataStoreTypes.iterator().next()).getScanByName(APP_ID, SCAN_NAME);

    ScanEntity scanEntity = hybridScanPersistenceService.getScanByName(APP_ID, SCAN_NAME);

    assertThat(scanEntity).isInstanceOf(expectedScanEntity.getClass());
    assertThat(scanEntity.getLocation()).isEqualTo(expectedScanEntity.getLocation());
    assertThat(readScan(scanEntity)).isEqualTo(readScan(expectedScanEntity));
  }

  @Test
  public void testCopyScanFile_FileToFile() throws Exception {
    ScanEntity source = createScan(fileScanPersistenceService);
    ScanEntity destination = fileScanPersistenceService.getScan(OTHER_APP_ID, OTHER_SCAN_ID);

    hybridScanPersistenceService.copyScanFile(source, destination);

    assertThat(readScan(destination)).isEqualTo(readScan(source));
  }

  @Test
  public void testCopyScanFile_FileToS3() throws Exception {
    ScanEntity source = createScan(fileScanPersistenceService);
    ScanEntity destination = s3ScanPersistenceService.getScan(OTHER_APP_ID, OTHER_SCAN_ID);

    hybridScanPersistenceService.copyScanFile(source, destination);

    assertThat(readScan(destination)).isEqualTo(readScan(source));
  }

  @Test
  public void testCopyScanFile_S3ToFile() throws Exception {
    ScanEntity source = createScan(s3ScanPersistenceService);
    ScanEntity destination = fileScanPersistenceService.getScan(OTHER_APP_ID, OTHER_SCAN_ID);

    hybridScanPersistenceService.copyScanFile(source, destination);

    assertThat(readScan(destination)).isEqualTo(readScan(source));
  }

  @Test
  public void testCopyScanFile_S3ToS3() throws Exception {
    ScanEntity source = createScan(s3ScanPersistenceService);
    ScanEntity destination = s3ScanPersistenceService.getScan(OTHER_APP_ID, OTHER_SCAN_ID);

    hybridScanPersistenceService.copyScanFile(source, destination);

    assertThat(readScan(destination)).isEqualTo(readScan(source));
  }

  @Test
  public void testDeleteScansFor() throws Exception {
    ScanEntity s3ScanEntity1 = createScan(s3ScanPersistenceService, APP_ID, SCAN_ID);
    ScanEntity s3ScanEntity2 = createScan(s3ScanPersistenceService, APP_ID, SCAN_ID + "2");
    ScanEntity fileScanEntity1 = createScan(fileScanPersistenceService, APP_ID, SCAN_ID);
    ScanEntity fileScanEntity2 = createScan(fileScanPersistenceService, APP_ID, SCAN_ID + "2");

    hybridScanPersistenceService.deleteScansFor(APP_ID);

    assertThat(s3ScanEntity1.exists()).isFalse();
    assertThat(s3ScanEntity2.exists()).isFalse();
    assertThat(fileScanEntity1.exists()).isFalse();
    assertThat(fileScanEntity2.exists()).isFalse();
  }

  @Test
  public void testAllScanFilesFor() throws Exception {
    ScanEntity s3ScanEntity1 = createScan(s3ScanPersistenceService, APP_ID, SCAN_ID);
    ScanEntity s3ScanEntity2 = createScan(s3ScanPersistenceService, APP_ID, SCAN_ID + "2");
    ScanEntity fileScanEntity1 = createScan(fileScanPersistenceService, APP_ID, SCAN_ID);
    ScanEntity fileScanEntity2 = createScan(fileScanPersistenceService, APP_ID, SCAN_ID + "2");

    assertThat(hybridScanPersistenceService.allScanFilesFor(APP_ID).toList()).containsExactlyInAnyOrder(
        s3ScanEntity1,
        s3ScanEntity2,
        fileScanEntity1,
        fileScanEntity2
    );
  }

  @Test
  public void testDeleteScan() throws Exception {
    ScanEntity s3ScanEntity = createScan(s3ScanPersistenceService, APP_ID, SCAN_ID);
    ScanEntity fileScanEntity = createScan(fileScanPersistenceService, APP_ID, SCAN_ID);

    hybridScanPersistenceService.deleteScan(APP_ID, SCAN_ID);

    assertThat(s3ScanEntity.exists()).isFalse();
    assertThat(fileScanEntity.exists()).isFalse();
  }

  @Test
  public void testDoGetScan_ExistsOnlyInSecondary_WarnEnabled() throws Exception {
    enableWarning();
    assertWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridScanPersistenceService.doGetScan(APP_ID, SCAN_ID));
  }

  @Test
  public void testDoGetScan_ExistsOnlyInSecondary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(1),
        () -> hybridScanPersistenceService.doGetScan(APP_ID, SCAN_ID));
  }

  @Test
  public void testDoGetScan_ExistsOnlyInPrimary_WarnEnabled() throws Exception {
    enableWarning();
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridScanPersistenceService.doGetScan(APP_ID, SCAN_ID));
  }

  @Test
  public void testDoGetScan_ExistsOnlyInPrimary_WarnDisabled() throws Exception {
    assertNoWarning(new ArrayList<>(dataStoreTypes).get(0),
        () -> hybridScanPersistenceService.doGetScan(APP_ID, SCAN_ID));
  }

  private void enableWarning() {
    systemConfigurationPropertyDAO.set(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS, "true");
    hybridScanPersistenceService.configurationChanged(
        Collections.singleton(SystemConfigurationProperty.WARN_ON_NON_PRIMARY_STORAGE_ACCESS));
  }

  private void assertWarning(final DataStoreType dataStoreType, final Callable<?> callable) throws Exception {
    ScanPersistenceService service = scanPersistenceServiceProvider.get(dataStoreType);
    createScan(service.getScan(APP_ID, SCAN_ID));

    callable.call();

    logOutput.assertThat().atWarnLevel().contains("Non-primary storage access");
  }

  private void assertNoWarning(final DataStoreType dataStoreType, final Callable<?> callable) throws Exception {
    ScanPersistenceService service = scanPersistenceServiceProvider.get(dataStoreType);
    createScan(service.getScan(APP_ID, SCAN_ID));

    callable.call();

    logOutput.assertThat().atWarnLevel().doesNotContain("Non-primary storage access");
  }

  private ScanEntity createScan(final ScanPersistenceService scanPersistenceService) throws Exception {
    return createScan(scanPersistenceService.createTempScan(APP_ID));
  }

  private ScanEntity createScan(
      final ScanPersistenceService scanPersistenceService,
      final String appId,
      final String scanId) throws Exception
  {
    return createScan(scanPersistenceService.getScan(appId, scanId));
  }

  private ScanEntity createScan(final ScanEntity scanEntity) throws Exception {
    assertThat(scanEntity.exists()).isFalse();
    if (scanEntity instanceof FileScanEntity fileScanEntity) {
      Files.createDirectories(fileScanEntity.path().getParent());
    }
    String content = TemporaryEntity.uuid();
    try (OutputStream outputStream = scanEntity.getOutputStream()) {
      outputStream.write(content.getBytes(StandardCharsets.UTF_8));
    }
    assertThat(scanEntity.exists()).isTrue();
    assertThat(readScan(scanEntity)).isEqualTo(content);
    return scanEntity;
  }

  private String readScan(final ScanEntity scanEntity) throws Exception {
    try (InputStream inputStream = scanEntity.getInputStream()) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
