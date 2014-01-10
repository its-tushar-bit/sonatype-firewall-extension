/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.naming.NamingException;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.CLMRealm;

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

    final ApplicationDTO applicationDTO = new ApplicationDTO();

    applicationDTO.setId(application.getId());
    applicationDTO.setName(application.getName());
    applicationDTO.setPublicId(application.getPublicId());

    String organizationId = application.getOrganizationId();
    if (organizationId != null) {
      applicationDTO.setOrganizationId(organizationId);
      applicationDTO.setOrganizationName(organizationDAO.getByIdNotNull(organizationId).getName());
    }

    applicationDTO.setContact(getContact(application.getContactInternalName()));

    return applicationDTO;
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
    summary.setOrganizationId(application.getOrganizationId());
    summary.setContact(getContact(application.getContactInternalName()));

    return summary;
  }

  /**
   * Get the contact DTO from the contact internal name (username)
   *
   * @param contactInternalName the contact internal name to look up the contact by
   * @return the contact DTO
   */
  private ContactDTO getContact(String contactInternalName) {

    if (contactInternalName == null) {
      return null;
    }

    ContactDTO contact = null;

    // Get the user from the database
    User user = userDAO.getByUsername(contactInternalName);
    if (user != null) {
      // Create the contact member and set it on the application DTO
      contact = new ContactDTO(user.getUsername(), user.calculateDisplayName(), user.getEmail(), CLMRealm.DISPLAY_NAME);
    }
    else if (ldapManager.isLdapEnabled()) {
      // If not found in DB and LDAP is enabled lookup user there
      String ldapServerName = null;
      try {
        ldapServerName = ldapManager.getLdapServerName();
        String[] names = {contactInternalName};
        List<LdapUser> ldapUsers = ldapManager.getUsers(names, 1);
        if (!ldapUsers.isEmpty()) {
          LdapUser ldapUser = ldapUsers.get(0);
          // Create the contact member and set it on the application DTO
          contact = new ContactDTO(ldapUser.getUsername(), ldapUser.getRealName(), ldapUser.getEmail(), ldapServerName);
        }
      }
      catch (NamingException | IllegalStateException e) {
        log.error("LDAP exception when trying to resolve user names", e);
        contact = createErrorContact(contactInternalName, ldapServerName, "LDAP error");
      }
    }

    if (contact == null) {
      // No contact found in CLM and LDAP not configured, so create a contact with an error message
      contact = createErrorContact(contactInternalName, null,
          "The username " + contactInternalName + " no longer exists");
    }

    return contact;
  }

  private ContactDTO createErrorContact(String internalName, String realm, String errorMessage) {

    ContactDTO contact = new ContactDTO(internalName, null, null, realm);
    contact.setError(errorMessage);

    return contact;
  }
}
