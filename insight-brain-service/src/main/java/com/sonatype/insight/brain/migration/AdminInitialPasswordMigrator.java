/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.security.PasswordService;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates the default admin password in initial installation only if a custom admin password is present in
 * config.yml. The property is initialAdminPassword and a value from environmental variables can be referenced
 * by using ${ENVIRONMENTAL_VARIABLE_TO_REFERENCE} as well.
 */
@Named
@Singleton
public class AdminInitialPasswordMigrator
{
  private static final Logger log = LoggerFactory.getLogger(AdminInitialPasswordMigrator.class);

  static final String ADMIN_PASSWORD_MIGRATOR_ID = "admin-initial-password";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final InsightWork insightWork;

  private final PasswordService passwordService;

  private final UserDAO userDAO;

  @Inject
  public AdminInitialPasswordMigrator(
      final MigrationTrackerDAO migrationTrackerDAO,
      final InsightWork insightWork,
      final PasswordService passwordService,
      final UserDAO userDAO)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.insightWork = insightWork;
    this.passwordService = passwordService;
    this.userDAO = userDAO;
  }

  public void migrate() {
    if (migrationTrackerDAO.isTrackerPresent(ADMIN_PASSWORD_MIGRATOR_ID)) {
      return;
    }

    if (StringUtils.isEmpty(insightWork.getInitialAdminPassword())) {
      log.info("Using the default initial password for admin user.");
      return;
    }

    User admin = userDAO.getById("ADMIN");
    admin.setPassword(passwordService.hashPassword(insightWork.getInitialAdminPassword()));
    log.info("Using the custom password for the admin user.");

    try (TransactionContext transactionContext = userDAO.createTransactionContext()) {
      transactionContext.begin();
      userDAO.update(transactionContext, admin);
      migrationTrackerDAO.insertTracker(transactionContext, ADMIN_PASSWORD_MIGRATOR_ID);
      transactionContext.commit();
      log.info("Successfully updated admin password with the custom password provided.");
    }
  }
}
