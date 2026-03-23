/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.migration;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlGroupDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserDAO;
import com.sonatype.insight.brain.dataaccess.security.SamlUserGroupDAO;
import com.sonatype.insight.brain.model.security.SamlGroup;
import com.sonatype.insight.brain.model.security.SamlUser;
import com.sonatype.insight.brain.model.security.SamlUserGroup;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.ods.Tables.SAML_USER;

@Named
public class SamlUserGroupMigrator
{
  private static final Logger log = LoggerFactory.getLogger(SamlUserGroupMigrator.class);

  // Visible for testing
  static final int MAX_BATCH_SIZE = 100;

  // Visible for testing
  static final String MIGRATION_ID = "saml-user-group-migrator";

  private final MigrationTrackerDAO migrationTrackerDAO;

  private final SamlUserDAO samlUserDAO;

  private final SamlGroupDAO samlGroupDAO;

  private final SamlUserGroupDAO samlUserGroupDAO;

  @Inject
  public SamlUserGroupMigrator(
      MigrationTrackerDAO migrationTrackerDAO,
      SamlUserDAO samlUserDAO,
      SamlGroupDAO samlGroupDAO,
      SamlUserGroupDAO samlUserGroupDAO)
  {
    this.migrationTrackerDAO = migrationTrackerDAO;
    this.samlUserDAO = samlUserDAO;
    this.samlGroupDAO = samlGroupDAO;
    this.samlUserGroupDAO = samlUserGroupDAO;
  }

  public void migrate() {
    long start = System.currentTimeMillis();

    if (migrationTrackerDAO.isTrackerPresent(MIGRATION_ID)) {
      log.debug("SAML user groups are already migrated.");
      return;
    }

    int samlUsersCount = 0;
    int samlUserGroupsCount = 0;
    int batchNumber = 0;
    List<SamlUser> samlUsers = getSamlUsersBatch(batchNumber++);
    while (!samlUsers.isEmpty()) {
      if (samlUsersCount % 1000 == 0) {
        log.debug("Migrated {} SAML user group(s) for {} SAML user(s)...", samlUserGroupsCount, samlUsersCount);
      }
      for (SamlUser samlUser : samlUsers) {
        try (TransactionContext tx = samlUserDAO.createTransactionContext()) {
          tx.begin();
          for (String group : samlUser.getGroups()) {
            SamlGroup samlGroup = new SamlGroup(group);
            samlGroupDAO.upsertByName(tx, samlGroup);
            samlUserGroupDAO.upsertBySamlUserIdAndSamlGroupId(tx,
                new SamlUserGroup(samlUser.getId(), samlGroup.getId()));
            samlUserGroupsCount++;
          }
          tx.commit();
          samlUsersCount++;
        }
      }
      samlUsers = getSamlUsersBatch(batchNumber++);
    }

    migrationTrackerDAO.insertTracker(MIGRATION_ID);

    log.info("Migrated {} SAML user group(s) for {} SAML user(s) in {} ms.", samlUserGroupsCount, samlUsersCount,
        System.currentTimeMillis() - start);
  }

  private List<SamlUser> getSamlUsersBatch(int batchNumber) {
    try (TransactionContext tx = samlUserDAO.createTransactionContext()) {
      return tx.dsl()
          .selectFrom(SAML_USER)
          .orderBy(SAML_USER.USERNAME)
          .offset(batchNumber * MAX_BATCH_SIZE)
          .limit(MAX_BATCH_SIZE)
          .fetchInto(SamlUser.class);
    }
  }
}
