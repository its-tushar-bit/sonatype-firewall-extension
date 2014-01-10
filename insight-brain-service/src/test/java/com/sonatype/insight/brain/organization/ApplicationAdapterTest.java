/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.LdapManager;
import com.sonatype.insight.brain.ldap.LdapUser;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.error.exception.NotFoundException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;

public class ApplicationAdapterTest
{

  private ApplicationAdapter applicationAdapter;

  private LdapManager mockLdapManager;

  private OrganizationDAO mockOrganizationDAO;

  private UserDAO mockUserDAO;

  // Application variables
  private String applicationId = "AppId";

  private String applicationName = "MyApplication";

  private String publicId = "publicId";

  private String organizationId = "OrgId";

  private String organizationName = "My Organization";

  // User/Contact variables
  private String userId = "userId";

  private String contactInternalName = "jsmith";

  private String userFirstName = "John";

  private String userLastName = "Smith";

  private String userEmail = "jsmith@gmail.com";

  @Before
  public void setUp() {

    mockLdapManager = Mockito.mock(LdapManager.class);
    mockOrganizationDAO = Mockito.mock(OrganizationDAO.class);
    mockUserDAO = Mockito.mock(UserDAO.class);
    applicationAdapter = new ApplicationAdapter(mockLdapManager, mockOrganizationDAO, mockUserDAO);

    // Return this organization when ever the mock organization DAO getByIdNotNull method is called
    Organization organization = new Organization();
    organization.setName(organizationName);
    organization.setId(organizationId);
    when(mockOrganizationDAO.getByIdNotNull(organizationId)).thenReturn(organization);
  }

  @Test
  public void testConvertApplicationWithUserFromClmRealm() {

    Application application = createApplication();

    // Return this user when ever the mock user DAO getByUsernameLowercase method is called
    User user = createUser();
    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(user);

    ApplicationDTO expectedApplicationDTO = createExpectedDTO("CLM", null);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test(expected = NotFoundException.class)
  public void testConvertApplicationOrganizationNotFound() {

    Application application = createApplication();

    // Return this user when ever the mock user DAO getByUsernameLowercase method is called
    User user = createUser();
    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(user);
    // Throw exception when ever the mock organization DAO getByIdNotNull is called
    when(mockOrganizationDAO.getByIdNotNull(organizationId)).thenThrow(new NotFoundException("Not Found"));

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
  }

  @Test
  public void testConvertApplicationWithUserFromLdapRealm() throws NamingException {

    Application application = createApplication();

    String ldapServerName = "LDAP";

    List<LdapUser> ldapUsers = new ArrayList<>();
    LdapUser ldapUser = createLdapUser();
    ldapUsers.add(ldapUser);

    // Return null when ever the mock user DAO getByUsernameLowercase method is called
    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(null);
    // Return true when ever the mock ldap manager's isLdapEnabled method is called
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(ldapServerName);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenReturn(ldapUsers);

    ApplicationDTO expectedApplicationDTO = createExpectedDTO(ldapServerName, null);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplicationWithUserFromLdapRealmUserNotFound() throws NamingException {

    Application application = createApplication();

    String ldapServerName = "LDAP";

    List<LdapUser> ldapUsers = new ArrayList<>();

    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(ldapServerName);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenReturn(ldapUsers);

    ApplicationDTO expectedApplicationDTO = createExpectedDTO(null,
        "The username " + contactInternalName + " no longer exists");

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplicationWithUserFromLdapRealmLdapNotConfigured() {

    Application application = createApplication();

    String ldapServerName = null;

    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenThrow(new IllegalStateException("Dummy Message"));

    ApplicationDTO expectedApplicationDTO = createExpectedDTO(ldapServerName, "LDAP error");

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplicationWithUserFromLdapRealmLdapErrorOnGetUsers() throws NamingException {

    Application application = createApplication();

    String ldapServerName = "LDAP";

    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(ldapServerName);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenThrow(new NamingException("Dummy Message"));

    ApplicationDTO expectedApplicationDTO = createExpectedDTO(ldapServerName, "LDAP error");

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  private ApplicationDTO createExpectedDTO(String contactRealm, String contactErrorMessage) {

    ApplicationDTO expectedApplicationDTO = new ApplicationDTO();
    expectedApplicationDTO.setPublicId(publicId);
    expectedApplicationDTO.setOrganizationName(organizationName);
    expectedApplicationDTO.setOrganizationId(organizationId);
    expectedApplicationDTO.setId(applicationId);
    expectedApplicationDTO.setName(applicationName);

    ContactDTO expectedContact = new ContactDTO();
    expectedContact.setInternalName(contactInternalName);
    if (contactErrorMessage == null) {
      expectedContact.setDisplayName(userFirstName + " " + userLastName);
      expectedContact.setEmail(userEmail);
    }
    else {
      expectedContact.setError(contactErrorMessage);
    }
    expectedContact.setRealm(contactRealm);
    expectedApplicationDTO.setContact(expectedContact);

    return expectedApplicationDTO;
  }

  private Application createApplication() {

    Application application = new Application();
    application.setId(applicationId);
    application.setOrganizationId(organizationId);
    application.setContactInternalName(contactInternalName);
    application.setPublicId(publicId);
    application.setName(applicationName);

    return application;
  }

  private User createUser() {

    User user = new User();
    user.setId(userId);
    user.setUsername(contactInternalName);
    user.setFirstName(userFirstName);
    user.setLastName(userLastName);
    user.setEmail(userEmail);

    return user;
  }

  private LdapUser createLdapUser() {

    LdapUser user = new LdapUser();
    user.setEmail(userEmail);
    user.setUsername(contactInternalName);
    user.setRealName(userFirstName + " " + userLastName);
    return user;
  }

  private void assertApplication(ApplicationDTO actualApplicationDTO, ApplicationDTO expectedApplicationDTO) {

    Assert.assertNotNull(actualApplicationDTO);
    Assert.assertNotNull(expectedApplicationDTO);
    Assert.assertThat(actualApplicationDTO.getId(), is(expectedApplicationDTO.getId()));
    Assert.assertThat(actualApplicationDTO.getName(), is(expectedApplicationDTO.getName()));
    Assert.assertThat(actualApplicationDTO.getOrganizationId(), is(expectedApplicationDTO.getOrganizationId()));
    Assert.assertThat(actualApplicationDTO.getOrganizationName(), is(expectedApplicationDTO.getOrganizationName()));
    Assert.assertThat(actualApplicationDTO.getPublicId(), is(expectedApplicationDTO.getPublicId()));

    assertContact(actualApplicationDTO.getContact(), expectedApplicationDTO.getContact());
  }

  private void assertContact(ContactDTO actualContact, ContactDTO expectedContact) {

    Assert.assertNotNull(actualContact);
    Assert.assertNotNull(expectedContact);
    Assert.assertThat(actualContact.getInternalName(), is(expectedContact.getInternalName()));
    Assert.assertThat(actualContact.getDisplayName(), is(expectedContact.getDisplayName()));
    Assert.assertThat(actualContact.getEmail(), is(expectedContact.getEmail()));
    Assert.assertThat(actualContact.getRealm(), is(expectedContact.getRealm()));
    Assert.assertThat(actualContact.getError(), is(expectedContact.getError()));
  }
}
