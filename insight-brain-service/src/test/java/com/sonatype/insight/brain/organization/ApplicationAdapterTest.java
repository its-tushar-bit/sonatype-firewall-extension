/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.UserDirectory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ApplicationAdapterTest
{

  private static final String REALM = "REALM";

  private static final String USER_DIRECTORY_ERROR = "User directory query result error.";

  private static final String TEST_MESSAGE = "Test Exception Message";

  private ApplicationAdapter applicationAdapter;

  private UserDirectory mockUserDirectory;

  private OrganizationDAO mockOrganizationDAO;

  // Application variables
  private String applicationId = "AppId";

  private String applicationName = "MyApplication";

  private String publicId = "publicId";

  private String organizationId = "OrgId";

  private String organizationName = "My Organization";

  private String contactInternalName = "jsmith";

  private String userFirstName = "John";

  private String userLastName = "Smith";

  private String userEmail = "jsmith@sonatype.com";

  @Before
  public void setUp() {

    mockOrganizationDAO = Mockito.mock(OrganizationDAO.class);
    mockUserDirectory = Mockito.mock(UserDirectory.class);
    applicationAdapter = new ApplicationAdapter(mockUserDirectory, mockOrganizationDAO);

    // Return this organization when ever the mock organization DAO getByIdNotNull method is called
    Organization organization = new Organization(organizationName);
    organization.setId(organizationId);
    when(mockOrganizationDAO.getByIdNotNull(organizationId)).thenReturn(organization);
  }

  @Test
  public void testConvertApplication() {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    // Return this member when ever the mock user directory get members by names is called.
    Member member = createMember(contactInternalName, userFirstName + " " + userLastName, userEmail, REALM);
    Set<String> userNames = Sets.newHashSet(contactInternalName);
    setQueryResultForGetMembersByNames(mockUserDirectory, userNames, Lists.newArrayList(member), null);

    ContactDTO expectedContactDTO = createExpectedContact(contactInternalName, userFirstName + " " + userLastName,
        REALM, userEmail);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplication_WithUpperCaseInternalName() {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    // Member has a name that is in all upper case but searching is done in a case-insensitive manner.
    Member member = createMember(contactInternalName.toUpperCase(Locale.ENGLISH), userFirstName + " " + userLastName,
        userEmail, REALM);
    Set<String> userNames = Sets.newHashSet(contactInternalName);
    setQueryResultForGetMembersByNames(mockUserDirectory, userNames, Lists.newArrayList(member), null);

    ContactDTO expectedContactDTO = createExpectedContact(contactInternalName.toUpperCase(Locale.ENGLISH),
        userFirstName + " " + userLastName, REALM, userEmail);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplication_WithUnfoundUser() {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);
    setQueryResultForGetMembersByNames(mockUserDirectory, Sets.newHashSet(contactInternalName),
        new ArrayList<Member>(), null);

    ContactDTO expectedContact = createExpectedContactForNotFoundError(contactInternalName);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContact);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplication_WithUserDirectoryException() {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);
    setQueryResultForGetMembersByNames(mockUserDirectory, Sets.newHashSet(contactInternalName),
        new ArrayList<Member>(), new Exception(TEST_MESSAGE));

    ContactDTO expectedContactDTO = createExpectedContactForUserDirectoryError(contactInternalName);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testCreateApplications() {
    List<ApplicationDTO> expectedApplicationDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();
    List<Member> members = new ArrayList<>();
    Set<String> memberNames = new HashSet<String>();

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

      ContactDTO expectedContactDTO = createExpectedContact(contactName, displayName, REALM, email);
      ApplicationDTO expectedApplicationDTO = createExpectedDTO(appName, appId, expectedContactDTO);
      expectedApplicationDTOs.add(expectedApplicationDTO);

      // These members will be returned by the user directory.
      Member member = createMember(contactName, firstName + " " + lastName, email, REALM);
      members.add(member);
      memberNames.add(contactName);
    }

    setQueryResultForGetMembersByNames(mockUserDirectory, memberNames, members, null);

    List<ApplicationDTO> actualApplicationDTOs = applicationAdapter.convert(applications);
    assertApplications(actualApplicationDTOs, expectedApplicationDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummaries() {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();

    Set<String> memberNames = new HashSet<>();
    List<Member> members = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String orgName = organizationName;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;
      String firstName = userFirstName + "-" + i;
      String lastName = userLastName + "-" + i;
      String email = userEmail + "-" + i;
      String displayName = firstName + " " + lastName;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContact(contactName, displayName, REALM, email);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, expectedContact));

      Member member = createMember(contactName, firstName + " " + lastName, email, REALM);
      memberNames.add(contactName);
      members.add(member);
    }

    setQueryResultForGetMembersByNames(mockUserDirectory, memberNames, members, null);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);

    // Expect only one call as all applications in the test have the same organization
    verify(mockOrganizationDAO, times(1)).getByIdNotNull(anyString());
  }

  @Test
  public void testCreateApplicationManagementSummaries_WithUnfoundUsers() {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    Set<String> memberNames = new HashSet<>();
    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String orgName = organizationName;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContactForNotFoundError(contactName);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, expectedContact));

      // The names of the members that will be queried but not found.
      memberNames.add(contactName);
    }

    List<Member> members = Collections.emptyList();

    setQueryResultForGetMembersByNames(mockUserDirectory, memberNames, members, null);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);

    // Expect only one call as all applications in the test have the same organization
    verify(mockOrganizationDAO, times(1)).getByIdNotNull(anyString());
  }

  @Test
  public void testCreateApplicationManagementSummaries_WithUserDirectoryException() {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    Set<String> memberNames = new HashSet<>();
    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String orgName = organizationName;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContactForUserDirectoryError(contactName);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, expectedContact));

      memberNames.add(contactName);
    }

    List<Member> members = Collections.emptyList();

    setQueryResultForGetMembersByNames(mockUserDirectory, memberNames, members,
 new Exception(TEST_MESSAGE));

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);

    // Expect only one call as all applications in the test have the same organization
    verify(mockOrganizationDAO, times(1)).getByIdNotNull(anyString());
  }

  @Test
  public void testCreateApplicationManagementSummaries_WithNullUserNames() {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String orgName = organizationName;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;

      Application application = createApplication(orgId, appName, appId, null);
      applications.add(application);

      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, null));
    }

    // Internally the null contact names get converted into a list of nulls, which gets converted into a set which would
    // only contain one null.
    Set<String> nullMemberNames = new HashSet<>();
    nullMemberNames.add(null);
    List<Member> members = Collections.emptyList();
        
    setQueryResultForGetMembersByNames(mockUserDirectory, nullMemberNames, members,
        null);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);

    // Expect only one call as all applications in the test have the same organization
    verify(mockOrganizationDAO, times(1)).getByIdNotNull(anyString());
  }

  @Test
  public void testCreateApplicationManagementSummaries_WithSameUser() {

    String displayName = userFirstName + " " + userLastName;

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();

    List<Application> applications = new ArrayList<>();
    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String orgName = organizationName;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;

      Application application = createApplication(orgId, appName, appId, contactInternalName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContact(contactInternalName, displayName, REALM, userEmail);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, expectedContact));
    }

    Set<String> memberNames = Sets.newHashSet(contactInternalName);
    List<Member> members = new ArrayList<>();
    members.add(createMember(contactInternalName, displayName, userEmail, REALM));

    setQueryResultForGetMembersByNames(mockUserDirectory, memberNames, members, null);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);

    // Expect only one call as all applications in the test have the same organization
    verify(mockOrganizationDAO, times(1)).getByIdNotNull(anyString());
  }

  @Test
  public void testCreateApplicationManagementSummaries_NotAllUsersFound() {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();
    Set<String> memberNames = new HashSet<>();
    List<Member> members = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String orgName = organizationName;
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

      // All names will be passed to the user directory, but only half of them will be found.
      memberNames.add(contactName);
      if (i % 2 == 0) {
        Member member = createMember(contactName, displayName, email, REALM);
        members.add(member);
        expectedContact = createExpectedContact(contactName, displayName, REALM, email);
      }
      else {
        expectedContact = createExpectedContactForNotFoundError(contactName);
      }
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, expectedContact));
    }

    setQueryResultForGetMembersByNames(mockUserDirectory, memberNames, members, null);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);

    // Expect only one call as all applications in the test have the same organization
    verify(mockOrganizationDAO, times(1)).getByIdNotNull(anyString());
  }

  private ApplicationManagementSummaryDTO createExpectedApplicationManagementSummaryDTO(String orgId, String orgName,
      String appName, String appId, ContactDTO contact)
  {

    ApplicationManagementSummaryDTO dto = new ApplicationManagementSummaryDTO();
    dto.setName(appName);
    dto.setId(appId);
    dto.setPublicId(publicId);
    dto.setOrganizationId(orgId);
    dto.setOrganizationName(orgName);
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
    expectedContact.setError("The username " + internalName + " no longer exists.");

    return expectedContact;
  }

  private ContactDTO createExpectedContactForUserDirectoryError(String internalName) {

    ContactDTO expectedContact = createExpectedContact(internalName, null, null, null);
    expectedContact.setError(USER_DIRECTORY_ERROR);

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
    Application application = new Application(publicId, appName, orgid);
    application.setId(appId);
    application.setContactInternalName(contact);

    return application;
  }

  private Member createMember(String internalName, String displayName, String email, String realm) {
    Member member = new Member();
    member.setInternalName(internalName);
    member.setDisplayName(displayName);
    member.setEmail(email);
    member.setRealm(realm);
    member.setType(MemberType.USER);

    return member;
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
    Assert.assertThat(actual.getOrganizationName(), is(expected.getOrganizationName()));
    Assert.assertThat(actual.getPublicId(), is(expected.getPublicId()));

    assertContact(actual.getContact(), expected.getContact());
  }

  private void setQueryResultForGetMembersByNames(UserDirectory userDirectory, Set<String> names,
      List<Member> members, Exception exception)
  {
    UserDirectory.QueryResult result = new UserDirectory.QueryResult(members, exception);
    when(userDirectory.getMembersByNames(names, false)).thenReturn(result);
  }

}
