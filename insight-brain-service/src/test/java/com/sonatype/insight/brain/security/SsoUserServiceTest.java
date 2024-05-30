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
import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlConfigurationDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserGroupDAO;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.SamlUserGroup;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SsoUserServiceTest
    extends AbstractComponentTest
{
  @Inject
  private SsoUserService ssoUserService;

  @Inject
  private SamlConfigurationDAO samlConfigurationDAO;

  @Inject
  private SamlUserDAO samlUserDAO;

  @Inject
  private SamlGroupDAO samlGroupDAO;

  @Inject
  private SamlUserGroupDAO samlUserGroupDAO;

  @Test
  public void testIsSsoRealm_False() {
    assertThat(ssoUserService.isSsoRealm(InternalRealm.ID)).isFalse();
  }

  @Test
  public void testIsSsoRealm_True() {
    assertThat(ssoUserService.isSsoRealm(SamlRealm.ID)).isTrue();
  }

  @Test
  public void testIsSsoConfigured_Saml_False() {
    samlConfigurationDAO.delete();

    assertThat(ssoUserService.isSsoConfigured()).isFalse();
  }

  @Test
  public void testIsSsoConfigured_Saml_True() {
    tempEntity.newSamlConfiguration();

    assertThat(ssoUserService.isSsoConfigured()).isTrue();
  }

  @Test
  public void testUpdateSsoUserAndGroups_Saml() {
    Set<String> groups = new LinkedHashSet<>(Arrays.asList("group1", "group2"));
    SamlUser samlUser = new SamlUser("username", "firstName", "lastName", "email@domain", groups);

    ssoUserService.updateSsoUserAndGroups(SsoUser.fromSamlUser(samlUser), groups);

    assertSamlUserGroups(samlUser, groups, Collections.singleton("id"));
  }

  @Test
  public void testGetSsoUsersByGroupName_Saml() {
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
  }

  @Test
  public void testFilterExistingSsoGroupNames_Saml() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    Set<String> groupNames =
        new LinkedHashSet<>(Arrays.asList("doesNotExist", samlGroup1.getName(), samlGroup2.getName()));

    assertThat(ssoUserService.filterExistingSsoGroupNames(groupNames)).containsExactly(samlGroup1.getName(),
        samlGroup2.getName());
  }

  @Test
  public void testGetSsoGroupMembers_Saml() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    Set<String> groupNames =
        new LinkedHashSet<>(Arrays.asList("doesNotExist", samlGroup1.getName(), samlGroup2.getName()));

    List<Member> members = ssoUserService.getSsoGroupMembers(groupNames);

    assertThat(members.stream().map(Member::getInternalName)).containsExactly(samlGroup1.getName(),
        samlGroup2.getName());
  }

  @Test
  public void testGetSsoUsersByUsernames_Saml() {
    SamlGroup samlGroup = tempEntity.newSamlGroup();
    SamlUser samlUser1 = tempEntity.newSamlUser("username1", "firstName", "lastName", "email@domain",
        Collections.singleton(samlGroup.getName()));
    SamlUser samlUser2 = tempEntity.newSamlUser("username2", "firstName", "lastName", "email@domain",
        Collections.singleton(samlGroup.getName()));
    Set<String> usernames = new HashSet<>(Arrays.asList("username1", "username2"));

    List<SsoUser> users = ssoUserService.getSsoByUsernames(usernames);

    assertThat(users.stream().map(SsoUser::getId)).containsExactly(samlUser1.getId(), samlUser2.getId());
  }

  @Test
  public void testFindSsoUsersByNameQuery_Saml() {
    SamlGroup samlGroup = tempEntity.newSamlGroup();
    SamlUser samlUser1 = tempEntity.newSamlUser("username1", "firstName", "lastName", "email@domain",
        Collections.singleton(samlGroup.getName()));
    String nameQuery = "username1";

    List<SsoUser> users = ssoUserService.findSsoUsersByNameOrUsernameQuery(nameQuery);

    assertThat(users.stream().map(SsoUser::getId)).containsExactly(samlUser1.getId());
  }

  @Test
  public void testFindSsoGroupsByNameQuery_Saml() {
    SamlGroup samlGroup = tempEntity.newSamlGroup("my-group");
    String nameQuery = "my-group";

    ssoUserService.findSsoGroupsByNameQuery(nameQuery);

    List<SsoGroup> groups = ssoUserService.findSsoGroupsByNameQuery(nameQuery);

    assertThat(groups.stream().map(SsoGroup::getId)).containsExactly(samlGroup.getId());
  }

  @Test
  public void testSetSsoGroupMembersByNameQuery_Saml() {
    SamlGroup samlGroup = tempEntity.newSamlGroup("my-group");
    String nameQuery = "my-group";

    List<Member> members = ssoUserService.getSsoGroupMembersByNameQuery(nameQuery);

    assertThat(members.stream().map(Member::getInternalName)).containsExactly(samlGroup.getName());
  }

  @Test
  public void testDeleteUser_Saml() {
    SamlUser samlUser = tempEntity.newSamlUser();

    ssoUserService.deleteSsoUser(SsoUser.fromSamlUser(samlUser));

    assertThat(samlUserDAO.getByUsername(samlUser.getUsername())).isNull();
  }

  @Test
  public void testGetByUsernameNotNull_Saml() {
    SamlUser samlUser = tempEntity.newSamlUser();

    assertThat(ssoUserService.getByUsernameNotNull(samlUser.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS).isEqualTo(SsoUser.fromSamlUser(samlUser));
  }

  @Test
  public void testGetAll_Saml() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();

    List<SsoUser> users = ssoUserService.getAll();
    assertThat(users).hasSize(2);
    assertThat(users.stream().map(SsoUser::getId)).contains(samlUser1.getId(), samlUser2.getId());
  }

  @Test
  public void testGetByUsername_Saml() {
    String username = "username1";
    SamlGroup samlGroup = tempEntity.newSamlGroup();
    SamlUser samlUser1 = tempEntity.newSamlUser(username, "firstName", "lastName", "email@domain",
        Collections.singleton(samlGroup.getName()));

    SsoUser user = ssoUserService.getByUsername(username);

    assertThat(user.getId()).isEqualTo(samlUser1.getId());
    assertThat(user.getUsername()).isEqualTo(samlUser1.getUsername());
    assertThat(user.getGroups()).isEqualTo(samlUser1.getGroups());
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
}
