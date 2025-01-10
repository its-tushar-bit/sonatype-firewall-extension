/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.sonatype.clm.dto.model.signature.ComponentWithSignatures;
import com.sonatype.clm.dto.model.signature.ComponentWithSignaturesList;
import com.sonatype.clm.dto.model.signature.Signature;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.report.ApplicationReport.ReportType;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.config.ReportDataStoreConfig;
import com.sonatype.insight.brain.service.config.ReportDataStoreConfig.ReportDataStoreType;
import com.sonatype.insight.brain.service.config.ReportDataStoreConfig.S3DataStoreConfig;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ContainerNode;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.paginators.ListObjectsV2Iterable;

import static com.sonatype.insight.brain.api.experimental.ApiVulnerabilitySignatureService.VULNERABILITY_SIGNATURE_JSON_FILENAME;
import static com.sonatype.insight.brain.report.AbstractS3ReportEntity.CACHE_KEY_FORMAT_PREFIX;
import static com.sonatype.insight.brain.report.ReportTestUtils.zipReportDir;
import static java.nio.charset.Charset.defaultCharset;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@Category(SlowTest.class)
@RunWith(Parameterized.class)
public class S3ReportDataStoreTest
{
  private static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:3.5.0");

  @Rule
  public LocalStackContainer localstack = new LocalStackContainer(localstackImage).withServices(S3);

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Mock
  private ReportDownloader reportDownloader;

  @Mock
  private InsightConfig insightConfig;

  @Mock
  private Configuration configuration;

  @Mock
  private InsightWork insightWork;

  private final String prefix;

  private final String expectedPrefix;

  private AutoCloseable mocks;

  @Parameters
  public static List<Object[]> prefixes() {
    return Arrays.asList(new Object[][]{
        {null, ""},
        {"", ""},
        {"valid-prefix/with/path", "valid-prefix/with/path/"},
        {"valid-prefix/with/path/ends-with-slash/", "valid-prefix/with/path/ends-with-slash/"}
    });
  }

  public S3ReportDataStoreTest(String configuredPrefix, String expectedPrefix) {
    this.prefix = configuredPrefix;
    this.expectedPrefix = expectedPrefix;
  }

  S3ReportDataStore reportDataStore;

  private static final String bucketName = "test-bucket";

  private S3Client s3Client;

