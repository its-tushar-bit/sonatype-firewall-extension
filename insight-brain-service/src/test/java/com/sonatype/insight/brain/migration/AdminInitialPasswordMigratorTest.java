/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.PasswordService;
import com.sonatype.insight.brain.service.AbstractComponentTest;
import com.sonatype.insight.brain.service.InsightConfig;

import org.junit.After;
import org.junit.Test;

import static com.sonatype.insight.brain.migration.AdminInitialPasswordMigrator.ADMIN_PASSWORD_MIGRATOR_ID;
import static org.assertj.core.api.Assertions.assertThat;

public class AdminInitialPasswordMigratorTest
    extends AbstractComponentTest
{
  // Same with the one in schema.sql
  private String defaultAdminPasswordHashed =
      "$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=";

  private String customPassword = "not-admin-123";

  private String adminId = "ADMIN";

  @Inject
  private UserDAO userDAO;

  @Inject
  private AdminInitialPasswordMigrator adminInitialPasswordMigrator;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private InsightConfig config;

  @Inject
  private PasswordService passwordService;

  @After
  public void after() {
    // tracker must always be present after migrator runs
    assertThat(migrationTrackerDAO.getById(ADMIN_PASSWORD_MIGRATOR_ID)).isNotNull();

    // reset admin password
    User admin = userDAO.getById(adminId);
    admin.setPassword(defaultAdminPasswordHashed);
    userDAO.update(admin);

    // reset config and db
    config.setInitialAdminPassword(null);
    migrationTrackerDAO.deleteById(ADMIN_PASSWORD_MIGRATOR_ID);
  }

  @Test
  public void testMigrate_NoTacker_CustomPasswordProvided() {
    migrationTrackerDAO.deleteById(ADMIN_PASSWORD_MIGRATOR_ID);
    config.setInitialAdminPassword(customPassword);

    adminInitialPasswordMigrator.migrate();

    User admin = userDAO.getById(adminId);
    assertThat(passwordService.passwordsMatch(customPassword, admin.getPassword())).isTrue();
  }

  @Test
  public void testMigrate_NoTracker_CustomPasswordNotProvided() {
    migrationTrackerDAO.deleteById(ADMIN_PASSWORD_MIGRATOR_ID);
    config.setInitialAdminPassword(null);

    adminInitialPasswordMigrator.migrate();

    User admin = userDAO.getById(adminId);
    assertThat(admin.getPassword()).isEqualTo(defaultAdminPasswordHashed);
  }

  @Test
  public void testMigrate_TrackerPresent_CustomPasswordProvided() {
    migrationTrackerDAO.insertTracker(ADMIN_PASSWORD_MIGRATOR_ID);
    config.setInitialAdminPassword(customPassword);

    adminInitialPasswordMigrator.migrate();

    User admin = userDAO.getById(adminId);
    assertThat(admin.getPassword()).isEqualTo(defaultAdminPasswordHashed);
  }

  @Test
  public void testMigrate_TrackerPresent_CustomPasswordNotProvided() {
    migrationTrackerDAO.insertTracker(ADMIN_PASSWORD_MIGRATOR_ID);
    config.setInitialAdminPassword(null);

    adminInitialPasswordMigrator.migrate();

    User admin = userDAO.getById(adminId);
    assertThat(admin.getPassword()).isEqualTo(defaultAdminPasswordHashed);
  }
}
