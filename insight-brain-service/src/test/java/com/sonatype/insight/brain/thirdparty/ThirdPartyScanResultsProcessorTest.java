/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyScanDAO;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.utils.Xpp3Util;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.scan.file.clair.ClairScannerResult;
import com.sonatype.insight.scan.file.clair.ClairScannerVulnerability;
import com.sonatype.insight.scan.model.ItemContentType;
import com.sonatype.insight.scan.model.ScanFileNames;
import com.sonatype.insight.test.LogOutput;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.cyclonedx.BomParser;
import org.cyclonedx.exception.ParseException;
import org.cyclonedx.model.Bom;
import org.cyclonedx.model.Component;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Spy;
import org.xmlunit.assertj.XmlAssert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class ThirdPartyScanResultsProcessorTest
    extends AbstractComponentTest
{
  private final String loggerName = ThirdPartyScanResultsProcessor.class.getName();

  @Spy
  private ClairScannerResultHandler clairHandlerSpy;

  @Spy
  private SbomResultHandler sbomHandlerSpy;

  @Rule
  public LogOutput logOutput = new LogOutput(loggerName);

  @Inject
  private ThirdPartyScanDAO thirdPartyScanDAO;

  @Inject
  private ThirdPartyFileDAO thirdPartyFileDAO;

  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  private ThirdPartyScanResultsProcessor thirdPartyScanResultsProcessorSpy;

  private static final Gson GSON = new Gson();

  @Rule
  public TemporaryFolder tmpDir = new TemporaryFolder();

  @Before
  public void before() {
    thirdPartyScanResultsProcessorSpy = spy(new ThirdPartyScanResultsProcessor(thirdPartyScanDAO, thirdPartyFileDAO));
    lenient().doReturn(clairHandlerSpy).when(thirdPartyScanResultsProcessorSpy)
        .createHandler(eq(ItemContentType.CLAIR_SCANNER));
    lenient().doReturn(sbomHandlerSpy).when(thirdPartyScanResultsProcessorSpy).createHandler(eq(ItemContentType.SBOM));
  }

  @Test
  public void testHandle_ClairScanner() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data.xml");

    doReturn("683620ac905c1d32b58c").when(clairHandlerSpy).buildHash(eq("debian-9:apt:1.4.8"));
    doReturn("e587ce87ed894c1d5283").when(clairHandlerSpy).buildHash(eq("debian-9:glibc:2.24-11+deb9u3"));
    doReturn("aff6a96471f042e1d975").when(clairHandlerSpy).buildHash(eq("debian-9:libxslt:1.1.29-2.1"));

    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);
    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(eq(ItemContentType.CLAIR_SCANNER));
    assertFilteredThirdPartyScanContentFile(scanFile, ItemContentType.CLAIR_SCANNER, false, 3);

    try (TransactionContext tx = thirdPartyScanDAO.createTransactionContext()) {
      ThirdPartyFile thirdPartyFile1 =
          thirdPartyFileDAO.getByHashAndScanId("30a7c753d9515c185d85", getScanId(scanRequestId)).get(0);
      assertThirdPartyFile(scanRequestId, tx, thirdPartyFile1, "clair-scanner-out/clair-scanner-output.json",
          "smart-brain-boost-api-dockerized_postgres", 1);

      ThirdPartyFile thirdPartyFile2 =
          thirdPartyFileDAO.getByHashAndScanId("48a7c753d9515c185d75", getScanId(scanRequestId)).get(0);
      assertThirdPartyFile(scanRequestId, tx, thirdPartyFile2, "clair-scanner-out/other/clair-scanner-output.json",
          "smart-brain-boost-api-dockerized_postgres", 1);
    }
  }

  @Test
  public void testHandle_sbom_api() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-data-api.xml");

    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);
    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SBOM));
    assertFilteredThirdPartyScanContentFile(scanFile, ItemContentType.SBOM, true, 2);
  }

  @Test
  public void testHandle_sbom_cli() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-data-cli.xml");

    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);
    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(eq(ItemContentType.SBOM));
    assertFilteredThirdPartyScanContentFile(scanFile, ItemContentType.SBOM, true, 2);
  }

  @Test
  public void testHandle_sbom_no_purl() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-no-purl.xml");

    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);
    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(eq(ItemContentType.SBOM));
    assertFilteredThirdPartyScanContentFile(scanFile, ItemContentType.SBOM, false, 0);
  }

  @Test
  public void testHandle_ClairScannerUsingSameClairFileMultipleTimes() throws Exception {
    doReturn("9510c290c07710d8c69b").when(clairHandlerSpy).buildHash(eq("debian-9:apt:1.3.8"));
    doReturn("08d7a1c700d1633dc309").when(clairHandlerSpy).buildHash(eq("debian-9:apt:1.3.9"));
    doReturn("e587ce87ed894c1d5283").when(clairHandlerSpy).buildHash(eq("debian-9:glibc:2.24-11+deb9u3"));

    File scanFile1 = getScanFile("scan-with-clair-scanner-for-multiple-times.xml");

    File scanFile2 = new File(tempDir.newFolder(), scanFile1.getName());
    FileUtils.copyFile(scanFile1, scanFile2);

    String scanRequestId1 = thirdPartyScanResultsProcessorSpy.handle(scanFile1);
    String scanRequestId2 = thirdPartyScanResultsProcessorSpy.handle(scanFile2);

    assertThat(scanRequestId1).isNotBlank();
    assertThat(scanRequestId2).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(any(ItemContentType.class));
    assertFilteredThirdPartyScanContentFile(scanFile2, ItemContentType.CLAIR_SCANNER, false, 3);

    try (TransactionContext tx = thirdPartyScanDAO.createTransactionContext()) {
      List<ThirdPartyFile> thirdPartyFiles =
          thirdPartyFileDAO.getByHashAndScanId("a7cea8ebc1ab163d7b1a", getScanId(scanRequestId1));

      assertThat(thirdPartyFiles).hasSize(2);
      assertThirdPartyFile(scanRequestId1, tx, thirdPartyFiles.get(0), "clair-scanner-output.json", "image-name", 1);
      assertThirdPartyFile(scanRequestId2, tx, thirdPartyFiles.get(1), "clair-scanner-output.json", "image-name", 1);
    }
  }

  @Test
  public void testHandle_ClairScannerUsingSameClairFileRepeatedContent() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-repeated-content.xml");

    doReturn("9510c290c07710d8c69b").when(clairHandlerSpy).buildHash(eq("debian-9:apt:1.3.8"));
    doReturn("08d7a1c700d1633dc309").when(clairHandlerSpy).buildHash(eq("debian-9:apt:1.3.9"));
    doReturn("e587ce87ed894c1d5283").when(clairHandlerSpy).buildHash(eq("debian-9:glibc:2.24-11+deb9u3"));

    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);

    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(any(ItemContentType.class));
    assertFilteredThirdPartyScanContentFile(scanFile, ItemContentType.CLAIR_SCANNER, false, 3);

    try (TransactionContext tx = thirdPartyScanDAO.createTransactionContext()) {
      ThirdPartyFile thirdPartyFile =
          thirdPartyFileDAO.getByHashAndScanId("a7cea8ebc1ab163d7b1x", getScanId(scanRequestId)).get(0);
      assertThirdPartyFile(scanRequestId, tx, thirdPartyFile, "clair-scanner-output.json", "test-image-name", 1);
    }
  }

  @Test
  public void testHandle_sbomUsingSameSbomFileRepeatedContent() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-repeated-content.xml");
    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);

    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(any(ItemContentType.class));
    assertFilteredThirdPartyScanContentFile(scanFile, ItemContentType.SBOM, true, 2);
  }

  @Test
  public void testHandle_EmptyItemElement() throws Exception {
    File scanFile = getScanFile("scan-with-empty-item-data.xml");
    thirdPartyScanResultsProcessorSpy.handle(scanFile);
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class));
    assertEmptyItemElement(scanFile);
  }
  
  @Test
  public void testHandle_thirdPartyWithOtherContent() throws Exception {
    File scanFile = getScanFile("scan-thirdparty-and-other-content.xml");

    doReturn("9510c290c07710d8c69b").when(clairHandlerSpy).buildHash(eq("debian-9:apt:1.4.8"));
    doReturn("e587ce87ed894c1d5283").when(clairHandlerSpy).buildHash(eq("debian-9:glibc:2.24-11+deb9u3"));
    doReturn("08d7a1c700d1633dc309").when(clairHandlerSpy).buildHash(eq("debian-9:libxslt:1.1.29-2.1"));
    thirdPartyScanResultsProcessorSpy.handle(scanFile);
    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(any(ItemContentType.class));
    assertXml(scanFile, "scan-thirdparty-and-other-content-expected.xml");
  }
  
  @Test
  public void testHandle_sbomNestedComponents() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-nested-component.xml");

    thirdPartyScanResultsProcessorSpy.handle(scanFile);
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(any(ItemContentType.class));
    assertXml(scanFile, "sbom/scan-with-sbom-nested-component-expected.xml");
  }

  private File getScanXMLFile(File scanFile) throws Exception {
    File output = tmpDir.newFile("scan-test.xml");
    try (GZIPInputStream gis = new GZIPInputStream(new FileInputStream(scanFile))) {
      IOUtils.copy(gis, new FileOutputStream(output));
    }
    return output;
  }

  private void assertXml(File scanFile, String expectedFileName) throws Exception {
    URL resource =
        getClass().getResource("/ThirdPartyResultsProcessorTest/" + expectedFileName);
    File expectedFile = new File(resource.toURI());
    File actualFile = getScanXMLFile(scanFile);
    XmlAssert.assertThat(actualFile).and(expectedFile).areIdentical();
  }

  @Test
  public void testHandle_CorruptFile() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data-corrupted.xml");
    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);

    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class));
    assertThat(thirdPartyScanDAO.getByScanRequestId(scanRequestId)).isEmpty();
  }

  @Test
  public void testHandle_CorruptSbomFile() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-sbom-data-corrupted.xml");
    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);

    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class));
    assertThat(thirdPartyScanDAO.getByScanRequestId(scanRequestId)).isEmpty();
    assertLogOutput("Error reading third party scan content from scan file");
  }

  @Test
  public void testHandle_InvalidJson() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data-invalid-json.xml");
    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);

    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(any(ItemContentType.class));

    try (TransactionContext tx = thirdPartyScanDAO.createTransactionContext()) {
      ThirdPartyFile thirdPartyFile =
          thirdPartyFileDAO.getByHashAndScanId("31a7c753d9515c185d85", getScanId(scanRequestId)).get(0);

      assertThirdPartyFile(scanRequestId, tx, thirdPartyFile, "clair-scanner-output.json", null, 1);
      assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId())).isEmpty();
    }
  }

  @Test
  public void testHandle_invalidSbom() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-invalid-sbom-data-cli.xml");
    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);

    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(any(ItemContentType.class));
    assertLogOutput("Error parsing third party scan file");
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
        if ("item".equals(parser.getName())) {
          String contentType = parser.getAttributeValue(null, "contentType");
          assertThat(contentType).isNotNull();
          Xpp3Dom itemElement = Xpp3Util.loadElement("item", parser);
          // Item element is empty, so it's skipped
          if (itemElement.getChildCount() > 0) {
            Xpp3Dom contentElement = itemElement.getChild("content");
            assertThat(contentElement.getValue()).isNotNull();
            if (itemContentType.equals(ItemContentType.CLAIR_SCANNER)) {
              assertFilteredClairScanContentFile(contentElement.getValue(), contentType, expectedComponentCount);
            }
            else if (itemContentType.equals(ItemContentType.SBOM)) {
              assertFilteredSbomScanContentFile(contentElement.getValue(), contentType, optionalValuesPresent,
                  expectedComponentCount);
            }
          }
          else {
            assertLogOutput(
                "scan file scan.xml.gz contained a third party scan item " + itemContentType + " without any content");
          }
        }
        eventType = parser.next();
      }
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

  private void assertFilteredSbomScanContentFile(
      String xml,
      String contentType,
      boolean optionalValuesPresent,
      int expectedComponentCount) throws Exception
  {
    assertThat(contentType).isEqualTo(ItemContentType.SBOM.name());
    Bom bom = getBom(xml);
    assertThat(bom).isNotNull();
    assertThat(bom.getComponents()).hasSize(expectedComponentCount);
    assertThat(bom.getSerialNumber()).isNull();
    assertThat(bom.getVersion()).isNotNull();
    assertThat(bom.getExternalReferences()).isNull();

    for (Component component : bom.getComponents()) {
      assertThat(component.getComponents()).isNull();
      assertThat(component.getName()).isNotNull();
      assertThat(component.getVersion()).isNotNull();
      assertThat(component.getType()).isNotNull();
      assertThat(component.getScope()).isNotNull();
      if (optionalValuesPresent) {
        assertThat(component.getPurl()).isNotNull();
      }
      else {
        assertThat(component.getPurl()).isNull();
      }

      assertThat(component.getComponents()).isNull();
      assertThat(component.getHashes()).isNull();
      assertThat(component.getCopyright()).isNull();
      assertThat(component.getDescription()).isNull();
      assertThat(component.getExternalReferences()).isNull();
      assertThat(component.getGroup()).isNull();
      assertThat(component.getLicenseChoice()).isNull();
      assertThat(component.getPedigree()).isNull();
      assertThat(component.getPublisher()).isNull();
    }
  }

  private Bom getBom(String content) throws ParseException {
    BomParser parser = new BomParser();
    return parser.parse(new StringReader(content));
  }

  @Test
  public void testHandle_NoThirdPartyContent() throws Exception {
    File scanFile = getScanFile("scan-without-thirdparty-content.xml");
    thirdPartyScanResultsProcessorSpy.handle(scanFile);
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class));
  }

  @Test
  public void testHandle_NoSbomContent() throws Exception {
    File scanFile = getScanFile("sbom/scan-with-empty-sbom-content.xml");
    thirdPartyScanResultsProcessorSpy.handle(scanFile);
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class));
  }

  @Test
  public void testHandle_InvalidFile() throws Exception {
    File scanFile = getScanFile("empty-scan.xml");
    thirdPartyScanResultsProcessorSpy.handle(scanFile);
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class));

    assertLogOutput("Error reading third party scan content from scan file");
  }

  private void assertLogOutput(final String message) {
    assertThat(logOutput.getErrorMessages(loggerName)).containsOnly(message);
  }

  private File getScanFile(final String fileName) throws Exception {
    URL resource = getClass().getResource("/ThirdPartyResultsProcessorTest/" + fileName);
    // Gzip the Third Party scan file
    File sonatypeScanGzipFile = tmpDir.newFile(ScanFileNames.SONATYPE_SCAN_FILENAME);
    try (GZIPOutputStream gzipStream = new GZIPOutputStream(new FileOutputStream(sonatypeScanGzipFile))) {
      FileUtils.copyFile(new File(resource.toURI()), gzipStream);
    }
    return sonatypeScanGzipFile;
  }

  private void assertThirdPartyFile(
      String scanRequestId,
      TransactionContext tx,
      ThirdPartyFile thirdPartyFileFound,
      String expectedFilename,
      String expectedImage,
      int expectedScans)
  {
    assertThat(thirdPartyFileFound).isNotNull();
    assertThat(thirdPartyFileFound.getFilename()).isEqualTo(expectedFilename);
    assertThat(thirdPartyFileFound.getImage()).isEqualTo(expectedImage);

    List<ThirdPartyScan> scans = thirdPartyScanDAO.getByThirdPartyFileId(tx, thirdPartyFileFound.getId());
    assertThat(scans).hasSize(expectedScans);
    assertThat(scans.stream().filter(scan -> scan.getScanRequestId().equals(scanRequestId))).hasSize(1);
  }

  private String getScanId(String scanRequestId) {
    return thirdPartyScanDAO.getByScanRequestId(scanRequestId).get(0).getScanId();
  }

  @After
  public void deleteFolder() {
    tempDir.delete();
  }
}
