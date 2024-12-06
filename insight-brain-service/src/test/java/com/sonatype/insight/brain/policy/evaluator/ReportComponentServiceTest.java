/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.ReportDownloader;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.BadRequestException;

import com.google.inject.Binder;
import com.google.inject.Inject;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ReportComponentServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ReportService reportService;

  @Inject
  private ComponentLoaderFactory componentLoaderFactory;

  @Inject
  private ClusterLockManager clusterLockManager;

  @Inject
  private ReportDataStore reportDataStore;

  private ReportComponentService reportComponentService;

  private MockReportDownloader mockReportDownloader;

  @Override
  public void configure(Binder binder) {
    mockReportDownloader = new MockReportDownloader();
    binder.bind(ReportDownloader.class).toInstance(mockReportDownloader.getMock());
    super.configure(binder);
  }

  @Before
  public void setup() {
    reportComponentService =
        new ReportComponentService(reportService, componentLoaderFactory, clusterLockManager, reportDataStore);
  }

  @Test
  public void testFetchReportAndComponents() throws Exception {
    // Setup
    Organization organization = tempEntity.newOrganization("my-org");
    Application application = tempEntity.newApplication("my-app", "my-app", organization.getId());
    String scanId = simulateReportIsAvailable("report");

    ReportComponentData result = reportComponentService.fetchReportAndComponents(application, scanId);

    assertNotNull(result);
    assertNotNull(result.applicationReport);
    assertNotNull(result.components);
    assertFalse(result.components.isEmpty());
  }

  @Test(expected = BadRequestException.class)
  public void testFetchReportAndComponents_MissingReportEntries() throws Exception {
    Organization organization = tempEntity.newOrganization("my-org");
    Application application = tempEntity.newApplication("my-app", "my-app", organization.getId());
    String scanId = simulateReportIsAvailable("empty-report");

    reportComponentService.fetchReportAndComponents(application, scanId);
  }

  private String simulateReportIsAvailable(String reportResourceName) {
    return mockReportDownloader.mockDownloadReport("/" + getClass().getSimpleName() + "/" + reportResourceName);
  }
}
