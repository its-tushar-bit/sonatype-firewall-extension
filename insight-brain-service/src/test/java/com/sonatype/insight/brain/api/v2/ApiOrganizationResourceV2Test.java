/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiOrganizationListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiTagDTO;
import com.sonatype.insight.brain.configuration.ldap.TestLdapServer;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.security.RoleDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.ldap.LdapServer;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiOrganizationResourceV2Test
    extends AbstractResourceTest
{
  private RoleDAO roleDAO = new RoleDAO();

  private Organization organization;

  private User userA;

  private User userB;

  @Rule
  public TestLdapServer embeddedLdapServer = new TestLdapServer();

  private HttpRequest roleMembersRequest(final String organizationId) {
    return restRequest().subpath(ApiOrganizationResourceV2.ROLE_MEMBERS_PATH).parameter(organizationId);
  }

  @Before
  public void setUp() throws Exception {
    organization = tempEntity.newOrganization("test-org");
    userA = tempEntity.newUser("user-a", "John", "Doe", "void@void.com");
    userB = tempEntity.newUser("user-b", "Jane", "Doe", "void@void.com");
  }

  @Test
  public void testGetAll() throws Exception {
    Tag tag = tempEntity.newTag(organization.getId());

    final HttpResponse response = restRequest().get();
    assertResponseStatus(200, response);
    final ApiOrganizationListDTO organizationListDTO = response.getBody(ApiOrganizationListDTO.class);
    assertThat(organizationListDTO).isNotNull();

    // One that was created for the test and one for the root org
    assertThat(organizationListDTO.organizations).hasSize(2);

    ApiOrganizationDTO retrievedOrg = organizationListDTO.organizations.get(0);
    if (Organization.ROOT_ORGANIZATION_ID.equals(retrievedOrg.id)) {
      retrievedOrg = organizationListDTO.organizations.get(1);
    }
    assertThat(retrievedOrg.id).isEqualTo(organization.getId());
    assertThat(retrievedOrg.name).isEqualTo(organization.getName());

    assertThat(retrievedOrg.tags).hasSize(1);

    ApiTagDTO retrievedTag = retrievedOrg.tags.get(0);
    assertThat(retrievedTag.id).isEqualTo(tag.getId());
    assertThat(retrievedTag.name).isEqualTo(tag.getName());
    assertThat(retrievedTag.description).isEqualTo(tag.getDescription());
    assertThat(retrievedTag.color).isEqualTo(tag.getColor());
  }

  @Test
  public void testGetAll_Unlicensed() throws Exception {
    uninstallLicense();
    final HttpResponse response = restRequest().get();
    assertResponseStatus(402, response);
  }

  @Test
  public void testLdapOrgRoles() throws Exception {
    embeddedLdapServer.start();
    embeddedLdapServer.loadData("/ApiOrganizationResourceV2Test/ldap_users.ldif");

    final LdapServer ldapServer = tempEntity.newLdapServer("LDAP");
    tempEntity.newLdapConnection(ldapServer.getId(), embeddedLdapServer.getPort());
    tempEntity.newLdapUserMapping(ldapServer.getId());

    // Initial state
    HttpRequest request = roleMembersRequest(organization.getId());
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    // Note: Org roles are same as app roles in DTO
    final List<Role> orgRoles = roleDAO.getApplicationRoles();

    ApiRoleMemberMappingListDTO roleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);
    assertThat(roleMemberMappings).isNotNull();
    assertThat(roleMemberMappings.memberMappings).hasSameSizeAs(orgRoles);

    // Create
    final ApiRoleMemberMappingListDTO roleMemberMappingListDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, User.ADMIN_USERNAME), newMember(MemberType.USER, "testuser"),
            newMember(MemberType.GROUP, "Alpha")), orgRoles.get(0).getId());

    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    // Read for created data
    response = request.get();
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO returnedRoleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);

    assertThat(returnedRoleMemberMappings).isNotNull();
    assertThat(returnedRoleMemberMappings.memberMappings).isNotNull();
    assertThat(returnedRoleMemberMappings.memberMappings).hasSameSizeAs(orgRoles);

    for (final ApiRoleMemberMappingDTO roleMember : returnedRoleMemberMappings.memberMappings) {
      if (roleMember.roleId.equals(orgRoles.get(0).getId())) {
        assertThat(roleMember.members).hasSize(3);
        final Map<String, MemberType> memberMap = new HashMap<>();
        for (final ApiMemberDTO member : roleMember.members) {
          memberMap.put(member.userOrGroupName, member.type);
        }
        MemberType type = memberMap.get("Alpha");
        assertThat(type).isEqualTo(MemberType.GROUP);
        type = memberMap.get("testuser");
        assertThat(type).isEqualTo(MemberType.USER);
        type = memberMap.get(User.ADMIN_USERNAME);
        assertThat(type).isEqualTo(MemberType.USER);
      }
      else {
        assertThat(roleMember.members).isEmpty();
      }
    }
  }

  @Test
  public void testCRUDOrgRoles() throws Exception {
    // Initial state
    HttpRequest request = roleMembersRequest(organization.getId());
    HttpResponse response = request.get();
    assertResponseStatus(200, response);

    // Note: Org roles are same as app roles in DTO
    final List<Role> orgRoles = roleDAO.getApplicationRoles();

    ApiRoleMemberMappingListDTO roleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);
    assertThat(roleMemberMappings).isNotNull();
    assertThat(roleMemberMappings.memberMappings).hasSameSizeAs(orgRoles);

    // Create
    ApiRoleMemberMappingListDTO roleMemberMappingListDTO = newMemberMapping(
        newMemberList(newMember(MemberType.USER, userB.getUsername())), orgRoles.get(0).getId());
    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    // Read for created data
    response = request.get();
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO returnedRoleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);

    assertThat(returnedRoleMemberMappings).isNotNull();
    assertThat(returnedRoleMemberMappings.memberMappings).isNotNull();
    assertThat(returnedRoleMemberMappings.memberMappings).hasSameSizeAs(orgRoles);

    ApiRoleMemberMappingDTO returnedRoleMemberMapping = null;
    for (final ApiRoleMemberMappingDTO roleMemberMapping : returnedRoleMemberMappings.memberMappings) {
      if (orgRoles.get(0).getId().equals(roleMemberMapping.roleId)) {
        returnedRoleMemberMapping = roleMemberMapping;
        break;
      }
    }
    assertApiRoleMemberMappingDTO(returnedRoleMemberMapping, orgRoles.get(0).getId(), userB, MemberType.USER);

    // Update
    roleMemberMappingListDTO = newMemberMapping(newMemberList(newMember(MemberType.USER, userA.getUsername())),
        orgRoles.get(0).getId());
    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    roleMemberMappingListDTO = newMemberMapping(newMemberList(newMember(MemberType.USER, userB.getUsername())),
        orgRoles.get(1).getId());
    response = request.body(roleMemberMappingListDTO).put();
    assertResponseStatus(204, response);

    // Read for updated data
    response = request.get();
    assertResponseStatus(200, response);
    returnedRoleMemberMappings = response.getBody(ApiRoleMemberMappingListDTO.class);
    assertThat(returnedRoleMemberMappings).isNotNull();
    assertThat(returnedRoleMemberMappings.memberMappings).isNotNull();
    assertThat(returnedRoleMemberMappings.memberMappings).hasSameSizeAs(orgRoles);

    ApiRoleMemberMappingDTO[] returnedRoleMemberMappingArray = new ApiRoleMemberMappingDTO[2];
    for (final ApiRoleMemberMappingDTO roleMemberMapping : returnedRoleMemberMappings.memberMappings) {
      if (orgRoles.get(0).getId().equals(roleMemberMapping.roleId)) {
        returnedRoleMemberMappingArray[0] = roleMemberMapping;
      }
      else if (orgRoles.get(1).getId().equals(roleMemberMapping.roleId)) {
        returnedRoleMemberMappingArray[1] = roleMemberMapping;
      }
    }
    assertThat(returnedRoleMemberMappingArray).hasSize(2);
    assertApiRoleMemberMappingDTO(returnedRoleMemberMappingArray[0], orgRoles.get(0).getId(), userA, MemberType.USER);
    assertApiRoleMemberMappingDTO(returnedRoleMemberMappingArray[1], orgRoles.get(1).getId(), userB, MemberType.USER);
  }

  @Test
  public void testAddOrganization() throws Exception {
    OrganizationDAO organizationDAO = new OrganizationDAO();

    ApiOrganizationDTO requestBody = new ApiOrganizationDTO(null, "test-create-organization");

    HttpRequest request = restRequest().body(requestBody);
    HttpResponse response = request.post();
    assertResponseStatus(200, response);

    ApiOrganizationDTO responseBody = response.getBody(ApiOrganizationDTO.class);
    assertThat(responseBody.id).isNotEmpty();

    Organization organization = organizationDAO.getByIdNotNull(responseBody.id);
    tempEntity.register(organization);

    assertThat(responseBody.name).isEqualTo(requestBody.name);
    assertThat(responseBody.tags).isEmpty();

    assertThat(organization.getName()).isEqualTo(requestBody.name);
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
    return new ApiMemberDTO(null /* ownerId */, null /* ownerType */, name, type);
  }

  private List<ApiMemberDTO> newMemberList(final ApiMemberDTO... members) {
    return Arrays.asList(members);
  }

  private void assertApiRoleMemberMappingDTO(final ApiRoleMemberMappingDTO apiRoleMemberMappingDTO,
                                             final String roleId,
                                             final User user,
                                             final MemberType type)
  {
    assertThat(apiRoleMemberMappingDTO).isNotNull();
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(roleId);
    final List<ApiMemberDTO> returnedMembers = apiRoleMemberMappingDTO.members;
    assertThat(returnedMembers).hasSize(1);
    assertThat(returnedMembers.get(0).type).isEqualTo(type);
    assertThat(returnedMembers.get(0).userOrGroupName).isEqualTo(user.getUsername());
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ORG_RESOURCE_PATH);
  }
}
