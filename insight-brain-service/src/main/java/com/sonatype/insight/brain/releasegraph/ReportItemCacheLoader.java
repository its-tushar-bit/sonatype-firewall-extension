/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ReportPopularity;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.cache.CacheLoader;

@Named
public class ReportItemCacheLoader
    extends CacheLoader<ReportItemKey, ReportPopularity>
{
  private final InsightWork work;

  private final ReportService reportService;

  private final ApplicationDAO applicationDAO;

  @Inject
  public ReportItemCacheLoader(InsightWork work, ReportService reportService, ApplicationDAO applicationDAO) {
    this.work = work;
    this.reportService = reportService;
    this.applicationDAO = applicationDAO;
  }

  @Override
  public ReportPopularity load(ReportItemKey key) throws Exception {
    Application application = applicationDAO.getByPublicIdNotNull(key.getApplicationPublicId());
    String appId = application.getId();

    final String name = Report.toEntryName("popularity.json");
    final File reportFile = reportService.getReport(work, appId, key.getScanId());
    ReportEntry reportEntry = Report.getEntry(reportFile, name);

    if (reportEntry == null) {
      throw new IllegalStateException("popularity.json is missing from report for scan " + key.getScanId());
    }
    return JsonUtils.parse(reportEntry.buf, ReportPopularity.class);
  }
}
