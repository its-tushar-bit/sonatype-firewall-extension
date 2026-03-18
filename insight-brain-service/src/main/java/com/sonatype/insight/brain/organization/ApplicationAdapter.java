/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.UserDirectory;

/**
 * Adapter class to translate between Application entity objects and ApplicationDTO and ApplicationManagementSummaryDTO
 * objects.
 * For performance reasons, it caches data in between calls, so instances of this class should have a short life span.
 * See https://issues.sonatype.org/browse/CLM-15996 for performance details.
 *
 * WARNING: This class is not thread-safe.
 *
 * @since 1.8
 */
@Named
public class ApplicationAdapter
{
  private final OrganizationDAO organizationDAO;

  private final Map<String, Organization> organizationCacheById = new HashMap<>();

  private final ApplicationContactLoader applicationContactLoader;

  @Inject
  public ApplicationAdapter(UserDirectory userDirectory, OrganizationDAO organizationDAO) {
    applicationContactLoader = ApplicationContactLoader.getInstance(userDirectory);
    this.organizationDAO = organizationDAO;
  }

  /**
   * Convert an Application entity into an ApplicationDTO
   *
   * @param application the application entity
   * @return the application DTO
   */
  public ApplicationDTO convert(Application application) {
    return convert(application, true);
  }

  public ApplicationDTO convert(Application application, boolean includeContact) {
    if (application == null) {
      return null;
    }

    ContactDTO contact =
        includeContact ? applicationContactLoader.getContact(application.getContactInternalName()) : null;
    return createApplicationDTO(application, contact);
  }

  /**
   * For performance reasons, the application contact details are not included in the result.
   */
  public List<ApplicationDTO> convert(List<Application> applicationList) {
    if (applicationList == null || applicationList.isEmpty()) {
      return Collections.emptyList();
    }

    List<ApplicationDTO> applicationDTOList = new ArrayList<>(applicationList.size());

    final List<String> internalNameList = new ArrayList<>(applicationList.size());
    for (final Application application : applicationList) {
      final String internalName = application.getContactInternalName();
      internalNameList.add(internalName);
    }

    for (int i = 0; i < applicationList.size(); i++) {
      final ApplicationDTO applicationDTO = createApplicationDTO(applicationList.get(i), null /* contact */);
      applicationDTOList.add(applicationDTO);
    }

    return applicationDTOList;
  }

  /**
   * Create a list of application summary DTOs from the list of applications
   *
   * @param applicationList the list of applications
   * @return the list of application summary DTOs
   */
  public List<ApplicationManagementSummaryDTO> createApplicationManagementSummaries(
      List<Application> applicationList,
      String nameFilter)
  {
    if (nameFilter != null) {
      nameFilter = nameFilter.toLowerCase(Locale.ENGLISH);
    }

    final List<ApplicationManagementSummaryDTO> applicationManagementSummaryDTOList = new ArrayList<>(
        applicationList.size());

    final List<String> internalNameList = new ArrayList<>(applicationList.size());
    for (final Application application : applicationList) {
      Organization organization =
          organizationCacheById.computeIfAbsent(application.getOrganizationId(), organizationDAO::getByIdNotNull);

      if (nameFilter != null && !application.getName().toLowerCase(Locale.ENGLISH).contains(nameFilter)
          && !organization.getName().toLowerCase(Locale.ENGLISH).contains(nameFilter))
      {
        continue;
      }

      ApplicationManagementSummaryDTO summary = new ApplicationManagementSummaryDTO();
      summary.setId(application.getId());
      summary.setName(application.getName());
      summary.setPublicId(application.getPublicId());
      summary.setOrganizationId(organization.getId());
      summary.setOrganizationName(organization.getName());
      summary.setContact(new ContactDTO(application.getContactInternalName()));
      applicationManagementSummaryDTOList.add(summary);

      internalNameList.add(application.getContactInternalName());
    }

    return applicationManagementSummaryDTOList;
  }

  /**
   * Create a list of application summary DTOs from the list of applications
   *
   * @param applicationList the list of applications
   * @return the list of application summary DTOs
   */
  public List<ApplicationManagementSummaryDTO> createApplicationManagementSummariesWithOnlyAppNameFilter(
      List<Application> applicationList,
      String nameFilter)
  {
    if (nameFilter != null) {
      nameFilter = nameFilter.toLowerCase(Locale.ENGLISH);
    }

    final List<ApplicationManagementSummaryDTO> applicationManagementSummaryDTOList = new ArrayList<>(
        applicationList.size());

    final List<String> internalNameList = new ArrayList<>(applicationList.size());
    for (final Application application : applicationList) {

      if (nameFilter != null && !application.getName().toLowerCase(Locale.ENGLISH).contains(nameFilter)) {
        continue;
      }

      ApplicationManagementSummaryDTO summary = new ApplicationManagementSummaryDTO();
      summary.setId(application.getId());
      summary.setName(application.getName());
      summary.setPublicId(application.getPublicId());
      summary.setContact(new ContactDTO(application.getContactInternalName()));
      applicationManagementSummaryDTOList.add(summary);

      internalNameList.add(application.getContactInternalName());
    }

    return applicationManagementSummaryDTOList;
  }

  /**
   * Create the application management summary DTO from the application entity
   *
   * @param application The application entity
   * @return the application management summary DTO
   */
  public ApplicationManagementSummaryDTO createApplicationManagementSummary(Application application) {
    ApplicationManagementSummaryDTO summary = new ApplicationManagementSummaryDTO();
    summary.setId(application.getId());
    summary.setName(application.getName());
    summary.setPublicId(application.getPublicId());
    String organizationId = application.getOrganizationId();
    summary.setOrganizationId(organizationId);
    summary.setOrganizationName(organizationDAO.getByIdNotNull(organizationId).getName());
    final ContactDTO contact = applicationContactLoader.getContact(application.getContactInternalName());
    summary.setContact(contact);

    return summary;
  }

  private ApplicationDTO createApplicationDTO(final Application application, ContactDTO contact) {
    if (application == null) {
      return null;
    }

    final ApplicationDTO applicationDTO = new ApplicationDTO();

    applicationDTO.setId(application.getId());
    applicationDTO.setName(application.getName());
    applicationDTO.setPublicId(application.getPublicId());

    final String organizationId = application.getOrganizationId();
    applicationDTO.setOrganizationId(organizationId);
    Organization org =
        organizationCacheById.computeIfAbsent(organizationId, key -> organizationDAO.getByIdNotNull(key));
    applicationDTO.setOrganizationName(org.getName());

    applicationDTO.setContact(contact);

    return applicationDTO;
  }
}
