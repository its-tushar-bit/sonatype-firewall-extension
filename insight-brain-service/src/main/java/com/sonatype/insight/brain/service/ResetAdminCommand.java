/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collections;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatabaseMigrator;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.db.DatabaseConfig;

import io.dropwizard.cli.ConfiguredCommand;
import io.dropwizard.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.66
 */
public class ResetAdminCommand
    extends ConfiguredCommand<InsightConfig>
{
  private static final Logger log = LoggerFactory.getLogger(ResetAdminCommand.class);

  // Visible for testing
  static final User DEFAULT_ADMIN = new User(User.ADMIN_USERNAME,
      "$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=", "Admin", "BuiltIn",
      "admin@localhost");

  static {
    DEFAULT_ADMIN.setId("ADMIN");
  }

  ResetAdminCommand() {
    super("reset-admin", "Resets the admin user back to its default configuration.");
  }

  @Override
  protected void run(Bootstrap<InsightConfig> bootstrap, Namespace namespace, InsightConfig insightConfig) {
    try (AuditSession auditSession = new AuditRecorder(new ErrorResponseGenerator())
        .recordSystemEvent(AuditEvent.RESET_USER_PASSWORD)) {
      AuditData.get().setData("username", DEFAULT_ADMIN.getUsername());
      try {
        DatabaseConfig databaseConfig = new DatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods);
        DataSourceFactory dataSourceFactory = new DataSourceFactory();
        DatabaseMigrator databaseMigrator = new DatabaseMigrator(dataSourceFactory);
        OperationalDataStore operationalDataStore =
            new DefaultOperationalDataStore(dataSourceFactory, databaseMigrator);
        operationalDataStore.initWithoutMigration(databaseConfig);
        resetAdminUser();
        log.info("Successfully reset the admin user back to its default configuration.");
      }
      catch (Exception e) {
        AuditData.get().setException(e);
        log.error("Failed to reset the admin user back to its default configuration.", e);
        throw e;
      }
    }
  }

  private void resetAdminUser() {
    UserDAO userDAO = new UserDAO();
    try (TransactionContext tx = userDAO.createTransactionContext()) {
      tx.begin();
      User admin = userDAO.getByUsername(tx, DEFAULT_ADMIN.getUsername());
      if (admin == null) {
        userDAO.insert(tx, DEFAULT_ADMIN);
      }
      else {
        admin.setPassword(DEFAULT_ADMIN.getPassword());
        userDAO.update(tx, admin);
      }
      setAdminMemberOfIfNeeded(tx, Role.SYSTEM_ADMIN_ROLE_ID);
      setAdminMemberOfIfNeeded(tx, Role.POLICY_ADMIN_ROLE_ID);
      tx.commit();
    }
  }

  private void setAdminMemberOfIfNeeded(TransactionContext tx, String roleId) {
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO();
    if (membershipMappingDAO.getByContextIdAndRoleId(MembershipMapping.GLOBAL_CONTEXT_ID, roleId).stream().noneMatch(
        membershipMapping -> membershipMapping.includes(DEFAULT_ADMIN.getUsername(), Collections.emptySet()))) {
      membershipMappingDAO.insert(tx,
          new MembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, roleId, DEFAULT_ADMIN.getUsername(),
              MemberType.USER));
    }
  }
}
