/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.UserDirectory;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

// Silent: setUp() configures default stubs that not every test triggers (e.g. organizationDAO, userDirectory).
@RunWith(MockitoJUnitRunner.Silent.class)
public class ApplicationAdapterTest
{
  private static final String USER_DIRECTORY_ERROR = "User directory query result error.";

  private static final String TEST_MESSAGE = "Test Exception Message";

  @Mock
  private UserDirectory userDirectory;

  @Mock
  private OrganizationDAO organizationDAO;

  private ApplicationAdapter applicationAdapter;

  // Application variables
  private final String applicationId = "AppId";

  private final String applicationName = "MyApplication";

  private final String publicId = "publicId";

  private String organizationId;

  private final String organizationName = "My Organization";

  private final String contactInternalName = "jsmith";

  private final String userFirstName = "John";

  private final String userLastName = "Smith";

  private final String userEmail = "jsmith@sonatype.com";

  @Before
  public void setUp() {
    Organization org = new Organization(organizationName);
    org.setId("org-id-1");
    organizationId = org.getId();

    when(organizationDAO.getByIdNotNull(organizationId)).thenReturn(org);

    // Default: when no users are found, return empty result
    when(userDirectory.getUsersByNames(any())).thenReturn(new UserDirectory.QueryResult(new ArrayList<>()));

    applicationAdapter = new ApplicationAdapter(userDirectory, organizationDAO);
  }

