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
import com.sonatype.insight.brain.api.v2.dto.ApiApplicableMembershipMappingsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiMemberDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiMemberWithDetailsDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleMemberMappingListDTO;
import com.sonatype.insight.brain.api.v2.dto.ApiRoleWithMembersByOwnerDTO;
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

  @Test
  public void testGetBulkRoleMembershipsApplicationOrOrganization_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newMembershipMapping(application.getId(), DEVELOPER_ROLE_ID, "user-one", USER);
    tempEntity.newMembershipMapping(application.getId(), OWNER_ROLE_ID, "user-two", USER);

    HttpResponse response = restRequest().path("application", application.getPublicId(), "roles").get();
    assertResponseStatus(200, response);

    ApiApplicableMembershipMappingsDTO result = response.getBody(ApiApplicableMembershipMappingsDTO.class);

    // Returns all applicable roles (not just ones with members)
    assertThat(result.membersByRole()).hasSize(5);

    // Find the Developer role in the results
    ApiRoleWithMembersByOwnerDTO developerRole = result.membersByRole().stream()
        .filter(role -> DEVELOPER_ROLE_ID.equals(role.roleId()))
        .findFirst()
        .orElseThrow(() -> new AssertionError("Developer role not found"));

    assertThat(developerRole.roleName()).isNotNull();
    assertThat(developerRole.roleDescription()).isNotNull();
    assertThat(developerRole.membersByOwner()).hasSizeGreaterThan(0);

    // Verify that user-one appears somewhere in the membersByOwner list
    // (could be under application or inherited from parent org)
    boolean foundUserOne = developerRole.membersByOwner().stream()
        .flatMap(mbo -> mbo.members().stream())
        .anyMatch(member -> USER.equals(member.type()) && "user-one".equals(member.internalName()));

    assertThat(foundUserOne).withFailMessage("Expected user-one to be in Developer role members").isTrue();
  }

  @Test
  public void testGetBulkRoleMembershipsApplicationOrOrganization_Organization() throws Exception {
    Organization org = tempEntity.newOrganization();
    tempEntity.newMembershipMapping(org.getId(), DEVELOPER_ROLE_ID, "org-user", USER);
    tempEntity.newMembershipMapping(org.getId(), OWNER_ROLE_ID, "org-group", GROUP);

    HttpResponse response = restRequest().path("organization", org.getId(), "roles").get();
    assertResponseStatus(200, response);

    ApiApplicableMembershipMappingsDTO result = response.getBody(ApiApplicableMembershipMappingsDTO.class);

    // Returns all applicable roles (not just ones with members)
    assertThat(result.membersByRole()).hasSize(5);

    // Verify it includes organization context
    assertThat(result.membersByRole().get(0).membersByOwner()).isNotEmpty();
    assertThat(result.membersByRole().get(0).membersByOwner().get(0).ownerType())
        .isEqualTo(OwnerType.ORGANIZATION.name());
  }

  @Test
  public void testGetBulkRoleMembershipsRepositoryContainer() throws Exception {
    tempEntity.newMembershipMapping(REPOSITORY_CONTAINER_ID, DEVELOPER_ROLE_ID, "repo-user", USER);

    HttpResponse response = restRequest().path("repository_container", "roles").get();
    assertResponseStatus(200, response);

    ApiApplicableMembershipMappingsDTO result = response.getBody(ApiApplicableMembershipMappingsDTO.class);

    // Returns all applicable roles (not just ones with members)
    assertThat(result.membersByRole()).hasSize(5);

    // Verify that repo-user appears somewhere in the response
    boolean foundRepoUser = result.membersByRole().stream()
        .filter(role -> DEVELOPER_ROLE_ID.equals(role.roleId()))
        .flatMap(role -> role.membersByOwner().stream())
        .flatMap(mbo -> mbo.members().stream())
        .anyMatch(member -> USER.equals(member.type()) && "repo-user".equals(member.internalName()));

    assertThat(foundRepoUser).withFailMessage("Expected repo-user to be in Developer role members").isTrue();
  }

  @Test
  public void testSetBulkRoleMembersApplicationOrOrganization_Application() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newUser("new-user");

    List<ApiMemberWithDetailsDTO> members = List.of(
        createApiMemberWithDetails(USER, "new-user", "New User", "new-user@test.com", "IQ Server")
    );

    HttpResponse response = restRequest()
        .path("application", application.getPublicId(), "role", DEVELOPER_ROLE_ID, "members")
        .body(members)
        .put();

    assertResponseStatus(204, response);

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(application.getId(), DEVELOPER_ROLE_ID, "new-user", USER);
    assertThat(membershipMapping).isNotNull();
  }

  @Test
  public void testSetBulkRoleMembersApplicationOrOrganization_ReplacesExistingMembers() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newMembershipMapping(application.getId(), DEVELOPER_ROLE_ID, "old-user", USER);
    tempEntity.newUser("new-user");

    List<ApiMemberWithDetailsDTO> members = List.of(
        createApiMemberWithDetails(USER, "new-user", "New User", "new-user@test.com", "IQ Server")
    );

    HttpResponse response = restRequest()
        .path("application", application.getPublicId(), "role", DEVELOPER_ROLE_ID, "members")
        .body(members)
        .put();

    assertResponseStatus(204, response);

    MembershipMapping oldMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(application.getId(), DEVELOPER_ROLE_ID,
            "old-user", USER);
    assertThat(oldMapping).isNull();

    MembershipMapping newMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(application.getId(), DEVELOPER_ROLE_ID,
            "new-user", USER);
    assertThat(newMapping).isNotNull();
  }

  @Test
  public void testSetBulkRoleMembersApplicationOrOrganization_EmptyList() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newMembershipMapping(application.getId(), DEVELOPER_ROLE_ID, "user-to-remove", USER);

    List<ApiMemberWithDetailsDTO> members = List.of();

    HttpResponse response = restRequest()
        .path("application", application.getPublicId(), "role", DEVELOPER_ROLE_ID, "members")
        .body(members)
        .put();

    assertResponseStatus(204, response);

    MembershipMapping removedMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(application.getId(), DEVELOPER_ROLE_ID,
            "user-to-remove", USER);
    assertThat(removedMapping).isNull();
  }

  @Test
  public void testSetBulkRoleMembersRepositoryContainer() throws Exception {
    tempEntity.newUser("repo-user");

    List<ApiMemberWithDetailsDTO> members = List.of(
        createApiMemberWithDetails(USER, "repo-user", "Repo User", "repo-user@test.com","IQ Server")
    );

    HttpResponse response = restRequest()
        .path("repository_container", "role", DEVELOPER_ROLE_ID, "members")
        .body(members)
        .put();

    assertResponseStatus(204, response);

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(REPOSITORY_CONTAINER_ID, DEVELOPER_ROLE_ID,
            "repo-user", USER);
    assertThat(membershipMapping).isNotNull();
  }

  @Test
  public void testSetBulkRoleMembersWithMultipleMembers() throws Exception {
    Application application = tempEntity.newApplicationWithParent();
    tempEntity.newUser("user-one");
    tempEntity.newUser("user-two");

    List<ApiMemberWithDetailsDTO> members = List.of(
        createApiMemberWithDetails(USER, "user-one", "User One", "user-one@test.com", "IQ Server"),
        createApiMemberWithDetails(USER, "user-two", "User Two", "user-two@test.com", "IQ Server")
    );

    HttpResponse response = restRequest()
        .path("application", application.getPublicId(), "role", OWNER_ROLE_ID, "members")
        .body(members)
        .put();

    assertResponseStatus(204, response);

    MembershipMapping mapping1 =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(application.getId(), OWNER_ROLE_ID, "user-one", USER);
    assertThat(mapping1).isNotNull();

    MembershipMapping mapping2 =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(application.getId(), OWNER_ROLE_ID, "user-two", USER);
    assertThat(mapping2).isNotNull();
  }

  @Test
  public void testGetBulkRoleMembershipsGlobal() throws Exception {
    HttpResponse response = restRequest().path("global", "roles").get();
    assertResponseStatus(200, response);

    ApiApplicableMembershipMappingsDTO result = response.getBody(ApiApplicableMembershipMappingsDTO.class);

    assertThat(result.membersByRole()).hasSizeGreaterThan(0);
  }

  @Test
  public void testSetBulkRoleMembersGlobal() throws Exception {
    tempEntity.newUser("global-admin");

    List<ApiMemberWithDetailsDTO> members = List.of(
        createApiMemberWithDetails(USER, "global-admin", "Global Admin", "global-admin@test.com", "IQ Server")
    );

    HttpResponse response = restRequest()
        .path("global", "role", SYSTEM_ADMIN_ROLE_ID, "members")
        .body(members)
        .put();

    assertResponseStatus(204, response);

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(GLOBAL_CONTEXT_ID, SYSTEM_ADMIN_ROLE_ID,
            "global-admin", USER);
    assertThat(membershipMapping).isNotNull();
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

  private ApiMemberWithDetailsDTO createApiMemberWithDetails(
      MemberType type,
      String internalName,
      String displayName,
      String email,
      String realm)
  {
    return new ApiMemberWithDetailsDTO(type, internalName, displayName, email, realm);
  }
}
