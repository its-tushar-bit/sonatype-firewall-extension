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

import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetrics;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.brain.organization.OrganizationService;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.json.store.JsonUtils;

import static org.apache.commons.collections.CollectionUtils.isEmpty;

/**
 * @since 1.36
 */
@Named
public class SuccessMetricsService
{
  private final ApplicationService applicationService;

  private final OrganizationService organizationService;

  private final SuccessMetricsDAO successMetricsDAO;

  private final CurrentUser currentUser;

  @Inject
  public SuccessMetricsService(ApplicationService applicationService,
                               OrganizationService organizationService,
                               SuccessMetricsDAO successMetricsDAO,
                               CurrentUser currentUser)
  {
    this.applicationService = applicationService;
    this.organizationService = organizationService;
    this.successMetricsDAO = successMetricsDAO;
    this.currentUser = currentUser;
  }

  List<SuccessMetricsDTO> getSuccessMetricsForCurrentUser() throws IOException {
    String username = currentUser.getUsername();

    List<SuccessMetricsDTO> successMetricsDTOs = new ArrayList<>();
    for (SuccessMetrics successMetrics : successMetricsDAO.getByUsername(username)) {
      SuccessMetricsScopeDTO dto = JsonUtils.parse(successMetrics.getScopeJson(), SuccessMetricsScopeDTO.class);

      SuccessMetricsDTO successMetricsDTO = new SuccessMetricsDTO();
      successMetricsDTO.id = successMetrics.getId();
      successMetricsDTO.name = successMetrics.getName();
      successMetricsDTO.scope = dto;

      pruneUnauthorizedApplicationIds(dto);
      pruneUnauthorizedOrganizationIds(dto);
      successMetricsDTOs.add(successMetricsDTO);
    }
    return successMetricsDTOs;
  }

  SuccessMetricsDTO createSuccessMetricsForCurrentUser(SuccessMetricsDTO successMetricsDTO) {
    String username = currentUser.getUsername();

    SuccessMetrics successMetrics = new SuccessMetrics(successMetricsDTO.name);
    successMetrics.setScopeJson(JsonUtils.format(successMetricsDTO.scope));
    successMetrics.setUsername(username);
    successMetricsDAO.insert(successMetrics);

    successMetricsDTO.id = successMetrics.getId();

    return successMetricsDTO;
  }

  void deleteSuccessMetricsForCurrentUser(String successMetricsId) {
    SuccessMetrics successMetrics = findSuccessMetricsByIdForCurrentUser(successMetricsId);

    successMetricsDAO.delete(successMetrics);
  }

  private SuccessMetrics findSuccessMetricsByIdForCurrentUser(String successMetricsId) {
    SuccessMetrics successMetrics = successMetricsDAO.getById(successMetricsId);

    if (successMetrics == null || (!currentUser.getUsername().equals(successMetrics.getUsername()))) {
      throw new NotFoundException(
          "Cannot find a success metrics with id " + successMetricsId + " for user id " + currentUser.getUsername() +
              ".");
    }
    return successMetrics;
  }

  private void pruneUnauthorizedApplicationIds(SuccessMetricsScopeDTO dto) {
    if (!isEmpty(dto.applicationIds)) {
      List<Application> apps = applicationService
          .getApplicationsByIdsAndOrganizationIdsAndTagIds(null, dto.applicationIds, null);
      dto.applicationIds.clear();
      for (Application app : apps) {
        dto.applicationIds.add(app.getId());
      }
    }
  }

  private void pruneUnauthorizedOrganizationIds(SuccessMetricsScopeDTO dto) {
    if (!isEmpty(dto.organizationIds)) {
      List<Organization> orgs = organizationService.getOrganizationsByIds(dto.organizationIds);
      dto.organizationIds.clear();
      for (Organization org : orgs) {
        dto.organizationIds.add(org.getId());
      }
    }
  }
}
