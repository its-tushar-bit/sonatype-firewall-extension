/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLock;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.error.exception.BadRequestException;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DEPENDENCIES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.LICENSES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SECURITY_JSON;

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

  public ReportComponentData fetchReportAndComponents(
      Application application,
      String scanId,
      String stageTypeId) throws IOException
  {
    ApplicationReport applicationReport;
    List<Component> components;

    try (ClusterLock clusterLock = clusterLockManager.createForPolicyEvaluation(application, scanId)) {
      clusterLock.lock();
      applicationReport = reportService.fetchReport(application, scanId, stageTypeId);
      Map<String, ReportEntry> entries = applicationReport.getEntries(List.of(
          LICENSES_JSON.getName(),
          SECURITY_JSON.getName(),
          BOM_JSON.getName(),
          DEPENDENCIES_JSON.getName()));
      final ReportEntry licenseReportEntry = entries.get(LICENSES_JSON.getName());
      final ReportEntry securityReportEntry = entries.get(SECURITY_JSON.getName());
      final ReportEntry bomReportEntry = entries.get(BOM_JSON.getName());
      final ReportEntry dependenciesReportEntry = entries.get(DEPENDENCIES_JSON.getName());

      if (bomReportEntry == null || securityReportEntry == null || licenseReportEntry == null
          || dependenciesReportEntry == null)
      {
        throw new BadRequestException("Unable to fetch report data, the scan " + scanId + " could not be processed.");
      }

      // Load data about components
      components = componentLoaderFactory.createComponentLoader(application)
          .getAll(licenseReportEntry.buf,
              securityReportEntry.buf, bomReportEntry.buf, dependenciesReportEntry.buf);
    }

    return new ReportComponentData(applicationReport, components);
  }

  public List<Component> getReportComponents(String scanId, Application owner) throws IOException {
    ApplicationReport applicationReport = reportService.getReport(owner.getId(), scanId);
    Map<String, ReportEntry> entries = applicationReport.getEntries(List.of(
        LICENSES_JSON.getName(),
        SECURITY_JSON.getName(),
        BOM_JSON.getName(),
        DEPENDENCIES_JSON.getName()));
    ReportEntry licenseReportEntry = entries.get(LICENSES_JSON.getName());
    ReportEntry securityReportEntry = entries.get(SECURITY_JSON.getName());
    ReportEntry bomReportEntry = entries.get(BOM_JSON.getName());
    ReportEntry dependenciesReportEntry = entries.get(DEPENDENCIES_JSON.getName());
    List<Component> components = componentLoaderFactory.createComponentLoader(owner)
        .getAll((licenseReportEntry == null) ? null : licenseReportEntry.buf,
            (securityReportEntry == null) ? null : securityReportEntry.buf,
            (bomReportEntry == null) ? null : bomReportEntry.buf,
            (dependenciesReportEntry == null) ? null : dependenciesReportEntry.buf);
    return components;
  }
}
