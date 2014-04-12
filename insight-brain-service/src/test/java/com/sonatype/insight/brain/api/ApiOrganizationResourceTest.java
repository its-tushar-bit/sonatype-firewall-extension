/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.AuthedRestAccess;
import com.sonatype.insight.brain.api.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.ldap.TestLdapServer;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import com.ning.http.client.Response;
import com.yammer.dropwizard.testing.JsonHelpers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class ApiOrganizationResourceTest
    extends AbstractResourceTest
{
  private RoleDAO roleDAO = new RoleDAO();

  private UserDAO userDAO = new UserDAO();

  private Organization organization;

  private User userA;

  private User userB;

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  @Before
  public void setUp() throws Exception {
    organization = tempEntity.newOrganization("test-org");
    userA = new User("user-a", "secret", "John", "Doe", "void@void.com");
    userDAO.insert(userA);
    userB = new User("user-b", "secret", "Jane", "Doe", "void@void.com");
    userDAO.insert(userB);
  }

  @After
  public void tearDown() throws Exception {
    if (userA != null) {
      userDAO.delete(userA);
    }
    if (userB != null) {
      userDAO.delete(userB);
    }
  }

  @Test
  public void testGetAll() throws Exception {
    final Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(200, response);
    final Organization[] organizations = JsonHelpers.fromJson(response.getResponseBody(), Organization[].class);
    assertThat(organizations, notNullValue());
    assertThat(organizations, arrayWithSize(1));

    final Organization organization = organizations[0];
    assertThat(organization, notNullValue());
    assertThat(organization.getId(), equalTo(organization.getId()));
    assertThat(organization.getName(), equalTo(organization.getName()));
  }

  @Test
  public void testGetAll_Unlicensed() throws Exception {
    uninstallLicense();
    final Response response = AuthedRestAccess.get(getServiceURL());
    assertResponseStatus(402, response);
  }

  @Test
  public void testLdapOrgRoles() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/UserResourceTest/ldap_users.ldif");

    final LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Initial state
    final String url = getGetRoleMembersUrl(organization.getId());
    Response response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);

    // Note: Org roles are same as app roles in DTO
    final List<Role> orgRoles = roleDAO.getApplicationRoles();
    assertThat(orgRoles, hasSize(2));

    ApiRoleMemberMappingListDTO roleMemberMappings = JsonHelpers.fromJson(response.getResponseBody(),
        ApiRoleMemberMappingListDTO.class);
    assertThat(roleMemberMappings, is(notNullValue()));
    assertThat(roleMemberMappings.getMemberMappings(), hasSize(orgRoles.size()));

    // Create
    final ApiRoleMemberMappingDTO roleMemberMappingDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, User.ADMIN_USERNAME), newMember(MemberType.USER, "testuser"),
            newMember(MemberType.GROUP, "Alpha")),
        orgRoles.get(0).getId()
    );

    response = AuthedRestAccess.put(url, JsonHelpers.asJson(roleMemberMappingDTO));
    assertResponseStatus(204, response);

    // Read for created data
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO returnedRoleMemberMappings = JsonHelpers.fromJson(response.getResponseBody(),
        ApiRoleMemberMappingListDTO.class);

    assertThat(returnedRoleMemberMappings, is(notNullValue()));
    final List<ApiRoleMemberMappingDTO> returnedRoleMemberMappingList = returnedRoleMemberMappings.getMemberMappings();
    assertThat(returnedRoleMemberMappingList, is(notNullValue()));
    assertThat(returnedRoleMemberMappingList, hasSize(orgRoles.size()));

    for (final ApiRoleMemberMappingDTO roleMember : returnedRoleMemberMappingList) {
      if (roleMember.getRoleId().equals(orgRoles.get(0).getId())) {
        assertThat(roleMember.getMembers(), hasSize(3));
        final Map<String, MemberType> memberMap = new HashMap<>();
        for (final ApiMemberDTO member : roleMember.getMembers()) {
          memberMap.put(member.getUserOrGroupName(), member.getType());
        }
        MemberType type = memberMap.get("Alpha");
        assertThat(type, is(MemberType.GROUP));
        type = memberMap.get("testuser");
        assertThat(type, is(MemberType.USER));
        type = memberMap.get(User.ADMIN_USERNAME);
        assertThat(type, is(MemberType.USER));
      }
      else {
        assertThat(roleMember.getMembers(), hasSize(0));
      }
    }
  }

  @Test
  public void testCRUDOrgRoles() throws Exception {
    // Initial state
    final String url = getGetRoleMembersUrl(organization.getId());
    Response response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);

    // Note: Org roles are same as app roles in DTO
    final List<Role> orgRoles = roleDAO.getApplicationRoles();
    assertThat(orgRoles, hasSize(2));

    ApiRoleMemberMappingListDTO roleMemberMappings = JsonHelpers.fromJson(response.getResponseBody(),
        ApiRoleMemberMappingListDTO.class);
    assertThat(roleMemberMappings, is(notNullValue()));
    assertThat(roleMemberMappings.getMemberMappings(), hasSize(orgRoles.size()));

    // Create
    ApiRoleMemberMappingDTO roleMemberMappingDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, userB.getUsername())),
        orgRoles.get(0).getId());
    response = AuthedRestAccess.put(url, JsonHelpers.asJson(roleMemberMappingDTO));
    assertResponseStatus(204, response);

    // Read for created data
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO returnedRoleMemberMappings = JsonHelpers.fromJson(response.getResponseBody(),
        ApiRoleMemberMappingListDTO.class);

    assertThat(returnedRoleMemberMappings, is(notNullValue()));
    List<ApiRoleMemberMappingDTO> returnedRoleMemberMappingList = returnedRoleMemberMappings.getMemberMappings();
    assertThat(returnedRoleMemberMappingList, is(notNullValue()));
    assertThat(returnedRoleMemberMappingList, hasSize(orgRoles.size()));

    ApiRoleMemberMappingDTO returnedRoleMemberMapping = null;
    for (final ApiRoleMemberMappingDTO roleMemberMapping : returnedRoleMemberMappingList) {
      if (orgRoles.get(0).getId().equals(roleMemberMapping.getRoleId())) {
        returnedRoleMemberMapping = roleMemberMapping;
        break;
      }
    }
    assertApiRoleMemberMappingDTO(returnedRoleMemberMapping, orgRoles.get(0).getId(), userB, MemberType.USER);

    // Update
    roleMemberMappingDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, userA.getUsername())),
        orgRoles.get(0).getId());
    response = AuthedRestAccess.put(url, JsonHelpers.asJson(roleMemberMappingDTO));
    assertResponseStatus(204, response);

    roleMemberMappingDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, userB.getUsername())),
        orgRoles.get(1).getId());
    response = AuthedRestAccess.put(url, JsonHelpers.asJson(roleMemberMappingDTO));
    assertResponseStatus(204, response);

    // Read for updated data
    response = AuthedRestAccess.get(url);
    assertResponseStatus(200, response);
    returnedRoleMemberMappings = JsonHelpers.fromJson(response.getResponseBody(), ApiRoleMemberMappingListDTO.class);
    assertThat(returnedRoleMemberMappings, is(notNullValue()));
    returnedRoleMemberMappingList = returnedRoleMemberMappings.getMemberMappings();
    assertThat(returnedRoleMemberMappingList, is(notNullValue()));
    assertThat(returnedRoleMemberMappingList, hasSize(orgRoles.size()));

    ApiRoleMemberMappingDTO[] returnedRoleMemberMappingArray = new ApiRoleMemberMappingDTO[2];
    for (final ApiRoleMemberMappingDTO roleMemberMapping : returnedRoleMemberMappingList) {
      if (orgRoles.get(0).getId().equals(roleMemberMapping.getRoleId())) {
        returnedRoleMemberMappingArray[0] = roleMemberMapping;
      }
      else if (orgRoles.get(1).getId().equals(roleMemberMapping.getRoleId())) {
        returnedRoleMemberMappingArray[1] = roleMemberMapping;
      }
    }
    assertThat(returnedRoleMemberMappingArray, arrayWithSize(2));
    assertApiRoleMemberMappingDTO(returnedRoleMemberMappingArray[0], orgRoles.get(0).getId(), userA, MemberType.USER);
    assertApiRoleMemberMappingDTO(returnedRoleMemberMappingArray[1], orgRoles.get(1).getId(), userB, MemberType.USER);
  }

  private ApiRoleMemberMappingDTO newMemberMapping(final List<ApiMemberDTO> memberList, final String roleId) {
    final ApiRoleMemberMappingDTO memberMappingDTO = new ApiRoleMemberMappingDTO();
    memberMappingDTO.setMembers(memberList);
    memberMappingDTO.setRoleId(roleId);
    return memberMappingDTO;
  }

  private ApiMemberDTO newMember(final MemberType type, final String name) {
    return new ApiMemberDTO(name, type);
  }

  private List<ApiMemberDTO> newMemberList(final ApiMemberDTO... members) {
    return Arrays.asList(members);
  }

  private void assertApiRoleMemberMappingDTO(final ApiRoleMemberMappingDTO apiRoleMemberMappingDTO,
      final String roleId, final User user, final MemberType type)
  {
    assertThat(apiRoleMemberMappingDTO, notNullValue());
    assertThat(apiRoleMemberMappingDTO.getRoleId(), is(roleId));
    final List<ApiMemberDTO> returnedMembers = apiRoleMemberMappingDTO.getMembers();
    assertThat(returnedMembers, hasSize(1));
    assertThat(returnedMembers.get(0).getType(), is(type));
    assertThat(returnedMembers.get(0).getUserOrGroupName(), is(user.getUsername()));
  }

  private String getGetRoleMembersUrl(final String organizationId) {
    return getServiceURL() + "/" +
        ApiOrganizationResource.ROLE_MEMBERS_PATH.replace("{organizationId}", organizationId);
  }

  private String getServiceURL() {
    return getRestBaseUrl() + PublicApiPaths.ORG_SERVICE_PATH;
  }
}
