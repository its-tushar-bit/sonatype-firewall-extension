/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.insight.brain.development.prioritization;

import java.io.File;
import java.io.IOException;
import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiReportRawDataDTOV2;
import com.sonatype.insight.brain.api.v2.service.ApiReportDataServiceV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.policy.evaluator.PolicyThreats;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
public class DevelopmentPrioritiesReportService
{
  private static final Logger log = LoggerFactory.getLogger(DevelopmentPrioritiesReportService.class);

  private static final String NOT_FOUND_ERROR_MESSAGE = "Could not find the requested report for prioritization.";

  private final ApplicationDAO applicationDAO;

  private final ReportService reportService;

  private final ApiReportDataServiceV2 apiReportDataServiceV2;

  @Inject
  public DevelopmentPrioritiesReportService(
      final ApplicationDAO applicationDAO,
      final ReportService reportService,
      final ApiReportDataServiceV2 apiReportDataServiceV2
  )
  {
    this.applicationDAO = applicationDAO;
    this.reportService = reportService;
    this.apiReportDataServiceV2 = apiReportDataServiceV2;
  }

  public ApiReportRawDataDTOV2 getDependencyInformation(final String applicationPublicId, final String scanId) {
    try {
      // getRawData performs authentication
      return this.apiReportDataServiceV2.getRawData(applicationPublicId, scanId);
    }
    catch (final IOException ioException) {
      log.warn("IOException fetching bom information from report files (" +
          applicationPublicId + ", " + scanId +  "): " + ioException.getMessage());
      throw new NotFoundException(NOT_FOUND_ERROR_MESSAGE);
    }
  }

  public PolicyThreats getPolicyThreatsNoAuth(
      final String applicationPublicId,
      final String scanId
  )
  {
    Application application = applicationDAO.getByPublicIdNotNull(applicationPublicId);
    File reportFile = reportService.getReport(application.getId(), scanId);

    if (reportFile == null) {
      log.warn("ReportService returned null for the requested application and scan id ("
          + applicationPublicId + ", " + scanId + ")");
      throw new NotFoundException(NOT_FOUND_ERROR_MESSAGE);
    }

    try {
      final ReportEntry reportEntry = Report.getEntry(reportFile, Report.POLICY_THREATS);

      if (reportEntry == null) {
        log.warn("Report.getEntry returned null for the requested application and scan id ("
            + applicationPublicId + ", " + scanId + ")");
        throw new NotFoundException(NOT_FOUND_ERROR_MESSAGE);
      }

      return JsonUtils.parse(reportEntry.buf, PolicyThreats.class);
    }
    catch (final IOException ioException) {
      log.warn("IOException fetching policythreats from report files "
          + applicationPublicId + ", " + scanId + "): " + ioException.getMessage());
      throw new NotFoundException(NOT_FOUND_ERROR_MESSAGE);
    }
  }
}
