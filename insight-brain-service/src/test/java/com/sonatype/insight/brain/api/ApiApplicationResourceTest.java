/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.api.dto.ApiApplicationDTO;
import com.sonatype.insight.brain.api.dto.ApiApplicationTagDTO;
import com.sonatype.insight.brain.api.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ApiApplicationResourceTest
    extends AbstractResourceTest
{

  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  private RoleDAO roleDAO = new RoleDAO();

  private Organization organization;

  private Application app;

  private User userA;

  private User userB;

  private Tag tag;

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  @Before
  public void setUp() throws Exception {
    organization = tempEntity.newOrganization("test-org");
    app = tempEntity.newApplication("test-app", "test-app", organization.getId());
    userA = tempEntity.newUser("user-a", "John", "Doe", "void@void.com");
    userB = tempEntity.newUser("user-b", "Jane", "Doe", "void@void.com");
    tag = tempEntity.newTag(organization.getId());
  }

  @Test
  public void testCRUD() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = organization.getId();
    applicationDTO.contactUserName = userA.getUsername();
    applicationDTO.applicationTags = new ArrayList<>();
    ApiApplicationTagDTO applicationTagDTO = new ApiApplicationTagDTO();
    applicationTagDTO.tagId = tag.getId();
    applicationDTO.applicationTags.add(applicationTagDTO);

    // Test the post
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(applicationDTO));
    assertResponseStatus(200, response);
    ApiApplicationDTO applicationResult = JsonHelpers.fromJson(response.getResponseBody(), ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);

    // Test the get
    response = AuthedRestAccess.get(getServiceURL() + "/" + applicationResult.id);
    assertResponseStatus(200, response);
    applicationResult = JsonHelpers.fromJson(response.getResponseBody(), ApiApplicationDTO.class);
    assertApplication(applicationResult, applicationDTO);

    // Test the delete
    response = AuthedRestAccess.delete(getServiceURL() + "/" + applicationResult.id);
    assertResponseStatus(204, response);

    final Application application = applicationDAO.getById(applicationResult.id);
    assertThat(application, nullValue());
  }

  @Test
  public void testDeleteNonExistentApplication() throws Exception {
    final String appId = "invalidAppId";
    final Response response = AuthedRestAccess.delete(getServiceURL() + "/" + appId);
    assertResponseStatus(404, response);
    assertThat(response.getResponseBody(), equalTo("Could not find an application with id " + appId + "."));
  }

  @Test
  public void testGetNotExistentApplication() throws Exception {
    final Response response = AuthedRestAccess.get(getServiceURL() + "/" + "invalidId");
    assertResponseStatus(404, response);
    assertThat(response.getResponseBody(), equalTo("Could not find an application with id invalidId."));
  }

  @Test
  public void testAddApplicationConflictingPublicId() throws Exception {
    final String applicationName = "test-application-name";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = app.getPublicId();
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = app.getOrganizationId();

    final Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(applicationDTO));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), equalTo(app.getPublicId() + " is already used as an ID."));
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
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(applicationDTO));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), equalTo("Application must not have an id set on creation."));
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
    Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(applicationDTO));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(),
        equalTo("Application has a contactUserName=" + contactUserName + " that does not exist."));
  }

  @Test
  public void testAddApplicationExceedsLicense() throws Exception {
    final int appLimit = 1;
    setApplicationLimit(appLimit);

    // Test Add Application, which should fail with 402 since we exceeded the limit
    final Application application = new Application();
    application.setName("testAddApplication_exceedsLicense_id_new_name");
    application.setPublicId("testAddApplication_exceedsLicense_id_new_id");

    final Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(application));
    assertResponseStatus(402, response);
    assertThat(response.getResponseBody(),
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

    final Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(applicationDTO));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(),
        equalTo("Application references an organization (id=" + orgId + ") that does not exist."));
  }

  @Test
  public void testAddApplicationNullOrg() throws Exception {
    final String applicationPublicId = "testID";
    final String applicationName = "test-application-name";

    final ApiApplicationDTO applicationDTO = new ApiApplicationDTO();
    applicationDTO.publicId = applicationPublicId;
    applicationDTO.name = applicationName;
    applicationDTO.organizationId = null;

    final Response response = AuthedRestAccess.post(getServiceURL(), JsonHelpers.asJson(applicationDTO));
    assertResponseStatus(400, response);
    assertThat(response.getResponseBody(), equalTo("Application must have a parent organization."));
  }

  @Test
  public void testLdapAppRoles() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserResourceTest/ldap_users.ldif");

    final LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Initial state
    final String url = getGetRoleMembersUrl(app.getId());
    Response response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);

    final List<Role> appRoles = roleDAO.getApplicationRoles();
    assertThat(appRoles, hasSize(2));

    ApiRoleMemberMappingListDTO roleMemberMappings = JsonHelpers.fromJson(response.getResponseBody(),
        ApiRoleMemberMappingListDTO.class);
    assertThat(roleMemberMappings, is(notNullValue()));
    assertThat(roleMemberMappings.memberMappings, hasSize(appRoles.size()));

    // Create
    final ApiRoleMemberMappingListDTO roleMemberMappingListDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, User.ADMIN_USERNAME), newMember(MemberType.USER, "testuser"),
            newMember(MemberType.GROUP, "Alpha")),
        appRoles.get(0).getId()
    );

    response = AuthedRestAccess.put(url, JsonHelpers.asJson(roleMemberMappingListDTO));
    assertResponseStatus(204, response);

    // Read for created data
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO returnedRoleMemberMappings = JsonHelpers.fromJson(response.getResponseBody(),
        ApiRoleMemberMappingListDTO.class);

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
    final String url = getGetRoleMembersUrl(app.getId());
    Response response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);

    final List<Role> appRoles = roleDAO.getApplicationRoles();
    assertThat(appRoles, hasSize(2));

    ApiRoleMemberMappingListDTO roleMemberMappings = JsonHelpers.fromJson(response.getResponseBody(),
        ApiRoleMemberMappingListDTO.class);
    assertThat(roleMemberMappings, is(notNullValue()));
    assertThat(roleMemberMappings.memberMappings, hasSize(appRoles.size()));

    // Create
    ApiRoleMemberMappingListDTO roleMemberMappingListDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, userB.getUsername())),
        appRoles.get(0).getId());
    response = AuthedRestAccess.put(url, JsonHelpers.asJson(roleMemberMappingListDTO));
    assertResponseStatus(204, response);

    // Read for created data
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO returnedRoleMemberMappings = JsonHelpers.fromJson(response.getResponseBody(),
        ApiRoleMemberMappingListDTO.class);

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
    roleMemberMappingListDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, userA.getUsername())),
        appRoles.get(0).getId());
    response = AuthedRestAccess.put(url, JsonHelpers.asJson(roleMemberMappingListDTO));
    assertResponseStatus(204, response);

    roleMemberMappingListDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, userB.getUsername())),
        appRoles.get(1).getId());
    response = AuthedRestAccess.put(url, JsonHelpers.asJson(roleMemberMappingListDTO));
    assertResponseStatus(204, response);

    // Read for updated data
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    returnedRoleMemberMappings = JsonHelpers.fromJson(response.getResponseBody(), ApiRoleMemberMappingListDTO.class);
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

    assertThat(returnedDTO.applicationTags.size(), is(sendDTO.applicationTags.size()));
    assertThat(returnedDTO.applicationTags.size(), is(1));
    assertThat(returnedDTO.applicationTags.get(0).tagId, is(sendDTO.applicationTags.get(0).tagId));
    assertThat(returnedDTO.applicationTags.get(0).applicationId, is(returnedDTO.id));
  }

  private String getGetRoleMembersUrl(final String applicationId) {
    return getServiceURL() + "/" + ApiApplicationResource.ROLE_MEMBERS_PATH.replace("{applicationId}", applicationId);
  }

  private String getServiceURL() {
    return getRestBaseUrl() + PublicApiPaths.APP_SERVICE_PATH;
  }

  private void assertApiRoleMemberMappingDTO(final ApiRoleMemberMappingDTO apiRoleMemberMappingDTO,
      final String roleId, final User user, final MemberType type)
  {
    assertThat(apiRoleMemberMappingDTO, notNullValue());
    assertThat(apiRoleMemberMappingDTO.roleId, is(roleId));
    assertThat(apiRoleMemberMappingDTO.members, hasSize(1));
    assertThat(apiRoleMemberMappingDTO.members.get(0).type, is(type));
    assertThat(apiRoleMemberMappingDTO.members.get(0).userOrGroupName, is(user.getUsername()));
  }
}
