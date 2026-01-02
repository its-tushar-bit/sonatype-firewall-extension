/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service.banning.rest.internal;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.Member;
import com.sonatype.insight.brain.security.UserResource;
import com.sonatype.insight.brain.security.UserService.ChangePasswordDTO;
import com.sonatype.insight.brain.security.UserService.FindMembersDTO;
import com.sonatype.insight.brain.service.AbstractMultiTenantBaseIntegrationTest;
import com.sonatype.insight.brain.common.test.SlowTest;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

@Category(SlowTest.class)
public class MultiTenantUserResourceTest
    extends AbstractMultiTenantBaseIntegrationTest
{
  @Override
  protected HttpRequest restRequest() {
    return super.restRequest().path(UserResource.RESOURCE_PATH);
  }

  private HttpRequest findRequest(OwnerType ownerType, String ownerId, String query) {
    return restRequest().path("{ownerType}/{ownerId}/query").query("q", query).parameter(ownerType, ownerId);
  }

  @Test
  public void test_getAll_shouldBeBanned() throws Exception {
    HttpResponse response = restRequest().get();
    assertResponseStatus(404, response);
  }

  @Test
  public void test_addUser_shouldBeBanned() throws Exception {
    User user = new User("testCRUD", "testCRUDPassword", "testCRUDFirstName", "testCRUDLastName",
        "testCRUD@sonatype.com");
    HttpResponse response = restRequest().body(user).post();
    assertResponseStatus(404, response);
  }

  @Test
  public void test_updateUser_shouldBeBanned() throws Exception {
    User user = new User("testCRUD", "testCRUDPassword", "testCRUDFirstName", "testCRUDLastName",
        "testCRUD@sonatype.com");
    HttpResponse response = restRequest().body(user).put();
    assertResponseStatus(404, response);
  }

  @Test
  public void test_deleteUser_shouldBeBanned() throws Exception {
    HttpResponse response = restRequest().path("{userId}").parameter("user-id").delete();
    assertResponseStatus(404, response);
  }

  @Test
  public void test_changeMyPassword_shouldBeBanned() throws Exception {
    ChangePasswordDTO dto = new ChangePasswordDTO();
    dto.oldPassword = "badPass";
    dto.newPassword = "doesntmatter";

    HttpRequest request = restRequest().path(UserResource.MY_PASSWORD_PATH);
    HttpResponse response = request.body(dto).put();
    assertResponseStatus(404, response);
  }

  @Test
  public void test_resetPassword_shouldBeBanned() throws Exception {
    HttpResponse response = restRequest().path(UserResource.RESET_PASSWORD_PATH).parameter("user-id").put();
    assertResponseStatus(404, response);
  }

  @Test
  public void test_shouldDisplayDefaultPasswordWarning_shouldBeBanned() throws Exception {
    HttpRequest request = restRequest();
    HttpResponse response = request.path(UserResource.SHOULD_DISPLAY_DEFAULT_PASSWORD_WARNING).get();
    assertResponseStatus(404, response);
  }

  @Test
  public void test_findMembersForGlobalRoles() throws Exception {
    HttpResponse response = findRequest(OwnerType.GLOBAL, "global", User.ADMIN_USERNAME + "*").get();
    assertMember(response, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost", "IQ Server");
  }

  @Test
  public void test_findMembersForNonGlobalRoles() throws Exception {
    Organization org = tenantTemporaryEntity.newOrganization();
    HttpResponse response = findRequest(OwnerType.ORGANIZATION, org.getId(), User.ADMIN_USERNAME + "*").get();
    assertMember(response, null, MemberType.USER, User.ADMIN_USERNAME, "Admin BuiltIn", "admin@localhost", "IQ Server");
  }

  private void assertMember(
      HttpResponse response,
      String error,
      MemberType type,
      String name,
      String displayName,
      String email,
      String realm)
  {
    assertResponseStatus(200, response);

    FindMembersDTO dto = response.getBody(FindMembersDTO.class);

    assertThat(dto.getError()).isEqualTo(error);

    Member[] members = dto.getMembers().toArray(new Member[0]);
    assertThat(members).hasSize(1);
    assertMember(members[0], type, name, displayName, email, realm);
  }

  private void assertMember(
      final Member member,
      final MemberType type,
      final String name,
      final String displayName,
      final String email,
      final String realm)
  {
    assertThat(member.getType()).isEqualTo(type);
    assertThat(member.getInternalName()).isEqualTo(name);
    assertThat(member.getDisplayName()).isEqualTo(displayName);
    assertThat(member.getEmail()).isEqualTo(email);
    assertThat(member.getRealm()).isEqualTo(realm);
  }
}
