/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.error.exception.BadRequestException;

import static com.sonatype.insight.brain.report.ApplicationReport.BOM_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.DEPENDENCIES_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.LICENSES_JSON_FILENAME;
import static com.sonatype.insight.brain.report.ApplicationReport.SECURITY_JSON_FILENAME;

@Named
public class ReportComponentService
{
  private final ReportService reportService;

  private final ComponentLoaderFactory componentLoaderFactory;

  private final ClusterLockManager clusterLockManager;

  @Inject
  public ReportComponentService(
      final ReportService reportService,
      final ComponentLoaderFactory componentLoaderFactory,
      final ClusterLockManager clusterLockManager)
  {
    this.reportService = reportService;
    this.componentLoaderFactory = componentLoaderFactory;
    this.clusterLockManager = clusterLockManager;
  }

  public ReportComponentData fetchReportAndComponents(Application application, String scanId) throws IOException {
    ApplicationReport applicationReport;
    List<Component> components;

    try (ClusterLock clusterLock = clusterLockManager.createForPolicyEvaluation(application, scanId)) {
      clusterLock.lock();
      applicationReport = reportService.fetchReport(application, scanId);
      final ReportEntry licenseReportEntry = applicationReport.getEntry(LICENSES_JSON_FILENAME);
      final ReportEntry securityReportEntry = applicationReport.getEntry(SECURITY_JSON_FILENAME);
      final ReportEntry bomReportEntry = applicationReport.getEntry(BOM_JSON_FILENAME);
      final ReportEntry dependenciesReportEntry = applicationReport.getEntry(DEPENDENCIES_JSON_FILENAME);

      if (bomReportEntry == null || securityReportEntry == null || licenseReportEntry == null
          || dependenciesReportEntry == null) {
        throw new BadRequestException("Unable to evaluate policy, the scan " + scanId + " could not be processed.");
      }

      // Load data about components
      components = componentLoaderFactory.createComponentLoader(application).getAll(licenseReportEntry.buf,
          securityReportEntry.buf, bomReportEntry.buf, dependenciesReportEntry.buf);
    }

    return new ReportComponentData(applicationReport, components);
  }
}
