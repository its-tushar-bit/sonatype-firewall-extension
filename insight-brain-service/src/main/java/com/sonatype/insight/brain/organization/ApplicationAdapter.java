/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.UserDirectory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter class to translate between Application entity objects and ApplicationDTO objects
 *
 * @since 1.8
 */
@Named
@Singleton
public class ApplicationAdapter
{
  private static final Logger log = LoggerFactory.getLogger(ApplicationAdapter.class);

  private final OrganizationDAO organizationDAO;

  private final UserDirectory userDirectory;

  @Inject
  public ApplicationAdapter(UserDirectory userDirectory) {
    this(userDirectory, new OrganizationDAO());
  }

  public ApplicationAdapter(UserDirectory userDirectory, OrganizationDAO organizationDAO) {
    this.userDirectory = userDirectory;
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

    final ContactDTO contact = includeContact ? getContact(application.getContactInternalName()) : null;
    return createApplicationDTO(application, contact);
  }

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

    final ContactDTO[] contacts = getContacts(internalNameList);

    for (int i = 0; i < applicationList.size(); i++) {
      final ApplicationDTO applicationDTO = createApplicationDTO(applicationList.get(i), contacts[i]);
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

    // Cache of Organizations to avoid hitting the DB multiple times for same organization
    final Map<String, Organization> organizationMap = new HashMap<>();

    final List<String> internalNameList = new ArrayList<>(applicationList.size());
    for (final Application application : applicationList) {
      Organization organization =
          organizationMap.computeIfAbsent(application.getOrganizationId(), organizationDAO::getByIdNotNull);

      if (nameFilter != null && !application.getName().toLowerCase(Locale.ENGLISH).contains(nameFilter)
          && !organization.getName().toLowerCase(Locale.ENGLISH).contains(nameFilter)) {
        continue;
      }

      ApplicationManagementSummaryDTO summary = new ApplicationManagementSummaryDTO();
      summary.setId(application.getId());
      summary.setName(application.getName());
      summary.setPublicId(application.getPublicId());
      summary.setOrganizationId(organization.getId());
      summary.setOrganizationName(organization.getName());
      applicationManagementSummaryDTOList.add(summary);

      internalNameList.add(application.getContactInternalName());
    }

    final ContactDTO[] contacts = getContacts(internalNameList);
    for (int i = 0; i < applicationManagementSummaryDTOList.size(); i++) {
      final ApplicationManagementSummaryDTO summary = applicationManagementSummaryDTOList.get(i);
      summary.setContact(contacts[i]);
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
    final ContactDTO contact = getContact(application.getContactInternalName());
    summary.setContact(contact);

    return summary;
  }

  public ContactDTO getContact(final String internalName) {
    return getContacts(Arrays.asList(internalName))[0];
  }

  /**
   * Get the contact DTO from the contact internal name (username)
   * 
   * @param internalNamesList the list of contact internal names to look up
   * @return the contact DTO array (guaranteed to be the same size as the input list)
   */
  public ContactDTO[] getContacts(List<String> internalNamesList) {
    if (internalNamesList == null || internalNamesList.isEmpty()) {
      return new ContactDTO[0];
    }

    // Preserving the original choice of an array and ordering as other parts of the API depend on this.
    ContactDTO[] contacts = new ContactDTO[internalNamesList.size()];

    Map<String, ContactDTO> nameToContactMap;
    UserDirectory.QueryResult result = userDirectory.getUsersByName(new HashSet<>(internalNamesList));
    if (result.hasException()) {
      log.error("An exception occurred while trying to resolve user names; " +
          "attempting to resolve user names using the local Nexus IQ realm.", result.getException());

      // Map the existing names potentially loaded by the CLM data store.
      nameToContactMap = mapNameToContact(result.get());
      // Add the remaining names as user directory errors.
      putUserDirectoryErrorContacts(internalNamesList, nameToContactMap);
    }
    else {
      nameToContactMap = mapNameToContact(result.get());
    }

    putUnknownErrorContacts(internalNamesList, nameToContactMap);

    // Place the contacts into the contact array in the order the names were given.
    for (int i = 0; i < contacts.length; i++) {
      contacts[i] = nameToContactMap.get(toLowerCase(internalNamesList.get(i)));
    }

    return contacts;
  }

  private String toLowerCase(String string) {
    if (string == null) {
      return null;
    }

    return string.toLowerCase(Locale.ENGLISH);
  }

  private void putUnknownErrorContacts(List<String> internalNamesList, Map<String, ContactDTO> nameToContactMap) {
    // If we've already mapped all the names no work needs to be done.
    if (nameToContactMap.size() == internalNamesList.size()) {
      return;
    }

    for (String internalName : internalNamesList) {
      if (internalName != null && !nameToContactMap.containsKey(toLowerCase(internalName))) {
        nameToContactMap.put(toLowerCase(internalName),
            createErrorContact(internalName, "The username " + internalName + " no longer exists."));
      }
    }
  }

  private void putUserDirectoryErrorContacts(List<String> internalNamesList, Map<String, ContactDTO> nameToContactMap) {
    // If we've already mapped all the names no work needs to be done.
    if (nameToContactMap.size() == internalNamesList.size()) {
      return;
    }

    for (String internalName : internalNamesList) {
      if (internalName != null && !nameToContactMap.containsKey(toLowerCase(internalName))) {
        nameToContactMap.put(toLowerCase(internalName),
            createErrorContact(internalName, "User directory query result error."));
      }
    }
  }

  private Map<String, ContactDTO> mapNameToContact(List<Member> members) {
    Map<String, ContactDTO> result = new HashMap<>();

    for (Member member : members) {
      result.put(member.getInternalNameLowerCase(), new ContactDTO(member.getInternalName(), member.getDisplayName(),
          member.getEmail(), member.getRealm()));
    }

    return result;
  }

  private ContactDTO createErrorContact(String internalName, String errorMessage) {

    ContactDTO contact = new ContactDTO(internalName, null, null, null);
    contact.setError(errorMessage);

    return contact;
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
    applicationDTO.setOrganizationName(organizationDAO.getByIdNotNull(organizationId).getName());

    applicationDTO.setContact(contact);

    return applicationDTO;
  }
}
