/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.security.OAuth2GroupDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserGroupDAO;
import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.OAuth2UserGroup;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.SamlUserGroup;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SsoUserServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SsoUserService ssoUserService;

  @Inject
  private SamlUserDAO samlUserDAO;

  @Inject
  private SamlGroupDAO samlGroupDAO;

  @Inject
  private SamlUserGroupDAO samlUserGroupDAO;

  @Inject
  private OAuth2UserDAO oAuth2UserDAO;

  @Inject
  private OAuth2GroupDAO oAuth2GroupDAO;

  @Inject
  private OAuth2UserGroupDAO oAuth2UserGroupDAO;

  @Test
  public void testIsSsoRealm_False() {
    assertThat(ssoUserService.isSsoRealm(InternalRealm.ID)).isFalse();
  }

  @Test
  public void testIsSsoRealm_Saml_True() {
    assertThat(ssoUserService.isSsoRealm(SamlRealm.ID)).isTrue();
  }

  @Test
  public void testIsSsoRealm_OAuth2_True() {
    assertThat(ssoUserService.isSsoRealm(OAuth2Realm.ID)).isTrue();
  }

  @Test
  public void testIsSsoConfigured_False() {
    disableSsoWithSaml();
    disableSsoWithOAuth2();

    assertThat(ssoUserService.isSsoConfigured()).isFalse();
  }

  @Test
  public void testIsSsoConfigured_Saml_True() {
    enableSsoWithSaml();
    disableSsoWithOAuth2();

    assertThat(ssoUserService.isSsoConfigured()).isTrue();
  }

  @Test
  public void testIsSsoConfigured_OAuth2_True() {
    enableSsoWithOAuth2();
    disableSsoWithSaml();

    assertThat(ssoUserService.isSsoConfigured()).isTrue();
  }

  @Test
  public void testUpdateSsoUserAndGroups_Saml() {
    Set<String> groups = new LinkedHashSet<>(Arrays.asList("group1", "group2"));
    SamlUser samlUser = new SamlUser("username", "firstName", "lastName", "email@domain", groups);

    testWithSamlSso(() -> {
      ssoUserService.updateSsoUserAndGroups(SsoUser.fromSamlUser(samlUser), groups);

      assertSamlUserGroups(samlUser, groups, Collections.singleton("id"));
    });
  }

  @Test
  public void testUpdateSsoUserAndGroups_OAuth2() {
    testWithOAuth2Sso(() -> {
      Set<String> groups = new LinkedHashSet<>(Arrays.asList("group1", "group2"));
      OAuth2User oAuth2User = new OAuth2User("username", "firstName", "lastName", "email@domain", groups);
      ssoUserService.updateSsoUserAndGroups(SsoUser.fromOAuth2User(oAuth2User), groups);

      assertOAuth2UserGroups(oAuth2User, groups, "id", "groupsJson");
    });
  }

  @Test
  public void testGetSsoUsersByGroupName_Saml() {
    testWithSamlSso(() -> {
      SamlGroup samlGroup = tempEntity.newSamlGroup();
      SamlUser samlUser1 = tempEntity.newSamlUser("username1", "firstName", "lastName", "email@domain",
          Collections.singleton(samlGroup.getName()));
      SamlUser samlUser2 = tempEntity.newSamlUser("username2", "firstName", "lastName", "email@domain",
          Collections.singleton(samlGroup.getName()));
      SamlGroup otherSamlGroup = tempEntity.newSamlGroup();
      SamlUser otherSamlUser = tempEntity.newSamlUser("other", "firstName", "lastName", "email@domain",
          Collections.singleton(otherSamlGroup.getName()));
      tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup.getId());
      tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup.getId());
      tempEntity.newSamlUserGroup(otherSamlUser.getId(), otherSamlGroup.getId());

      List<SsoUser> users = ssoUserService.getSsoUsersByGroupName(samlGroup.getName());

      assertThat(users.stream().map(SsoUser::getId)).containsExactly(samlUser1.getId(), samlUser2.getId());
    });
  }

  @Test
  public void testGetSsoUsersByGroupName_Oauth2() {
    testWithOAuth2Sso(() -> {
      OAuth2Group oauth2Group = tempEntity.newOAuth2Group();
      OAuth2User oauth2User1 = tempEntity.newOAuth2User("username1", "firstName", "lastName", "email@domain",
          Collections.singleton(oauth2Group.getName()));
      OAuth2User oauth2User2 = tempEntity.newOAuth2User("username2", "firstName", "lastName", "email@domain",
          Collections.singleton(oauth2Group.getName()));
      OAuth2Group otherOAuth2Group = tempEntity.newOAuth2Group();
      OAuth2User otherOAuth2User = tempEntity.newOAuth2User("other", "firstName", "lastName", "email@domain",
          Collections.singleton(otherOAuth2Group.getName()));
      tempEntity.newOAuth2UserGroup(oauth2User1.getId(), oauth2Group.getId());
      tempEntity.newOAuth2UserGroup(oauth2User2.getId(), oauth2Group.getId());
      tempEntity.newOAuth2UserGroup(otherOAuth2User.getId(), otherOAuth2Group.getId());

      List<SsoUser> users = ssoUserService.getSsoUsersByGroupName(oauth2Group.getName());

      assertThat(users.stream().map(SsoUser::getId)).containsExactly(oauth2User1.getId(), oauth2User2.getId());
    });
  }

  @Test
  public void testFilterExistingSsoGroupNames_Saml() {
    testWithSamlSso(() -> {
      SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
      SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
      Set<String> groupNames =
          new LinkedHashSet<>(Arrays.asList("doesNotExist", samlGroup1.getName(), samlGroup2.getName()));

      assertThat(ssoUserService.filterExistingSsoGroupNames(groupNames)).containsExactly(samlGroup1.getName(),
          samlGroup2.getName());
    });
  }

  @Test
  public void testFilterExistingSsoGroupNames_OAuth2() {
    testWithOAuth2Sso(() -> {
      OAuth2Group oauth2Group1 = tempEntity.newOAuth2Group("group1");
      OAuth2Group oauth2Group2 = tempEntity.newOAuth2Group("group2");
      Set<String> groupNames =
          new LinkedHashSet<>(Arrays.asList("doesNotExist", oauth2Group1.getName(), oauth2Group2.getName()));

      assertThat(ssoUserService.filterExistingSsoGroupNames(groupNames)).containsExactly(oauth2Group1.getName(),
          oauth2Group2.getName());
    });
  }

  @Test
  public void testGetSsoGroupMembers_Saml() {
    testWithSamlSso(() -> {
      SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
      SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
      Set<String> groupNames =
          new LinkedHashSet<>(Arrays.asList("doesNotExist", samlGroup1.getName(), samlGroup2.getName()));

      List<Member> members = ssoUserService.getSsoGroupMembers(groupNames);

      assertThat(members.stream().map(Member::getInternalName)).containsExactly(samlGroup1.getName(),
          samlGroup2.getName());
    });
  }

  @Test
  public void testGetSsoGroupMembers_OAuth2() {
    testWithOAuth2Sso(() -> {
      OAuth2Group oauth2Group1 = tempEntity.newOAuth2Group("group1");
      OAuth2Group oauth2Group2 = tempEntity.newOAuth2Group("group2");
      Set<String> groupNames =
          new LinkedHashSet<>(Arrays.asList("doesNotExist", oauth2Group1.getName(), oauth2Group2.getName()));

      List<Member> members = ssoUserService.getSsoGroupMembers(groupNames);

      assertThat(members.stream().map(Member::getInternalName)).containsExactly(oauth2Group1.getName(),
          oauth2Group2.getName());
    });
  }

  @Test
  public void testGetSsoUsersByUsernames_Saml() {
    testWithSamlSso(() -> {
      SamlGroup samlGroup = tempEntity.newSamlGroup();
      SamlUser samlUser1 = tempEntity.newSamlUser("username1", "firstName", "lastName", "email@domain",
          Collections.singleton(samlGroup.getName()));
      SamlUser samlUser2 = tempEntity.newSamlUser("username2", "firstName", "lastName", "email@domain",
          Collections.singleton(samlGroup.getName()));
      tempEntity.newSamlUser("username3", "firstName", "lastName", "email@domain",
          Collections.singleton(samlGroup.getName()));
      Set<String> usernames = new HashSet<>(Arrays.asList("username1", "username2"));

      List<SsoUser> users = ssoUserService.getSsoUsersByUsernames(usernames);

      assertThat(users.stream().map(SsoUser::getId)).containsExactly(samlUser1.getId(), samlUser2.getId());
    });
  }

  @Test
  public void testGetSsoUsersByUsernames_OAuth2() {
    testWithOAuth2Sso(() -> {
      OAuth2Group oauth2Group = tempEntity.newOAuth2Group();
      OAuth2User oauth2User1 = tempEntity.newOAuth2User("username1", "firstName", "lastName", "email@domain",
          Collections.singleton(oauth2Group.getName()));
      OAuth2User oauth2User2 = tempEntity.newOAuth2User("username2", "firstName", "lastName", "email@domain",
          Collections.singleton(oauth2Group.getName()));
      tempEntity.newOAuth2User("username3", "firstName", "lastName", "email@domain",
          Collections.singleton(oauth2Group.getName()));
      Set<String> usernames = new HashSet<>(Arrays.asList("username1", "username2"));

      List<SsoUser> users = ssoUserService.getSsoUsersByUsernames(usernames);

      assertThat(users.stream().map(SsoUser::getId)).containsExactly(oauth2User1.getId(), oauth2User2.getId());
    });
  }

  @Test
  public void testGetSsoUsersByEmails_Saml() {
    testWithSamlSso(() -> {
      SamlGroup samlGroup = tempEntity.newSamlGroup();
      SamlUser samlUser1 = tempEntity.newSamlUser("username1", "firstName", "lastName", "username1@sonatype.com",
          Collections.singleton(samlGroup.getName()));
      SamlUser samlUser2 = tempEntity.newSamlUser("username2", "firstName", "lastName", "username2@sonatype.com",
          Collections.singleton(samlGroup.getName()));
      tempEntity.newSamlUser("username3", "firstName", "lastName", "username3@sonatype.com",
          Collections.singleton(samlGroup.getName()));
      Set<String> emails = Set.of("username1@sonatype.com", "username2@sonatype.com");

      List<SsoUser> users = ssoUserService.getSsoUsersByEmails(emails);

      assertThat(users.stream().map(SsoUser::getId)).containsExactly(samlUser1.getId(), samlUser2.getId());
    });
  }

  @Test
  public void testGetSsoUsersByEmails_OAuth2() {
    testWithOAuth2Sso(() -> {
      OAuth2Group oauth2Group = tempEntity.newOAuth2Group();
      OAuth2User oauth2User1 = tempEntity.newOAuth2User("username1", "firstName", "lastName", "username1@sonatype.com",
          Collections.singleton(oauth2Group.getName()));
      OAuth2User oauth2User2 = tempEntity.newOAuth2User("username2", "firstName", "lastName", "username2@sonatype.com",
          Collections.singleton(oauth2Group.getName()));
      tempEntity.newOAuth2User("username3", "firstName", "lastName", "username3@sonatype.com",
          Collections.singleton(oauth2Group.getName()));
      Set<String> emails = Set.of("username1@sonatype.com", "username2@sonatype.com");

      List<SsoUser> users = ssoUserService.getSsoUsersByEmails(emails);

      assertThat(users.stream().map(SsoUser::getId)).containsExactly(oauth2User1.getId(), oauth2User2.getId());
    });
  }

  @Test
  public void testGetSsoUsersByRealNames_Saml() {
    testWithSamlSso(() -> {
      SamlGroup samlGroup = tempEntity.newSamlGroup();
      SamlUser samlUser1 = tempEntity.newSamlUser("username1", "firstName1", "lastName1", null,
          Collections.singleton(samlGroup.getName()));
      SamlUser samlUser2 = tempEntity.newSamlUser("username2", "firstName2", "lastName2", null,
          Collections.singleton(samlGroup.getName()));
      tempEntity.newSamlUser("username3", "firstName3", "lastName3", null, Collections.singleton(samlGroup.getName()));
      Set<String> realNames = Set.of("firstName1 lastName1", "firstName2 lastName2");

      List<SsoUser> users = ssoUserService.getSsoUsersByRealNames(realNames);

      assertThat(users.stream().map(SsoUser::getId)).containsExactly(samlUser1.getId(), samlUser2.getId());
    });
  }

  @Test
  public void testGetSsoUsersByRealNames_OAuth2() {
    testWithOAuth2Sso(() -> {
      OAuth2Group oauth2Group = tempEntity.newOAuth2Group();
      OAuth2User oauth2User1 = tempEntity.newOAuth2User("username1", "firstName1", "lastName1", null,
          Collections.singleton(oauth2Group.getName()));
      OAuth2User oauth2User2 = tempEntity.newOAuth2User("username2", "firstName2", "lastName2", null,
          Collections.singleton(oauth2Group.getName()));
      tempEntity.newOAuth2User("username3", "firstName3", "lastName3", null,
          Collections.singleton(oauth2Group.getName()));
      Set<String> realNames = Set.of("firstName1 lastName1", "firstName2 lastName2");

      List<SsoUser> users = ssoUserService.getSsoUsersByRealNames(realNames);

      assertThat(users.stream().map(SsoUser::getId)).containsExactly(oauth2User1.getId(), oauth2User2.getId());
    });
  }

  @Test
  public void testFindSsoUsersByNameQuery_Saml() {
    testWithSamlSso(() -> {
      SamlGroup samlGroup = tempEntity.newSamlGroup();
      SamlUser samlUser1 = tempEntity.newSamlUser("username1", "firstName", "lastName", "email@domain",
          Collections.singleton(samlGroup.getName()));
      String nameQuery = "username1";

      List<SsoUser> users = ssoUserService.findSsoUsersByNameOrUsernameQuery(nameQuery);

      assertThat(users.stream().map(SsoUser::getId)).containsExactly(samlUser1.getId());
    });
  }

  @Test
  public void testFindSsoUsersByNameQuery_OAuth2() {
    testWithOAuth2Sso(() -> {
      OAuth2Group oauth2Group = tempEntity.newOAuth2Group();
      OAuth2User oauthUser1 = tempEntity.newOAuth2User("username1", "firstName", "lastName", "email@domain",
          Collections.singleton(oauth2Group.getName()));
      String nameQuery = "username1";

      List<SsoUser> users = ssoUserService.findSsoUsersByNameOrUsernameQuery(nameQuery);

      assertThat(users.stream().map(SsoUser::getId)).containsExactly(oauthUser1.getId());
    });
  }

  @Test
  public void testFindSsoGroupsByNameQuery_Saml() {
    testWithSamlSso(() -> {
      SamlGroup samlGroup = tempEntity.newSamlGroup("my-group");
      String nameQuery = "my-group";

      List<SsoGroup> groups = ssoUserService.findSsoGroupsByNameQuery(nameQuery);

      assertThat(groups.stream().map(SsoGroup::getId)).containsExactly(samlGroup.getId());
    });
  }

  @Test
  public void testFindSsoGroupsByNameQuery_OAuth2() {
    testWithOAuth2Sso(() -> {
      OAuth2Group oauth2Group = tempEntity.newOAuth2Group("my-group");
      String nameQuery = "my-group";

      List<SsoGroup> groups = ssoUserService.findSsoGroupsByNameQuery(nameQuery);

      assertThat(groups.stream().map(SsoGroup::getId)).containsExactly(oauth2Group.getId());
    });
  }

  @Test
  public void testSetSsoGroupMembersByNameQuery_Saml() {
    testWithSamlSso(() -> {
      SamlGroup samlGroup = tempEntity.newSamlGroup("my-group");
      String nameQuery = "my-group";

      List<Member> members = ssoUserService.getSsoGroupMembersByNameQuery(nameQuery);

      assertThat(members.stream().map(Member::getInternalName)).containsExactly(samlGroup.getName());
    });
  }

  @Test
  public void testSetSsoGroupMembersByNameQuery_OAuth2() {
    testWithOAuth2Sso(() -> {
      OAuth2Group oauth2Group = tempEntity.newOAuth2Group("my-group");
      String nameQuery = "my-group";

      List<Member> members = ssoUserService.getSsoGroupMembersByNameQuery(nameQuery);

      assertThat(members.stream().map(Member::getInternalName)).containsExactly(oauth2Group.getName());
    });
  }

  @Test
  public void testDeleteUser_Saml() {
    testWithSamlSso(() -> {
      SamlUser samlUser = tempEntity.newSamlUser();

      ssoUserService.deleteSsoUser(SsoUser.fromSamlUser(samlUser));

      assertThat(samlUserDAO.getByUsername(samlUser.getUsername())).isNull();
    });
  }

  @Test
  public void testDeleteUser_OAuth2() {
    testWithOAuth2Sso(() -> {
      OAuth2User oauth2User = tempEntity.newOAuth2User();

      ssoUserService.deleteSsoUser(SsoUser.fromOAuth2User(oauth2User));

      assertThat(samlUserDAO.getByUsername(oauth2User.getUsername())).isNull();
    });
  }

  @Test
  public void testGetByUsernameNotNull_Saml() {
    testWithSamlSso(() -> {
      SamlUser samlUser = tempEntity.newSamlUser();

      assertThat(ssoUserService.getByUsernameNotNull(samlUser.getUsername())).usingRecursiveComparison()
          .ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(SsoUser.fromSamlUser(samlUser));
    });
  }

  @Test
  public void testGetByUsernameNotNull_OAuth2() {
    testWithOAuth2Sso(() -> {
      OAuth2User oauth2User = tempEntity.newOAuth2User();

      assertThat(ssoUserService.getByUsernameNotNull(oauth2User.getUsername())).usingRecursiveComparison()
          .ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(SsoUser.fromOAuth2User(oauth2User));
    });
  }

  @Test
  public void testGetAll_Saml() {
    testWithSamlSso(() -> {
      SamlUser samlUser1 = tempEntity.newSamlUser();
      SamlUser samlUser2 = tempEntity.newSamlUser();

      List<SsoUser> users = ssoUserService.getAll();
      assertThat(users).hasSize(2);
      assertThat(users.stream().map(SsoUser::getId)).contains(samlUser1.getId(), samlUser2.getId());
    });
  }

  @Test
  public void testGetAll_OAuth2() {
    testWithOAuth2Sso(() -> {
      OAuth2User oauth2User1 = tempEntity.newOAuth2User();
      OAuth2User oauth2User2 = tempEntity.newOAuth2User();

      List<SsoUser> users = ssoUserService.getAll();
      assertThat(users).hasSize(2);
      assertThat(users.stream().map(SsoUser::getId)).contains(oauth2User1.getId(), oauth2User2.getId());
    });
  }

  @Test
  public void testGetByUsername_Saml() {
    testWithSamlSso(() -> {
      String username = "username1";
      SamlGroup samlGroup = tempEntity.newSamlGroup();
      SamlUser samlUser1 = tempEntity.newSamlUser(username, "firstName", "lastName", "email@domain",
          Collections.singleton(samlGroup.getName()));

      SsoUser user = ssoUserService.getByUsername(username);

      assertThat(user.getId()).isEqualTo(samlUser1.getId());
      assertThat(user.getUsername()).isEqualTo(samlUser1.getUsername());
      assertThat(user.getGroups()).isEqualTo(samlUser1.getGroups());
    });
  }

  @Test
  public void testGetByUsername_OAuth2() {
    testWithOAuth2Sso(() -> {
      String username = "username1";
      OAuth2Group oauth2Group = tempEntity.newOAuth2Group();
      OAuth2User oauth2User = tempEntity.newOAuth2User(username, "firstName", "lastName", "email@domain",
          Collections.singleton(oauth2Group.getName()));

      SsoUser user = ssoUserService.getByUsername(username);

      assertThat(user.getId()).isEqualTo(oauth2User.getId());
      assertThat(user.getUsername()).isEqualTo(oauth2User.getUsername());
      assertThat(user.getGroups()).isEqualTo(oauth2User.getGroups());
    });
  }

  @Test
  public void testSyncSsoProviderDataSources() {
    String samlUsername = "samlUserName";
    String samlGroupName1 = "samlGroupName1";
    String samlGroupName2 = "samlGroupName2";
    Set<String> samlUserGroups = new HashSet<>(Arrays.asList(samlGroupName1, samlGroupName2));

    String oAuth2Username = "oAuth2UserName";
    String oAuth2GroupName1 = "oAuth2GroupName1";
    String oAuth2GroupName2 = "oAuth2GroupName2";
    Set<String> oAuth2UserGroups = new HashSet<>(Arrays.asList(oAuth2GroupName1, oAuth2GroupName2));

    // Create SAML User
    SamlUser samlUser =
        tempEntity.newSamlUser(samlUsername, samlUserGroups);
    SamlGroup samlGroup1 = tempEntity.newSamlGroup(samlGroupName1);
    SamlGroup samlGroup2 = tempEntity.newSamlGroup(samlGroupName2);
    tempEntity.newSamlUserGroup(samlUser.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser.getId(), samlGroup2.getId());

    // Create OAuth2 User
    OAuth2User oAuth2User =
        tempEntity.newOAuth2User(oAuth2Username, oAuth2UserGroups);
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group(oAuth2GroupName1);
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group(oAuth2GroupName2);
    tempEntity.newOAuth2UserGroup(oAuth2User.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User.getId(), oAuth2Group2.getId());

    // Confirm OAuth2 and SAML data sources are not synced
    assertThat(samlUserDAO.getByUsername(oAuth2Username)).isNull();
    assertThat(oAuth2UserDAO.getByUsername(samlUsername)).isNull();

    // Sync data sources
    ssoUserService.syncSsoProviderDataSources();

    // Confirm OAuth2 and SAML data sources are synced
    assertSamlUserExistsAndIsTheExpected(samlUsername, samlUserGroups);
    assertSamlUserExistsAndIsTheExpected(oAuth2Username, oAuth2UserGroups);
    assertOAuth2UserExistsAndIsTheExpected(samlUsername, samlUserGroups);
    assertOAuth2UserExistsAndIsTheExpected(oAuth2Username, oAuth2UserGroups);
  }

  private void assertSamlUserExistsAndIsTheExpected(final String username, final Set<String> samlUserGroups) {
    SamlUser user = samlUserDAO.getByUsername(username);
    assertThat(user).isNotNull();
    assertThat(user.getGroups()).containsAll(samlUserGroups);
  }

  private void assertOAuth2UserExistsAndIsTheExpected(final String username, final Set<String> samlUserGroups) {
    OAuth2User user = oAuth2UserDAO.getByUsername(username);
    assertThat(user).isNotNull();
    assertThat(user.getGroups()).containsAll(samlUserGroups);
  }

  private void assertSamlUserGroups(
      SamlUser expectedSamlUser,
      Set<String> expectedGroupNames,
      Set<String> extraIgnoreFields)
  {
    SamlUser samlUser = samlUserDAO.getByUsername(expectedSamlUser.getUsername());
    assertThat(samlUser).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringFields("id")
        .isEqualTo(expectedSamlUser);
    List<String> ignoreFields = new ArrayList<>(Arrays.asList(JPA.IGNORE_FIELDS));
    ignoreFields.addAll(extraIgnoreFields);
    List<SamlGroup> samlGroups = samlGroupDAO.getAll();
    assertThat(samlGroups)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(ignoreFields.toArray(new String[0]))
        .containsExactlyElementsOf(expectedGroupNames.stream().map(SamlGroup::new).collect(Collectors.toList()));
    assertThat(samlUserGroupDAO.getAll())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(ignoreFields.toArray(new String[0]))
        .containsExactlyInAnyOrderElementsOf(samlGroups.stream().map(samlGroup ->
            new SamlUserGroup(samlUser.getId(), samlGroup.getId())).collect(Collectors.toList()));
  }

  private void assertOAuth2UserGroups(
      OAuth2User expectedOAuth2User,
      Set<String> expectedGroupNames,
      String... extraFieldsToIgnore)
  {
    OAuth2User oAuth2User = oAuth2UserDAO.getByUsername(expectedOAuth2User.getUsername());

    assertThat(oAuth2User).usingRecursiveComparison()
        .ignoringFields(ignoreFields(extraFieldsToIgnore))
        .isEqualTo(expectedOAuth2User);

    List<OAuth2Group> oAuth2Groups = oAuth2GroupDAO.getAll();
    List<OAuth2Group> expectedOAuth2Groups =
        expectedGroupNames.stream().map(OAuth2Group::new).collect(Collectors.toList());
    assertThat(oAuth2Groups)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(ignoreFields(extraFieldsToIgnore))
        .containsExactlyElementsOf(expectedOAuth2Groups);

    List<OAuth2UserGroup> oAuth2UserGroups = oAuth2UserGroupDAO.getAll();
    List<OAuth2UserGroup> expectedOAuth2UserGroups =
        oAuth2Groups.stream().map(group -> new OAuth2UserGroup(oAuth2User.getId(), group.getId()))
            .collect(Collectors.toList());
    assertThat(oAuth2UserGroups)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(ignoreFields(extraFieldsToIgnore))
        .containsExactlyInAnyOrderElementsOf(expectedOAuth2UserGroups);
  }

  private String[] ignoreFields(String... fields) {
    return (String[]) ArrayUtils.addAll(JPA.IGNORE_FIELDS, fields);
  }

  private void testWithSamlSso(Runnable test) {
    enableSsoWithSaml();
    disableSsoWithOAuth2();
    test.run();
  }

  private void testWithOAuth2Sso(Runnable test) {
    enableSsoWithOAuth2();
    disableSsoWithSaml();
    test.run();
  }
}
