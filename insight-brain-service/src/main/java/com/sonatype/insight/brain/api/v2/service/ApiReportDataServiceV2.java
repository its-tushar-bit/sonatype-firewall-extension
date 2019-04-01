/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportComponentDTOV2;
import com.sonatype.insight.brain.api.v2.dto.ApiReportDataDTOV2;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.MatchState;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.AuthzContext;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.json.store.JsonUtils;

import com.fasterxml.jackson.databind.node.ContainerNode;

/**
 * Provides data from an application's composition report in a format suitable for consumption by 3rd-party clients.
 * 
 * @since 1.13.0
 */
@Named
public class ApiReportDataServiceV2
{
  private final InsightWork work;

  private final ApplicationDAO appDAO;

  private final ComponentDAO componentDAO;

  private final ReportService reportService;

  private final ApiLicenseDataAdapter licenseDataAdapter;

  private final ApiSecurityDataAdapter securityDataAdapter;

  @Inject
  public ApiReportDataServiceV2(InsightWork work,
                                ApplicationDAO appDAO,
                                ComponentDAO componentDAO,
                                ReportService reportService,
                                ApiLicenseDataAdapter licenseDataAdapter,
                                ApiSecurityDataAdapter securityDataAdapter)
  {
    this.work = work;
    this.appDAO = appDAO;
    this.componentDAO = componentDAO;
    this.reportService = reportService;
    this.licenseDataAdapter = licenseDataAdapter;
    this.securityDataAdapter = securityDataAdapter;
  }

  @Authorize(permission = Permission.READ)
  public ApiReportDataDTOV2 getData(@AuthzContext(AuthzContext.Key.APPLICATION_PUBLIC_ID) String applicationPublicId,
                                    String scanId) throws IOException
  {
    return getDataNoAuth(applicationPublicId, scanId);
  }

  public ApiReportDataDTOV2 getDataNoAuth(String applicationPublicId, String scanId) throws IOException {
    Application app = appDAO.getByPublicIdNotNull(applicationPublicId);
    File reportFile = reportService.getReport(work, app.getId(), scanId);

    ReportEntry bomEntry = Report.getEntry(reportFile, "bom.json");
    ReportEntry securityEntry = Report.getEntry(reportFile, "security.json");
    ReportEntry licenseEntry = Report.getEntry(reportFile, "licenses.json");
    ReportEntry dataEntry = Report.getEntry(reportFile, "data.json");

    if (bomEntry == null || securityEntry == null || licenseEntry == null || dataEntry == null) {
      throw new BadRequestException("The report with ID " + scanId + " contains no component data.");
    }

    List<Component> components = componentDAO.getAll(app, licenseEntry.buf, securityEntry.buf, bomEntry.buf);

    ApiReportDataDTOV2 data = new ApiReportDataDTOV2();
    for (Component comp : components) {
      ApiReportComponentDTOV2 component = new ApiReportComponentDTOV2();
      component.hash = comp.getHash();
      component.componentIdentifier = ApiComponentIdentifierDTOV2
          .fromComponentIdentifier(comp.getComponentIdentifier());

      component.matchState = comp.getMatchState().getId();
      component.proprietary = comp.isProprietary();
      for (String pathname : comp.getPathnames()) {
        if (!pathname.startsWith("dependency:")) {
          component.pathnames.add(pathname);
        }
      }
      if (!MatchState.UNKNOWN.equals(comp.getMatchState())) {
        component.securityData = securityDataAdapter.convertToDTO(comp);
        component.licenseData = licenseDataAdapter.convertToDTOV2(comp);
      }

      data.components.add(component);
    }

    ContainerNode<?> dataJson = JsonUtils.parse(dataEntry.buf);
    data.matchSummary.knownComponentCount = dataJson.get("knownArtifactCount").intValue();
    data.matchSummary.totalComponentCount = dataJson.get("totalArtifactCount").intValue();

    return data;
  }
}
