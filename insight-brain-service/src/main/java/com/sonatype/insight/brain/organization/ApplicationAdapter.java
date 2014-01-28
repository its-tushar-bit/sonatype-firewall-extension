/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingException;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.CLMRealm;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
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

  private final UserDAO userDAO;

  private final LdapManager ldapManager;

  @Inject
  public ApplicationAdapter(final LdapManager ldapManager) {
    this(ldapManager, new OrganizationDAO(), new UserDAO());
  }

  public ApplicationAdapter(final LdapManager ldapManager, OrganizationDAO organizationDAO, UserDAO userDAO) {
    this.ldapManager = ldapManager;
    this.organizationDAO = organizationDAO;
    this.userDAO = userDAO;
  }

  /**
   * Convert an Application entity into an ApplicationDTO
   *
   * @param application the application entity
   * @return the application DTO
   */
  public ApplicationDTO convert(Application application) {

    if (application == null) {
      return null;
    }

    final ContactDTO contact = getContact(application.getContactInternalName());
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
  public List<ApplicationManagementSummaryDTO> createApplicationManagementSummaries(List<Application> applicationList) {

    final List<ApplicationManagementSummaryDTO> applicationManagementSummaryDTOList = new ArrayList<>(
        applicationList.size());

    // Cache of Organizations to avoid hitting the DB multiple times for same organization
    final Map<String, Organization> organizationMap = new HashMap<>();

    final List<String> internalNameList = new ArrayList<>(applicationList.size());
    for (final Application application : applicationList) {
      final String internalName = application.getContactInternalName();
      internalNameList.add(internalName);
      String organizationId = application.getOrganizationId();
      if (!organizationMap.containsKey(organizationId)) {
        organizationMap.put(organizationId, organizationDAO.getByIdNotNull(organizationId));
      }
    }

    final ContactDTO[] contacts = getContacts(internalNameList);
    for (int i = 0; i < applicationList.size(); i++) {
      Application application = applicationList.get(i);
      final ApplicationManagementSummaryDTO summary = new ApplicationManagementSummaryDTO();
      summary.setId(application.getId());
      summary.setName(application.getName());
      summary.setPublicId(application.getPublicId());

      String organizationId = application.getOrganizationId();
      summary.setOrganizationId(organizationId);
      Organization org = organizationMap.get(organizationId);
      summary.setOrganizationName(org.getName());

      summary.setContact(contacts[i]);
      applicationManagementSummaryDTOList.add(summary);
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

  private ContactDTO getContact(final String internalName) {

    return getContacts(Arrays.asList(internalName))[0];
  }

  /**
   * Get the contact DTO from the contact internal name (username)
   *
   * @param internalNamesList the list of contact internal names to look up
   * @return the contact DTO array (guaranteed to be the same size as the input list)
   */
  private ContactDTO[] getContacts(List<String> internalNamesList) {

    if (internalNamesList == null || internalNamesList.isEmpty()) {
      return new ContactDTO[0];
    }

    final ContactDTO[] contacts = new ContactDTO[internalNamesList.size()];

    // Multi-map to keep the internal names that need to be looked up in LDAP (also the positions in the array)
    final ListMultimap<String, Integer> notFoundInClmMap = ArrayListMultimap.create();

    // First look up each internal name in the CLM database
    int i = 0;
    for (String internalName : internalNamesList) {
      if (internalName == null) {
        // No internal name for this entry, so set the contact to null
        contacts[i] = null;
      }
      else {
        // Look up user in database
        User user = userDAO.getByUsername(internalName);
        if (user != null) {
          // Found in CLM database so add contact for this entry
          ContactDTO contact = new ContactDTO(user.getUsername(), user.calculateDisplayName(), user.getEmail(),
              CLMRealm.DISPLAY_NAME);
          contacts[i] = contact;
        }
        else if (ldapManager.isLdapEnabled()) {
          // Not found in CLM and LDAP is configured so add to the LDAP map
          // Since LDAP is case-insensitive we normalize the map with only lowercase keys
          notFoundInClmMap.put(internalName.toLowerCase(Locale.ENGLISH), i);
        }
        else {
          // No contact found in CLM and LDAP not configured, so create a contact with an error message
          ContactDTO contact = createErrorContact(internalName, "The username " + internalName + " no longer exists");
          contacts[i] = contact;
        }
      }
      i++;
    }

    // Now look up the items not found in the CLM database from LDAP
    // If LDAP is enabled we lookup any users not found in the CLM database
    // Note this map will be empty if ldap is not enabled or if all users found in CLM database
    if (!notFoundInClmMap.isEmpty()) {

      List<LdapUser> ldapUsers = null;
      String ldapServerName = null;

      Set<String> keys = notFoundInClmMap.keySet();
      String[] internalNames = keys.toArray(new String[keys.size()]);
      try {
        ldapServerName = ldapManager.getLdapServerName();
        ldapUsers = ldapManager.getUsers(internalNames, internalNames.length);
      }
      catch (NamingException | IllegalStateException e) {
        log.error("LDAP exception when trying to resolve user names", e);

        // Create LDAP general error for all items in the map and return the contact list
        for (Entry<String, Integer> entry : notFoundInClmMap.entries()) {
          String internalName = entry.getKey();
          Integer index = entry.getValue();
          ContactDTO contact = createErrorContact(internalName, "LDAP error");
          contacts[index] = contact;
        }
        return contacts;
      }

      if (ldapUsers != null) {
        for (LdapUser ldapUser : ldapUsers) {
          // Create the contact member and set it on the application DTO
          final ContactDTO contact = new ContactDTO(ldapUser.getUsername(), ldapUser.getRealName(), ldapUser.getEmail(),
              ldapServerName);
          // remove the item from the map and add the contact to the list at the desired positions
          // Since LDAP is case-insensitive we normalize the map with only lowercase keys
          final List<Integer> positions = notFoundInClmMap
              .removeAll(contact.getInternalName().toLowerCase(Locale.ENGLISH));
          for (int position : positions) {
            contacts[position] = contact;
          }
        }
      }

      // Create errors for any items left in the map
      for (final Entry<String, Integer> entry : notFoundInClmMap.entries()) {
        String internalName = entry.getKey();
        Integer index = entry.getValue();
        ContactDTO contact = createErrorContact(internalName, "The username " + internalName + " no longer exists");
        contacts[index] = contact;
      }
    }

    return contacts;
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
