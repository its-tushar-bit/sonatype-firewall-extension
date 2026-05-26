/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.security.OAuth2GroupDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserDAO;
import com.sonatype.insight.brain.dataaccess.security.OAuth2UserGroupDAO;
import com.sonatype.insight.brain.model.security.OAuth2Group;
import com.sonatype.insight.brain.model.security.OAuth2User;
import com.sonatype.insight.brain.model.security.OAuth2UserGroup;
import com.sonatype.insight.brain.security.oauth2.OAuth2Realm;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang.ArrayUtils;
import org.junit.Test;

public class OAuth2SsoUserProviderTest
    extends AbstractComponentTest
{
  public static final String ID_FIELD = "id";

  public static final String GROUPS_JSON_FIELD = "groupsJson";

  @Inject
  private OAuth2SsoUserProvider oAuth2SsoUserProvider;

  @Inject
  private OAuth2UserDAO oAuth2UserDAO;

  @Inject
  private OAuth2GroupDAO oAuth2GroupDAO;

  @Inject
  private OAuth2UserGroupDAO oAuth2UserGroupDAO;

  private OAuth2UserDAO spyOAuth2UserDAO;

  @Override
  protected List<BeanFieldOverride> getBeanFieldOverrides() {
    spyOAuth2UserDAO = spy(oAuth2UserDAO);
    return Collections.singletonList(beanFieldOverride(OAuth2SsoUserProvider.class, "oAuth2UserDAO", spyOAuth2UserDAO));
  }

  @Test
  public void testGetSsoRealm() {
    assertThat(oAuth2SsoUserProvider.getSsoRealm()).isEqualTo(OAuth2Realm.ID);
  }

  @Test
  public void testIsSsoRealm_False() {
    assertThat(oAuth2SsoUserProvider.isSsoRealm(InternalRealm.ID)).isFalse();
  }

  @Test
  public void testIsSsoRealm_True() {
    assertThat(oAuth2SsoUserProvider.isSsoRealm(OAuth2Realm.ID)).isTrue();
  }

  @Test
  public void testIsSamlConfigured_False() {
    assertThat(oAuth2SsoUserProvider.isSsoConfigured()).isFalse();
  }

  @Test
  public void testIsSamlConfigured_True() {
    tempEntity.newOAuth2Configuration();

    assertThat(oAuth2SsoUserProvider.isSsoConfigured()).isTrue();
  }

  @Test
  public void testUpdateOAuth2UserAndGroups_NewOAuth2User_NewGroups() {
    Set<String> groups = new LinkedHashSet<>(Arrays.asList("group1", "group2"));
    OAuth2User oAuth2User = new OAuth2User("username", "firstName", "lastName", "email@domain", groups);

    oAuth2SsoUserProvider.updateSsoUserAndGroups(SsoUser.fromOAuth2User(oAuth2User), groups);

    assertOAuth2UserGroups(oAuth2User, groups, ID_FIELD, GROUPS_JSON_FIELD);
  }

  @Test
  public void testUpdateOAuth2UserAndGroups_ExistingOAuth2User_NewGroups() {
    OAuth2User oAuth2User =
        tempEntity.newOAuth2User("username", "firstName", "lastName", "email@domain", Collections.emptySet());
    Set<String> groups = new LinkedHashSet<>(Arrays.asList("group1", "group2"));
    oAuth2User.setGroups(groups);

    oAuth2SsoUserProvider.updateSsoUserAndGroups(SsoUser.fromOAuth2User(oAuth2User), groups);

    assertOAuth2UserGroups(oAuth2User, groups, ID_FIELD, GROUPS_JSON_FIELD);
  }

  @Test
  public void testUpdateOAuth2UserAndGroups_NewOAuth2User_ExistingGroups() {
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group("group1");
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group("group2");
    Set<String> groups = new LinkedHashSet<>(Arrays.asList(oAuth2Group1.getName(), oAuth2Group2.getName()));
    OAuth2User oAuth2User = new OAuth2User("username", "firstName", "lastName", "email@domain", groups);

    oAuth2SsoUserProvider.updateSsoUserAndGroups(SsoUser.fromOAuth2User(oAuth2User), groups);

    assertOAuth2UserGroups(oAuth2User, groups, ID_FIELD, GROUPS_JSON_FIELD);
  }

  @Test
  public void testUpdateOAuth2UserAndGroups_ExistingOAuth2User_ExistingGroups() {
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group("group1");
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group("group2");
    Set<String> groups = new LinkedHashSet<>(Arrays.asList(oAuth2Group1.getName(), oAuth2Group2.getName()));
    OAuth2User oAuth2User = tempEntity.newOAuth2User("username", "firstName", "lastName", "email@domain", groups);
    tempEntity.newOAuth2UserGroup(oAuth2User.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User.getId(), oAuth2Group2.getId());

    oAuth2SsoUserProvider.updateSsoUserAndGroups(SsoUser.fromOAuth2User(oAuth2User), groups);

    assertOAuth2UserGroups(oAuth2User, groups, ID_FIELD, GROUPS_JSON_FIELD);
  }

  @Test
  public void testUpdateOAuth2UserAndGroups_ExistingOAuth2User_ExistingAndNewGroups() {
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group("group1");
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group("group2");
    Set<String> groups = new LinkedHashSet<>(Arrays.asList(oAuth2Group1.getName(), oAuth2Group2.getName()));
    OAuth2User oAuth2User = tempEntity.newOAuth2User("username", "firstName", "lastName", "email@domain", groups);
    tempEntity.newOAuth2UserGroup(oAuth2User.getId(), oAuth2Group1.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User.getId(), oAuth2Group2.getId());
    groups.add("group3");
    oAuth2User.setGroups(groups);

    oAuth2SsoUserProvider.updateSsoUserAndGroups(SsoUser.fromOAuth2User(oAuth2User), groups);

    assertOAuth2UserGroups(oAuth2User, groups, ID_FIELD, GROUPS_JSON_FIELD);
  }

  @Test
  public void testUpdateOAuth2UserAndGroups_NewOAuth2User_ExistingAndNewGroups() {
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group("group1");
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group("group2");
    Set<String> groups = new LinkedHashSet<>(Arrays.asList(oAuth2Group1.getName(), oAuth2Group2.getName(), "group3"));
    OAuth2User oAuth2User = new OAuth2User("username", "firstName", "lastName", "email@domain", groups);

    oAuth2SsoUserProvider.updateSsoUserAndGroups(SsoUser.fromOAuth2User(oAuth2User), groups);

    assertOAuth2UserGroups(oAuth2User, groups, ID_FIELD, GROUPS_JSON_FIELD);
  }

  @Test
  public void testUpdateOAuth2UserAndGroups_RemovesGroupsWithNoMembers() {
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group("group1");
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group("group2");
    OAuth2Group oAuth2Group3 = tempEntity.newOAuth2Group("group3");
    OAuth2User oAuth2User1 =
        tempEntity.newOAuth2User("username1", "firstName", "lastName", "email@domain",
            new LinkedHashSet<>(Arrays.asList(oAuth2Group1.getName(), oAuth2Group3.getName())));
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("username2", "firstName", "lastName", "email@domain",
        new LinkedHashSet<>(Arrays.asList(oAuth2Group2.getName(), oAuth2Group3.getName())));
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group1.getId());
    OAuth2UserGroup oAuth2UserGroup22 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group2.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group3.getId());
    OAuth2UserGroup oAuth2UserGroup23 = tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group3.getId());
    oAuth2User1.setGroups(Collections.emptySet());

    oAuth2SsoUserProvider.updateSsoUserAndGroups(SsoUser.fromOAuth2User(oAuth2User1), Collections.emptySet());

    assertThat(oAuth2UserDAO.getAll())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(ignoreFields(GROUPS_JSON_FIELD))
        .containsExactlyInAnyOrder(oAuth2User1, oAuth2User2);
    assertThat(oAuth2UserGroupDAO.getAll())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(JPA.IGNORE_FIELDS)
        .containsExactlyInAnyOrder(oAuth2UserGroup22, oAuth2UserGroup23);
    assertThat(oAuth2GroupDAO.getAll())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(JPA.IGNORE_FIELDS)
        .containsExactly(oAuth2Group2, oAuth2Group3);
  }

  @Test
  public void testGetOAuth2UsersByGroupName_GroupDoesNotExist() {
    assertThat(oAuth2SsoUserProvider.getSsoUsersByGroupName("doesNotExist")).isEmpty();
  }

  @Test
  public void testGetOAuth2UsersByGroupName_GroupExists_NoUsers() {
    OAuth2Group oAuth2Group = tempEntity.newOAuth2Group();

    assertThat(oAuth2SsoUserProvider.getSsoUsersByGroupName(oAuth2Group.getName())).isEmpty();
  }

  @Test
  public void testGetOAuth2UsersByGroupName_GroupExists_SomeUsers() {
    OAuth2Group oAuth2Group = tempEntity.newOAuth2Group();
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("username1", "firstName", "lastName", "email@domain",
        Collections.singleton(oAuth2Group.getName()));
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("username2", "firstName", "lastName", "email@domain",
        Collections.singleton(oAuth2Group.getName()));
    OAuth2Group otherOAuth2Group = tempEntity.newOAuth2Group();
    OAuth2User otherOAuth2User = tempEntity.newOAuth2User("other", "firstName", "lastName", "email@domain",
        Collections.singleton(otherOAuth2Group.getName()));
    tempEntity.newOAuth2UserGroup(oAuth2User1.getId(), oAuth2Group.getId());
    tempEntity.newOAuth2UserGroup(oAuth2User2.getId(), oAuth2Group.getId());
    tempEntity.newOAuth2UserGroup(otherOAuth2User.getId(), otherOAuth2Group.getId());

    assertThat(oAuth2SsoUserProvider.getSsoUsersByGroupName(oAuth2Group.getName()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(SsoUser.fromOAuth2User(oAuth2User1), SsoUser.fromOAuth2User(oAuth2User2));
  }

  @Test
  public void testFilterExistingOAuth2GroupNames_Empty() {
    assertThat(oAuth2SsoUserProvider.filterExistingSsoGroupNames(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testFilterExistingOAuth2GroupNames() {
    OAuth2Group oAuth2Group1 = tempEntity.newOAuth2Group("group1");
    OAuth2Group oAuth2Group2 = tempEntity.newOAuth2Group("group2");
    Set<String> groupNames =
        new LinkedHashSet<>(Arrays.asList("doesNotExist", oAuth2Group1.getName(), oAuth2Group2.getName()));

    assertThat(oAuth2SsoUserProvider.filterExistingSsoGroupNames(groupNames)).containsExactly(oAuth2Group1.getName(),
        oAuth2Group2.getName());
  }

  @Test
  public void testGetOAuth2UsersByUsernames() {
    Set<String> usernames = new HashSet<>(Arrays.asList("username1", "username2"));

    oAuth2SsoUserProvider.getSsoUsersByUsernames(usernames);

    verify(spyOAuth2UserDAO).getByUsernames(usernames);
  }

  @Test
  public void testGetSsoUsersByEmails() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", null, null, "usera@sonatype.com", null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB", null, null, "userb@sonatype.com", null);
    tempEntity.newOAuth2User();

    Set<String> emails = Set.of(oAuth2User1.getEmail(), oAuth2User2.getEmail());

    List<SsoUser> result = oAuth2SsoUserProvider.getSsoUsersByEmails(emails);

    assertThat(result).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(SsoUser.fromOAuth2User(oAuth2User1), SsoUser.fromOAuth2User(oAuth2User2));
  }

  @Test
  public void testGetSsoUsersByRealNames() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User("userA", "Mark", "Mywords", null, null);
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User("userB", "Justin", "Time", null, null);
    tempEntity.newOAuth2User();

    Set<String> realNames = Set.of("Mark Mywords", "Justin Time");

    List<SsoUser> result = oAuth2SsoUserProvider.getSsoUsersByRealNames(realNames);

    assertThat(result).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(SsoUser.fromOAuth2User(oAuth2User1), SsoUser.fromOAuth2User(oAuth2User2));
  }

  @Test
  public void testFindOAuth2UsersByNameQuery() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();

    List<SsoUser> users = oAuth2SsoUserProvider.findSsoUsersByNameOrUsernameQuery(oAuth2User.getUsername());

    assertOAuth2User(oAuth2User, users);
  }

  @Test
  public void testFindOAuth2GroupsByNameQuery() {
    OAuth2Group oAuth2Group = tempEntity.newOAuth2Group();

    List<SsoGroup> groups = oAuth2SsoUserProvider.findSsoGroupsByNameQuery(oAuth2Group.getName());

    assertOAuth2Group(oAuth2Group, groups);
  }

  @Test
  public void testDeleteUser() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();

    oAuth2SsoUserProvider.deleteSsoUser(SsoUser.fromOAuth2User(oAuth2User));

    assertThat(oAuth2UserDAO.getByUsername(oAuth2User.getUsername())).isNull();
  }

  @Test
  public void testGetByUsername() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();
    tempEntity.newOAuth2User();

    assertThat(oAuth2SsoUserProvider.getByUsername(oAuth2User.getUsername())).usingRecursiveComparison()
        .ignoringFields(ignoreFields(GROUPS_JSON_FIELD))
        .isEqualTo(SsoUser.fromOAuth2User(oAuth2User));
  }

  @Test
  public void testUpsertByUsername_Insert() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();

    oAuth2SsoUserProvider.upsertByUsername(SsoUser.fromOAuth2User(oAuth2User));

    assertThat(oAuth2UserDAO.getByUsername(oAuth2User.getUsername())).usingRecursiveComparison()
        .ignoringFields(ignoreFields(GROUPS_JSON_FIELD))
        .isEqualTo(oAuth2User);
  }

  @Test
  public void testUpsertByUsername_Update() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();
    oAuth2User.setFirstName(oAuth2User.getFirstName() + "2");
    oAuth2User.setLastName(oAuth2User.getLastName() + "2");
    oAuth2User.setEmail(oAuth2User.getEmail() + "2");
    oAuth2User.setGroups(new LinkedHashSet<>(Arrays.asList("someGroup3", "someGroup4")));

    oAuth2SsoUserProvider.upsertByUsername(SsoUser.fromOAuth2User(oAuth2User));

    assertThat(oAuth2UserDAO.getByUsername(oAuth2User.getUsername())).usingRecursiveComparison()
        .ignoringFields(ignoreFields(GROUPS_JSON_FIELD))
        .isEqualTo(oAuth2User);
  }

  @Test
  public void testGetByUsernameNotNull_Exists() {
    OAuth2User oAuth2User = tempEntity.newOAuth2User();

    assertThat(oAuth2SsoUserProvider.getByUsernameNotNull(oAuth2User.getUsername())).usingRecursiveComparison()
        .ignoringFields(ignoreFields(GROUPS_JSON_FIELD))
        .isEqualTo(SsoUser.fromOAuth2User(oAuth2User));
  }

  @Test
  public void testGetByUsernameNotNull_DoesNotExist() {
    String username = "doesNotExist";

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> oAuth2SsoUserProvider.getByUsernameNotNull(username))
        .withMessageContaining("Cannot find a OAuth2 user with username " + username + ".");
  }

  @Test
  public void testGetAll() {
    OAuth2User oAuth2User1 = tempEntity.newOAuth2User();
    OAuth2User oAuth2User2 = tempEntity.newOAuth2User();

    List<SsoUser> users = oAuth2SsoUserProvider.getAll();
    assertThat(users).hasSize(2);
    assertOAuth2User(oAuth2User1, users);
    assertOAuth2User(oAuth2User2, users);
  }

  private void assertOAuth2User(OAuth2User expectedOAuth2User, List<SsoUser> users) {
    SsoUser foundUser = users.stream()
        .filter(oAuth2User -> expectedOAuth2User.getUsername().equals(oAuth2User.getUsername()))
        .findFirst()
        .orElse(null);
    assertThat(foundUser).isNotNull()
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(SsoUser.fromOAuth2User(expectedOAuth2User));
  }

  private void assertOAuth2Group(OAuth2Group expectedOAuth2Group, List<SsoGroup> groups) {
    SsoGroup foundGroup = groups.stream()
        .filter(oAuth2User -> expectedOAuth2Group.getId().equals(oAuth2User.getId()))
        .findFirst()
        .orElse(null);
    assertThat(foundGroup).isNotNull()
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(SsoGroup.fromOAuth2Group(expectedOAuth2Group));
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
        oAuth2Groups.stream()
            .map(group -> new OAuth2UserGroup(oAuth2User.getId(), group.getId()))
            .collect(Collectors.toList());
    assertThat(oAuth2UserGroups)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(ignoreFields(extraFieldsToIgnore))
        .containsExactlyInAnyOrderElementsOf(expectedOAuth2UserGroups);
  }

  private String[] ignoreFields(String... fields) {
    return (String[]) ArrayUtils.addAll(JPA.IGNORE_FIELDS, fields);
  }
}
