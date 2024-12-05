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
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.report.ReportUtils;
import com.sonatype.insight.error.exception.BadRequestException;

@Named
public class ReportComponentService
{
  private final ReportService reportService;

  private final ComponentLoaderFactory componentLoaderFactory;

  private final ClusterLockManager clusterLockManager;

  private final ReportUtils reportUtils;

  @Inject
  public ReportComponentService(
      final ReportService reportService,
      final ComponentLoaderFactory componentLoaderFactory,
      final ClusterLockManager clusterLockManager,
      final ReportUtils reportUtils)
  {
    this.reportService = reportService;
    this.componentLoaderFactory = componentLoaderFactory;
    this.clusterLockManager = clusterLockManager;
    this.reportUtils = reportUtils;
  }

  public ReportComponentData fetchReportAndComponents(Application application, String scanId) throws IOException {
    Report reportFile;
    List<Component> components;

    try (ClusterLock clusterLock = clusterLockManager.createForPolicyEvaluation(application, scanId)) {
      clusterLock.lock();
      reportFile = reportService.fetchReport(application, scanId);
      final ReportEntry licenseReportEntry = reportUtils.getEntry(reportFile, ReportUtils.LICENSES_JSON_FILENAME);
      final ReportEntry securityReportEntry = reportUtils.getEntry(reportFile, ReportUtils.SECURITY_JSON_FILENAME);
      final ReportEntry bomReportEntry = reportUtils.getEntry(reportFile, ReportUtils.BOM_JSON_FILENAME);
      final ReportEntry dependenciesReportEntry =
          reportUtils.getEntry(reportFile, ReportUtils.DEPENDENCIES_JSON_FILENAME);

      if (bomReportEntry == null || securityReportEntry == null || licenseReportEntry == null
          || dependenciesReportEntry == null) {
        throw new BadRequestException("Unable to evaluate policy, the scan " + scanId + " could not be processed.");
      }

      // Load data about components
      components = componentLoaderFactory.createComponentLoader(application).getAll(licenseReportEntry.buf,
          securityReportEntry.buf, bomReportEntry.buf, dependenciesReportEntry.buf);
    }

    return new ReportComponentData(reportFile, components);
  }
}
