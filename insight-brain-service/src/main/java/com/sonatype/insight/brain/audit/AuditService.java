/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.tag.Tag;

@Singleton
@Named
public class AuditService
{
  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final TagDAO applicationCategoryDAO;

  @Inject
  public AuditService(
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final TagDAO applicationCategoryDAO)
  {
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.applicationCategoryDAO = applicationCategoryDAO;
  }

  public List<OrganizationAuditDTO> getSelectedOrganizationsById(final Set<String> queriedOrganizationIds) {
    if (queriedOrganizationIds == null || queriedOrganizationIds.isEmpty()) {
      return Collections.emptyList();
    }

    // Batch fetch organizations to avoid N+1 pattern (handles large collections via partitioning)
    Map<String, Organization> organizationsById = organizationDAO.getByIds(queriedOrganizationIds)
        .stream()
        .collect(Collectors.toMap(Organization::getId, Function.identity()));

    List<OrganizationAuditDTO> organizationAuditDTOs = new ArrayList<>();
    for (String queriedOrganizationId : queriedOrganizationIds) {
      organizationAuditDTOs.add(new OrganizationAuditDTO(
          queriedOrganizationId,
          organizationsById.get(queriedOrganizationId)));
    }
    return organizationAuditDTOs;
  }

  public List<ApplicationAuditDTO> getSelectedApplicationsById(
      final Set<String> applicationIds,
      final Set<String> organizationIds,
      final Map<String, Application> applicationsById)
  {
    List<ApplicationAuditDTO> applicationAuditDTOs = new ArrayList<>();
    if (applicationIds != null) {
      for (String applicationId : applicationIds) {
        Application application = applicationsById.get(applicationId);
        if (application == null) {
          application = applicationDAO.getById(applicationId);
        }
        if (application == null || organizationIds == null ||
            !organizationIds.contains(application.getOrganizationId()))
        {
          applicationAuditDTOs.add(new ApplicationAuditDTO(applicationId, application));
        }
      }
    }
    return applicationAuditDTOs;
  }

  public List<ApplicationAuditDTO> getSelectedApplicationsById(
      final Set<String> applicationIds,
      final Set<String> organizationIds)
  {
    List<Application> applications = applicationDAO.getByIds(applicationIds);
    return getSelectedApplicationsById(applicationIds, organizationIds, applications);
  }

  public List<ApplicationAuditDTO> getSelectedApplicationsById(
      Set<String> applicationIds,
      Set<String> organizationIds,
      List<Application> applications)
  {
    Map<String, Application> applicationsById = applications.stream()
        .collect(Collectors.toMap(Application::getId, Function.identity()));
    return getSelectedApplicationsById(applicationIds, organizationIds, applicationsById);
  }

  public List<ApplicationCategoryAuditDTO> getSelectedApplicationCategoriesById(
      Set<String> applicationCategoryIds)
  {
    List<ApplicationCategoryAuditDTO> applicationCategoryDTOs = new ArrayList<>();

    if (applicationCategoryIds == null || applicationCategoryIds.isEmpty()) {
      return applicationCategoryDTOs;
    }

    // Collect non-null IDs for batch fetch
    Set<String> nonNullCategoryIds = applicationCategoryIds.stream()
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());

    // Batch fetch application categories to avoid N+1 pattern
    Map<String, Tag> categoriesById = applicationCategoryDAO.getByIds(new ArrayList<>(nonNullCategoryIds))
        .stream()
        .collect(Collectors.toMap(Tag::getId, Function.identity()));

    // Build result list maintaining order of input IDs
    for (String applicationCategoryId : applicationCategoryIds) {
      if (applicationCategoryId == null) {
        applicationCategoryDTOs.add(new ApplicationCategoryAuditDTO(null, "(Uncategorized)"));
      }
      else {
        Tag applicationCategory = categoriesById.get(applicationCategoryId);
        if (applicationCategory == null) {
          applicationCategoryDTOs.add(new ApplicationCategoryAuditDTO(applicationCategoryId, null));
        }
        else {
          applicationCategoryDTOs.add(new ApplicationCategoryAuditDTO(applicationCategory));
        }
      }
    }

    return applicationCategoryDTOs;
  }
}
