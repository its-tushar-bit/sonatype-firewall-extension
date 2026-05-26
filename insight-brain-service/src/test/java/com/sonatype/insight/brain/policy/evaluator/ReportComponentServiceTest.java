/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

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
  private InsightWork insightWork;

  private ReportComponentService reportComponentService;

  private MockReportDownloader mockReportDownloader;

  @Before
  public void setup() {
    reportComponentService =
        new ReportComponentService(reportService, componentLoaderFactory, clusterLockManager);

    mockReportDownloader = new MockReportDownloader(tempDir);
    mockReportDownloader.setInsightWork(insightWork);
    applyBeanFieldOverride(ReportDataStore.class, "reportDownloader", mockReportDownloader.getMock());
  }

  @Test
  public void testFetchReportAndComponents() throws Exception {
    // Setup
    Organization organization = tempEntity.newOrganization("my-org");
    Application application = tempEntity.newApplication("my-app", "my-app", organization.getId());
    String scanId = simulateReportIsAvailable("report");

    ReportComponentData result =
        reportComponentService.fetchReportAndComponents(application, scanId, StageTypes.STAGE_RELEASE.getId());

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

    reportComponentService.fetchReportAndComponents(application, scanId, StageTypes.STAGE_RELEASE.getId());
  }

  private String simulateReportIsAvailable(String reportResourceName) {
    return mockReportDownloader.mockDownloadReport("/" + getClass().getSimpleName() + "/" + reportResourceName);
  }

  @Test
  public void testGetReportComponents() throws Exception {
    Organization organization = tempEntity.newOrganization("my-org");
    Application application = tempEntity.newApplication("my-app", "my-app", organization.getId());
    String scanId = simulateReportIsAvailable("report");
    reportComponentService.fetchReportAndComponents(application, scanId, StageTypes.STAGE_RELEASE.getId());

    List<Component> components = reportComponentService.getReportComponents(scanId, application);

    assertNotNull(components);
    assertFalse(components.isEmpty());
    assertEquals(components.size(), 28);
  }
}
