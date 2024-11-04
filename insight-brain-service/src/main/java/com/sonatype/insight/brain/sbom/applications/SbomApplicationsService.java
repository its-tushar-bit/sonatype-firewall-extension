/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.sbom.applications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomApplicationSummaryDTO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.SbomApplicationsSortableField;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.organization.ApplicationAdapter;
import com.sonatype.insight.brain.organization.ApplicationManagementSummaryDTO;
import com.sonatype.insight.brain.organization.ApplicationService;
import com.sonatype.insight.error.exception.BadRequestException;

@Named
@Singleton
public class SbomApplicationsService
{
  private final ApplicationService applicationService;

  private final ApplicationAdapter applicationAdapter;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ApplicationDAO applicationDAO;

  @Inject
  public SbomApplicationsService(final ApplicationService applicationService,
                                 final ApplicationAdapter applicationAdapter,
                                 final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
                                 final ApplicationDAO applicationDAO)
  {
    this.applicationService = applicationService;
    this.applicationAdapter = applicationAdapter;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.applicationDAO = applicationDAO;
  }

  public List<SbomApplicationSummaryDTO> getApplications(
      String applicationName,
      SbomApplicationsSortableField sortBy,
      boolean asc,
      int page,
      int pageSize)
  {
    validatePagination(page, pageSize);
    List<Application> applications = applicationService.getApplications();
    if (applications.isEmpty()) {
      return new ArrayList<>();
    }

    List<ApplicationManagementSummaryDTO> filteredByApplicationName = applicationAdapter
        .createApplicationManagementSummariesWithOnlyAppNameFilter(applications, applicationName);
    Set<String> filteredApplicationIds = filteredByApplicationName.stream().map(ApplicationManagementSummaryDTO::getId)
        .collect(Collectors.toSet());
    if (filteredApplicationIds.isEmpty()) {
      return new ArrayList<>();
    }
    boolean hasPermissionInAllAppsWithNameFilter = filteredApplicationIds.size() == applicationDAO.getCount();
    filteredApplicationIds = hasPermissionInAllAppsWithNameFilter ? Collections.emptySet() : filteredApplicationIds;
    return thirdPartySbomMetadataDAO.getSbomApplicationsWithRecentlyImportedSbomVersion(
        filteredApplicationIds, sortBy, asc, page, pageSize);
  }

  private void validatePagination(int page, int pageSize) {
    if (pageSize < 1) {
      throw new BadRequestException("pageSize must not be less than one!");
    }
    if (page < 1) {
      throw new BadRequestException("page index must not be less than one!");
    }
  }
}
