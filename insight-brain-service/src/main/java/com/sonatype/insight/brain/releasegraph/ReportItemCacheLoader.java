/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ReportPopularity;
import com.sonatype.insight.brain.report.ReportDataStore;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.cache.CacheLoader;

@Named
public class ReportItemCacheLoader
    extends CacheLoader<ReportItemKey, ReportPopularity>
{
  private final ReportService reportService;

  private final ApplicationDAO applicationDAO;

  private final ReportDataStore reportDataStore;

  @Inject
  public ReportItemCacheLoader(
      ReportService reportService,
      ApplicationDAO applicationDAO,
      ReportDataStore reportDataStore)
  {
    this.reportService = reportService;
    this.applicationDAO = applicationDAO;
    this.reportDataStore = reportDataStore;
  }

  @Override
  public ReportPopularity load(ReportItemKey key) throws Exception {
    Application application = applicationDAO.getByPublicIdNotNull(key.getApplicationPublicId());
    String appId = application.getId();

    final String name = reportDataStore.toEntryName("popularity.json");
    final ApplicationReport applicationReport = reportService.getReport(appId, key.getScanId());
    ReportEntry reportEntry = reportDataStore.getEntry(applicationReport, name);

    if (reportEntry == null) {
      throw new IllegalStateException("popularity.json is missing from report for scan " + key.getScanId());
    }
    return JsonUtils.parse(reportEntry.buf, ReportPopularity.class);
  }
}
