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

import com.google.inject.Binder;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class SamlUserGroupHelperTest
    extends AbstractComponentTest
{
  @Inject
  private SamlUserGroupHelper samlUserGroupHelper;

  @Inject
  private SamlConfigurationDAO samlConfigurationDAO;

  @Inject
  private SamlUserDAO samlUserDAO;

  private final SamlUserDAO spySamlUserDAO = spy(new SamlUserDAO());

  @Inject
  private SamlGroupDAO samlGroupDAO;

  private final SamlGroupDAO spySamlGroupDAO = spy(new SamlGroupDAO());

  @Inject
  private SamlUserGroupDAO samlUserGroupDAO;

  @Override
  public void configure(Binder binder) {
    binder.bind(SamlUserDAO.class).toInstance(spySamlUserDAO);
    binder.bind(SamlGroupDAO.class).toInstance(spySamlGroupDAO);
    super.configure(binder);
  }

  @Test
  public void testIsSamlConfigured_False() {
    samlConfigurationDAO.delete();

    assertThat(samlUserGroupHelper.isSamlConfigured()).isFalse();
  }

  @Test
  public void testIsSamlConfigured_True() {
    tempEntity.newSamlConfiguration();

    assertThat(samlUserGroupHelper.isSamlConfigured()).isTrue();
  }

  @Test
  public void testUpdateSamlUserAndGroups_NewSamlUser_NewGroups() {
    Set<String> groups = new LinkedHashSet<>(Arrays.asList("group1", "group2"));
    SamlUser samlUser = new SamlUser("username", "firstName", "lastName", "email@domain", groups);

    samlUserGroupHelper.updateSamlUserAndGroups(samlUser, groups);

    assertSamlUserGroups(samlUser, groups, Collections.singleton("id"));
  }

  @Test
  public void testUpdateSamlUserAndGroups_ExistingSamlUser_NewGroups() {
    SamlUser samlUser =
        tempEntity.newSamlUser("username", "firstName", "lastName", "email@domain", Collections.emptySet());
    Set<String> groups = new LinkedHashSet<>(Arrays.asList("group1", "group2"));
    samlUser.setGroups(groups);

    samlUserGroupHelper.updateSamlUserAndGroups(samlUser, groups);

    assertSamlUserGroups(samlUser, groups, Collections.singleton("id"));
  }

  @Test
  public void testUpdateSamlUserAndGroups_NewSamlUser_ExistingGroups() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    Set<String> groups = new LinkedHashSet<>(Arrays.asList(samlGroup1.getName(), samlGroup2.getName()));
    SamlUser samlUser = new SamlUser("username", "firstName", "lastName", "email@domain", groups);

    samlUserGroupHelper.updateSamlUserAndGroups(samlUser, groups);

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

    samlUserGroupHelper.updateSamlUserAndGroups(samlUser, groups);

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

    samlUserGroupHelper.updateSamlUserAndGroups(samlUser, groups);

    assertSamlUserGroups(samlUser, groups, Collections.singleton("id"));
  }

  @Test
  public void testUpdateSamlUserAndGroups_NewSamlUser_ExistingAndNewGroups() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    Set<String> groups = new LinkedHashSet<>(Arrays.asList(samlGroup1.getName(), samlGroup2.getName(), "group3"));
    SamlUser samlUser = new SamlUser("username", "firstName", "lastName", "email@domain", groups);

    samlUserGroupHelper.updateSamlUserAndGroups(samlUser, groups);

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

    samlUserGroupHelper.updateSamlUserAndGroups(samlUser1, Collections.emptySet());

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
    assertThat(samlUserGroupHelper.getSamlUsersByGroupName("doesNotExist")).isEmpty();
  }

  @Test
  public void testGetSamlUsersByGroupName_GroupExists_NoUsers() {
    SamlGroup samlGroup = tempEntity.newSamlGroup();

    assertThat(samlUserGroupHelper.getSamlUsersByGroupName(samlGroup.getName())).isEmpty();
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

    assertThat(samlUserGroupHelper.getSamlUsersByGroupName(samlGroup.getName()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2);
  }

  @Test
  public void testFilterExistingSamlGroupNames_Empty() {
    assertThat(samlUserGroupHelper.filterExistingSamlGroupNames(Collections.emptySet())).isEmpty();
  }

  @Test
  public void testFilterExistingSamlGroupNames() {
    SamlGroup samlGroup1 = tempEntity.newSamlGroup("group1");
    SamlGroup samlGroup2 = tempEntity.newSamlGroup("group2");
    Set<String> groupNames =
        new LinkedHashSet<>(Arrays.asList("doesNotExist", samlGroup1.getName(), samlGroup2.getName()));

    assertThat(samlUserGroupHelper.filterExistingSamlGroupNames(groupNames)).containsExactly(samlGroup1.getName(),
        samlGroup2.getName());
  }

  @Test
  public void testGetSamlUsersByUsernames() {
    Set<String> usernames = new HashSet<>(Arrays.asList("username1", "username2"));

    samlUserGroupHelper.getSamlUsersByUsernames(usernames);

    verify(spySamlUserDAO).getByUsernames(usernames);
  }

  @Test
  public void testFindSamlUsersByNameQuery() {
    String nameQuery = "nameQuery";

    samlUserGroupHelper.findSamlUsersByNameQuery(nameQuery);

    verify(spySamlUserDAO).findUsersByNameQuery(nameQuery);
  }

  @Test
  public void testFindSamlGroupsByNameQuery() {
    String nameQuery = "nameQuery";

    samlUserGroupHelper.findSamlGroupsByNameQuery(nameQuery);

    verify(samlGroupDAO).findGroupsByNameQuery(nameQuery);
  }

  private void assertSamlUserGroups(
      SamlUser expectedSamlUser,
      Set<String> expectedGroupNames,
      Set<String> extraIgnoreFields)
  {
    assertThat(expectedSamlUser.getId()).isNotNull();
    assertThat(samlUserDAO.getById(expectedSamlUser.getId())).usingRecursiveComparison()
        .ignoringFields(JPA.IGNORE_FIELDS)
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
            new SamlUserGroup(expectedSamlUser.getId(), samlGroup.getId())).collect(Collectors.toList()));
  }
}