  @Test
  public void testConvert_Application() {
    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    Member member = createMember(contactInternalName, userFirstName, userLastName, userEmail);
    setupUserDirectoryForSingleUser(contactInternalName, member);

    ContactDTO expectedContactDTO = createExpectedContactDTO(contactInternalName, userFirstName + " " + userLastName,
        InternalRealm.DISPLAY_NAME, userEmail);
    ApplicationDTO expectedApplicationDTO = createExpectedApplicationDTO(applicationName, applicationId,
        expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplicationDTO(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvert_Application_WithUpperCaseInternalName() {
    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    // Member has a name that is in all upper case but searching is done in a case-insensitive manner.
    Member member = createMember(
        contactInternalName.toUpperCase(Locale.ENGLISH), userFirstName, userLastName, userEmail);
    setupUserDirectoryForSingleUser(contactInternalName, member);

    ContactDTO expectedContactDTO = createExpectedContactDTO(contactInternalName.toUpperCase(Locale.ENGLISH),
        userFirstName + " " + userLastName, InternalRealm.DISPLAY_NAME, userEmail);
    ApplicationDTO expectedApplicationDTO = createExpectedApplicationDTO(applicationName, applicationId,
        expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplicationDTO(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvert_Application_WithUnfoundUser() {
    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    ContactDTO expectedContact = createExpectedContactDTOForNotFoundError(contactInternalName);
    ApplicationDTO expectedApplicationDTO = createExpectedApplicationDTO(applicationName, applicationId,
        expectedContact);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application);
    assertApplicationDTO(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvert_Application_WithUserDirectoryException() {
    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    UserDirectory.QueryResult result = new UserDirectory.QueryResult(new ArrayList<>(),
        new Exception(TEST_MESSAGE));
    UserDirectory mockUserDirectory = mock(UserDirectory.class);
    when(mockUserDirectory.getUsersByNames(Collections.singleton(contactInternalName))).thenReturn(result);

    ContactDTO expectedContactDTO = createExpectedContactDTOForUserDirectoryError(contactInternalName);
    ApplicationDTO expectedApplicationDTO = createExpectedApplicationDTO(applicationName, applicationId,
        expectedContactDTO);

    ApplicationDTO actualApplicationDTO =
        new ApplicationAdapter(mockUserDirectory, organizationDAO).convert(application);
    assertApplicationDTO(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvert_Application_ExcludeContact() {
    Application application = createApplication(organizationId, applicationName, applicationId, contactInternalName);

    ContactDTO expectedContactDTO = null;
    ApplicationDTO expectedApplicationDTO = createExpectedApplicationDTO(applicationName, applicationId,
        expectedContactDTO);

    ApplicationDTO actualApplicationDTO = applicationAdapter.convert(application, false);
    assertApplicationDTO(actualApplicationDTO, expectedApplicationDTO);
  }

  @Test
  public void testConvert_Applications() {
    List<ApplicationDTO> expectedApplicationDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;
      String contactName = contactInternalName + "-" + i;

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ApplicationDTO expectedApplicationDTO = createExpectedApplicationDTO(appName, appId, null /* ContactDTO */);
      expectedApplicationDTOs.add(expectedApplicationDTO);
    }

    List<ApplicationDTO> actualApplicationDTOs = applicationAdapter.convert(applications);
    assertApplicationDTOs(actualApplicationDTOs, expectedApplicationDTOs);
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

      Application application = createApplication(orgId, appName, appId, contactName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContactDTO(contactName, null, null, null);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, expectedContact));
    }

    List<ApplicationManagementSummaryDTO> actualDTOs =
        applicationAdapter.createApplicationManagementSummaries(applications, null);

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

      // createApplicationManagementSummaries always creates a ContactDTO, even with a null name
      ContactDTO expectedContact = createExpectedContactDTO(null, null, null, null);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, expectedContact));
    }

    List<ApplicationManagementSummaryDTO> actualDTOs =
        applicationAdapter.createApplicationManagementSummaries(applications, null);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  @Test
  public void testCreateApplicationManagementSummaries_WithSameUser() {
    List<ApplicationManagementSummaryDTO> expectedDTOs = new ArrayList<>();
    List<Application> applications = new ArrayList<>();

    for (int i = 1; i <= 5; i++) {
      String orgId = organizationId;
      String orgName = organizationName;
      String appName = applicationName + "-" + i;
      String appId = applicationId + "-" + i;

      Application application = createApplication(orgId, appName, appId, contactInternalName);
      applications.add(application);

      ContactDTO expectedContact = createExpectedContactDTO(contactInternalName, null, null, null);
      expectedDTOs.add(createExpectedApplicationManagementSummaryDTO(orgId, orgName, appName, appId, expectedContact));
    }

    List<ApplicationManagementSummaryDTO> actualDTOs =
        applicationAdapter.createApplicationManagementSummaries(applications, null);

    assertApplicationManagementSummaryDTOs(actualDTOs, expectedDTOs);
  }

  private void setupUserDirectoryForSingleUser(String internalName, Member member) {
    List<Member> members = new ArrayList<>();
    members.add(member);
    UserDirectory.QueryResult result = new UserDirectory.QueryResult(members);
    when(userDirectory.getUsersByNames(Collections.singleton(internalName))).thenReturn(result);
  }

  private ApplicationManagementSummaryDTO createExpectedApplicationManagementSummaryDTO(
      String orgId,
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

  private ApplicationDTO createExpectedApplicationDTO(String appName, String appId, ContactDTO contact) {
    ApplicationDTO expectedApplicationDTO = new ApplicationDTO();
    expectedApplicationDTO.setPublicId(publicId);
    expectedApplicationDTO.setOrganizationName(organizationName);
    expectedApplicationDTO.setOrganizationId(organizationId);
    expectedApplicationDTO.setId(appId);
    expectedApplicationDTO.setName(appName);
    expectedApplicationDTO.setContact(contact);

    return expectedApplicationDTO;
  }

  private ContactDTO createExpectedContactDTOForNotFoundError(String internalName) {
    ContactDTO expectedContact = createExpectedContactDTO(internalName, null, null, null);
    expectedContact.setError("The username " + internalName + " no longer exists.");

    return expectedContact;
  }

  private ContactDTO createExpectedContactDTOForUserDirectoryError(String internalName) {
    ContactDTO expectedContact = createExpectedContactDTO(internalName, null, null, null);
    expectedContact.setError(USER_DIRECTORY_ERROR);

    return expectedContact;
  }

  private ContactDTO createExpectedContactDTO(String internalName, String displayName, String realm, String email) {
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
    Member member = new Member();
    member.setInternalName(username);
    member.setDisplayName(firstName + " " + lastName);
    member.setEmail(email);
    member.setRealm(InternalRealm.DISPLAY_NAME);
    member.setType(MemberType.USER);

    return member;
  }

  private void assertApplicationDTOs(
      List<ApplicationDTO> actualApplicationDTOs,
      List<ApplicationDTO> expectedApplicationDTOs)
  {
    assertThat(actualApplicationDTOs).hasSameSizeAs(expectedApplicationDTOs);
    for (int i = 0; i < actualApplicationDTOs.size(); i++) {
      assertApplicationDTO(actualApplicationDTOs.get(i), expectedApplicationDTOs.get(i));
    }
  }

  private void assertApplicationDTO(ApplicationDTO actualApplicationDTO, ApplicationDTO expectedApplicationDTO) {
    assertThat(actualApplicationDTO).isNotNull();
    assertThat(expectedApplicationDTO).isNotNull();
    assertThat(actualApplicationDTO.getId()).isEqualTo(expectedApplicationDTO.getId());
    assertThat(actualApplicationDTO.getName()).isEqualTo(expectedApplicationDTO.getName());
    assertThat(actualApplicationDTO.getOrganizationId()).isEqualTo(expectedApplicationDTO.getOrganizationId());
    assertThat(actualApplicationDTO.getOrganizationName()).isEqualTo(expectedApplicationDTO.getOrganizationName());
    assertThat(actualApplicationDTO.getPublicId()).isEqualTo(expectedApplicationDTO.getPublicId());

    assertContactDTO(actualApplicationDTO.getContact(), expectedApplicationDTO.getContact());
  }

  private void assertContactDTO(ContactDTO actualContact, ContactDTO expectedContact) {
    if (actualContact == null || expectedContact == null) {
      assertThat(actualContact).isEqualTo(expectedContact);
      return;
    }

    assertThat(actualContact.getInternalName()).isEqualTo(expectedContact.getInternalName());
    assertThat(actualContact.getDisplayName()).isEqualTo(expectedContact.getDisplayName());
    assertThat(actualContact.getEmail()).isEqualTo(expectedContact.getEmail());
    assertThat(actualContact.getRealm()).isEqualTo(expectedContact.getRealm());
    assertThat(actualContact.getError()).isEqualTo(expectedContact.getError());
  }

  private void assertApplicationManagementSummaryDTOs(
      List<ApplicationManagementSummaryDTO> actualList,
      List<ApplicationManagementSummaryDTO> expectedList)
  {
    assertThat(actualList).hasSameSizeAs(expectedList);

    for (int i = 0; i < actualList.size(); i++) {
      ApplicationManagementSummaryDTO actual = actualList.get(i);
      ApplicationManagementSummaryDTO expected = expectedList.get(i);

      assertApplicationManagementSummaryDTO(actual, expected);
    }
  }

  private void assertApplicationManagementSummaryDTO(
      ApplicationManagementSummaryDTO actual,
      ApplicationManagementSummaryDTO expected)
  {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getOrganizationId()).isEqualTo(expected.getOrganizationId());
    assertThat(actual.getOrganizationName()).isEqualTo(expected.getOrganizationName());
    assertThat(actual.getPublicId()).isEqualTo(expected.getPublicId());

    assertContactDTO(actual.getContact(), expected.getContact());
  }
}
