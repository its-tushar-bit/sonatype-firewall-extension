/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
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

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.UserDirectory;

import org.eclipse.sisu.launch.InjectedTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApplicationAdapterTest
    extends InjectedTest
{
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity();

  private static final String USER_DIRECTORY_ERROR = "User directory query result error.";

  private static final String TEST_MESSAGE = "Test Exception Message";

  @Inject
  private ApplicationAdapter applicationAdapter;

  // Application variables
  private String applicationId = "AppId";

  private String applicationName = "MyApplication";

  private String publicId = "publicId";

  private String organizationId;

  private String organizationName = "My Organization";

  private String contactInternalName = "jsmith";

  private String userFirstName = "John";

  private String userLastName = "Smith";

  private String userEmail = "jsmith@sonatype.com";

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    Organization org = tempEntity.newOrganization(organizationName);
    organizationId = org.getId();
  }

  @Test
  public void testConvertApplication() {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    createMember(contactInternalName, userFirstName, userLastName, userEmail);

    ContactDTO expectedContactDTO = createExpectedContact(contactInternalName, userFirstName + " " + userLastName,
        InternalRealm.DISPLAY_NAME, userEmail);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplication_WithUpperCaseInternalName() {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    // Member has a name that is in all upper case but searching is done in a case-insensitive manner.
    createMember(contactInternalName.toUpperCase(Locale.ENGLISH), userFirstName, userLastName, userEmail);

    ContactDTO expectedContactDTO = createExpectedContact(contactInternalName.toUpperCase(Locale.ENGLISH),
        userFirstName + " " + userLastName, InternalRealm.DISPLAY_NAME, userEmail);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplication_WithUnfoundUser() {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    ContactDTO expectedContact = createExpectedContactForNotFoundError(contactInternalName);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContact);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplication_WithUserDirectoryException() {

    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    UserDirectory.QueryResult result = new UserDirectory.QueryResult(new ArrayList<Member>(),
        new Exception(TEST_MESSAGE));
    UserDirectory mockUserDirectory = mock(UserDirectory.class);
    when(mockUserDirectory.getUsersByName(Collections.singleton(contactInternalName))).thenReturn(result);

    ContactDTO expectedContactDTO = createExpectedContactForUserDirectoryError(contactInternalName);
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    applicationAdapter = new ApplicationAdapter(mockUserDirectory);
    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvertApplication_ExcludeContact() {
    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    ContactDTO expectedContactDTO = null;
    ApplicationDTO expectedApplicationDTO = createExpectedDTO(applicationName, applicationId, expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application, false);
    assertApplication(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testCreateApplications() {
    List<ApplicationDTO> expectedApplicationDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();
    List<Member> members = new ArrayList<>();
    Set<String> memberNames = new HashSet<>();

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

      ContactDTO expectedContactDTO = createExpectedContact(contactName, displayName, InternalRealm.DISPLAY_NAME,
          email);
      ApplicationDTO expectedApplicationDTO = createExpectedDTO(appName, appId, expectedContactDTO);
      expectedApplicationDTOs.add(expectedApplicationDTO);

      // These members will be returned by the user directory.
      Member member = createMember(contactName, firstName, lastName, email);
      members.add(member);
      memberNames.add(contactName);
    }

    List<ApplicationDTO> actualApplicationDTOs = applicationAdapter.convert(applications);
    assertApplications(actualApplicationDTOs, expectedApplicationDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummaries() {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();

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

      ContactDTO expectedContact = createExpectedContact(contactName, displayName, InternalRealm.DISPLAY_NAME, email);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, expectedContact));

      createMember(contactName, firstName, lastName, email);
    }

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
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

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
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

    UserDirectory.QueryResult result = new UserDirectory.QueryResult(members, new Exception(TEST_MESSAGE));
    UserDirectory mockUserDirectory = mock(UserDirectory.class);
    when(mockUserDirectory.getUsersByName(memberNames)).thenReturn(result);

    applicationAdapter = new ApplicationAdapter(mockUserDirectory);
    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
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

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
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

      ContactDTO expectedContact = createExpectedContact(contactInternalName, displayName, InternalRealm.DISPLAY_NAME,
          userEmail);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, expectedContact));
    }

    createMember(contactInternalName, userFirstName, userLastName, userEmail);

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummaries_NotAllUsersFound() {

    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
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

      ContactDTO expectedContact;

      // All names will be passed to the user directory, but only half of them will be found.
      if (i % 2 == 0) {
        createMember(contactName, firstName, lastName, email);
        expectedContact = createExpectedContact(contactName, displayName, InternalRealm.DISPLAY_NAME, email);
      }
      else {
        expectedContact = createExpectedContactForNotFoundError(contactName);
      }
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, expectedContact));
    }

    List<ApplicationManagementSummaryDTO> actualDTOs = applicationAdapter
        .createApplicationManagementSummaries(applications);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  private ApplicationManagementSummaryDTO createExpectedApplicationManagementSummaryDTO(String orgId,
                                                                                        String orgName,
                                                                                        String appName,
                                                                                        String appId,
                                                                                        ContactDTO contact)
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

  private Member createMember(String username, String firstName, String lastName, String email) {
    tempEntity.newUser(username, firstName, lastName, email);
    Member member = new Member();
    member.setInternalName(username);
    member.setDisplayName(firstName + " " + lastName);
    member.setEmail(email);
    member.setRealm(InternalRealm.DISPLAY_NAME);
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
}
