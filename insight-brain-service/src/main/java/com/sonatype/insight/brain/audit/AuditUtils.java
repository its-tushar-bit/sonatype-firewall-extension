/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.audit;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.tag.Tag;

public class AuditUtils
{
  private static final OrganizationDAO organizationDAO = new OrganizationDAO();

  private static final ApplicationDAO applicationDAO = new ApplicationDAO();

  private static final TagDAO applicationCategoryDAO = new TagDAO();

  private AuditUtils() {
    // Utility class
  }

  public static List<OrganizationAuditDTO> getSelectedOrganizationsById(final Set<String> queriedOrganizationIds) {
    List<OrganizationAuditDTO> organizationAuditDTOs = new ArrayList<>();

    if (queriedOrganizationIds != null) {
      for (String queriedOrganizationId : queriedOrganizationIds) {
        organizationAuditDTOs
            .add(new OrganizationAuditDTO(queriedOrganizationId, organizationDAO.getById(queriedOrganizationId)));
      }
    }
    return organizationAuditDTOs;
  }

  public static List<ApplicationAuditDTO> getSelectedApplicationsById(final Set<String> applicationIds,
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
            !organizationIds.contains(application.getOrganizationId())) {
          applicationAuditDTOs.add(new ApplicationAuditDTO(applicationId, application));
        }
      }
    }
    return applicationAuditDTOs;
  }

  public static List<ApplicationAuditDTO> getSelectedApplicationsById(final Set<String> applicationIds,
                                                                      final Set<String> organizationIds)
  {
    List<Application> applications = applicationDAO.getByIds(applicationIds);
    return getSelectedApplicationsById(applicationIds, organizationIds, applications);
  }

  public static List<ApplicationAuditDTO> getSelectedApplicationsById(Set<String> applicationIds,
                                                                      Set<String> organizationIds,
                                                                      List<Application> applications)
  {
    Map<String, Application> applicationsById = applications.stream()
        .collect(Collectors.toMap(Application::getId, Function.identity()));
    return getSelectedApplicationsById(applicationIds, organizationIds, applicationsById);
  }

  @SuppressWarnings("checkstyle:LineLength")
  public static List<ApplicationCategoryAuditDTO> getSelectedApplicationCategoriesById(Set<String> applicationCategoryIds) {
    List<ApplicationCategoryAuditDTO> applicationCategoryDTOs = new ArrayList<>();

    if (applicationCategoryIds == null) {
      return applicationCategoryDTOs;
    }

    for (String applicationCategoryId : applicationCategoryIds) {
      if (applicationCategoryId == null) {
        applicationCategoryDTOs.add(new ApplicationCategoryAuditDTO(null, "(Uncategorized)"));
      }
      else {
        Tag applicationCategory = applicationCategoryDAO.getById(applicationCategoryId);
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
