/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class ApplicationAdapterTest
{

  private static final String LDAP_REALM = "LDAP";

  private static final String CLM_REALM = "CLM";

  private static final String LDAP_ERROR = "LDAP error";

  private static final String TEST_MESSAGE = "Test Exception Message";

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

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    // Return this user when ever the mock user DAO getByUsernameLowercase method is called
    User user = createUser(userId, contactInternalName, userFirstName, userLastName, userEmail);
    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(user);

    ContactDTO expectedContactDTO = createExpectedContact(contactInternalName, userFirstName + " " + userLastName,
        CLM_REALM, userEmail);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test(expected = NotFoundException.class)
  public void testConvertApplicationOrganizationNotFound() {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    // Return this user when ever the mock user DAO getByUsernameLowercase method is called
    User user = createUser(userId, contactInternalName, userFirstName, userLastName, userEmail);
    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(user);
    // Throw exception when ever the mock organization DAO getByIdNotNull is called
    when(mockOrganizationDAO.getByIdNotNull(organizationId)).thenThrow(new NotFoundException(TEST_MESSAGE));

    applicationAdapter.convert(application);
  }

  @Test
  public void testConvertApplicationWithUserFromLdapRealm() throws NamingException {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    List<LdapUser> ldapUsers = new ArrayList<>();
    LdapUser ldapUser = createLdapUser(contactInternalName, userFirstName + " " + userLastName, userEmail);
    ldapUsers.add(ldapUser);

    // Return null when ever the mock user DAO getByUsernameLowercase method is called
    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(null);
    // Return true when ever the mock ldap manager's isLdapEnabled method is called
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenReturn(ldapUsers);

    ContactDTO expectedContactDTO = createExpectedContact(contactInternalName, userFirstName + " " + userLastName,
        LDAP_REALM, userEmail);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplicationWithUserFromLdapCase() throws NamingException {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    List<LdapUser> ldapUsers = new ArrayList<>();
    LdapUser ldapUser = createLdapUser(contactInternalName.toUpperCase(Locale.ENGLISH),
        userFirstName + " " + userLastName, userEmail);
    ldapUsers.add(ldapUser);

    // Return null when ever the mock user DAO getByUsernameLowercase method is called
    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(null);
    // Return true when ever the mock ldap manager's isLdapEnabled method is called
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenReturn(ldapUsers);

    ContactDTO expectedContactDTO = createExpectedContact(contactInternalName.toUpperCase(Locale.ENGLISH),
        userFirstName + " " + userLastName,
        LDAP_REALM, userEmail);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplicationWithUserFromLdapRealmUserNotFound() throws NamingException {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);
    List<LdapUser> ldapUsers = Collections.emptyList();

    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenReturn(ldapUsers);

    ContactDTO expectedContact = createExpectedContactForNotFoundError(contactInternalName);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContact);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplicationWithUserFromLdapRealmLdapNotConfigured() {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenThrow(new IllegalStateException(TEST_MESSAGE));

    ContactDTO expectedContactDTO = createExpectedContactForLdapError(contactInternalName);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplicationWithUserFromLdapRealmLdapErrorOnGetUsers() throws NamingException {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenThrow(new NamingException(TEST_MESSAGE));

    ContactDTO expectedContactDTO = createExpectedContactForLdapError(contactInternalName);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testCreateApplicationsWithUsersFromClm() {

    List<ApplicationDTO> expectedApplicationDTOs = new ArrayList<>();

    List<Application> applications = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;
      String firstName = userFirstName + "-" + i;
      String lastName = userLastName + "-" + i;
      String email = userEmail + "-" + i;
      String displayName = firstName + " " + lastName;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContactDTO = createExpectedContact(contactName, displayName, CLM_REALM, email);
      ApplicationDTO expectedApplicationDTO = createExpectedDTO(appName, appId, expectedContactDTO);
      expectedApplicationDTOs.add(expectedApplicationDTO);
      // Return this user when ever the mock user DAO getByUsernameLowercase method is called
      User user = createUser(userId + "-" + i, contactName, firstName, lastName, email);
      when(mockUserDAO.getByUsername(contactName)).thenReturn(user);
    }
    when(mockLdapManager.isLdapEnabled()).thenReturn(false);

    List<ApplicationDTO> actualApplicationDTOs = applicationAdapter.convert(applications);
    assertApplications(actualApplicationDTOs, expectedApplicationDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithUserFromClm() {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();

    List<Application> applications = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;
      String firstName = userFirstName + "-" + i;
      String lastName = userLastName + "-" + i;
      String email = userEmail + "-" + i;
      String displayName = firstName + " " + lastName;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContact(contactName, displayName, CLM_REALM, email);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, expectedContact));
      // Return this user when ever the mock user DAO getByUsernameLowercase method is called
      User user = createUser(userId + "-" + i, contactName, firstName, lastName, email);
      when(mockUserDAO.getByUsername(contactName)).thenReturn(user);
    }
    when(mockLdapManager.isLdapEnabled()).thenReturn(false);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test(expected = NotFoundException.class)
  public void testCreateApplicationManagementSummariesWhenOrganizationNotFound() {

    List<Application> applications = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;
      String firstName = userFirstName + "-" + i;
      String lastName = userLastName + "-" + i;
      String email = userEmail + "-" + i;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      // Return this user when ever the mock user DAO getByUsernameLowercase method is called
      User user = createUser(userId + "-" + i, contactName, firstName, lastName, email);
      when(mockUserDAO.getByUsername(contactName)).thenReturn(user);
      // Throw exception when ever the mock organization DAO getByIdNotNull is called
      when(mockOrganizationDAO.getByIdNotNull(orgId)).thenThrow(new NotFoundException(TEST_MESSAGE));
    }

    @SuppressWarnings("UnusedDeclaration")
    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithUserFromLdap() throws NamingException {

    LdapGetUsersAnswer ldapGetUsersAnswer = new LdapGetUsersAnswer();
    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();

    List<Application> applications = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;
      String firstName = userFirstName + "-" + i;
      String lastName = userLastName + "-" + i;
      String email = userEmail + "-" + i;
      String displayName = firstName + " " + lastName;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContact(contactName, displayName, LDAP_REALM, email);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, expectedContact));

      LdapUser ldapUser = createLdapUser(contactName, displayName, email);
      ldapGetUsersAnswer.addLdapUser(ldapUser);
    }

    // Return null when ever the mock user DAO getByUsernameLowercase method is called
    when(mockUserDAO.getByUsername(anyString())).thenReturn(null);
    // Return true when ever the mock ldap manager's isLdapEnabled method is called
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenAnswer(ldapGetUsersAnswer);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithUserFromLdapUserNotFound() throws NamingException {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContactForNotFoundError(contactName);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, expectedContact));
    }

    List<LdapUser> ldapUsers = Collections.emptyList();

    // Return null when ever the mock user DAO getByUsernameLowercase method is called
    when(mockUserDAO.getByUsername(anyString())).thenReturn(null);
    // Return true when ever the mock ldap manager's isLdapEnabled method is called
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenReturn(ldapUsers);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithUserFromLdapNotConfigured() {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContactForLdapError(contactName);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, expectedContact));
    }

    when(mockUserDAO.getByUsername(anyString())).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenThrow(new IllegalStateException(TEST_MESSAGE));

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithUserFromLdapErrorOnGetUsers()
      throws NamingException
  {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContactForLdapError(contactName);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, expectedContact));
    }


    when(mockUserDAO.getByUsername(anyString())).thenReturn(null);
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenThrow(new NamingException(TEST_MESSAGE));

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithUserFromClmAndLdap() throws NamingException {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();
    LdapGetUsersAnswer ldapGetUsersAnswer = new LdapGetUsersAnswer();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;
      String firstName = userFirstName + "-" + i;
      String lastName = userLastName + "-" + i;
      String email = userEmail + "-" + i;
      String displayName = firstName + " " + lastName;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact;

      if (i % 2 == 0) {
        // Even numbered will be CLM
        User user = createUser(userId + "-" + i, contactName, firstName, lastName, email);
        when(mockUserDAO.getByUsername(contactName)).thenReturn(user);
        expectedContact = createExpectedContact(contactName, displayName, CLM_REALM, email);
      }
      else {
        // Odd numbered will be LDAP
        LdapUser ldapUser = createLdapUser(contactName, displayName, email);
        ldapGetUsersAnswer.addLdapUser(ldapUser);
        when(mockUserDAO.getByUsername(contactName)).thenReturn(null);
        expectedContact = createExpectedContact(contactName, displayName, LDAP_REALM, email);
      }
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, expectedContact));
    }

    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenAnswer(ldapGetUsersAnswer);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithNullContacts() throws NamingException {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;

      Application application = createApplication(orgId, appName, appId, null);
      applications.add(application);

      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, null));
    }

    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenThrow(new IllegalStateException(TEST_MESSAGE));

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithSameUserFromLdap() throws NamingException {

    String displayName = userFirstName + " " + userLastName;

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();

    List<Application> applications = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;

      Application application = createApplication(orgId, appName, appId, contactInternalName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContact(contactInternalName, displayName, LDAP_REALM, userEmail);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, expectedContact));
    }

    List<LdapUser> ldapUsers = new ArrayList<>();
    LdapUser ldapUser = createLdapUser(contactInternalName, displayName, userEmail);
    ldapUsers.add(ldapUser);

    // Return null when ever the mock user DAO getByUsernameLowercase method is called
    when(mockUserDAO.getByUsername(anyString())).thenReturn(null);
    // Return true when ever the mock ldap manager's isLdapEnabled method is called
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenReturn(ldapUsers);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithSameUserFromClm() {

    String displayName = userFirstName + " " + userLastName;

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;

      Application application = createApplication(orgId, appName, appId, contactInternalName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContact(contactInternalName, displayName, CLM_REALM, userEmail);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, expectedContact));

      // Return this user when ever the mock user DAO getByUsernameLowercase method is called
      User user = createUser(userId, contactInternalName, userFirstName, userLastName, userEmail);
      when(mockUserDAO.getByUsername(contactInternalName)).thenReturn(user);
    }

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithSameUserFromClmAndLdap() throws NamingException {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();
    List<LdapUser> ldapUsers = new ArrayList<>();
    Map<String, LdapUser> ldapUserMap = new LinkedHashMap<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;

      String suffix = "-" + i % 2;
      String contactName = contactInternalName + suffix;
      String firstName = userFirstName + suffix;
      String lastName = userLastName + suffix;
      String email = userEmail + suffix;
      String displayName = firstName + " " + lastName;

      ContactDTO expectedContact;
      if (i % 2 == 0) {
        // Even numbered will be CLM
        User user = createUser(userId, contactName, firstName, lastName, email);
        when(mockUserDAO.getByUsername(contactName)).thenReturn(user);
        expectedContact = createExpectedContact(contactName, displayName, CLM_REALM, email);
      }
      else {
        // Odd numbered will be LDAP
        LdapUser ldapUser = createLdapUser(contactName, displayName, email);
        ldapUserMap.put(ldapUser.getUsername(), ldapUser);
        when(mockUserDAO.getByUsername(contactName)).thenReturn(null);
        expectedContact = createExpectedContact(contactName, displayName, LDAP_REALM, email);
      }
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, expectedContact));

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);
    }
    ldapUsers.addAll(ldapUserMap.values());

    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenReturn(ldapUsers);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithUserFromLdapNotAllFound() throws NamingException {

    LdapGetUsersAnswer ldapGetUsersAnswer = new LdapGetUsersAnswer();
    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;
      String firstName = userFirstName + "-" + i;
      String lastName = userLastName + "-" + i;
      String email = userEmail + "-" + i;
      String displayName = firstName + " " + lastName;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact;

      // Every other item will be found by LDAP
      if (i % 2 == 0) {
        LdapUser ldapUser = createLdapUser(contactName, displayName, email);
        ldapGetUsersAnswer.addLdapUser(ldapUser);
        expectedContact = createExpectedContact(contactName, displayName, LDAP_REALM, email);
      }
      else {
        expectedContact = createExpectedContactForNotFoundError(contactName);
      }
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, expectedContact));
    }

    // Return null when ever the mock user DAO getByUsernameLowercase method is called
    when(mockUserDAO.getByUsername(anyString())).thenReturn(null);
    // Return true when ever the mock ldap manager's isLdapEnabled method is called
    when(mockLdapManager.isLdapEnabled()).thenReturn(true);
    when(mockLdapManager.getLdapServerName()).thenReturn(LDAP_REALM);
    when(mockLdapManager.getUsers(any(String[].class), anyInt())).thenAnswer(ldapGetUsersAnswer);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummariesWithUserFromClmNotAllFound() {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;
      String firstName = userFirstName + "-" + i;
      String lastName = userLastName + "-" + i;
      String email = userEmail + "-" + i;
      String displayName = firstName + " " + lastName;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact;

      // Every other item will be found by LDAP
      if (i % 2 == 0) {
        // Return this user when ever the mock user DAO getByUsernameLowercase method is called
        User user = createUser(userId + "-" + i, contactName, firstName, lastName, email);
        when(mockUserDAO.getByUsername(contactName)).thenReturn(user);
        expectedContact = createExpectedContact(contactName, displayName, CLM_REALM, email);
      }
      else {
        when(mockUserDAO.getByUsername(contactName)).thenReturn(null);
        expectedContact = createExpectedContactForNotFoundError(contactName);
      }

      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, appName, appId, expectedContact));
    }
    when(mockLdapManager.isLdapEnabled()).thenReturn(false);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter.createApplicationManagementSummaries(
        applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  private ApplicationManagementSummaryDTO createExpectedApplicationManagementSummaryDTO(String orgId, String appName,
      String appId, ContactDTO contact)
  {

    ApplicationManagementSummaryDTO dto = new ApplicationManagementSummaryDTO();
    dto.setName(appName);
    dto.setId(appId);
    dto.setPublicId(publicId);
    dto.setOrganizationId(orgId);
    dto.setContact(contact);

    return dto;
  }


  private ApplicationDTO createExpectedDTO(String appName, String appId, ContactDTO contact) {

    ApplicationDTO expectedApplicationDTO = new ApplicationDTO();
    expectedApplicationDTO.setPublicId(publicId);
    expectedApplicationDTO.setOrganizationName(organizationName);
    expectedApplicationDTO.setOrganizationId(organizationId);
    expectedApplicationDTO.setId(appId);
    expectedApplicationDTO.setName(appName);
    expectedApplicationDTO.setContact(contact);

    return expectedApplicationDTO;
  }

  private ContactDTO createExpectedContactForNotFoundError(String internalName) {

    ContactDTO expectedContact = createExpectedContact(internalName, null, null, null);
    expectedContact.setError("The username " + internalName + " no longer exists");

    return expectedContact;
  }

  private ContactDTO createExpectedContactForLdapError(String internalName) {

    ContactDTO expectedContact = createExpectedContact(internalName, null, null, null);
    expectedContact.setError(LDAP_ERROR);

    return expectedContact;
  }


  private ContactDTO createExpectedContact(String internalName, String displayName, String realm, String email) {

    ContactDTO expectedContact = new ContactDTO();
    expectedContact.setInternalName(internalName);
    expectedContact.setDisplayName(displayName);
    expectedContact.setRealm(realm);
    expectedContact.setEmail(email);

    return expectedContact;
  }

  private Application createApplication(String orgid, String appName, String appId, String contact) {

    Application application = new Application();
    application.setId(appId);
    application.setOrganizationId(orgid);
    application.setContactInternalName(contact);
    application.setPublicId(publicId);
    application.setName(appName);

    return application;
  }

  private User createUser(String id, String internalName, String firstName, String lastName, String email) {

    User user = new User();
    user.setId(id);
    user.setUsername(internalName);
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setEmail(email);

    return user;
  }

  private LdapUser createLdapUser(String internalName, String realName, String email) {

    LdapUser user = new LdapUser();
    user.setEmail(email);
    user.setUsername(internalName);
    user.setRealName(realName);
    return user;
  }

  private void assertApplications(List<ApplicationDTO> actualApplicationDTOs,
      List<ApplicationDTO> expectedApplicationDTOs)
  {

    Assert.assertThat(actualApplicationDTOs.size(), is(expectedApplicationDTOs.size()));
    for (int i = 0; i < actualApplicationDTOs.size(); i++) {
      assertApplication(actualApplicationDTOs.get(i), expectedApplicationDTOs.get(i));
    }
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

    if (actualContact == null || expectedContact == null) {
      Assert.assertThat(actualContact, is(expectedContact));
      return;
    }

    Assert.assertThat(actualContact.getInternalName(), is(expectedContact.getInternalName()));
    Assert.assertThat(actualContact.getDisplayName(), is(expectedContact.getDisplayName()));
    Assert.assertThat(actualContact.getEmail(), is(expectedContact.getEmail()));
    Assert.assertThat(actualContact.getRealm(), is(expectedContact.getRealm()));
    Assert.assertThat(actualContact.getError(), is(expectedContact.getError()));
  }

  private void assertApplicationManagementSummaryDTOs(List<ApplicationManagementSummaryDTO> actualList,
      List<ApplicationManagementSummaryDTO> expectedList)
  {
    Assert.assertThat(actualList.size(), is(expectedList.size()));

    for (int i = 0; i < actualList.size(); i++) {
      ApplicationManagementSummaryDTO actual = actualList.get(i);
      ApplicationManagementSummaryDTO expected = expectedList.get(i);

      assertApplicationManagementSummaryDTO(actual, expected);
    }
  }

  private void assertApplicationManagementSummaryDTO(ApplicationManagementSummaryDTO actual,
      ApplicationManagementSummaryDTO expected)
  {

    Assert.assertThat(actual.getId(), is(expected.getId()));
    Assert.assertThat(actual.getName(), is(expected.getName()));
    Assert.assertThat(actual.getOrganizationId(), is(expected.getOrganizationId()));
    Assert.assertThat(actual.getPublicId(), is(expected.getPublicId()));

    assertContact(actual.getContact(), expected.getContact());
  }

  class LdapGetUsersAnswer
      implements Answer<List<LdapUser>>
  {

    private Map<String, LdapUser> ldapUserMap = new HashMap<>();

    public LdapGetUsersAnswer() {

    }

    public void addLdapUser(LdapUser user) {

      ldapUserMap.put(user.getUsername().toLowerCase(Locale.ENGLISH), user);
    }

    public LdapUser getLdapUser(String userName) {

      return ldapUserMap.get(userName.toLowerCase(Locale.ENGLISH));
    }

    @Override
    public List<LdapUser> answer(final InvocationOnMock invocationOnMock) throws Throwable {

      Object[] objects = invocationOnMock.getArguments();
      String[] names = (String[]) objects[0];
      if (names == null || names.length == 0) {
        return Collections.emptyList();
      }

      List<LdapUser> users = new ArrayList<>(names.length);
      for (String name : names) {
        LdapUser user = getLdapUser(name);
        if (user != null) {
          users.add(user);
        }
      }

      return users;
    }
  }
}
