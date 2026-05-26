/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.audit.AuditEvent;
import com.sonatype.insight.brain.audit.AuditRecorder;
import com.sonatype.insight.brain.audit.AuditSession;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.filter.DashboardFilterDAO;
import com.sonatype.insight.brain.dataaccess.filter.UserFilterDAO;
import com.sonatype.insight.brain.dataaccess.ide.UserIdePolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.notification.UserViewedProductNotificationDAO;
import com.sonatype.insight.brain.dataaccess.security.MembershipMappingDAO;
import com.sonatype.insight.brain.dataaccess.security.UserDAO;
import com.sonatype.insight.brain.dataaccess.security.UserTokenDAO;
import com.sonatype.insight.brain.db.DatabaseConfigProviderFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.DataSourceProviderFactory;
import com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.security.MemberType;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.spring.InsightBrainCompatibilityCommand;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @since 1.66
 */
@Named
public class ResetAdminCommand
    implements InsightBrainCompatibilityCommand
{
  public static final String NAME = "reset-admin";

  public static final String DESCRIPTION = "Resets the admin user back to its default configuration.";

  private static final Logger log = LoggerFactory.getLogger(ResetAdminCommand.class);

  static final User DEFAULT_ADMIN = new User(User.ADMIN_USERNAME,
      "$shiro1$SHA-256$10$7PC5QqeewnJK3iBQLPoq+Q==$5G44CC6HIYL8113tbp9lL0lNDP5CQJzbar0mWWkKbIM=", "Admin", "BuiltIn",
      "admin@localhost");

  static {
    DEFAULT_ADMIN.setId("ADMIN");
  }

  private final InsightConfig insightConfig;

  private OperationalDataStore operationalDataStore;

  ResetAdminCommand() {
    this(new InsightConfig());
  }

  @Inject
  public ResetAdminCommand(InsightConfig insightConfig) {
    this.insightConfig = insightConfig;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public void run(String... args) {
    run(insightConfig);
  }

  void run(Object ignoredBootstrap, Object ignoredNamespace, InsightConfig runtimeConfig) {
    run(runtimeConfig);
  }

  public void run(InsightConfig runtimeConfig) {
    try (AuditSession auditSession = new AuditRecorder(new ErrorResponseGenerator())
        .recordSystemEvent(AuditEvent.RESET_USER_PASSWORD))
    {
      AuditData.get().setData("username", DEFAULT_ADMIN.getUsername());
      try {
        DatabaseConfig databaseConfig = DatabaseConfigProviderFactory.createDatabaseConfigProvider(runtimeConfig)
            .getDatabaseConfig(DatabaseName.ods);
        operationalDataStore = getOperationalDataStore(databaseConfig);
        operationalDataStore.initialize();
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

  protected OperationalDataStore getOperationalDataStore(final DatabaseConfig databaseConfig) {
    DatabaseEngine databaseEngine = DatabaseUtil.getDatabaseEngine(databaseConfig);
    DataSourceProvider dataSourceProvider = DataSourceProviderFactory.createDataSourceProvider(databaseEngine);
    return new DefaultOperationalDataStore(dataSourceProvider, databaseConfig);
  }

  private void resetAdminUser() {
    UserDAO userDAO = getUserDAO();
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

  private UserDAO getUserDAO() {
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO(operationalDataStore);
    UserTokenDAO userTokenDAO = new UserTokenDAO(operationalDataStore);
    DashboardFilterDAO dashboardFilterDAO = new DashboardFilterDAO(operationalDataStore);
    UserFilterDAO userFilterDAO = new UserFilterDAO(operationalDataStore);
    UserViewedProductNotificationDAO userViewedProductNotificationDAO =
        new UserViewedProductNotificationDAO(operationalDataStore);
    UserIdePolicyEvaluationDAO userIdePolicyEvaluationDAO = new UserIdePolicyEvaluationDAO(operationalDataStore);
    SystemConfigurationPropertyDAO systemConfigurationPropertyDAO =
        new SystemConfigurationPropertyDAO(operationalDataStore);
    return new UserDAO(operationalDataStore, membershipMappingDAO, userTokenDAO, dashboardFilterDAO, userFilterDAO,
        userViewedProductNotificationDAO, userIdePolicyEvaluationDAO, systemConfigurationPropertyDAO);
  }

  private void setAdminMemberOfIfNeeded(TransactionContext tx, String roleId) {
    MembershipMappingDAO membershipMappingDAO = new MembershipMappingDAO(operationalDataStore);
    if (membershipMappingDAO.getByContextIdAndRoleId(tx, MembershipMapping.GLOBAL_CONTEXT_ID, roleId)
        .stream()
        .noneMatch(
            membershipMapping -> membershipMapping.includes(DEFAULT_ADMIN.getUsername(), Collections.emptySet())))
    {
      membershipMappingDAO.insert(tx,
          new MembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, roleId, DEFAULT_ADMIN.getUsername(),
              MemberType.USER));
    }
  }
}
