/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.successmetrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDAO;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
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

  @Inject
  public SuccessMetricsReportService(SuccessMetricsReportDAO successMetricsReportDAO, CurrentUser currentUser) {
    this.successMetricsReportDAO = successMetricsReportDAO;
    this.currentUser = currentUser;
  }

  List<SuccessMetricsReportDTO> getSuccessMetricsReportsForCurrentUser() throws IOException {
    String username = currentUser.getUsername();

    List<SuccessMetricsReportDTO> successMetricsDTOs = new ArrayList<>();
    for (SuccessMetricsReport successMetricsReport : successMetricsReportDAO.getByUsername(username)) {
      SuccessMetricsReportScopeDTO dto = JsonUtils.parse(successMetricsReport.getScopeJson(),
          SuccessMetricsReportScopeDTO.class);

      SuccessMetricsReportDTO successMetricsDTO = new SuccessMetricsReportDTO();
      successMetricsDTO.id = successMetricsReport.getId();
      successMetricsDTO.name = successMetricsReport.getName();
      successMetricsDTO.scope = dto;

      successMetricsDTOs.add(successMetricsDTO);
    }
    return successMetricsDTOs;
  }

  SuccessMetricsReportDTO createSuccessMetricsReportForCurrentUser(SuccessMetricsReportDTO successMetricsDTO) {
    if (successMetricsDTO.scope == null) {
      throw new BadRequestException("Scope cannot be null or missing.");
    }

    String username = currentUser.getUsername();

    SuccessMetricsReport successMetricsReport = new SuccessMetricsReport(successMetricsDTO.name);
    successMetricsReport.setScopeJson(JsonUtils.format(successMetricsDTO.scope));
    successMetricsReport.setUsername(username);
    successMetricsReportDAO.insert(successMetricsReport);

    successMetricsDTO.id = successMetricsReport.getId();

    return successMetricsDTO;
  }

  void deleteSuccessMetricsReportForCurrentUser(String successMetricsId) {
    SuccessMetricsReport successMetricsReport = findSuccessMetricsByIdReportForCurrentUser(successMetricsId);

    successMetricsReportDAO.delete(successMetricsReport);
  }

  private SuccessMetricsReport findSuccessMetricsByIdReportForCurrentUser(String successMetricsId) {
    SuccessMetricsReport successMetricsReport = successMetricsReportDAO.getById(successMetricsId);

    if (successMetricsReport == null || (!currentUser.getUsername().equals(successMetricsReport.getUsername()))) {
      throw new NotFoundException("Cannot find a success metrics report with id " + successMetricsId + " for user id "
          + currentUser.getUsername() + ".");
    }
    return successMetricsReport;
  }
}
