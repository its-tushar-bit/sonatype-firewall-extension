/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import com.sonatype.clm.dto.model.ScanReceipt;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.scan.archive.TFileUtils;
import com.sonatype.insight.scan.model.ClientScanType;
import com.sonatype.insight.scan.model.DirectoryScanItem;
import com.sonatype.insight.scan.model.Scan;
import com.sonatype.insight.scan.model.ScanItem;
import com.sonatype.insight.scan.model.io.ScanReader;

import com.google.inject.Binder;
import de.schlichtherle.truezip.file.TArchiveDetector;
import de.schlichtherle.truezip.fs.FsDriver;
import de.schlichtherle.truezip.fs.FsScheme;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ScanHandlerTest
    extends AbstractComponentTest
{
  @Inject
  private InsightWork work;

  @Inject
  private ScanHandler scanHandler;

  @Inject
  private ScanReader scanReader;

  @Mock
  private HdsClient hdsClient;

  @Override
  public void configure(Binder binder) {
    binder.bind(HdsClient.class).toInstance(hdsClient);
    super.configure(binder);
  }

  @Test
  public void testHandle_ExpandedConverageScanType() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt scanReceipt = new ScanReceipt();
    String scanId = "test-scan-id";
    scanReceipt.setScanId(scanId);

    String scanFileContent = "test scan file content";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamImpl(scanFileContent));
    when(hdsClient.relay(eq(servletRequest), any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class),
        eq((Map<String, String>) null), any(String[].class))).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.EXPANDED_COVERAGE);
    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
    File scanFile = work.getScanFile(app.getId(), scanId);
    assertThat(scanFile).doesNotExist();
  }

  @Test
  public void testHandle_SonatypeScanType() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt scanReceipt = new ScanReceipt();
    String scanId = "test-scan-id";
    scanReceipt.setScanId(scanId);

    String scanFileContent = "test scan file content";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(new ServletInputStreamImpl(scanFileContent));
    when(hdsClient.relay(eq(servletRequest), any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class),
        eq((Map<String, String>) null), any(String[].class))).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE);
    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
    File scanFile = work.getScanFile(app.getId(), scanId);
    assertThat(scanFile).isFile().usingCharset(StandardCharsets.UTF_8).hasContent(scanFileContent);
  }

  @Test
  public void testHandle_TwistlockScanType() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    tempEntity.newProprietaryConfig(app.getId(), null /* packages */, Collections.singletonList("/opt/b.*"));
    ScanReceipt scanReceipt = new ScanReceipt();
    String scanId = "test-scan-id";
    scanReceipt.setScanId(scanId);

    File inputScanFile = TwistlockScanTestHelper.createInputScanFile(tempDir,
        new File("target/test-classes/ScanHandlerTest/twistlock-scan"));
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream())
        .thenReturn(new ServletInputStreamImpl(FileUtils.readFileToByteArray(inputScanFile)));
    when(hdsClient.relay(eq(servletRequest), any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class),
        eq((Map<String, String>) null), any(String[].class))).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.TWISTLOCK);
    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
    File scanFile = work.getScanFile(app.getId(), scanId);
    assertThat(scanFile).isFile();

    Scan scan = scanReader.read(scanFile);

    // Verify the top scan item in the scan
    List<ScanItem> scanItems = scan.getItems();
    assertThat(scanItems).hasSize(1);
    ScanItem scanItem = scanItems.get(0);
    assertThat(scanItem).isInstanceOf(DirectoryScanItem.class);
    assertThat(scanItem.getPath()).isEqualTo("DockerImage");

    // Verify the sub scan items in the scan
    List<? extends ScanItem> subScanItems = scanItem.getItems();
    assertThat(subScanItems).hasSize(3);
    scanItem = subScanItems.get(0);
    assertThat(scanItem).isInstanceOf(ScanItem.class);
    assertThat(scanItem.getPath()).isEqualTo("/bin/bash");
    assertThat(scanItem.isProprietary()).isNull();
    scanItem = subScanItems.get(1);
    assertThat(scanItem).isInstanceOf(DirectoryScanItem.class);
    assertThat(scanItem.getPath()).isEqualTo("/opt/foo.tar");
    assertThat(scanItem.isProprietary()).isNull();
    scanItem = subScanItems.get(2);
    assertThat(scanItem).isInstanceOf(DirectoryScanItem.class);
    assertThat(scanItem.getPath()).isEqualTo("/opt/bar.tar");
    assertThat(scanItem.isProprietary()).isTrue();

    // Verify some other scan values
    assertThat(scan.getSummary().getStartTime()).isNotNull();
    assertThat(scan.getSummary().getEndTime()).isNotNull();
    assertThat(scan.getSummary().getClientInfo()).isNotEmpty();
  }

  @Test
  public void testHandle_TwistlockScanType_DetectsAllArchiveTypes() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt scanReceipt = new ScanReceipt();
    String scanId = "test-scan-id";
    scanReceipt.setScanId(scanId);

    File inputScanFile = TwistlockScanTestHelper.createInputScanFile(tempDir,
        new File("target/test-classes/ScanHandlerTest/twistlock-scan-all-archive-types"));
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream())
        .thenReturn(new ServletInputStreamImpl(FileUtils.readFileToByteArray(inputScanFile)));
    when(hdsClient.relay(eq(servletRequest), any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class),
        eq((Map<String, String>) null), any(String[].class))).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.TWISTLOCK);
    assertThat(scanReceipt.getScanId()).isEqualTo(scanId);
    File scanFile = work.getScanFile(app.getId(), scanId);
    assertThat(scanFile).isFile();

    Scan scan = scanReader.read(scanFile);

    // Verify the top scan item in the scan
    List<ScanItem> scanItems = scan.getItems();
    assertThat(scanItems).hasSize(1);
    ScanItem scanItem = scanItems.get(0);
    assertThat(scanItem).isInstanceOf(DirectoryScanItem.class);
    assertThat(scanItem.getPath()).isEqualTo("DockerImage");

    // Verify the sub scan items in the scan
    Set<FsScheme> supportedFsSchemesForArchives = getSupportedFsSchemesForArchives();
    Set<FsScheme> detectedFsSchemesForArchives = new HashSet<>();
    List<? extends ScanItem> subScanItems = scanItem.getItems();
    for (ScanItem subScanItem : subScanItems) {
      assertThat(subScanItem).as("Item not detected as archive: " + subScanItem.getPath())
          .isInstanceOf(DirectoryScanItem.class);
      detectedFsSchemesForArchives.add(new FsScheme(subScanItem.getPath().substring("/opt/foo.".length())));
    }
    assertThat(detectedFsSchemesForArchives).isEqualTo(supportedFsSchemesForArchives);
  }

  private Set<FsScheme> getSupportedFsSchemesForArchives() {
    TArchiveDetector archiveDetector = TFileUtils.getArchiveDetector(Collections.emptyMap(), null /* badExtensions */);
    Set<FsScheme> supportedFsSchemes = new HashSet<>();
    Map<FsScheme, FsDriver> fsDriversByScheme = archiveDetector.get();
    // truezip considers a file to be an archive only if there is a driver for that file's suffix that returns
    // isFederated()=true.
    for (FsScheme fsScheme : fsDriversByScheme.keySet()) {
      if (fsDriversByScheme.get(fsScheme).isFederated()) {
        supportedFsSchemes.add(fsScheme);
      }
    }

    return supportedFsSchemes;
  }

  @Test
  public void testHandle_SendAnalyticsToHds() throws Exception {
    Application app = tempEntity.newApplicationWithParent("test-app-id");
    ScanReceipt scanReceipt = new ScanReceipt();
    scanReceipt.setScanId("test-scan-Id");

    HdsClientAnalytics expectedAnalyticsData = HdsClientAnalytics.forOwner(app);

    ServletInputStream stream = mock(ServletInputStream.class);
    when(stream.read(any(byte[].class))).thenReturn(-1);
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(stream);

    ArgumentCaptor<HdsClientAnalytics> analyticsArg = ArgumentCaptor.forClass(HdsClientAnalytics.class);
    when(hdsClient.relay(eq(servletRequest), analyticsArg.capture(), eq(ScanReceipt.class), any(String.class),
        eq((Map<String, String>) null), any(String[].class))).thenReturn(scanReceipt);

    scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.SONATYPE);

    HdsClientAnalytics analytics = analyticsArg.getValue();
    assertThat(analytics).isEqualTo(expectedAnalyticsData);
  }

  @Test
  public void testHandle_ApplicationDoesNotExist() throws Exception {
    String appPublicId = "NoSuchAppPublicID";
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);

    assertThatExceptionOfType(NotFoundException.class).isThrownBy(() -> {
      scanHandler.handle(servletRequest, appPublicId, ClientScanType.SONATYPE);
    }).withMessage("Could not find an application with public ID NoSuchAppPublicID.");
  }
}
