/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserGroupDAO;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.SamlUserGroup;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import com.sonatype.insight.test.LogOutput;

import org.assertj.core.util.Sets;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class SamlUserGroupMigratorTest
    extends AbstractComponentH2Test
{
  @Rule
  public LogOutput logOutput = new LogOutput(SamlUserGroupMigrator.class);

  @Inject
  private SamlUserGroupMigrator samlUserGroupMigrator;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private SamlUserDAO samlUserDAO;

  @Inject
  private SamlGroupDAO samlGroupDAO;

  @Inject
  private SamlUserGroupDAO samlUserGroupDAO;

  @BeforeEach
  @AfterEach
  public void clear() {
    migrationTrackerDAO.deleteById(SamlUserGroupMigrator.MIGRATION_ID);
  }

  @Test
  public void testMigrate() {
    SamlUser samlUser1 = tempEntity.newSamlUser("username1", Sets.newLinkedHashSet("group1", "group3"));
    SamlUser samlUser2 = tempEntity.newSamlUser("username2", Sets.newLinkedHashSet("group2", "group3"));
    SamlUser samlUser3 = tempEntity.newSamlUser("username3", Sets.newLinkedHashSet("group3"));
    SamlUser samlUser4 = tempEntity.newSamlUser("username4", Sets.newLinkedHashSet("group4"));
    SamlUser samlUser5 = tempEntity.newSamlUser("username5", Sets.newLinkedHashSet());

    samlUserGroupMigrator.migrate();

    assertThat(samlUserDAO.getAll())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(samlUser1, samlUser2, samlUser3, samlUser4, samlUser5);
    List<SamlGroup> samlGroups = samlGroupDAO.getAll();
    assertThat(samlGroups)
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(ignoreFieldsWithJPA("id"))
        .containsExactlyElementsOf(getExpectedSamlGroups(samlUser1, samlUser2, samlUser3, samlUser4, samlUser5));
    assertThat(samlUserGroupDAO.getAll())
        .usingRecursiveFieldByFieldElementComparatorIgnoringFields(ignoreFieldsWithJPA("id"))
        .containsExactlyInAnyOrderElementsOf(
            getExpectedSamlUserGroups(samlUser1, samlUser2, samlUser3, samlUser4, samlUser5));
    assertThat(logOutput).contains(
        "Migrated 6 SAML user group(s) for 5 SAML user(s)");
    assertThat(migrationTrackerDAO.isTrackerPresent(SamlUserGroupMigrator.MIGRATION_ID)).isTrue();
  }

  @Test
  public void testMigrate_AlreadyMigrated() {
    migrationTrackerDAO.insertTracker(SamlUserGroupMigrator.MIGRATION_ID);
    tempEntity.newSamlUser("username1", Sets.newLinkedHashSet("group1", "group2"));

    samlUserGroupMigrator.migrate();

    assertThat(logOutput).contains("SAML user groups are already migrated.");
    assertThat(samlGroupDAO.getAll()).isEmpty();
    assertThat(samlUserGroupDAO.getAll()).isEmpty();
  }

  @Test
  public void testMigrate_MultipleBatches() {
    int samlUsersCount = SamlUserGroupMigrator.MAX_BATCH_SIZE * 20 + 1;
    for (int i = 0; i < samlUsersCount; i++) {
      tempEntity.newSamlUser("username" + i, Sets.newLinkedHashSet("group" + i));
    }

    samlUserGroupMigrator.migrate();

    assertThat(migrationTrackerDAO.isTrackerPresent(SamlUserGroupMigrator.MIGRATION_ID)).isTrue();
    assertThat(samlGroupDAO.getAll()).hasSize(samlUsersCount);
    assertThat(samlUserGroupDAO.getAll()).hasSize(samlUsersCount);
    assertThat(logOutput).contains(
        "Migrated 1000 SAML user group(s) for 1000 SAML user(s)...");
    assertThat(logOutput).contains(
        "Migrated 2000 SAML user group(s) for 2000 SAML user(s)...");
    assertThat(logOutput).contains(
        "Migrated " + samlUsersCount + " SAML user group(s) for " + samlUsersCount + " SAML user(s)");
  }

  private List<SamlGroup> getExpectedSamlGroups(SamlUser... samlUsers) {
    return Arrays.stream(samlUsers)
        .flatMap(samlUser -> samlUser.getGroups().stream())
        .distinct()
        .map(SamlGroup::new)
        .sorted(Comparator.comparing(SamlGroup::getName))
        .collect(Collectors.toList());
  }

  private List<SamlUserGroup> getExpectedSamlUserGroups(SamlUser... samlUsers) {
    List<SamlUserGroup> result = new ArrayList<>();
    Arrays.stream(samlUsers)
        .forEach(samlUser -> samlUser.getGroups()
            .forEach(samlGroupName -> result.add(
                new SamlUserGroup(samlUser.getId(), samlGroupDAO.getByName(samlGroupName).getId()))));
    return result;
  }

  private String[] ignoreFieldsWithJPA(String... extraIgnoreFields) {
    List<String> ignoreFields = new ArrayList<>(Arrays.asList(JPA.IGNORE_FIELDS));
    ignoreFields.addAll(Arrays.asList(extraIgnoreFields));
    return ignoreFields.toArray(new String[0]);
  }
}
