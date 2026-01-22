/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.PasswordService;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates the default admin password in initial installation only if a custom admin password is present in
 * `NXIQ_INITIAL_ADMIN_PASSWORD` environment variable.
 */
@Named
@Singleton
public class AdminInitialPasswordMigrator
{
  private static final Logger log = LoggerFactory.getLogger(AdminInitialPasswordMigrator.class);

  static final String ADMIN_PASSWORD_MIGRATOR_ID = "admin-initial-password";

  //visible for testing
  public static final String NXIQ_INITIAL_ADMIN_PASSWORD = "NXIQ_INITIAL_ADMIN_PASSWORD";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final PasswordService passwordService;

  private final UserDAO userDAO;

  @Inject
  public AdminInitialPasswordMigrator(
      final MigrationTrackerDAO migrationTrackerDAO,
      final PasswordService passwordService,
      final UserDAO userDAO)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.passwordService = passwordService;
    this.userDAO = userDAO;
  }

  public void migrate() {
    if (migrationTrackerDAO.isTrackerPresent(ADMIN_PASSWORD_MIGRATOR_ID)) {
      return;
    }

    if (StringUtils.isEmpty(getInitialAdminPassword())) {
      log.info("Using the default initial password for admin user.");
      migrationTrackerDAO.insertTracker(ADMIN_PASSWORD_MIGRATOR_ID);
      return;
    }

    User admin = userDAO.getById("ADMIN");

    // Built-In admin is deleted for MTIQ when creating a new tenant
    if (admin == null) {
      log.info("Default Admin user does not exist.");
      migrationTrackerDAO.insertTracker(ADMIN_PASSWORD_MIGRATOR_ID);
      return;
    }

    admin.setPassword(passwordService.hashPassword(getInitialAdminPassword()));
    log.info("Using the custom password for the admin user.");

    try (TransactionContext transactionContext = userDAO.createTransactionContext()) {
      transactionContext.begin();
      userDAO.update(transactionContext, admin);
      migrationTrackerDAO.insertTracker(transactionContext, ADMIN_PASSWORD_MIGRATOR_ID);
      transactionContext.commit();
      log.info("Successfully updated admin password with the custom password provided.");
    }
  }

  private String getInitialAdminPassword() {
    return System.getenv(NXIQ_INITIAL_ADMIN_PASSWORD);
  }
}
