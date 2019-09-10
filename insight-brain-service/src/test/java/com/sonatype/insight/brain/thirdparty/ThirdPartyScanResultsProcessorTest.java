/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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
import org.apache.commons.text.StringEscapeUtils;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.MXParser;
import org.codehaus.plexus.util.xml.pull.XmlPullParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Spy;

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

  @Before
  public void before() {
    thirdPartyScanResultsProcessorSpy = spy(new ThirdPartyScanResultsProcessor(thirdPartyScanDAO, thirdPartyFileDAO));
    lenient().doReturn(clairHandlerSpy).when(thirdPartyScanResultsProcessorSpy)
        .createHandler(eq(ItemContentType.CLAIR_SCANNER));
  }

  @Test
  public void testHandle_ClairScanner() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data.xml");

    doReturn("683620ac905c1d32b58c").when(clairHandlerSpy).buildHash(eq("debian:9:apt:1.4.8"));
    doReturn("e587ce87ed894c1d5283").when(clairHandlerSpy).buildHash(eq("debian:9:glibc:2.24-11+deb9u3"));
    doReturn("aff6a96471f042e1d975").when(clairHandlerSpy).buildHash(eq("debian:9:libxslt:1.1.29-2.1"));

    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);
    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(eq(ItemContentType.CLAIR_SCANNER));
    assertFilteredThirdPartyScanContentFile(scanFile);

    try (TransactionContext tx = thirdPartyScanDAO.createTransactionContext()) {
      ThirdPartyFile thirdPartyFile1 = thirdPartyFileDAO.getByHash("30a7c753d9515c185d85");
      assertThirdPartyFile(scanRequestId, tx, thirdPartyFile1, "clair-scanner-out/clair-scanner-output.json",
          "smart-brain-boost-api-dockerized_postgres", 1);

      ThirdPartyFile thirdPartyFile2 = thirdPartyFileDAO.getByHash("48a7c753d9515c185d75");
      assertThirdPartyFile(scanRequestId, tx, thirdPartyFile2, "clair-scanner-out/other/clair-scanner-output.json",
          "smart-brain-boost-api-dockerized_postgres", 1);
    }
  }

  @Test
  public void testHandle_ClairScannerUsingSameClairFileMultipleTimes() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-for-multiple-times.xml");

    doReturn("9510c290c07710d8c69b").when(clairHandlerSpy).buildHash(eq("debian:9:apt:1.3.8"));
    doReturn("08d7a1c700d1633dc309").when(clairHandlerSpy).buildHash(eq("debian:9:apt:1.3.9"));
    doReturn("e587ce87ed894c1d5283").when(clairHandlerSpy).buildHash(eq("debian:9:glibc:2.24-11+deb9u3"));

    thirdPartyScanResultsProcessorSpy.handle(scanFile);
    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);

    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(any(ItemContentType.class));
    assertFilteredThirdPartyScanContentFile(scanFile);

    try (TransactionContext tx = thirdPartyScanDAO.createTransactionContext()) {
      ThirdPartyFile thirdPartyFile = thirdPartyFileDAO.getByHash("a7cea8ebc1ab163d7b1a");
      assertThirdPartyFile(scanRequestId, tx, thirdPartyFile, "clair-scanner-output.json",
          "smart-brain-boost-api-dockerized_postgres", 2);
    }
  }

  @Test
  public void testHandle_ClairScannerUsingSameClairFileRepeatedContent() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-repeated-content.xml");

    doReturn("9510c290c07710d8c69b").when(clairHandlerSpy).buildHash(eq("debian:9:apt:1.3.8"));
    doReturn("08d7a1c700d1633dc309").when(clairHandlerSpy).buildHash(eq("debian:9:apt:1.3.9"));
    doReturn("e587ce87ed894c1d5283").when(clairHandlerSpy).buildHash(eq("debian:9:glibc:2.24-11+deb9u3"));

    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);

    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(2)).createHandler(any(ItemContentType.class));
    assertFilteredThirdPartyScanContentFile(scanFile);

    try (TransactionContext tx = thirdPartyScanDAO.createTransactionContext()) {
      ThirdPartyFile thirdPartyFile = thirdPartyFileDAO.getByHash("a7cea8ebc1ab163d7b1x");
      assertThirdPartyFile(scanRequestId, tx, thirdPartyFile, "clair-scanner-output.json", "test-image-name", 1);
    }
  }

  @Test
  public void testHandle_EmptyItemElement() throws Exception {
    File scanFile = getScanFile("scan-with-empty-item-data.xml");
    thirdPartyScanResultsProcessorSpy.handle(scanFile);
    verify(thirdPartyScanResultsProcessorSpy, times(0)).createHandler(any(ItemContentType.class));
    assertEmptyItemElement(scanFile);
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
  public void testHandle_InvalidJson() throws Exception {
    File scanFile = getScanFile("scan-with-clair-scanner-data-invalid-json.xml");
    String scanRequestId = thirdPartyScanResultsProcessorSpy.handle(scanFile);

    assertThat(scanRequestId).isNotBlank();
    verify(thirdPartyScanResultsProcessorSpy, times(1)).createHandler(any(ItemContentType.class));

    try (TransactionContext tx = thirdPartyScanDAO.createTransactionContext()) {
      ThirdPartyFile thirdPartyFile = thirdPartyFileDAO.getByHash("31a7c753d9515c185d85");

      assertThirdPartyFile(scanRequestId, tx, thirdPartyFile, "clair-scanner-output.json", null, 1);
      assertThat(thirdPartyFileCoordinateDAO.getByThirdPartyFileId(thirdPartyFile.getId())).isEmpty();
    }
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

  private void assertFilteredThirdPartyScanContentFile(File scanFile) throws Exception {
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
            assertFilteredClairScanContentFile(contentElement.getValue(), contentType);
          }
          else {
            assertLogOutput(
                "scan file scan.xml.gz contained a third party scan item CLAIR_SCANNER without any content");
          }
        }
        eventType = parser.next();
      }
    }
  }

  private void assertFilteredClairScanContentFile(String json, String contentType) {
    assertThat(contentType).isEqualTo(ItemContentType.CLAIR_SCANNER.name());
    ClairScannerResult clairScannerResult =
        GSON.fromJson(StringEscapeUtils.unescapeXml(json), ClairScannerResult.class);
    assertThat(clairScannerResult).isNotNull();
    assertThat(clairScannerResult.getImage()).isNull();
    assertThat(clairScannerResult.getVulnerabilities()).isNotNull();
    assertThat(clairScannerResult.getVulnerabilities()).hasSize(3);

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

  @Test
  public void testHandle_NoThirdPartyContent() throws Exception {
    File scanFile = getScanFile("scan-without-thirdparty-content.xml");
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
    File stagingDir = tempDir.newFolder("staging");
    URL resource = getClass().getResource("/ThirdPartyResultsProcessorTest/" + fileName);
    // Gzip the Third Party scan file
    File sonatypeScanGzipFile = new File(stagingDir, ScanFileNames.SONATYPE_SCAN_FILENAME);
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

  @After
  public void deleteFolder() {
    tempDir.delete();
  }
}
