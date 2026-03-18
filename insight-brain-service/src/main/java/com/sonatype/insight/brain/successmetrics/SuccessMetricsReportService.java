/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditService;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDAO;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

/**
 * @since 1.37
 */
@Named
public class SuccessMetricsReportService
{
  private final SuccessMetricsReportDAO successMetricsReportDAO;

  private final CurrentUser currentUser;

  private final AuditService auditService;

  @Inject
  public SuccessMetricsReportService(
      final SuccessMetricsReportDAO successMetricsReportDAO,
      final CurrentUser currentUser,
      final AuditService auditService)
  {
    this.successMetricsReportDAO = successMetricsReportDAO;
    this.currentUser = currentUser;
    this.auditService = auditService;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  List<SuccessMetricsReportDTO> getSuccessMetricsReportsForCurrentUser() throws IOException {
    String username = currentUser.getUsername();

    List<SuccessMetricsReportDTO> successMetricsDTOs = new ArrayList<>();
    for (SuccessMetricsReport successMetricsReport : successMetricsReportDAO.getByUsername(username)) {
      SuccessMetricsReportScopeDTO dto = JsonUtils.parse(successMetricsReport.getScopeJson(),
          SuccessMetricsReportScopeDTO.class);

      SuccessMetricsReportDTO successMetricsDTO = new SuccessMetricsReportDTO();
      successMetricsDTO.id = successMetricsReport.getId();
      successMetricsDTO.name = successMetricsReport.getName();
      successMetricsDTO.includeLatestData = successMetricsReport.getIncludeLatestData();
      successMetricsDTO.scope = dto;

      successMetricsDTOs.add(successMetricsDTO);
    }
    return successMetricsDTOs;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  SuccessMetricsReportDTO createSuccessMetricsReportForCurrentUser(SuccessMetricsReportDTO successMetricsDTO) {
    if (successMetricsDTO.scope == null) {
      throw new BadRequestException("Scope cannot be null or missing.");
    }

    String username = currentUser.getUsername();

    SuccessMetricsReport successMetricsReport = new SuccessMetricsReport(successMetricsDTO.name);
    successMetricsReport.setScopeJson(JsonUtils.format(successMetricsDTO.scope));
    successMetricsReport.setUsername(username);
    successMetricsReport.setIncludeLatestData(successMetricsDTO.includeLatestData);
    successMetricsReportDAO.insert(successMetricsReport);

    successMetricsDTO.id = successMetricsReport.getId();
    auditSuccessMetricsReport(successMetricsReport, successMetricsDTO.scope.applicationIds,
        successMetricsDTO.scope.organizationIds);

    return successMetricsDTO;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  void deleteSuccessMetricsReportForCurrentUser(String successMetricsId) {
    AuditData.get().setData("reportId", successMetricsId);
    SuccessMetricsReport successMetricsReport = findSuccessMetricsReportByIdForCurrentUser(successMetricsId);
    Set<String> applicationIds = successMetricsReport.getScopeApplicationIds();
    Set<String> organizationIds = successMetricsReport.getScopeOrganizationIds();

    auditSuccessMetricsReport(successMetricsReport, applicationIds, organizationIds);
    successMetricsReportDAO.delete(successMetricsReport);
  }

  /**
   * @since 1.39
   */
  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public SuccessMetricsReport findSuccessMetricsReportByIdForCurrentUser(String successMetricsId) {
    SuccessMetricsReport successMetricsReport = successMetricsReportDAO.getById(successMetricsId);

    if (successMetricsReport == null || (!currentUser.getUsername().equals(successMetricsReport.getUsername()))) {
      throw new NotFoundException("Cannot find a success metrics report with id " + successMetricsId + " for user id "
          + currentUser.getUsername() + ".");
    }
    return successMetricsReport;
  }

  private void auditSuccessMetricsReport(
      final SuccessMetricsReport report,
      final Set<String> applicationIds,
      final Set<String> organizationIds)
  {
    AuditData.get()
        .setSuccessMetricsReport(report)
        .setData("selectedOrganizations",
            auditService.getSelectedOrganizationsById(organizationIds))
        .setData("selectedApplications", auditService.getSelectedApplicationsById(applicationIds, organizationIds));
  }
}
