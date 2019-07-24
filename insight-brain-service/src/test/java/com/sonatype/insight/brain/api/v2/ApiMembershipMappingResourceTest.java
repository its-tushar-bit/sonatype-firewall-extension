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
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.service.AbstractResourceTest;

import org.junit.Test;

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

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(applicationId, DEVELOPER_ROLE_ID, username, USER);

    // Original state: mapping not existing in db
    assertThat(membershipMapping).isNull();

    HttpResponse response =
        restRequest().path("application", applicationId, "role", DEVELOPER_ROLE_ID, "user", username).put();
    assertResponseStatus(204, response);

    membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(applicationId, DEVELOPER_ROLE_ID, username, USER);
    assertThat(membershipMapping).isNotNull();
  }

  @Test
  public void testGrantMembershipMappingApplicationOrOrganization_Organization() throws Exception {
    String organizationId = tempEntity.newOrganization().getId();
    String username = tempEntity.newUser("a-user").getUsername();

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(organizationId, DEVELOPER_ROLE_ID, username, USER);

    // Original state: mapping not existing in db
    assertThat(membershipMapping).isNull();

    HttpResponse response =
        restRequest().path("organization", organizationId, "role", DEVELOPER_ROLE_ID, "user", username).put();
    assertResponseStatus(204, response);

    membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(organizationId, DEVELOPER_ROLE_ID, username, USER);
    assertThat(membershipMapping).isNotNull();
  }

  @Test
  public void testGrantMembershipMappingGlobalOrRepositoryContainer_Global() throws Exception {
    String username = tempEntity.newUser("a-user").getUsername();

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(GLOBAL_CONTEXT_ID, SYSTEM_ADMIN_ROLE_ID, username, GROUP);

    // Original state: mapping not existing in db
    assertThat(membershipMapping).isNull();

    HttpResponse response =
        restRequest().path("global", "role", SYSTEM_ADMIN_ROLE_ID, "group", username).put();
    assertResponseStatus(204, response);

    membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(GLOBAL_CONTEXT_ID, SYSTEM_ADMIN_ROLE_ID, username, GROUP);
    assertThat(membershipMapping).isNotNull();
  }

  @Test
  public void testGrantMembershipMappingGlobalOrRepositoryContainer_RepositoryContainer() throws Exception {
    String username = tempEntity.newUser("a-user").getUsername();

    MembershipMapping membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(REPOSITORY_CONTAINER_ID, DEVELOPER_ROLE_ID, username,
            USER);

    // Original state: mapping not existing in db
    assertThat(membershipMapping).isNull();

    HttpResponse response =
        restRequest().path("repository_container", "role", DEVELOPER_ROLE_ID, "user", username).put();
    assertResponseStatus(204, response);

    membershipMapping =
        dao.getByContextIdAndRoleIdAndMemberNameAndMemberType(REPOSITORY_CONTAINER_ID, DEVELOPER_ROLE_ID, username,
            USER);
    assertThat(membershipMapping).isNotNull();
  }

  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(PublicApiPaths.MEMBERSHIP_MAPPING_PATH_V2);
  }
}
