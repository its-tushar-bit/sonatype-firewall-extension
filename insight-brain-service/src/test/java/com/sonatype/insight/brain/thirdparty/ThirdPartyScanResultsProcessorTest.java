/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.utils.Xpp3Util;
import com.sonatype.insight.scan.manifest.ClairScannerResult;
import com.sonatype.insight.scan.manifest.ClairScannerVulnerability;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.scan.model.ScanFileNames;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;
import com.sonatype.insight.test.LogOutput;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.cyclonedx.parsers.Parser;
import org.cyclonedx.parsers.XmlParser;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.xmlunit.assertj.XmlAssert;

import static com.sonatype.insight.scan.model.ItemContentType.IAC_FILE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ThirdPartyScanResultsProcessorTest
    extends AbstractComponentTest
{
  private final String loggerName = ThirdPartyScanResultsProcessor.class.getName();

  @Rule
  public LogOutput logOutput = new LogOutput(loggerName);

  @Inject
  private ThirdPartyScanDAO thirdPartyScanDAO;

  @Inject
  private ThirdPartyFileDAO thirdPartyFileDAO;

  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Mock
  private TelemetrySender telemetrySender;

  private ThirdPartyScanResultsProcessor thirdPartyScanResultsProcessorSpy;

  private static final Gson GSON = new Gson();

  @Before
  public void before() {
    thirdPartyScanResultsProcessorSpy =
        spy(new ThirdPartyScanResultsProcessor(thirdPartyScanDAO, thirdPartyFileDAO, telemetrySender));
  }

  @Test
  public void testHandle_EmptyItemElement() throws Exception {
    File scanFile = getScanFile("scan-with-empty-item-data.xml");
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempScanFile, tempDir.getRoot(), null);
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class));
    assertEmptyItemElement(tempScanFile);
  }

  @Test
  public void testHandle_thirdPartyWithOtherContent() throws Exception {
    File scanFile = getScanFile("scan-thirdparty-and-other-content.xml");
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempScanFile, tempDir.getRoot(), null);
    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(any(ItemContentType.class));
    assertXml(tempScanFile, "scan-thirdparty-and-other-content-expected.xml");
  }

  @Test
  public void testHandle_sbom_api() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-data-api.xml");
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempScanFile, tempDir.getRoot(), null);
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SBOM));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SBOM, true, 2);
  }

  @Test
  public void testHandle_sbom_api_TelemetryData() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-data-api.xml");
    TelemetryData telemetryData = buildThirdPartyScanTelemetryData();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempDir.newFile(), tempDir.getRoot(), telemetryData);
    verify(telemetrySender).send(telemetryData);
    assertTelemetryData(telemetryData, "SBOM");
  }

  @Test
  public void testHandle_sbom_cli() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-data-cli.xml");
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempScanFile, tempDir.getRoot(), null);

    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(eq(ItemContentType.SBOM));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SBOM, true, 2);
  }

  @Test
  public void testHandle_container_content() throws Exception {
    File scanFile = getScanFile("container/scan-with-container-content.xml");
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempScanFile, tempDir.getRoot(), null);

    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.CONTAINER_URI));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.CONTAINER_URI, true, 9);
  }

  @Test
  public void testHandle_sbomUsingSameSbomFileRepeatedContent() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-repeated-content.xml");

    File tempScanFile = tempDir.newFile();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempScanFile, tempDir.getRoot(), null);

    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(any(ItemContentType.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.SBOM, true, 2);
  }

  @Test
  public void testHandle_InvalidJson() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data-invalid-json.xml");
    String scanId = tempEntity.uuid();
    String scanRequestId =
        thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempDir.newFile(), tempDir.getRoot(), null);
    thirdPartyScanResultsProcessorSpy.postHandle(scanId, scanRequestId);

    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(any(ItemContentType.class));

    List<ThirdPartyFile> thirdPartyFiles = thirdPartyFileDAO.getByScanId(scanId);
    assertThirdPartyFile(thirdPartyFiles, 1, "clair-scanner-output.json");
    assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFiles.get(0).getId())).isEmpty();
  }

  @Test
  public void testHandle_ClairScanner_TelemetryData() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-repeated-content.xml");
    TelemetryData telemetryData = buildThirdPartyScanTelemetryData();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempDir.newFile(), tempDir.getRoot(), telemetryData);
    verify(telemetrySender, times(2)).send(telemetryData);
    assertTelemetryData(telemetryData, "CLAIR_SCANNER");
  }

  @Test
  public void testHandle_ClairScanner() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data.xml");
    String scanId = tempEntity.uuid();

    File tempScanFile = tempDir.newFile();
    String scanRequestId =
        thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempScanFile, tempDir.getRoot(), null);
    thirdPartyScanResultsProcessorSpy.postHandle(scanId, scanRequestId);

    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(eq(ItemContentType.CLAIR_SCANNER));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.CLAIR_SCANNER, false, 3);

    List<ThirdPartyFile> thirdPartyFileList = thirdPartyFileDAO.getByScanId(scanId);
    assertThirdPartyFile(thirdPartyFileList, 2, "clair-scanner-out/clair-scanner-output.json",
        "clair-scanner-out/other/clair-scanner-output.json");
  }

  @Test
  public void testHandle_ClairScannerUsingSameClairFileRepeatedContent() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-repeated-content.xml");
    String scanId = tempEntity.uuid();

    File tempScanFile = tempDir.newFile();
    String scanRequestId =
        thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempScanFile, tempDir.getRoot(), null);
    thirdPartyScanResultsProcessorSpy.postHandle(scanId, scanRequestId);

    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(any(ItemContentType.class));
    assertFilteredThirdPartyScanContentFile(tempScanFile, ItemContentType.CLAIR_SCANNER, false, 3);

    assertThat(scanId).isNotNull();
    List<ThirdPartyFile> thirdPartyFileList = thirdPartyFileDAO.getByScanId(scanId);
    assertThirdPartyFile(thirdPartyFileList, 2, "clair-scanner-output.json", "clair-scanner-output.json");
  }

  @Test
  public void testHandle_iac_content() throws Exception {
    TelemetryData telemetryData = buildThirdPartyScanTelemetryData();
    File scanFile = getScanFile("iac/scan-with-iac-content.xml");
    File tempScanFile = tempDir.newFile();
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempScanFile, tempDir.getRoot(), telemetryData);

    verify(telemetrySender, times(1)).send(telemetryData);
    assertFilteredThirdPartyScanContentFile(tempScanFile, IAC_FILE, true, 0);
  }

  @Test
  public void testHandle_ClairCorruptFile() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data-corrupted.xml");
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
      thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempDir.newFile(), tempDir.getRoot(), null);
    }).withMessage("Error reading/processing third party scan content from scan file");
  }

  @Test
  public void testHandle_corruptSbomFile() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-data-corrupted.xml");
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> {
      thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempDir.newFile(), tempDir.getRoot(), null);
    }).withMessage("Error reading/processing third party scan content from scan file");
  }

  @Test
  public void testHandle_noSbomContent() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-empty-sbom-content.xml");
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempDir.newFile(), tempDir.getRoot(), null);
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class));
  }

  @Test
  public void testHandle_SbomDependencyTree_basic() throws Exception {
    testHandle_SbomDependencyTree("DependencyGraph/scan-sbom-dependencies-basic.xml",
        "DependencyGraph/scan-sbom-dependencies-basic-expected.xml");
  }

  @Test
  public void testHandle_SbomDependencyTree_cyclic() throws Exception {
    testHandle_SbomDependencyTree("DependencyGraph/scan-sbom-dependencies-cyclic.xml",
        "DependencyGraph/scan-sbom-dependencies-cyclic-expected.xml");
  }

  @Test
  public void testHandle_SbomDependencyTree_multiformats() throws Exception {
    testHandle_SbomDependencyTree("DependencyGraph/scan-sbom-dependencies-multiformat.xml",
        "DependencyGraph/scan-sbom-dependencies-multiformat-expected.xml");
  }

  private void testHandle_SbomDependencyTree(final String s, final String s2) throws Exception {
    File scanFile = getScanFile(s);
    File tempScanFile = tempDir.newFile();

    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempScanFile, tempDir.getRoot(), null);
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(ItemContentType.SBOM);
    XmlAssert.assertThat(contentOf(tempScanFile))
        .and(IOUtils.toString(getTestResource(s2), UTF_8))
        .ignoreWhitespace()
        .areIdentical();
  }

  private String contentOf(File gzippedScanFile) throws IOException {
    return IOUtils.toString(new GZIPInputStream(new FileInputStream(gzippedScanFile)), UTF_8);
  }

  private File getScanXMLFile(File scanFile) throws Exception {
    File output = tempDir.newFile("scan-test.xml");
    try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(scanFile))) {
      IOUtils.copy(gis, new FileOutputStream(output));
    }
    return output;
  }

  private void assertXml(File scanFile, String expectedFileName) throws Exception {
    URL resource = getTestResource(expectedFileName);
    File expectedFile = new File(resource.toURI());
    File actualFile = getScanXMLFile(scanFile);
    XmlAssert.assertThat(actualFile).and(expectedFile).areIdentical().ignoreWhitespace();
  }

  private void assertEmptyItemElement(File scanFile) throws Exception {
    try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(scanFile))) {
      XmlPullParser parser = new MXParser();
      parser.setInput(new XmlStreamReader(gis));

      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if ("item".equals(parser.getName())) {
          String content = parser.getAttributeValue(null, "contentType");
          Xpp3Dom itemElement = Xpp3Util.loadElement("item", parser);
          assertThat(itemElement.getChildren()).isEmpty();
          assertThat(content).isNull();
          assertThat(logOutput.getErrorMessages(loggerName)).isNullOrEmpty();
        }
        eventType = parser.next();
      }
    }
  }

  @Test
  public void testHandle_NoThirdPartyContent() throws Exception {
    File scanFile = getScanFile("scan-without-thirdparty-content.xml");
    thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempDir.newFile(), tempDir.getRoot(), null);
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class));
  }

  @Test
  public void testHandle_InvalidFile() throws Exception {
    File scanFile = getScanFile("empty-scan.xml");
    assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
        thirdPartyScanResultsProcessorSpy.filterAndSaveData(scanFile, tempDir.newFile(), tempDir.getRoot(), null)
    ).withMessage("Error reading/processing third party scan content from scan file");
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class));
  }

  private void assertLogOutput(final String message) {
    assertThat(logOutput.getErrorMessages(loggerName)).containsOnly(message);
  }

  private File getScanFile(final String fileName) throws Exception {
    URL resource = getTestResource(fileName);
    // Gzip the Third Party scan file
    File sonatypeScanGzipFile = tempDir.newFile(ScanFileNames.SONATYPE_SCAN_FILENAME);
    try (GZIPOutputStream gzipStream = new GZIPOutputStream(new FileOutputStream(sonatypeScanGzipFile))) {
      FileUtils.copyFile(new File(resource.toURI()), gzipStream);
    }
    return sonatypeScanGzipFile;
  }

  private URL getTestResource(final String fileName) {
    return getClass().getResource("/ThirdPartyResultsProcessorTest/" + fileName);
  }

  private void assertFilteredThirdPartyScanContentFile(
      File scanFile,
      ItemContentType itemContentType,
      boolean optionalValuesPresent,
      int expectedComponentCount) throws Exception
  {
    try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(scanFile))) {
      XmlPullParser parser = new MXParser();
      parser.setInput(new XmlStreamReader(gis));

      int eventType = parser.getEventType();
      while (eventType != XmlPullParser.END_DOCUMENT) {
        if (eventType == XmlPullParser.START_TAG) {
          String elementName = parser.getName();
          if ("item".equals(elementName)) {
            String contentType = parser.getAttributeValue(null, "contentType");
            assertThat(contentType).isNotNull();
            Xpp3Dom itemElement = Xpp3Util.loadElement("item", parser);
            // Item element is empty, so it's skipped
            if (itemElement.getChildCount() > 0) {
              Xpp3Dom contentElement = itemElement.getChild("content");
              assertThat(contentElement.getValue()).isNotNull();
              if (ItemContentType.CLAIR_SCANNER == itemContentType) {
                assertFilteredClairScanContentFile(contentElement.getValue(), contentType, expectedComponentCount);
              }
              else if (ItemContentType.SBOM == itemContentType) {
                assertFilteredScanContentFile(contentElement.getValue(), contentType, optionalValuesPresent,
                    expectedComponentCount, ItemContentType.SBOM);
              }
              else if (ItemContentType.CONTAINER_URI == itemContentType) {
                assertFilteredScanContentFile(contentElement.getValue(), contentType, optionalValuesPresent,
                    expectedComponentCount, ItemContentType.CONTAINER_URI);
              }
              else if (IAC_FILE == itemContentType) {
                final String expectedIacXmlContent = "terraform";
                assertThat(contentElement.getValue()).isEqualTo(expectedIacXmlContent);
              }
            }
            else {
              assertLogOutput("scan file scan.xml.gz contained a third party scan item " + itemContentType
                  + " without any content");
            }
          }
        }
        eventType = parser.next();
      }
    }
  }

  private void assertFilteredScanContentFile(
      final String xml,
      final String contentType,
      final boolean optionalValuesPresent,
      final int expectedComponentCount,
      final ItemContentType itemContentType) throws ParseException
  {
    assertThat(contentType).isEqualTo(itemContentType.name());
    Bom bom = getBom(xml);
    assertThat(bom).isNotNull();
    assertThat(bom.getComponents()).hasSize(expectedComponentCount);
    assertThat(bom.getSerialNumber()).isNull();
    assertThat(bom.getVersion()).isNotNull();
    assertThat(bom.getExternalReferences()).isNull();
    assertThat(bom.getExternalReferences()).isNull();

    for (Component component : bom.getComponents()) {
      assertThat(component.getComponents()).isNull();
      assertThat(component.getName()).isNotNull();
      assertThat(component.getVersion()).isNotNull();
      assertThat(component.getType()).isNotNull();
      if (optionalValuesPresent) {
        assertThat(component.getPurl()).isNotNull();
      }
      else {
        assertThat(component.getPurl()).isNull();
        assertThat(component.getHashes()).isNull();
      }

      assertThat(component.getComponents()).isNull();
      assertThat(component.getCopyright()).isNull();
      assertThat(component.getDescription()).isNull();
      assertThat(component.getExternalReferences()).isNull();
      assertThat(component.getExtensibleTypes()).isNull();
      assertThat(component.getGroup()).isNotNull();
      assertThat(component.getLicenseChoice()).isNull();
      assertThat(component.getPedigree()).isNull();
      assertThat(component.getPublisher()).isNull();
    }
  }

  private void assertFilteredClairScanContentFile(String json, String contentType, int expectedComponentCount) {
    assertThat(contentType).isEqualTo(ItemContentType.CLAIR_SCANNER.name());
    ClairScannerResult clairScannerResult =
        GSON.fromJson(StringEscapeUtils.unescapeXml(json), ClairScannerResult.class);
    assertThat(clairScannerResult).isNotNull();
    assertThat(clairScannerResult.getImage()).isNull();
    assertThat(clairScannerResult.getVulnerabilities()).isNotNull();
    assertThat(clairScannerResult.getVulnerabilities()).hasSize(expectedComponentCount);

    for (ClairScannerVulnerability vulnerability : clairScannerResult.getVulnerabilities()) {
      assertThat(vulnerability).isNotNull();
      assertThat(vulnerability.getFeatureName()).isNotNull();
      assertThat(vulnerability.getFeatureVersion()).isNotNull();
      assertThat(vulnerability.getNamespace()).isNotNull();

      assertThat(vulnerability.getDescription()).isNull();
      assertThat(vulnerability.getVulnerability()).isNull();
      assertThat(vulnerability.getLink()).isNull();
      assertThat(vulnerability.getSeverity()).isNull();
    }
  }

  private Bom getBom(String content) throws ParseException {
    Parser parser = new XmlParser();
    return parser.parse(new StringReader(content));
  }

  private void assertThirdPartyFile(
      List<ThirdPartyFile> thirdPartyFileList,
      int expectedFiles,
      String... expectedFilenames)
  {
    assertThat(thirdPartyFileList).isNotEmpty();
    assertThat(thirdPartyFileList).hasSize(expectedFiles);
    List<String> fileNames = thirdPartyFileList.stream().map(ThirdPartyFile::getFilename).collect(Collectors.toList());
    assertThat(fileNames).containsExactlyInAnyOrder(expectedFilenames);

    for (ThirdPartyFile thirdPartyFile : thirdPartyFileList) {
      assertThat(thirdPartyFile.getFilename()).isNotNull();
    }
  }

  private void assertTelemetryData(TelemetryData telemetryData, String contentType) {
    assertThat(telemetryData.getAttributes()).hasSize(5);
    assertThat(telemetryData.getAttributes())
        .contains(entry("application_id", "appId"), entry("stage_id", "build"), entry("source", "api"),
            entry("user_agent", "agent"), entry("content_type", contentType));
  }

  private TelemetryData buildThirdPartyScanTelemetryData() {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put("application_id", "appId");
    attributes.put("stage_id", "build");
    attributes.put("source", "api");
    attributes.put("user_agent", "agent");

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.THIRD_PARTY_SCAN_USAGE);
    telemetryData.setAttributes(attributes);
    return telemetryData;
  }
}
