/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

import static com.sonatype.insight.brain.api.v2.ApiMembershipMappingResource.APPLICATION_OR_ORGANIZATION;
import static com.sonatype.insight.brain.api.v2.ApiMembershipMappingResource.GLOBAL_OR_REPOSITORY_CONTAINER;
import static com.sonatype.insight.brain.model.repository.RepositoryContainer.REPOSITORY_CONTAINER_ID;
import static com.sonatype.insight.brain.model.security.MemberType.GROUP;
import static com.sonatype.insight.brain.model.security.MemberType.USER;
import static com.sonatype.insight.brain.model.security.MembershipMapping.GLOBAL_CONTEXT_ID;
import static com.sonatype.insight.brain.model.security.Role.DEVELOPER_ROLE_ID;
import static com.sonatype.insight.brain.model.security.Role.SYSTEM_ADMIN_ROLE_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class ApiMembershipMappingResourceTest
    extends AbstractResourceTest
{
  private MembershipMappingDAO dao = new MembershipMappingDAO();

  @Test
  public void testGrantMembershipMappingApplicationOrOrganization_Application() throws Exception {
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
  public void testGrantMembershipMappingApplicationOrOrganization_Organization() throws Exception {
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
  public void testGrantMembershipMappingGlobalOrRepositoryContainer_Global() throws Exception {
    String username = tempEntity.newUser("a-user").getUsername();

    HttpResponse response =
        restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
            .parameter("global", SYSTEM_ADMIN_ROLE_ID, "group", username)
            .put();
    assertResponseStatus(204, response);

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(GLOBAL_CONTEXT_ID, SYSTEM_ADMIN_ROLE_ID, username, GROUP);
    assertThat(membershipMapping).isNotNull();
  }

  @Test
  public void testGrantMembershipMappingGlobalOrRepositoryContainer_RepositoryContainer() throws Exception {
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
  public void testRevokeMembershipMappingApplicationOrOrganization_Application() throws Exception {
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
  public void testRevokeMembershipMappingApplicationOrOrganization_Organization() throws Exception {
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
  public void testRevokeMembershipMappingGlobalOrRepositoryContainer_Global() throws Exception {
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
  public void testRevokeMembershipMappingGlobalOrRepositoryContainer_RepositoryContainer() throws Exception {
    MembershipMapping membershipMapping =
        tempEntity.newMembershipMapping(REPOSITORY_CONTAINER_ID, DEVELOPER_ROLE_ID, "repo-user", USER);

    HttpResponse response =
        restRequest().path(GLOBAL_OR_REPOSITORY_CONTAINER)
            .parameter("repository_container", DEVELOPER_ROLE_ID, "user", "repo-user")
            .delete();

    assertResponseStatus(204, response);
    assertThat(dao.getById(membershipMapping.getId())).isNull();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.MEMBERSHIP_MAPPING_PATH_V2);
  }
}
