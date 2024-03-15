/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import java.util.List;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.api.v2.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiRoleMembershipResource.APPLICATION_OR_ORGANIZATION;
import static com.sonatype.insight.brain.api.v2.ApiRoleMembershipResource.GLOBAL_OR_REPOSITORY_CONTAINER;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static com.sonatype.insight.brain.model.security.MemberType.GROUP;
import static com.sonatype.insight.brain.model.security.MemberType.USER;
import static com.sonatype.insight.brain.model.security.MembershipMapping.GLOBAL_CONTEXT_ID;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.OWNER_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.POLICY_ADMIN_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.SYSTEM_ADMIN_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiRoleMembershipResourceTest
    extends AbstractResourceTest
{
  private MembershipMappingDAO dao;

  @Before
  public void setUp() {
    dao = lookup(MembershipMappingDAO.class);
  }

  @Test
  public void testGrantRoleMembershipApplicationOrOrganization_Application() throws Exception {
    String applicationId = tempEntity.newApplicationWithParent().getId();
    String username = tempEntity.newUser("a-user").getUsername();

    HttpResponse response =
        restRequest().path(APPLICATION_OR_ORGANIZATION)
            .parameter("application", applicationId, DEVELOPER_ROLE_ID, "user", username)
            .put();
    assertResponseStatus(204, response);

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(applicationId, DEVELOPER_ROLE_ID, username, USER);
    assertThat(membershipMapping).isNotNull();
  }

  @Test
  public void testGrantRoleMembershipApplicationOrOrganization_Organization() throws Exception {
    String organizationId = tempEntity.newOrganization().getId();
    String username = tempEntity.newUser("a-user").getUsername();

    HttpResponse response =
        restRequest().path(APPLICATION_OR_ORGANIZATION)
            .parameter("organization", organizationId, DEVELOPER_ROLE_ID, "user", username)
            .put();

    assertResponseStatus(204, response);

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(organizationId, DEVELOPER_ROLE_ID, username, USER);
    assertThat(membershipMapping).isNotNull();
  }

  @Test
  public void testGrantRoleMembershipGlobalOrRepositoryContainer_Global() throws Exception {
    String username = tempEntity.newUser("a-user").getUsername();

    HttpResponse response =
        restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
            .parameter("global", SYSTEM_ADMIN_ROLE_ID, "user", username)
            .put();
    assertResponseStatus(204, response);

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(GLOBAL_CONTEXT_ID, SYSTEM_ADMIN_ROLE_ID, username, USER);
    assertThat(membershipMapping).isNotNull();
  }

  @Test
  public void testGrantRoleMembershipGlobalOrRepositoryContainer_RepositoryContainer() throws Exception {
    String username = tempEntity.newUser("a-user").getUsername();

    HttpResponse response =
        restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
            .parameter("repository_container", DEVELOPER_ROLE_ID, "user", username)
            .put();
    assertResponseStatus(204, response);

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(REPOSITORY_CONTAINER_ID, DEVELOPER_ROLE_ID, username,
            USER);
    assertThat(membershipMapping).isNotNull();
  }

  @Test
  public void testRevokeRoleMembershipApplicationOrOrganization_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    MembershipMapping membershipMapping =
        tempEntity.newMembershipMapping(application.getId(), DEVELOPER_ROLE_ID, "a-user", USER);

    HttpResponse response =
        restRequest().path(APPLICATION_OR_ORGANIZATION)
            .parameter("application", application.getId(), DEVELOPER_ROLE_ID, "user", "a-user")
            .delete();

    assertResponseStatus(204, response);
    assertThat(dao.getById(membershipMapping.getId())).isNull();
  }

  @Test
  public void testRevokeRoleMembershipApplicationOrOrganization_Organization() throws Exception {
    Organization organization = tempEntity.newOrganization();
    MembershipMapping membershipMapping =
        tempEntity.newMembershipMapping(organization.getId(), DEVELOPER_ROLE_ID, "a-user", USER);

    HttpResponse response =
        restRequest().path(APPLICATION_OR_ORGANIZATION)
            .parameter("organization", organization.getId(), DEVELOPER_ROLE_ID, "user", "a-user")
            .delete();

    assertResponseStatus(204, response);
    assertThat(dao.getById(membershipMapping.getId())).isNull();
  }

  @Test
  public void testRevokeRoleMembershipGlobalOrRepositoryContainer_Global() throws Exception {
    MembershipMapping membershipMapping =
        tempEntity.newMembershipMapping("global", SYSTEM_ADMIN_ROLE_ID, "groupname", GROUP);

    HttpResponse response =
        restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
            .parameter("global", SYSTEM_ADMIN_ROLE_ID, "group", "groupname")
            .delete();

    assertResponseStatus(204, response);
    assertThat(dao.getById(membershipMapping.getId())).isNull();
  }

  @Test
  public void testRevokeRoleMembershipGlobalOrRepositoryContainer_RepositoryContainer() throws Exception {
    MembershipMapping membershipMapping =
        tempEntity.newMembershipMapping(REPOSITORY_CONTAINER_ID, DEVELOPER_ROLE_ID, "repo-user", USER);

    HttpResponse response =
        restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
            .parameter("repository_container", DEVELOPER_ROLE_ID, "user", "repo-user")
            .delete();

    assertResponseStatus(204, response);
    assertThat(dao.getById(membershipMapping.getId())).isNull();
  }

  @Test
  public void testGetRoleMembershipsApplicationOrOrganization_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newMembershipMapping(application.getId(), DEVELOPER_ROLE_ID, "user-one", USER);
    tempEntity.newMembershipMapping(application.getId(), DEVELOPER_ROLE_ID, "user-two", USER);

    HttpResponse response = restRequest().path("application", application.getId()).get();
    assertResponseStatus(200, response);

    List<ApiRoleMemberMappingDTO> memberMappings = response.getBody(ApiRoleMemberMappingListDTO.class).memberMappings;
    assertThat(memberMappings).hasSize(1);

    ApiRoleMemberMappingDTO roleMappingDto = memberMappings.iterator().next();
    assertThat(roleMappingDto.roleId).isEqualTo(DEVELOPER_ROLE_ID);

    List<ApiMemberDTO> members = roleMappingDto.members;
    assertThat(members).hasSize(2);
    assertApiMemberDTO(members.get(0), application.getId(), OwnerType.APPLICATION, USER, "user-one");
    assertApiMemberDTO(members.get(1), application.getId(), OwnerType.APPLICATION, USER, "user-two");
  }

  @Test
  public void testGetRoleMembershipsApplicationOrOrganization_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    tempEntity.newMembershipMapping(org.getId(), DEVELOPER_ROLE_ID, "user-one", USER);
    tempEntity.newMembershipMapping(org.getId(), OWNER_ROLE_ID, "user-two", USER);

    HttpResponse response = restRequest().path("organization", org.getId()).get();
    assertResponseStatus(200, response);

    List<ApiRoleMemberMappingDTO> memberMappings = response.getBody(ApiRoleMemberMappingListDTO.class).memberMappings;
    assertThat(memberMappings).hasSize(2);

    ApiRoleMemberMappingDTO apiRoleMemberMappingDTO = memberMappings.get(0);
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(DEVELOPER_ROLE_ID);
    assertThat(apiRoleMemberMappingDTO.members).hasSize(1);
    ApiMemberDTO apiMemberDTO = apiRoleMemberMappingDTO.members.get(0);
    assertApiMemberDTO(apiMemberDTO, org.getId(), OwnerType.ORGANIZATION, USER, "user-one");

    apiRoleMemberMappingDTO = memberMappings.get(1);
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(OWNER_ROLE_ID);
    assertThat(apiRoleMemberMappingDTO.members).hasSize(1);
    apiMemberDTO = apiRoleMemberMappingDTO.members.get(0);
    assertApiMemberDTO(apiMemberDTO, org.getId(), OwnerType.ORGANIZATION, USER, "user-two");
  }

  @Test
  public void testGetRoleMembershipsGlobalOrRepositoryContainer_Global() throws Exception {
    HttpResponse response = restRequest().path("global").get();
    assertResponseStatus(200, response);

    ApiRoleMemberMappingListDTO memberMappingList = response.getBody(ApiRoleMemberMappingListDTO.class);
    List<ApiRoleMemberMappingDTO> memberMappings = memberMappingList.memberMappings;
    assertThat(memberMappings).hasSize(2);

    ApiRoleMemberMappingDTO apiRoleMemberMappingDTO = memberMappings.get(0);
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(POLICY_ADMIN_ROLE_ID);
    assertThat(apiRoleMemberMappingDTO.members).hasSize(1);
    ApiMemberDTO apiMemberDTO = apiRoleMemberMappingDTO.members.get(0);
    assertApiMemberDTO(apiMemberDTO, MembershipMapping.GLOBAL_CONTEXT_ID, OwnerType.GLOBAL, USER, "admin");

    apiRoleMemberMappingDTO = memberMappings.get(1);
    assertThat(apiRoleMemberMappingDTO.roleId).isEqualTo(SYSTEM_ADMIN_ROLE_ID);
    assertThat(apiRoleMemberMappingDTO.members).hasSize(1);
    apiMemberDTO = apiRoleMemberMappingDTO.members.get(0);
    assertApiMemberDTO(apiMemberDTO, MembershipMapping.GLOBAL_CONTEXT_ID, OwnerType.GLOBAL, USER, "admin");
  }

  @Test
  public void testGetRoleMembershipsGlobalOrRepositoryContainer_RepositoryContainer_User() throws Exception {
    tempEntity.newMembershipMapping(REPOSITORY_CONTAINER_ID, DEVELOPER_ROLE_ID, "a-user");

    HttpResponse response = restRequest().path("repository_container").get();
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO memberMappingList = response.getBody(ApiRoleMemberMappingListDTO.class);

    assertThat(memberMappingList.memberMappings).hasSize(1);

    ApiRoleMemberMappingDTO roleMembers = memberMappingList.memberMappings.get(0);
    assertThat(roleMembers.roleId).isEqualTo(DEVELOPER_ROLE_ID);

    List<ApiMemberDTO> members = roleMembers.members;
    assertThat(members).hasSize(1);

    ApiMemberDTO apiMemberDTO = members.get(0);
    assertApiMemberDTO(apiMemberDTO, REPOSITORY_CONTAINER_ID, OwnerType.REPOSITORY_CONTAINER, USER, "a-user");
  }

  @Test
  public void testGetRoleMembershipsGlobalOrRepositoryContainer_RepositoryContainer_Group() throws Exception {
    tempEntity.newMembershipMapping(REPOSITORY_CONTAINER_ID, OWNER_ROLE_ID, "a-group", GROUP);
    HttpResponse response = restRequest().path("repository_container").get();
    assertResponseStatus(200, response);
    ApiRoleMemberMappingListDTO memberMappingList = response.getBody(ApiRoleMemberMappingListDTO.class);

    assertThat(memberMappingList.memberMappings).hasSize(1);

    ApiRoleMemberMappingDTO roleMembers = memberMappingList.memberMappings.get(0);
    assertThat(roleMembers.roleId).isEqualTo(OWNER_ROLE_ID);

    List<ApiMemberDTO> members = roleMembers.members;
    assertThat(members).hasSize(1);

    ApiMemberDTO apiMemberDTO = members.get(0);
    assertApiMemberDTO(apiMemberDTO, REPOSITORY_CONTAINER_ID, OwnerType.REPOSITORY_CONTAINER, GROUP, "a-group");
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.ROLE_MEMBERSHIP_PATH_V2);
  }

  private void assertApiMemberDTO(
      ApiMemberDTO actual,
      String expectedOwnerId,
      OwnerType expectedOwnerType,
      MemberType expectedMemberType,
      String expectedUserOrGroupName)
  {
    assertThat(actual.ownerId).isEqualTo(expectedOwnerId);
    assertThat(actual.ownerType).isEqualTo(expectedOwnerType.name());
    assertThat(actual.type).isEqualTo(expectedMemberType);
    assertThat(actual.userOrGroupName).isEqualTo(expectedUserOrGroupName);
  }
}
