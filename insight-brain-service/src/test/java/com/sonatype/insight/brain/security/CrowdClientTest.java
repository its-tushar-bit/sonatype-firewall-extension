/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import com.sonatype.insight.brain.model.security.Group;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.model.security.UserToken;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightConfig.ExperimentalFeature;

import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.exception.InactiveAccountException;
import com.atlassian.crowd.exception.InvalidAuthenticationException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.integration.rest.entity.GroupEntity;
import com.atlassian.crowd.integration.rest.entity.PasswordEntity;
import com.atlassian.crowd.integration.rest.entity.UserEntity;
import com.atlassian.crowd.model.group.GroupType;
import com.atlassian.crowd.search.query.entity.restriction.BooleanRestriction;
import com.atlassian.crowd.search.query.entity.restriction.BooleanRestriction.BooleanLogic;
import com.atlassian.crowd.search.query.entity.restriction.MatchMode;
import com.atlassian.crowd.search.query.entity.restriction.TermRestriction;
import com.atlassian.crowd.search.query.entity.restriction.constants.UserTermKeys;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.collect.ImmutableMap.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class CrowdClientTest
    extends AbstractComponentTest
{
  @Rule
  public CrowdMockServerRule crowdMockServer = new CrowdMockServerRule();

  @Inject
  private InsightConfig insightConfig;

  @Inject
  private PasswordHandler passwordHandler;

  @Inject
  private CrowdClientFactory crowdClientFactory;

  private CrowdClient crowdClient;

  @Before
  public void before() {
    insightConfig.setExperimentalFeatures(of(ExperimentalFeature.CROWD_INTEGRATION.getFlag(), true));
    tempEntity.newCrowdConfiguration(crowdMockServer.getBaseUrl() + "/crowd", "iq server",
        passwordHandler.encryptPassword("password".toCharArray()));
    crowdClient = crowdClientFactory.createCrowdClient();
  }

  @Test
  public void testAuthenticateUser() throws Exception {
    String displayName = "displayName";
    String[] groups = new String[]{"group1", "group2", "group3"};
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password".toCharArray());
    crowdMockServer.mockAuthenticateUser(usernamePasswordToken.getUsername(), displayName);
    crowdMockServer.mockGetGroupsForNestedUser(usernamePasswordToken.getUsername(), groups);

    UserPrincipal userPrincipal = crowdClient.authenticateUser(usernamePasswordToken);

    assertUserPrincipalFromCrowdRealm(userPrincipal, usernamePasswordToken.getUsername(), displayName, groups);
  }

  @Test
  public void testAuthenticateUser_NoGroups() throws Exception {
    String displayName = "displayName";
    String[] groups = new String[]{};
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password".toCharArray());
    crowdMockServer.mockAuthenticateUser(usernamePasswordToken.getUsername(), displayName);
    crowdMockServer.mockGetGroupsForNestedUser(usernamePasswordToken.getUsername(), groups);

    UserPrincipal userPrincipal = crowdClient.authenticateUser(usernamePasswordToken);

    assertUserPrincipalFromCrowdRealm(userPrincipal, usernamePasswordToken.getUsername(), displayName, groups);
  }

  @Test
  public void testAuthenticateUser_SomeGroupsInactive() throws Exception {
    String displayName = "displayName";
    String[] groups = new String[]{"group1", "group2", "group3"};
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password".toCharArray());
    crowdMockServer.mockAuthenticateUser(usernamePasswordToken.getUsername(), displayName);
    GroupEntity groupEntity1 = new GroupEntity(groups[0], "description", GroupType.GROUP, true);
    GroupEntity groupEntity2 = new GroupEntity(groups[1], "description", GroupType.GROUP, true);
    GroupEntity groupEntity3 = new GroupEntity(groups[2], "description", GroupType.GROUP, false);
    crowdMockServer.mockGetGroupsForNestedUser(usernamePasswordToken.getUsername(), groupEntity1, groupEntity2,
        groupEntity3);

    UserPrincipal userPrincipal = crowdClient.authenticateUser(usernamePasswordToken);

    assertUserPrincipalFromCrowdRealm(userPrincipal, usernamePasswordToken.getUsername(), displayName, groups[0],
        groups[1]);
  }

  @Test
  public void testAuthenticateUser_AuthenticateUserError() {
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");
    crowdMockServer.mockAuthenticateUserError(usernamePasswordToken.getUsername(), 401);

    assertThatExceptionOfType(InvalidAuthenticationException.class).isThrownBy(
        () -> crowdClient.authenticateUser(usernamePasswordToken))
        .withMessageContaining("Error");
  }

  @Test
  public void testAuthenticateUser_GetGroupsForNestedUserError() throws Exception {
    String displayName = "displayName";
    UsernamePasswordToken usernamePasswordToken = new UsernamePasswordToken("username", "password");
    crowdMockServer.mockAuthenticateUser(usernamePasswordToken.getUsername(), displayName);
    crowdMockServer.mockGetGroupsForNestedUserError(usernamePasswordToken.getUsername(), 401);

    assertThatExceptionOfType(InvalidAuthenticationException.class).isThrownBy(
        () -> crowdClient.authenticateUser(usernamePasswordToken))
        .withMessageContaining("Error");
  }

  @Test
  public void testGetUser() throws Exception {
    String displayName = "displayName";
    String[] groups = new String[]{"group1", "group2", "group3"};
    UserToken userToken = new UserToken();
    userToken.setUsername("username");
    crowdMockServer.mockGetUser(userToken.getUsername(), displayName);
    crowdMockServer.mockGetGroupsForNestedUser(userToken.getUsername(), groups);

    UserPrincipal userPrincipal = crowdClient.getUser(userToken);

    assertUserPrincipalFromUserTokenRealm(userPrincipal, userToken.getUsername(), displayName, groups);
  }

  @Test
  public void testGetUser_InactiveUser() throws Exception {
    String displayName = "displayName";
    String[] groups = new String[]{"group1", "group2", "group3"};
    UserToken userToken = new UserToken();
    userToken.setUsername("username");
    UserEntity userEntity = new UserEntity(userToken.getUsername(), "firstName", "lastName", displayName, "email",
        new PasswordEntity("password"), false, null);
    crowdMockServer.mockGetUser(userEntity);
    crowdMockServer.mockGetGroupsForNestedUser(userToken.getUsername(), groups);

    assertThatExceptionOfType(InactiveAccountException.class).isThrownBy(
        () -> crowdClient.getUser(userToken))
        .withMessageContaining(String.format("Account with name <%s> is inactive", userToken.getUsername()));
  }

  @Test
  public void testGetUser_NoGroups() throws Exception {
    String displayName = "displayName";
    String[] groups = new String[]{};
    UserToken userToken = new UserToken();
    userToken.setUsername("username");
    crowdMockServer.mockGetUser(userToken.getUsername(), displayName);
    crowdMockServer.mockGetGroupsForNestedUser(userToken.getUsername(), groups);

    UserPrincipal userPrincipal = crowdClient.getUser(userToken);

    assertUserPrincipalFromUserTokenRealm(userPrincipal, userToken.getUsername(), displayName, groups);
  }

  @Test
  public void testGetUser_SomeGroupsInactive() throws Exception {
    String displayName = "displayName";
    String[] groups = new String[]{"group1", "group2", "group3"};
    UserToken userToken = new UserToken();
    userToken.setUsername("username");
    crowdMockServer.mockGetUser(userToken.getUsername(), displayName);
    GroupEntity groupEntity1 = new GroupEntity(groups[0], "description", GroupType.GROUP, true);
    GroupEntity groupEntity2 = new GroupEntity(groups[1], "description", GroupType.GROUP, true);
    GroupEntity groupEntity3 = new GroupEntity(groups[2], "description", GroupType.GROUP, false);
    crowdMockServer.mockGetGroupsForNestedUser(userToken.getUsername(), groupEntity1, groupEntity2,
        groupEntity3);

    UserPrincipal userPrincipal = crowdClient.getUser(userToken);

    assertUserPrincipalFromUserTokenRealm(userPrincipal, userToken.getUsername(), displayName, groups[0], groups[1]);
  }

  @Test
  public void testGetUser_GetUserError() throws Exception {
    UserToken userToken = new UserToken();
    userToken.setUsername("username");
    crowdMockServer.mockGetUserError(userToken.getUsername(), 401);

    assertThatExceptionOfType(InvalidAuthenticationException.class).isThrownBy(
        () -> crowdClient.getUser(userToken))
        .withMessageContaining("Error");
  }

  @Test
  public void testGetUser_GetGroupsForNestedUserError() throws Exception {
    String displayName = "displayName";
    UserToken userToken = new UserToken();
    userToken.setUsername("username");
    crowdMockServer.mockGetUser(userToken.getUsername(), displayName);
    crowdMockServer.mockGetGroupsForNestedUserError(userToken.getUsername(), 401);

    assertThatExceptionOfType(InvalidAuthenticationException.class).isThrownBy(
        () -> crowdClient.getUser(userToken))
        .withMessageContaining("Error");
  }

  @Test
  public void testTestConnection() throws Exception {
    crowdMockServer.mockTestConnection();

    crowdClient.testConnection();
  }

  @Test
  public void testTestConnection_Error() {
    crowdMockServer.mockTestConnectionError(400);

    assertThatExceptionOfType(OperationFailedException.class).isThrownBy(() -> crowdClient.testConnection())
        .withMessageContaining("Bad Request");
  }

  @Test
  public void testSearchUsersByUsernames() throws Exception {
    List<String> usernames = Arrays.asList("username1", "username2", "username3");
    crowdMockServer.mockSearchUsers(crowdClient.anyNameMatchesAndActive(new LinkedHashSet<>(usernames)),
        usernames.toArray(new String[0]));

    Set<Member> members = crowdClient.searchUsersByUsernames(new LinkedHashSet<>(usernames));

    assertThat(members).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        new Member(MemberType.USER, "username1", "username1DisplayName", "username1Email", CrowdRealm.ID),
        new Member(MemberType.USER, "username2", "username2DisplayName", "username2Email", CrowdRealm.ID),
        new Member(MemberType.USER, "username3", "username3DisplayName", "username3Email", CrowdRealm.ID)
    );
  }

  @Test
  public void testSearchUsersByUsernames_Error() throws Exception {
    List<String> usernames = Arrays.asList("username1", "username2", "username3");
    crowdMockServer.mockSearchUsersError(crowdClient.anyNameMatchesAndActive(new LinkedHashSet<>(usernames)), 400);

    assertThatExceptionOfType(OperationFailedException.class).isThrownBy(
        () -> crowdClient.searchUsersByUsernames(new LinkedHashSet<>(usernames))).withMessageContaining("Bad Request")
        .withStackTraceContaining("Error");
  }

  @Test
  public void testSearchGroupsByGroupNames() throws Exception {
    List<String> groupNames = Arrays.asList("group1", "group2", "group3");
    crowdMockServer.mockSearchGroups(crowdClient.anyNameMatchesAndActive(new LinkedHashSet<>(groupNames)),
        groupNames.toArray(new String[0]));

    Set<Member> members = crowdClient.searchGroupsByGroupNames(new LinkedHashSet<>(groupNames));

    assertThat(members).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        new Member(MemberType.GROUP, "group1", "group1", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group2", "group2", null, CrowdRealm.ID),
        new Member(MemberType.GROUP, "group3", "group3", null, CrowdRealm.ID)
    );
  }

  @Test
  public void testSearchGroupsByGroupNames_Error() throws Exception {
    List<String> groupNames = Arrays.asList("group1", "group2", "group3");
    crowdMockServer.mockSearchGroupsError(crowdClient.anyNameMatchesAndActive(new LinkedHashSet<>(groupNames)), 400);

    assertThatExceptionOfType(OperationFailedException.class).isThrownBy(
        () -> crowdClient.searchGroupsByGroupNames(new LinkedHashSet<>(groupNames)))
        .withMessageContaining("Bad Request").withStackTraceContaining("Error");
  }

  @Test
  public void testAnyNameMatchesAndActive() {
    List<String> usernames = Arrays.asList("username1", "username2", "username3");

    SearchRestriction anyNameMatchesAndActive =
        crowdClient.anyNameMatchesAndActive(new LinkedHashSet<>(usernames));

    assertThat(anyNameMatchesAndActive).isInstanceOf(BooleanRestriction.class);
    BooleanRestriction anyNameMatchesAndActiveRestriction = (BooleanRestriction) anyNameMatchesAndActive;
    assertThat(anyNameMatchesAndActiveRestriction.getBooleanLogic()).isEqualTo(BooleanLogic.AND);
    Collection<SearchRestriction> restrictions = anyNameMatchesAndActiveRestriction.getRestrictions();
    assertThat(restrictions).hasSize(2);
    BooleanRestriction anyNameMatchesRestriction = (BooleanRestriction) restrictions.stream()
        .filter(r -> r instanceof BooleanRestriction)
        .findFirst()
        .orElse(null);
    assertThat(anyNameMatchesRestriction).isNotNull();
    assertThat(anyNameMatchesRestriction.getBooleanLogic()).isEqualTo(BooleanLogic.OR);
    Collection<SearchRestriction> nameMatchesRestrictions = anyNameMatchesRestriction.getRestrictions();
    assertThat(nameMatchesRestrictions).hasSize(usernames.size());
    for (String username : usernames) {
      TermRestriction<String> nameRestriction =
          (TermRestriction<String>) nameMatchesRestrictions.stream().filter(r -> r instanceof TermRestriction)
              .filter(r -> ((TermRestriction<String>) r).getValue().equals(username)).findFirst().orElse(null);
      assertThat(nameRestriction).isNotNull();
      assertThat(nameRestriction.getProperty()).isEqualTo(UserTermKeys.USERNAME);
      assertThat(nameRestriction.getMatchMode()).isEqualTo(MatchMode.EXACTLY_MATCHES);
    }
    TermRestriction<Boolean> activeRestriction = (TermRestriction<Boolean>) restrictions.stream()
        .filter(r -> r instanceof TermRestriction)
        .findFirst()
        .orElse(null);
    assertThat(activeRestriction.getProperty()).isEqualTo(UserTermKeys.ACTIVE);
    assertThat(activeRestriction.getMatchMode()).isEqualTo(MatchMode.EXACTLY_MATCHES);
    assertThat(activeRestriction.getValue()).isTrue();
  }

  @Test
  public void testGetUsersByGroupName() throws Exception {
    String groupName = "groupName";
    UserEntity userEntity1 = new UserEntity("username1", "firstName", "lastName", "displayName1", "email1",
        new PasswordEntity("password"), true, null);
    UserEntity userEntity2 = new UserEntity("username2", "firstName", "lastName", "displayName2", "email2",
        new PasswordEntity("password"), true, null);
    UserEntity userEntity3 = new UserEntity("username3", "firstName", "lastName", "displayName3", "email3",
        new PasswordEntity("password"), false, null);
    crowdMockServer.mockGetNestedUsersOfGroup(groupName, userEntity1, userEntity2, userEntity3);

    Set<Member> usersByGroup = crowdClient.getUsersByGroupName(groupName);

    assertThat(usersByGroup).usingRecursiveFieldByFieldElementComparator().containsExactlyInAnyOrder(
        new Member(MemberType.USER, "username1", "displayName1", "email1", CrowdRealm.ID),
        new Member(MemberType.USER, "username2", "displayName2", "email2", CrowdRealm.ID)
    );
  }

  @Test
  public void testGetUsersByGroupName_Error() {
    String groupName = "groupName";
    crowdMockServer.mockGetNestedUsersOfGroupError(groupName, 400);

    assertThatExceptionOfType(OperationFailedException.class).isThrownBy(
        () -> crowdClient.getUsersByGroupName(groupName)).withMessageContaining("Bad Request")
        .withStackTraceContaining("Error");
  }

  private void assertUserPrincipalFromCrowdRealm(
      UserPrincipal userPrincipal,
      String expectedUsername,
      String expectedDisplayName,
      String... expectedGroupsWithoutAuthenticatedUsers)
  {
    assertUserPrincipal(userPrincipal, expectedUsername, expectedDisplayName, CrowdRealm.ID,
        expectedGroupsWithoutAuthenticatedUsers);
  }

  private void assertUserPrincipalFromUserTokenRealm(
      UserPrincipal userPrincipal,
      String expectedUsername,
      String expectedDisplayName,
      String... expectedGroupsWithoutAuthenticatedUsers)
  {
    assertUserPrincipal(userPrincipal, expectedUsername, expectedDisplayName, UserTokenRealm.ID,
        expectedGroupsWithoutAuthenticatedUsers);
  }

  private void assertUserPrincipal(
      UserPrincipal userPrincipal,
      String expectedUsername,
      String expectedDisplayName,
      String expectedRealmId,
      String... expectedGroupsWithoutAuthenticatedUsers)
  {
    assertThat(userPrincipal).isNotNull();
    assertThat(userPrincipal.getUsername()).isEqualTo(expectedUsername);
    assertThat(userPrincipal.getDisplayName()).isEqualTo(expectedDisplayName);
    assertThat(userPrincipal.getRealmId()).isEqualTo(expectedRealmId);
    Set<String> expectedGroups = new LinkedHashSet<>(Arrays.asList(expectedGroupsWithoutAuthenticatedUsers));
    expectedGroups.add(Group.AUTHENTICATED_USERS_GROUP_ID);
    assertThat(userPrincipal.getMembership()).isEqualTo(expectedGroups);
  }
}