  @Before
  public void setUp() throws Exception {
    mocks = MockitoAnnotations.openMocks(this);
    ReportDataStoreConfig reportDataStoreConfig = new ReportDataStoreConfig();
    reportDataStoreConfig.setType(ReportDataStoreType.S3);
    S3DataStoreConfig s3Config = new S3DataStoreConfig();
    s3Config.setBucketName(bucketName);
    s3Config.setRegion("us-east-2");
    s3Config.setObjectKeyPrefix(prefix);
    s3Config.setEndpoint(localstack.getEndpoint());
    reportDataStoreConfig.setS3Config(s3Config);
    when(insightConfig.getReportDataStoreConfig()).thenReturn(reportDataStoreConfig);

    s3Client = S3Client.builder()
        .endpointOverride(localstack.getEndpoint())
        .region(Region.of(s3Config.getRegion()))
        .credentialsProvider(StaticCredentialsProvider.create(
            AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()))).build();
    s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());

    reportDataStore =
        spy(new S3ReportDataStore(insightConfig, reportDownloader, configuration, s3Client, insightWork));
  }

  @After
  public void tearDown() throws Exception {
    mocks.close();
  }

  @Test(expected = NotFoundException.class)
  public void testDownloadReport_ThrowsExceptionWhenDownloadFails() throws IOException {
    String appId = "app";
    String scanId = "scanId";

    when(reportDownloader.downloadReport(eq(scanId), any(), anyInt(), anyInt())).thenReturn(false);

    reportDataStore.downloadReport(appId, scanId, (s, t, a) -> {
    });
  }

  @Test
  public void testDownloadReport_AllEntriesExistAfterDownload() throws Exception {
    String appId = "app";
    String scanId = "scanId";

    downloadReport(scanId, appId, "/S3ObjectStorageReportDataStoreTest/report");

    S3ApplicationReport applicationReport = reportDataStore.getApplicationReport(appId, scanId);

    assertThat(applicationReport.exists()).isTrue();

    List<ReportEntry> actualEntries =
        List.of(applicationReport.getEntry("allartifacts.json"), applicationReport.getEntry("badges.json"),
            applicationReport.getEntry("bom.json"), applicationReport.getEntry("data.json"),
            applicationReport.getEntry("dependencies.json"), applicationReport.getEntry("index.html"),
            applicationReport.getEntry("licenselist.json"), applicationReport.getEntry("licenses.json"),
            applicationReport.getEntry("licensethreats.json"), applicationReport.getEntry("partialmatched.json"),
            applicationReport.getEntry("popularity.json"), applicationReport.getEntry("security.json"),
            applicationReport.getEntry("summary.json"));

    assertThat(actualEntries)
        .allSatisfy(reportEntry -> assertThat(reportEntry.buf).hasSizeGreaterThan(0));
  }

  @Test
  public void testGetReportEntityByName_CanReadAndWriteObjects() throws IOException {
    String appId = "app";
    String scanId = "scanId";
    ReportEntity vulnSignatures =
        reportDataStore.getReportEntityByName(appId, scanId, VULNERABILITY_SIGNATURE_JSON_FILENAME);
    assertThat(vulnSignatures.exists()).isFalse();

    ComponentWithSignaturesList signaturesList = new ComponentWithSignaturesList();
    signaturesList.setComponents(List.of(new ComponentWithSignatures("fake-package-url", new Signature())));
    try (OutputStream os = vulnSignatures.getOutputStream()) {
      JsonUtils.write(os, signaturesList);
    }

    assertThat(vulnSignatures.exists()).isTrue();

    try (InputStream is = vulnSignatures.getInputStream()) {
      signaturesList = JsonUtils.read(is, ComponentWithSignaturesList.class);
    }

    assertThat(signaturesList.getComponents()).hasSize(1);
  }

  @Test
  public void testGetApplicationReport_CanGetAndPutEntries() throws IOException {
    String appId = "app";
    String scanId = "scanId";

    S3ApplicationReport applicationReport = reportDataStore.getApplicationReport(appId, scanId);

    String newEntryName = "new-entry.txt";
    ReportEntry newEntry = applicationReport.getEntry(newEntryName);

    assertThat(newEntry).isNull();

    String originalJson = "{\"json\":\"data\"}";

    applicationReport.putEntry(newEntryName, originalJson.getBytes(StandardCharsets.UTF_8));
    newEntry = applicationReport.getEntry(newEntryName);

    assertThat(newEntry).isNotNull();
    assertThat(new String(newEntry.buf, StandardCharsets.UTF_8)).isEqualTo("{\"json\":\"data\"}");

    String updatedJson = "{\"json\":\"data updated\"}";
    ContainerNode<?> originalContainerNode = JsonUtils.parse(updatedJson);

    applicationReport.saveReportEntry(newEntryName, originalContainerNode);
    newEntry = applicationReport.getEntry(newEntryName);

    assertThat(newEntry).isNotNull();
    assertThat(new String(newEntry.buf, StandardCharsets.UTF_8)).isEqualTo("""
        {
          "json" : "data updated"
        }""");

    ContainerNode<?> actualContainerNode = applicationReport.loadReportEntry(newEntryName);
    assertThat(actualContainerNode).isEqualTo(originalContainerNode);
  }

  @Test
  public void testGetEntry_DoesNotAllowDotDotPaths() throws URISyntaxException, IOException {
    String appId = "app";
    String scanId = "scanId";

    S3ApplicationReport applicationReport = reportDataStore.getApplicationReport(appId, scanId);

    downloadReport(scanId, appId, "/S3ObjectStorageReportDataStoreTest/report");

    assertThat(applicationReport.getEntry("../")).isNull();
  }

  @Test
  public void testDeleteCacheDir_WillDeleteCacheDirectoryObjects() throws URISyntaxException, IOException {
    String appId = "appId";
    String scanId = "scanId";

    downloadReport(scanId, appId, "/S3ObjectStorageReportDataStoreTest/report");

    S3ApplicationReport applicationReport = reportDataStore.getApplicationReport(appId, scanId);
    applicationReport.putEntry("some-entry.json", "{}".getBytes(StandardCharsets.UTF_8));
    applicationReport.putEntry("another-entry.json", "{}".getBytes(StandardCharsets.UTF_8));

    ListObjectsV2Request listObjectsV2Request = ListObjectsV2Request.builder().bucket(bucketName)
        .prefix(expectedPrefix + String.format(CACHE_KEY_FORMAT_PREFIX, appId, scanId)).build();

    List<String> objectKeys = getObjectKeys(listObjectsV2Request);
    assertThat(objectKeys).containsExactlyInAnyOrder(
        expectedPrefix + "sonatype-work/report/appId/scanId/report.cache/some-entry.json",
        expectedPrefix + "sonatype-work/report/appId/scanId/report.cache/another-entry.json");

    applicationReport.deleteCacheDir();

    objectKeys = getObjectKeys(listObjectsV2Request);
    assertThat(objectKeys).isEmpty();
  }

  @Test
  public void testDeletePdfReport_WillDeletePdfReport() throws URISyntaxException, IOException {
    String appId = "appId";
    String scanId = "scanId";

    downloadReport(scanId, appId, "/S3ObjectStorageReportDataStoreTest/report");
    S3ApplicationReport applicationReport = reportDataStore.getApplicationReport(appId, scanId);
    ReportPdf reportPdf = reportDataStore.getReportPdf(appId, scanId);

    assertThat(reportPdf.exists()).isFalse();

    try (OutputStream outputStream = reportPdf.getOutputStream()) {
      outputStream.write("fake-pdf-data".getBytes(StandardCharsets.UTF_8));
    }

    assertThat(reportPdf.exists()).isTrue();
    applicationReport.deletePdfReport();
    assertThat(reportPdf.exists()).isFalse();
  }

  @Test
  public void testGetType_WillDetermineTheCorrectTypes() throws URISyntaxException, IOException {
    String appId = "appId";
    String scanId = "scanId";

    downloadReport(scanId, appId, "/S3ObjectStorageReportDataStoreTest/report");
    S3ApplicationReport applicationReport = reportDataStore.getApplicationReport(appId, scanId);
    S3ApplicationReport incompleteReport = reportDataStore.getApplicationReport("incomplete-report", scanId);

    assertThat(applicationReport.getType()).isEqualTo(ReportType.FULL);
    assertThat(incompleteReport.getType()).isEqualTo(ReportType.ERROR);
  }

  @Test
  public void testGetTemplateProperties_WillLoadTemplateProperties() throws URISyntaxException, IOException {
    String scanId = "scanId";
    String appId = "appId";

    downloadReport(scanId, appId, "/S3ObjectStorageReportDataStoreTest/report");
    S3ApplicationReport applicationReport = reportDataStore.getApplicationReport(appId, scanId);
    Properties noProperties = applicationReport.getTemplateProperties();
    assertThat(noProperties.keySet()).isEmpty();

    String appIdWithTemplateProperties = "with-template-resources";
    downloadReport(scanId, appIdWithTemplateProperties,
        "/S3ObjectStorageReportDataStoreTest/report-with-template-props");
    S3ApplicationReport withTemplateProperties =
        reportDataStore.getApplicationReport(appIdWithTemplateProperties, scanId);
    Properties templateProperties = withTemplateProperties.getTemplateProperties();
    assertThat(templateProperties.stringPropertyNames()).containsExactlyInAnyOrder("cip.details.path", "data.version",
        "cip.list.path");
  }

  @Test(expected = NoSuchKeyException.class)
  public void testGetInputStream_WillThrowExceptionWhenDoesNotExist() throws IOException {
    String scanId = "scanId";
    String appId = "appId";

    S3ApplicationReport applicationReport = reportDataStore.getApplicationReport(appId, scanId);
    IOUtils.toByteArray(applicationReport.getInputStream());
  }

  @Test
  public void testGetOutputStream_WillOverwriteExistingObjects() throws IOException, URISyntaxException {
    String scanId = "scanId";
    String appId = "appId";
    String objectName = "some_extra_file.txt";

    downloadReport(scanId, appId, "/S3ObjectStorageReportDataStoreTest/report");

    ReportEntity reportEntityByName = reportDataStore.getReportEntityByName(appId, scanId, objectName);
    try (OutputStream os = reportEntityByName.getOutputStream()) {
      IOUtils.write("first", os, defaultCharset());
    }
    reportEntityByName = reportDataStore.getReportEntityByName(appId, scanId, objectName);
    assertThat(reportEntityByName.exists()).isTrue();
    try (InputStream is = reportEntityByName.getInputStream()) {
      assertThat(IOUtils.toString(is, defaultCharset())).isEqualTo("first");
    }

    try (OutputStream os = reportEntityByName.getOutputStream()) {
      IOUtils.write("second", os, defaultCharset());
    }
    reportEntityByName = reportDataStore.getReportEntityByName(appId, scanId, objectName);
    try (InputStream is = reportEntityByName.getInputStream()) {
      assertThat(IOUtils.toString(is, defaultCharset())).isEqualTo("second");
    }
  }

  private List<String> getObjectKeys(ListObjectsV2Request listObjectsV2Request) {
    List<String> objectKeys = new ArrayList<>();
    ListObjectsV2Iterable listObjectsV2Iterable = s3Client.listObjectsV2Paginator(listObjectsV2Request);
    for (ListObjectsV2Response response : listObjectsV2Iterable) {
      List<S3Object> objects = response.contents();
      if (objects.isEmpty()) {
        break;
      }
      objectKeys.addAll(objects.stream().map(S3Object::key).toList());
    }
    return objectKeys;
  }

  private void downloadReport(final String scanId, final String appId, final String reportResourceName)
      throws URISyntaxException, IOException
  {
    File reportZip = zipReportDir(reportResourceName, tempDir);
    FileApplicationReport fileReportEntity = new FileApplicationReport(reportZip);
    when(reportDownloader.downloadReport(eq(scanId), eq(fileReportEntity), anyInt(), anyInt())).thenReturn(true);
    when(reportDataStore.tempReport(appId, scanId)).thenReturn(fileReportEntity);
    reportDataStore.downloadReport(appId, scanId, (s, t, a) -> {
    });
  }
}
