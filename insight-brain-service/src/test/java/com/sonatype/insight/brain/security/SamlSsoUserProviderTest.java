/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.verify;

import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.security.SamlGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserGroupDAO;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.SamlUserGroup;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.error.exception.NotFoundException;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.Test;

public class SamlSsoUserProviderTest
    extends AbstractComponentTest
{
  @Inject
  private SamlSsoUserProvider samlSsoUserProvider;

  @Inject
  private SamlConfigurationService samlConfigurationService;

  @Inject
  private SamlUserDAO samlUserDAO;

  @Inject
  private SamlGroupDAO samlGroupDAO;

  @Inject
  private SamlUserGroupDAO samlUserGroupDAO;

  private SamlUserDAO spySamlUserDAO;

  private SamlGroupDAO spySamlGroupDAO;

  @Test
  public void testGetSsoRealm() {
    assertThat(samlSsoUserProvider.getSsoRealm()).isEqualTo(SamlRealm.ID);
  }

  @Test
  public void testIsSsoRealm_False() {
    assertThat(samlSsoUserProvider.isSsoRealm(InternalRealm.ID)).isFalse();
  }

  @Test
  public void testIsSsoRealm_True() {
    assertThat(samlSsoUserProvider.isSsoRealm(SamlRealm.ID)).isTrue();
  }

  @Test
  public void testIsSamlConfigured_False() {
    samlConfigurationService.delete();

    assertThat(samlSsoUserProvider.isSsoConfigured()).isFalse();
  }

  @Test
  public void testIsSamlConfigured_True() {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration());

    assertThat(samlSsoUserProvider.isSsoConfigured()).isTrue();
  }

  @Test
  public void testUpdateSamlUserAndGroups_NewSamlUser_NewGroups() {
    Set<String> groups = new LinkedHashSet<>(Arrays.asList("group1", "group2"));
    SamlUser samlUser = new SamlUser("username", "firstName", "lastName", "email@domain", groups);

    samlSsoUserProvider.updateSsoUserAndGroups(SsoUser.fromSamlUser(samlUser), groups);

    assertSamlUserGroups(samlUser, groups, Collections.singleton("id"));
  }

  @Test
  public void testUpdateSamlUserAndGroups_ExistingSamlUser_NewGroups() {
    SamlUser samlUser =
        tempEntity.newSamlUser("username", "firstName", "lastName", "email@domain", Collections.emptySet());
    Set<String> groups = new LinkedHashSet<>(Arrays.asList("group1", "group2"));
    samlUser.setGroups(groups);

    samlSsoUserProvider.updateSsoUserAndGroups(SsoUser.fromSamlUser(samlUser), groups);

    assertSamlUserGroups(samlUser, groups, Collections.singleton("id"));
  }

  @Test
  public void testUpdateSamlUserAndGroups_NewSamlUser_ExistingGroups() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    Set<String> groups = new LinkedHashSet<>(Arrays.asList(samlGroup1.getName(), samlGroup2.getName()));
    SamlUser samlUser = new SamlUser("username", "firstName", "lastName", "email@domain", groups);

    samlSsoUserProvider.updateSsoUserAndGroups(SsoUser.fromSamlUser(samlUser), groups);

    assertSamlUserGroups(samlUser, groups, Collections.singleton("id"));
  }

  @Test
  public void testUpdateSamlUserAndGroups_ExistingSamlUser_ExistingGroups() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    Set<String> groups = new LinkedHashSet<>(Arrays.asList(samlGroup1.getName(), samlGroup2.getName()));
    SamlUser samlUser = tempEntity.newSamlUser("username", "firstName", "lastName", "email@domain", groups);
    tempEntity.newSamlUserGroup(samlUser.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser.getId(), samlGroup2.getId());

    samlSsoUserProvider.updateSsoUserAndGroups(SsoUser.fromSamlUser(samlUser), groups);

    assertSamlUserGroups(samlUser, groups, Collections.singleton("id"));
  }

  @Test
  public void testUpdateSamlUserAndGroups_ExistingSamlUser_ExistingAndNewGroups() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    Set<String> groups = new LinkedHashSet<>(Arrays.asList(samlGroup1.getName(), samlGroup2.getName()));
    SamlUser samlUser = tempEntity.newSamlUser("username", "firstName", "lastName", "email@domain", groups);
    tempEntity.newSamlUserGroup(samlUser.getId(), samlGroup1.getId());
    tempEntity.newSamlUserGroup(samlUser.getId(), samlGroup2.getId());
    groups.add("group3");
    samlUser.setGroups(groups);

    samlSsoUserProvider.updateSsoUserAndGroups(SsoUser.fromSamlUser(samlUser), groups);

    assertSamlUserGroups(samlUser, groups, Collections.singleton("id"));
  }

  @Test
  public void testUpdateSamlUserAndGroups_NewSamlUser_ExistingAndNewGroups() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    Set<String> groups = new LinkedHashSet<>(Arrays.asList(samlGroup1.getName(), samlGroup2.getName(), "group3"));
    SamlUser samlUser = new SamlUser("username", "firstName", "lastName", "email@domain", groups);

    samlSsoUserProvider.updateSsoUserAndGroups(SsoUser.fromSamlUser(samlUser), groups);

    assertSamlUserGroups(samlUser, groups, Collections.singleton("id"));
  }

  @Test
  public void testUpdateSamlUserAndGroups_RemovesGroupsWithNoMembers() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    SamlGroup samlGroup3 = tempEntity.newSamlGroup("group3");
    SamlUser samlUser1 =
        tempEntity.newSamlUser("username1", "firstName", "lastName", "email@domain",
            new LinkedHashSet<>(Arrays.asList(samlGroup1.getName(), samlGroup3.getName())));
    SamlUser samlUser2 = tempEntity.newSamlUser("username2", "firstName", "lastName", "email@domain",
        new LinkedHashSet<>(Arrays.asList(samlGroup2.getName(), samlGroup3.getName())));
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup1.getId());
    SamlUserGroup samlUserGroup22 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup2.getId());
    tempEntity.newSamlUserGroup(samlUser1.getId(), samlGroup3.getId());
    SamlUserGroup samlUserGroup23 = tempEntity.newSamlUserGroup(samlUser2.getId(), samlGroup3.getId());
    samlUser1.setGroups(Collections.emptySet());

    samlSsoUserProvider.updateSsoUserAndGroups(SsoUser.fromSamlUser(samlUser1), Collections.emptySet());

    assertThat(samlUserDAO.getAll())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(JPA.IGNORE_FIELDS)
        .containsExactlyInAnyOrder(samlUser1, samlUser2);
    assertThat(samlUserGroupDAO.getAll())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(JPA.IGNORE_FIELDS)
        .containsExactlyInAnyOrder(samlUserGroup22, samlUserGroup23);
    assertThat(samlGroupDAO.getAll())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(JPA.IGNORE_FIELDS)
        .containsExactly(samlGroup2, samlGroup3);
  }

  @Test
  public void testGetSamlUsersByGroupName_GroupDoesNotExist() {
    assertThat(samlSsoUserProvider.getSsoUsersByGroupName("doesNotExist")).isEmpty();
  }

  @Test
  public void testGetSamlUsersByGroupName_GroupExists_NoUsers() {
    SamlGroup samlGroup = tempEntity.newSamlGroup();

    assertThat(samlSsoUserProvider.getSsoUsersByGroupName(samlGroup.getName())).isEmpty();
  }

  @Test
  public void testGetSamlUsersByGroupName_GroupExists_SomeUsers() {
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

    assertThat(samlSsoUserProvider.getSsoUsersByGroupName(samlGroup.getName()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(SsoUser.fromSamlUser(samlUser1), SsoUser.fromSamlUser(samlUser2));
  }

  @Test
  public void testFilterExistingSamlGroupNames_Empty() {
    assertThat(samlSsoUserProvider.filterExistingSsoGroupNames(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testFilterExistingSamlGroupNames() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    Set<String> groupNames =
        new LinkedHashSet<>(Arrays.asList("doesNotExist", samlGroup1.getName(), samlGroup2.getName()));

    assertThat(samlSsoUserProvider.filterExistingSsoGroupNames(groupNames)).containsExactly(samlGroup1.getName(),
        samlGroup2.getName());
  }

  @Test
  public void testGetSamlUsersByUsernames() {
    Set<String> usernames = new HashSet<>(Arrays.asList("username1", "username2"));

    samlSsoUserProvider.getSsoUsersByUsernames(usernames);

    verify(spySamlUserDAO).getByUsernames(usernames);
  }

  @Test
  public void testGetSsoUsersByEmails() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA", null, null, "usera@sonatype.com", null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB", null, null, "userb@sonatype.com", null);
    tempEntity.newSamlUser();

    Set<String> emails = Set.of(samlUser1.getEmail(), samlUser2.getEmail());

    List<SsoUser> result = samlSsoUserProvider.getSsoUsersByEmails(emails);

    assertThat(result).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(SsoUser.fromSamlUser(samlUser1), SsoUser.fromSamlUser(samlUser2));
  }

  @Test
  public void testGetSsoUsersByRealNames() {
    SamlUser samlUser1 = tempEntity.newSamlUser("userA", "Mark", "Mywords", null, null);
    SamlUser samlUser2 = tempEntity.newSamlUser("userB", "Justin", "Time", null, null);
    tempEntity.newSamlUser();

    Set<String> realNames = Set.of("Mark Mywords", "Justin Time");

    List<SsoUser> result = samlSsoUserProvider.getSsoUsersByRealNames(realNames);

    assertThat(result).usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(SsoUser.fromSamlUser(samlUser1), SsoUser.fromSamlUser(samlUser2));
  }

  @Test
  public void testFindSamlUsersByNameQuery() {
    String nameQuery = "nameQuery";

    samlSsoUserProvider.findSsoUsersByNameOrUsernameQuery(nameQuery);

    verify(spySamlUserDAO).findUsersByNameOrUsernameQuery(nameQuery);
  }

  @Test
  public void testFindSamlGroupsByNameQuery() {
    String nameQuery = "nameQuery";

    samlSsoUserProvider.findSsoGroupsByNameQuery(nameQuery);

    verify(samlGroupDAO).findGroupsByNameQuery(nameQuery);
  }

  @Test
  public void testDeleteUser() {
    SamlUser samlUser = tempEntity.newSamlUser();

    samlSsoUserProvider.deleteSsoUser(SsoUser.fromSamlUser(samlUser));

    assertThat(samlUserDAO.getByUsername(samlUser.getUsername())).isNull();
  }

  @Test
  public void testGetByUsername() {
    SamlUser samlUser = tempEntity.newSamlUser();
    tempEntity.newSamlUser();

    assertThat(samlSsoUserProvider.getByUsername(samlUser.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(SsoUser.fromSamlUser(samlUser));
  }

  @Test
  public void testUpsertByUsername_Insert() {
    SamlUser samlUser = tempEntity.newSamlUser();

    samlSsoUserProvider.upsertByUsername(SsoUser.fromSamlUser(samlUser));

    assertThat(samlUserDAO.getByUsername(samlUser.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(samlUser);
  }

  @Test
  public void testUpsertByUsername_Update() {
    SamlUser samlUser = tempEntity.newSamlUser();
    samlUser.setFirstName(samlUser.getFirstName() + "2");
    samlUser.setLastName(samlUser.getLastName() + "2");
    samlUser.setEmail(samlUser.getEmail() + "2");
    samlUser.setGroups(new LinkedHashSet<>(Arrays.asList("someGroup3", "someGroup4")));

    samlSsoUserProvider.upsertByUsername(SsoUser.fromSamlUser(samlUser));

    assertThat(samlUserDAO.getByUsername(samlUser.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(samlUser);
  }

  @Test
  public void testGetByUsernameNotNull_Exists() {
    SamlUser samlUser = tempEntity.newSamlUser();

    assertThat(samlSsoUserProvider.getByUsernameNotNull(samlUser.getUsername())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .isEqualTo(SsoUser.fromSamlUser(samlUser));
  }

  @Test
  public void testGetByUsernameNotNull_DoesNotExist() {
    String username = "doesNotExist";

    assertThatExceptionOfType(NotFoundException.class)
        .isThrownBy(() -> samlSsoUserProvider.getByUsernameNotNull(username))
        .withMessageContaining("Cannot find a SAML user with username " + username + ".");
  }

  @Test
  public void testGetAll() {
    SamlUser samlUser1 = tempEntity.newSamlUser();
    SamlUser samlUser2 = tempEntity.newSamlUser();

    List<SsoUser> users = samlSsoUserProvider.getAll();
    assertThat(users).hasSize(2);
    assertSamlUser(samlUser1, users);
    assertSamlUser(samlUser2, users);
  }

  private void assertSamlUser(SamlUser expectedSamlUser, List<SsoUser> users) {
    SsoUser foundUser = users.stream()
        .filter(samlUser -> expectedSamlUser.getUsername().equals(samlUser.getUsername()))
        .findFirst()
        .orElse(null);
    assertThat(foundUser).isNotNull()
        .usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
        .ignoringFields("groupsString", "realmId", "groups")
        .isEqualTo(SsoUser.fromSamlUser(expectedSamlUser));
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
        .containsExactlyInAnyOrderElementsOf(samlGroups.stream()
            .map(samlGroup -> new SamlUserGroup(samlUser.getId(), samlGroup.getId()))
            .collect(Collectors.toList()));
  }
}
