/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.v1.ApiApplicationAdapter;
import com.sonatype.insight.brain.api.v1.ApiApplicationResource;
import com.sonatype.insight.brain.api.v1.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiApplicationListDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiApplicationTagDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiRoleDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiRoleListDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.v1.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.api.v1.service.ApiApplicationTagAdapter;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Color;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public abstract class AbstractApiApplicationResourceTest
    extends AbstractResourceTest
{

  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  private final ApiApplicationAdapter apiApplicationAdapter = new ApiApplicationAdapter();

  private final ApiApplicationTagAdapter apiApplicationTagAdapter = new ApiApplicationTagAdapter();

  private final RoleDAO roleDAO = new RoleDAO();

  private Organization organization;

  private Application app;

  private User userA;

  private User userB;

  private Tag tagA;

  private Tag tagB;

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  private HttpRequest roleMembersRequest(String applicationId) {
    return restRequest().subpath(ApiApplicationResource.ROLE_MEMBERS_PATH).parameter(applicationId);
  }

  @Before
  public void setUp() throws Exception {
    organization = tempEntity.newOrganization("test-org");
    app = tempEntity.newApplication("test-app", "test-app", organization.getId());
    userA = tempEntity.newUser("user-a", "John", "Doe", "void@void.com");
    userB = tempEntity.newUser("user-b", "Jane", "Doe", "void@void.com");
    tagA = tempEntity.newTag(organization.getId(), "TagA", Color.dark_red);
    tagB = tempEntity.newTag(organization.getId(), "TagB", Color.dark_red);
  }

  @Test
  public void testCRUD() throws Exception {
    ApiApplicationDTO applicationDTO = createApplicationDTO(null);

    // Test the post
    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);

    // Test the get
    response = restRequest().path(applicationResult.id).get();
    assertResponseStatus(200, response);
    applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);

    // Test the update
    applicationDTO = applicationResult;
    applicationDTO.contactUserName = userB.getUsername();
    ApiApplicationTagDTO applicationTagBDTO = new ApiApplicationTagDTO();
    applicationTagBDTO.tagId = tagB.getId();
    applicationDTO.applicationTags.clear();
    applicationDTO.applicationTags.add(applicationTagBDTO);
    response = restRequest().path(applicationResult.id).body(applicationDTO).put();
    assertResponseStatus(200, response);
    applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);

    // Test the delete
    response = restRequest().path(applicationResult.id).delete();
    assertResponseStatus(204, response);

    final Application application = applicationDAO.getById(applicationResult.id);
    assertThat(application, nullValue());
  }

  @Test
  public void testGetApplications() throws Exception {
    int numApps = 2;
    tempEntity.newApplications(organization.getId(), numApps);

    HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    ApiApplicationListDTO applicationListDTO = response.getBody(ApiApplicationListDTO.class);
    assertThat(applicationListDTO, notNullValue());
    assertThat(applicationListDTO.applications, hasSize(numApps + 1));
  }

  @Test
  public void testGetApplications_ByPublicId() throws Exception {
    int numApps = 2;
    List<Application> applications = tempEntity.newApplications(organization.getId(), numApps);
    Map<String, List<ApplicationTag>> appTagMap = new HashMap<>();
    for (Application application : applications) {
      List<ApplicationTag> applicationTags = new ArrayList<>();
      applicationTags.add(tempEntity.newApplicationTag(application.getId(), tagA.getId()));
      appTagMap.put(application.getId(), applicationTags);
    }

    HttpResponse response = restRequest().query("publicId", applications.get(0).getPublicId(),
        applications.get(1).getPublicId()).get();
    assertResponseStatus(200, response);
    ApiApplicationListDTO applicationListDTO = response.getBody(ApiApplicationListDTO.class);
    assertThat(applicationListDTO, notNullValue());
    List<ApiApplicationDTO> expectedApplications = new ArrayList<>(numApps);
    for (Application application : applications) {
      ApiApplicationDTO apiApplicationDTO = apiApplicationAdapter.convertToDTO(application);
      apiApplicationDTO.applicationTags = apiApplicationTagAdapter.convertToDTO(appTagMap.get(apiApplicationDTO.id));
      expectedApplications.add(apiApplicationDTO);
    }
    assertApplications(applicationListDTO.applications, expectedApplications);
  }

  @Test
  public void testUpdateApplication_MismatchedIds() throws Exception {
    ApiApplicationDTO applicationDTO = createApplicationDTO("Junk");
    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    assertResponseStatus(400, response);
    String errorMessage = response.getBodyText();
    assertThat(errorMessage, is("The applicationId=" + app.getId() + " provided in the url did not match the id="
        + applicationDTO.id + " provided in the json."));
  }

  @Test
  public void testUpdateApplication_NullId() throws Exception {
    ApiApplicationDTO applicationDTO = apiApplicationAdapter.convertToDTO(app);
    applicationDTO.id = null;
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);
  }

  @Test
  public void testUpdateApplication_EmptyId() throws Exception {
    ApiApplicationDTO applicationDTO = apiApplicationAdapter.convertToDTO(app);
    applicationDTO.id = "  ";
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);
  }

  @Test
  public void testUpdateApplication_ChangeOrganizationId() throws Exception {
    ApiApplicationDTO applicationDTO = apiApplicationAdapter.convertToDTO(app);
    applicationDTO.id = null;
    Organization anotherOrg = tempEntity.newOrganization("Another Org");
    applicationDTO.organizationId = anotherOrg.getId();
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    assertResponseStatus(400, response);
    String errorMessage = response.getBodyText();
    assertThat(errorMessage, is("Cannot change the parent organization of an application."));
  }

  @Test
  public void testUpdateApplication_ChangePublicId() throws Exception {
    ApiApplicationDTO applicationDTO = apiApplicationAdapter.convertToDTO(app);
    applicationDTO.id = null;
    applicationDTO.publicId = "NewPublicId";
    addApplicationTagDTOs(applicationDTO);

    // Test the update
    HttpResponse response = restRequest().path(app.getId()).body(applicationDTO).put();
    assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = response.getBody(ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);
  }

  @Test
  public void testDeleteNonExistentApplication() throws Exception {
    final String appId = "invalidAppId";
    final HttpResponse response = restRequest().path(appId).delete();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText(), equalTo("Could not find an application with ID " + appId + "."));
  }

  @Test
  public void testGetNotExistentApplication() throws Exception {
    final HttpResponse response = restRequest().path("invalidId").get();
    assertResponseStatus(404, response);
    assertThat(response.getBodyText(), equalTo("Could not find an application with ID invalidId."));
  }

  @Test
  public void testAddApplicationConflictingPublicId() throws Exception {
    final String applicationName = "test-application-name";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = app.getPublicId();
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = app.getOrganizationId();

    final HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), equalTo(app.getPublicId() + " is already used as an ID."));
  }

  @Test
  public void testAddApplicationNonNullIdFails() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.id = "BadIdAsWeAreCallingAddApplication";
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = organization.getId();
    applicationDTO.contactUserName = userA.getUsername();

    // Test the post
    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), equalTo("Application must not have an ID set on creation."));
  }

  @Test
  public void testAddApplicationInvalidContact() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";
    final String contactUserName = "testContact";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = organization.getId();
    applicationDTO.contactUserName = contactUserName;

    // Test the post
    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), equalTo("Application has a contactUserName=" + contactUserName
        + " that does not exist."));
  }

  @Test
  public void testAddApplicationExceedsLicense() throws Exception {
    final int appLimit = 1;
    setApplicationLimit(appLimit);

    // Test Add Application, which should fail with 402 since we exceeded the limit
    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.name = "testAddApplication_exceedsLicense_id_new_name";
    applicationDTO.publicId = "testAddApplication_exceedsLicense_id_new_id";
    applicationDTO.organizationId = organization.getId();

    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(402, response);
    assertThat(response.getBodyText(),
        equalTo("You have exceeded the licensed limit of " + appLimit + " applications."));
  }

  @Test
  public void testAddApplicationInvalidOrg() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";
    final String orgId = "invalidOrgId";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = orgId;

    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), equalTo("Application references an organization (ID=" + orgId
        + ") that does not exist."));
  }

  @Test
  public void testAddApplicationNullOrg() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = null;

    HttpResponse response = restRequest().body(applicationDTO).post();
    assertResponseStatus(400, response);
    assertThat(response.getBodyText(), equalTo("Application must have a parent organization."));
  }

  @Test
  public void testLdapAppRoles() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/AbstractApiApplicationResourceTest/ldap_users.ldif");

    final LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Initial state
    HttpRequest request = roleMembersRequest(app.getId());
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    final List<Role> appRoles = roleDAO.getApplicationRoles();

    ApiRoleMemberMappingListDTO roleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);
    assertThat(roleMemberMappings, is(notNullValue()));
    assertThat(roleMemberMappings.memberMappings, hasSize(appRoles.size()));

    // Create
    final ApiRoleMemberMappingListDTO roleMemberMappingListDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, User.ADMIN_USERNAME), newMember(MemberType.USER, "testuser"),
            newMember(MemberType.GROUP, "Alpha")), appRoles.get(0).getId());

    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    // Read for created data
    response = request.get();
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO returnedRoleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);

    assertThat(returnedRoleMemberMappings, is(notNullValue()));
    final List<ApiRoleMemberMappingDTO> returnedRoleMemberMappingList = returnedRoleMemberMappings.memberMappings;
    assertThat(returnedRoleMemberMappingList, is(notNullValue()));
    assertThat(returnedRoleMemberMappingList, hasSize(appRoles.size()));

    for (final ApiRoleMemberMappingDTO roleMember : returnedRoleMemberMappingList) {
      if (roleMember.roleId.equals(appRoles.get(0).getId())) {
        assertThat(roleMember.members, hasSize(3));
        final Map<String, MemberType> memberMap = new HashMap<>();
        for (final ApiMemberDTO member : roleMember.members) {
          memberMap.put(member.userOrGroupName, member.type);
        }
        MemberType type = memberMap.get("Alpha");
        assertThat(type, is(MemberType.GROUP));
        type = memberMap.get("testuser");
        assertThat(type, is(MemberType.USER));
        type = memberMap.get(User.ADMIN_USERNAME);
        assertThat(type, is(MemberType.USER));
      }
      else {
        assertThat(roleMember.members, hasSize(0));
      }
    }
  }

  @Test
  public void testCRUDAppRoles() throws Exception {
    // Initial state
    HttpRequest request = roleMembersRequest(app.getId());
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    final List<Role> appRoles = roleDAO.getApplicationRoles();

    ApiRoleMemberMappingListDTO roleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);
    assertThat(roleMemberMappings, is(notNullValue()));
    assertThat(roleMemberMappings.memberMappings, hasSize(appRoles.size()));

    // Create
    ApiRoleMemberMappingListDTO roleMemberMappingListDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, userB.getUsername())), appRoles.get(0).getId());
    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    // Read for created data
    response = request.get();
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO returnedRoleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);

    assertThat(returnedRoleMemberMappings, is(notNullValue()));
    List<ApiRoleMemberMappingDTO> returnedRoleMemberMappingList = returnedRoleMemberMappings.memberMappings;
    assertThat(returnedRoleMemberMappingList, is(notNullValue()));
    assertThat(returnedRoleMemberMappingList, hasSize(appRoles.size()));

    ApiRoleMemberMappingDTO returnedRoleMemberMapping = null;
    for (final ApiRoleMemberMappingDTO roleMemberMapping : returnedRoleMemberMappingList) {
      if (appRoles.get(0).getId().equals(roleMemberMapping.roleId)) {
        returnedRoleMemberMapping = roleMemberMapping;
        break;
      }
    }
    assertApiRoleMemberMappingDTO(returnedRoleMemberMapping, appRoles.get(0).getId(), userB, MemberType.USER);

    // Update
    roleMemberMappingListDTO = newMemberMapping(newMemberList(newMember(MemberType.USER, userA.getUsername())),
        appRoles.get(0).getId());
    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    roleMemberMappingListDTO = newMemberMapping(newMemberList(newMember(MemberType.USER, userB.getUsername())),
        appRoles.get(1).getId());
    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    // Read for updated data
    response = request.get();
    assertResponseStatus(200, response);
    returnedRoleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);
    assertThat(returnedRoleMemberMappings, is(notNullValue()));
    returnedRoleMemberMappingList = returnedRoleMemberMappings.memberMappings;
    assertThat(returnedRoleMemberMappingList, is(notNullValue()));
    assertThat(returnedRoleMemberMappingList, hasSize(appRoles.size()));

    ApiRoleMemberMappingDTO[] returnedRoleMemberMappingArray = new ApiRoleMemberMappingDTO[2];
    for (final ApiRoleMemberMappingDTO roleMemberMapping : returnedRoleMemberMappingList) {
      if (appRoles.get(0).getId().equals(roleMemberMapping.roleId)) {
        returnedRoleMemberMappingArray[0] = roleMemberMapping;
      }
      else if (appRoles.get(1).getId().equals(roleMemberMapping.roleId)) {
        returnedRoleMemberMappingArray[1] = roleMemberMapping;
      }
    }
    assertThat(returnedRoleMemberMappingArray, arrayWithSize(2));
    assertApiRoleMemberMappingDTO(returnedRoleMemberMappingArray[0], appRoles.get(0).getId(), userA, MemberType.USER);
    assertApiRoleMemberMappingDTO(returnedRoleMemberMappingArray[1], appRoles.get(1).getId(), userB, MemberType.USER);
  }

  @Test
  public void testGetApplicationRoles() throws Exception {
    HttpResponse response = restRequest().path(ApiApplicationResource.ROLE_PATH).get();
    assertResponseStatus(200, response);

    ApiRoleListDTO appRoles = response.getBody(ApiRoleListDTO.class);
    assertThat(appRoles, notNullValue());
    assertThat(appRoles.roles, hasSize(4));

    Set<String> roleNames = new HashSet<>();
    for (ApiRoleDTO appRole : appRoles.roles) {
      roleNames.add(appRole.name);
    }
    assertThat(roleNames, hasItems("Owner", "Developer", "Application Evaluator", "Component Evaluator"));
  }

  private ApiRoleMemberMappingListDTO newMemberMapping(final List<ApiMemberDTO> memberList, final String roleId) {
    final ApiRoleMemberMappingDTO memberMappingDTO = new ApiRoleMemberMappingDTO();
    memberMappingDTO.members = memberList;
    memberMappingDTO.roleId = roleId;

    ApiRoleMemberMappingListDTO memberMappingListDTO = new ApiRoleMemberMappingListDTO();
    memberMappingListDTO.memberMappings = new ArrayList<>();
    memberMappingListDTO.memberMappings.add(memberMappingDTO);
    return memberMappingListDTO;
  }

  private ApiMemberDTO newMember(final MemberType type, final String name) {
    return new ApiMemberDTO(name, type);
  }

  private List<ApiMemberDTO> newMemberList(final ApiMemberDTO... members) {
    return Arrays.asList(members);
  }

  private void assertApplication(final ApiApplicationDTO returnedDTO, final ApiApplicationDTO sendDTO) {
    assertThat(returnedDTO.publicId, equalTo(sendDTO.publicId));
    assertThat(returnedDTO.name, equalTo(sendDTO.name));
    assertThat(returnedDTO.organizationId, equalTo(sendDTO.organizationId));
    assertThat(returnedDTO.contactUserName, equalTo(sendDTO.contactUserName));

    if (returnedDTO.applicationTags == null) {
      assertThat(sendDTO.applicationTags, nullValue());
    }
    else {
      assertThat(returnedDTO.applicationTags.size(), is(sendDTO.applicationTags.size()));
      assertThat(returnedDTO.applicationTags.size(), is(1));
      assertThat(returnedDTO.applicationTags.get(0).tagId, is(sendDTO.applicationTags.get(0).tagId));
      assertThat(returnedDTO.applicationTags.get(0).applicationId, is(returnedDTO.id));
    }
  }

  private void assertApiRoleMemberMappingDTO(final ApiRoleMemberMappingDTO apiRoleMemberMappingDTO,
                                             final String roleId,
                                             final User user,
                                             final MemberType type)
  {
    assertThat(apiRoleMemberMappingDTO, notNullValue());
    assertThat(apiRoleMemberMappingDTO.roleId, is(roleId));
    assertThat(apiRoleMemberMappingDTO.members, hasSize(1));
    assertThat(apiRoleMemberMappingDTO.members.get(0).type, is(type));
    assertThat(apiRoleMemberMappingDTO.members.get(0).userOrGroupName, is(user.getUsername()));
  }

  private ApiApplicationDTO createApplicationDTO(String applicationId) {
    ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.id = applicationId;
    applicationDTO.publicId = "testID";
    applicationDTO.name = "test-application-name";
    applicationDTO.organizationId = organization.getId();
    applicationDTO.contactUserName = userA.getUsername();
    addApplicationTagDTOs(applicationDTO);
    return applicationDTO;
  }

  private void addApplicationTagDTOs(ApiApplicationDTO applicationDTO) {
    applicationDTO.applicationTags = new ArrayList<>();
    ApiApplicationTagDTO applicationTagADTO = new ApiApplicationTagDTO();
    applicationTagADTO.tagId = tagA.getId();
    applicationDTO.applicationTags.add(applicationTagADTO);
  }

  private void assertApplications(List<ApiApplicationDTO> actualApplications,
                                  List<ApiApplicationDTO> expectedApplications)
  {
    assertThat(actualApplications.size(), is(expectedApplications.size()));

    Collections.sort(actualApplications, new ApiApplicationDTOComparator());
    Collections.sort(expectedApplications, new ApiApplicationDTOComparator());

    for (int i = 0; i < actualApplications.size(); i++) {
      ApiApplicationDTO actualApplication = actualApplications.get(i);
      ApiApplicationDTO expectedApplication = expectedApplications.get(i);
      assertThat(actualApplication.id, is(expectedApplication.id));
      assertThat(actualApplication.name, is(expectedApplication.name));
      assertThat(actualApplication.organizationId, is(expectedApplication.organizationId));
      assertThat(actualApplication.publicId, is(expectedApplication.publicId));
      assertThat(actualApplication.contactUserName, is(expectedApplication.contactUserName));

      assertTags(actualApplication.applicationTags, expectedApplication.applicationTags);
    }
  }

  private void assertTags(List<ApiApplicationTagDTO> actualTags, List<ApiApplicationTagDTO> expectedTags) {
    if (actualTags == null) {
      assertThat(expectedTags, nullValue());
      return;
    }

    assertThat(actualTags.size(), is(expectedTags.size()));

    Collections.sort(actualTags, new ApiApplicationTagDTOComparator());
    Collections.sort(expectedTags, new ApiApplicationTagDTOComparator());

    for (int i = 0; i < actualTags.size(); i++) {
      assertThat(actualTags.get(i).id, is(expectedTags.get(i).id));
      assertThat(actualTags.get(i).tagId, is(expectedTags.get(i).tagId));
      assertThat(actualTags.get(i).applicationId, is(expectedTags.get(i).applicationId));
    }
  }

  private static class ApiApplicationDTOComparator
      implements Comparator<ApiApplicationDTO>
  {
    @Override
    public int compare(final ApiApplicationDTO o1, final ApiApplicationDTO o2) {
      return o1.id.compareTo(o2.id);
    }
  }

  private static class ApiApplicationTagDTOComparator
      implements Comparator<ApiApplicationTagDTO>
  {
    @Override
    public int compare(final ApiApplicationTagDTO o1, final ApiApplicationTagDTO o2) {
      return o1.id.compareTo(o2.id);
    }
  }
}
