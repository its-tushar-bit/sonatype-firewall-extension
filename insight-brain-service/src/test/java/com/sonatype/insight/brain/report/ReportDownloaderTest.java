/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import java.io.File;
import java.io.InputStream;

import javax.inject.Inject;

import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.test.LogOutput;

import com.google.inject.Binder;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ReportDownloaderTest
    extends AbstractComponentTest
{
  @Inject
  private ReportDownloader reportDownloader;

  @Inject
  private InsightWork work;

  @Rule
  public LogOutput log = new LogOutput(ReportDownloader.class);

  private HdsClient mockHdsClient;

  @Override
  public void configure(Binder binder) {
    super.configure(binder);
    mockHdsClient = mock(HdsClient.class);
    binder.bind(HdsClient.class).toInstance(mockHdsClient);
  }

  @Test
  public void testDownloadReportNonExistentScanId_DoesNotCreateParentDir() throws Exception {
    Application app = tempEntity.newApplicationWithParent("dummyApp");
    String scanId = "NonExistentScanId";

    NotFoundException expectedException = new NotFoundException("test");
    when(mockHdsClient.get(InputStream.class, ReportDownloader.HDS_PATH, null, scanId)).thenThrow(expectedException);

    File reportFile = work.getReportFile(app.getId(), scanId);
    boolean rc = reportDownloader.downloadReport(scanId, reportFile, 0, 0);
    assertThat(rc, is(false));
    assertThat(reportFile.getParentFile().exists(), is(false));
    log.assertError("test", expectedException);
  }
}
