/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
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

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    binder.bind(HdsClient.class).toInstance(hdsClient);
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
    when(hdsClient.get(eq(servletRequest), any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class),
        eq((Map<String, String>) null), (String[]) anyVararg())).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.EXPANDED_COVERAGE);
    assertThat(scanReceipt.getScanId(), is(scanId));
    File scanFile = work.getScanFile(app.getId(), scanId);
    assertThat(scanFile.exists(), is(false));
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
    when(hdsClient.get(eq(servletRequest), any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class),
        eq((Map<String, String>) null), (String[]) anyVararg())).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId());
    assertThat(scanReceipt.getScanId(), is(scanId));
    File scanFile = work.getScanFile(app.getId(), scanId);
    assertThat(scanFile.exists(), is(true));
    assertThat(FileUtils.readFileToString(scanFile, "UTF-8"), is(scanFileContent));
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
    when(hdsClient.get(eq(servletRequest), any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class),
        eq((Map<String, String>) null), (String[]) anyVararg())).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.TWISTLOCK);
    assertThat(scanReceipt.getScanId(), is(scanId));
    File scanFile = work.getScanFile(app.getId(), scanId);
    assertThat(scanFile.exists(), is(true));

    Scan scan = scanReader.read(scanFile);

    // Verify the top scan item in the scan
    List<ScanItem> scanItems = scan.getItems();
    assertThat(scanItems, hasSize(1));
    ScanItem scanItem = scanItems.get(0);
    assertThat(scanItem, instanceOf(DirectoryScanItem.class));
    assertThat(scanItem.getPath(), is("DockerImage"));

    // Verify the sub scan items in the scan
    List<? extends ScanItem> subScanItems = scanItem.getItems();
    assertThat(subScanItems, hasSize(3));
    scanItem = subScanItems.get(0);
    assertThat(scanItem, instanceOf(ScanItem.class));
    assertThat(scanItem.getPath(), is("/bin/bash"));
    assertThat(scanItem.isProprietary(), is(nullValue()));
    scanItem = subScanItems.get(1);
    assertThat(scanItem, instanceOf(DirectoryScanItem.class));
    assertThat(scanItem.getPath(), is("/opt/foo.tar"));
    assertThat(scanItem.isProprietary(), is(nullValue()));
    scanItem = subScanItems.get(2);
    assertThat(scanItem, instanceOf(DirectoryScanItem.class));
    assertThat(scanItem.getPath(), is("/opt/bar.tar"));
    assertThat(scanItem.isProprietary(), is(true));

    // Verify some other scan values
    assertThat(scan.getSummary().getStartTime(), notNullValue());
    assertThat(scan.getSummary().getEndTime(), notNullValue());
    assertThat(scan.getSummary().getClientInfo().size(), greaterThan(0));
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
    when(hdsClient.get(eq(servletRequest), any(HdsClientAnalytics.class), eq(ScanReceipt.class), any(String.class),
        eq((Map<String, String>) null), (String[]) anyVararg())).thenReturn(scanReceipt);

    scanReceipt = scanHandler.handle(servletRequest, app.getPublicId(), ClientScanType.TWISTLOCK);
    assertThat(scanReceipt.getScanId(), is(scanId));
    File scanFile = work.getScanFile(app.getId(), scanId);
    assertThat(scanFile.exists(), is(true));

    Scan scan = scanReader.read(scanFile);

    // Verify the top scan item in the scan
    List<ScanItem> scanItems = scan.getItems();
    assertThat(scanItems, hasSize(1));
    ScanItem scanItem = scanItems.get(0);
    assertThat(scanItem, instanceOf(DirectoryScanItem.class));
    assertThat(scanItem.getPath(), is("DockerImage"));

    // Verify the sub scan items in the scan
    Set<FsScheme> supportedFsSchemesForArchives = getSupportedFsSchemesForArchives();
    Set<FsScheme> detectedFsSchemesForArchives = new HashSet<>();
    List<? extends ScanItem> subScanItems = scanItem.getItems();
    for (ScanItem subScanItem : subScanItems) {
      assertThat("Item not detected as archive: " + subScanItem.getPath(), subScanItem,
          instanceOf(DirectoryScanItem.class));
      detectedFsSchemesForArchives.add(new FsScheme(subScanItem.getPath().substring("/opt/foo.".length())));
    }
    assertThat(detectedFsSchemesForArchives, is(supportedFsSchemesForArchives));
  }

  private Set<FsScheme> getSupportedFsSchemesForArchives() {
    TArchiveDetector archiveDetector = TFileUtils.getArchiveDetector(Collections.<TFileUtils.Driver, String> emptyMap(),
        null /* badExtensions */);
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

    HdsClientAnalytics expectedAnalyticsData = HdsClientAnalytics.forApplication(app.getId());

    ServletInputStream stream = mock(ServletInputStream.class);
    when(stream.read(any(byte[].class))).thenReturn(-1);
    HttpServletRequest servletRequest = mock(HttpServletRequest.class);
    when(servletRequest.getInputStream()).thenReturn(stream);

    ArgumentCaptor<HdsClientAnalytics> analyticsArg = ArgumentCaptor.forClass(HdsClientAnalytics.class);
    when(hdsClient.get(eq(servletRequest), analyticsArg.capture(), eq(ScanReceipt.class), any(String.class),
        eq((Map<String, String>) null), (String[]) anyVararg())).thenReturn(scanReceipt);

    scanHandler.handle(servletRequest, app.getPublicId());

    HdsClientAnalytics analytics = analyticsArg.getValue();
    assertThat(analytics, is(equalTo(expectedAnalyticsData)));
  }

  private static class ServletInputStreamImpl
      extends ServletInputStream
  {
    // ByteArrayInputStream.close is a noop, so we don't need to close this stream
    private ByteArrayInputStream wrappedInputStream;

    public ServletInputStreamImpl(String data) throws UnsupportedEncodingException {
      wrappedInputStream = new ByteArrayInputStream(data.getBytes("UTF-8"));
    }

    public ServletInputStreamImpl(byte[] data) {
      wrappedInputStream = new ByteArrayInputStream(data);
    }

    @Override
    public int read() throws IOException {
      return wrappedInputStream.read();
    }
  }
}
