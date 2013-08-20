/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.io.File;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ReportPopularity;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportResource;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.cache.CacheLoader;

public class ReportItemCacheLoader
    extends CacheLoader<ReportItemKey, ReportPopularity>
{

  private ApplicationDAO applicationDAO = new ApplicationDAO();

  @Override
  public ReportPopularity load(ReportItemKey key) throws Exception {
    Application application = applicationDAO.getByPublicIdNotNull(key.getApplicationPublicId());
    String appId = application.getId();

    final String name = Report.toEntryName("popularity.json");
    final File reportFile = ReportResource.fetchReport(key.getReportDownloader(), key.getWork(), appId,
        key.getScanId(), false);
    ReportEntry reportEntry = Report.getEntry(reportFile, name);

    if (reportEntry == null) {
      throw new IllegalStateException("popularity.json is missing from report for scan " + key.getScanId());
    }
    return JsonUtils.parse(reportEntry.buf, ReportPopularity.class);
  }
}
