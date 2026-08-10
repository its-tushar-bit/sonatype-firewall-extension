/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.report.MockReportDownloader;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.error.exception.BadRequestException;
import jakarta.inject.Inject;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@ComponentH2Test
public class ReportComponentServiceTest
    extends AbstractComponentH2Test
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

  @BeforeEach
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

    assertThat(result).isNotNull();
    assertThat(result.lifecycleReport).isNotNull();
    assertThat(result.components).isNotNull();
    assertThat(result.components).isNotEmpty();
  }

  @Test
  public void testFetchReportAndComponents_MissingReportEntries() throws Exception {
    Organization organization = tempEntity.newOrganization("my-org");
    Application application = tempEntity.newApplication("my-app", "my-app", organization.getId());
    String scanId = simulateReportIsAvailable("empty-report");

    assertThrows(BadRequestException.class,
        () -> reportComponentService.fetchReportAndComponents(application, scanId,
            StageTypes.STAGE_RELEASE.getId()));
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

    assertThat(components).isNotNull();
    assertThat(components).isNotEmpty();
    assertThat(components.size()).isEqualTo(28);
  }
}
