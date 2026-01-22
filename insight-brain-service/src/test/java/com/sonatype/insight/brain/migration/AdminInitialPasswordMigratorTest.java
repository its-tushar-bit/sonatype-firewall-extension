/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.PasswordService;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import static com.sonatype.insight.brain.migration.AdminInitialPasswordMigrator.ADMIN_PASSWORD_MIGRATOR_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class AdminInitialPasswordMigratorTest
    extends AbstractComponentTest
{
  // Same with the one in schema.sql
  private static final String DEFAULT_ADMIN_PASSWORD_HASHED =
      "$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=";

  private static final String CUSTOM_PASSWORD = "not-admin-123";

  private static final String ADMIN_ID = "ADMIN";

  @Rule
  public EnvironmentVariables environmentVariables = new EnvironmentVariables();

  @Inject
  private UserDAO userDAO;

  @Inject
  private AdminInitialPasswordMigrator adminInitialPasswordMigrator;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private PasswordService passwordService;

  @After
  public void after() {
    // tracker must always be present after migrator runs
    assertThat(migrationTrackerDAO.getById(ADMIN_PASSWORD_MIGRATOR_ID)).isNotNull();

    // reset admin password
    User admin = userDAO.getById(ADMIN_ID);
    admin.setPassword(DEFAULT_ADMIN_PASSWORD_HASHED);
    userDAO.update(admin);

    // reset config and db
    migrationTrackerDAO.deleteById(ADMIN_PASSWORD_MIGRATOR_ID);
  }

  @Test
  public void testMigrate_NoTacker_CustomPasswordProvided() {
    migrationTrackerDAO.deleteById(ADMIN_PASSWORD_MIGRATOR_ID);
    environmentVariables.set(AdminInitialPasswordMigrator.NXIQ_INITIAL_ADMIN_PASSWORD, CUSTOM_PASSWORD);

    adminInitialPasswordMigrator.migrate();

    User admin = userDAO.getById(ADMIN_ID);
    assertThat(passwordService.passwordsMatch(CUSTOM_PASSWORD, admin.getPassword())).isTrue();
  }

  @Test
  public void testMigrate_NoTracker_CustomPasswordNotProvided() {
    migrationTrackerDAO.deleteById(ADMIN_PASSWORD_MIGRATOR_ID);
    environmentVariables.set(AdminInitialPasswordMigrator.NXIQ_INITIAL_ADMIN_PASSWORD, null);

    adminInitialPasswordMigrator.migrate();

    User admin = userDAO.getById(ADMIN_ID);
    assertThat(admin.getPassword()).isEqualTo(DEFAULT_ADMIN_PASSWORD_HASHED);
  }

  @Test
  public void testMigrate_TrackerPresent_CustomPasswordProvided() {
    migrationTrackerDAO.insertTracker(ADMIN_PASSWORD_MIGRATOR_ID);
    environmentVariables.set(AdminInitialPasswordMigrator.NXIQ_INITIAL_ADMIN_PASSWORD, CUSTOM_PASSWORD);

    adminInitialPasswordMigrator.migrate();

    User admin = userDAO.getById(ADMIN_ID);
    assertThat(admin.getPassword()).isEqualTo(DEFAULT_ADMIN_PASSWORD_HASHED);
  }

  @Test
  public void testMigrate_TrackerPresent_CustomPasswordNotProvided() {
    migrationTrackerDAO.insertTracker(ADMIN_PASSWORD_MIGRATOR_ID);
    environmentVariables.set(AdminInitialPasswordMigrator.NXIQ_INITIAL_ADMIN_PASSWORD, null);

    adminInitialPasswordMigrator.migrate();

    User admin = userDAO.getById(ADMIN_ID);
    assertThat(admin.getPassword()).isEqualTo(DEFAULT_ADMIN_PASSWORD_HASHED);
  }
}
