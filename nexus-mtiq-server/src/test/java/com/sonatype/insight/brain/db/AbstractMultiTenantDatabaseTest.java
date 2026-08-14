/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import jakarta.inject.Provider;

import com.sonatype.insight.brain.StaticInjectionTestHelper;
import com.sonatype.insight.brain.api.admin.service.TenantService;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.dataaccess.tenancy.DeletedTenantDAO;
import com.sonatype.insight.brain.db.rule.MultiTenantDatabaseContainerRule;
import com.sonatype.insight.brain.service.TenantLifecycle;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantManaged;
import com.sonatype.insight.brain.tenancy.TenantManager;
import com.sonatype.insight.brain.tenancy.TenantTestHelper.ConsumerWithException;
import com.sonatype.insight.brain.tenancy.TenantValidator;
import com.sonatype.insight.brain.testing.AbstractMultiTenantTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.mockito.Mockito;

public abstract class AbstractMultiTenantDatabaseTest
    extends AbstractMultiTenantTest
{
  @Rule(order = 1)
  public MultiTenantDatabaseContainerRule databaseRule = MultiTenantDatabaseContainerRule.getInstance();

  protected TenantManager tenantManager;

  protected DAOFactory daoFactory;

  @Before
  public void setup() {
    daoFactory = new TestDAOFactory(databaseRule);

    DatabaseContainer databaseContainer = databaseRule.getDatabaseContainer();

    Provider<Set<TenantManaged>> tenantManagedBeans = Collections::emptySet;
    Provider<TenantLifecycle> tenantLifecycleProvider = () -> Mockito.mock(TenantLifecycle.class);
    TenantValidator tenantValidator = new TenantValidator(databaseRule.getOperationalDataStore());
    DeletedTenantDAO deletedTenantDAO = daoFactory.createDeletedTenantDAO();
    TenantService tenantService = new TenantService(databaseContainer.getOperationalDataStore());

    tenantManager = new TenantManager(tenantManagedBeans, tenantLifecycleProvider,
        databaseContainer.getDatabaseProvisioner(), tenantValidator, deletedTenantDAO, tenantService);

    // Re-inject classes that have static dependencies
    StaticInjectionTestHelper.inject(daoFactory);
  }

  @After
  public void cleanUp() {
    databaseRule.resetMocks();
  }

  // JUnit 5 (Jupiter): the @Rule(order=1) does not fire under Jupiter, so provision the (reused) multi-tenant
  // database fixture from a @BeforeEach and then run the JUnit 4 setup() body. Inert under the Vintage engine,
  // which drives the @Rule and @Before instead. Runs after the superclass @BeforeEach hooks (multi-tenant mode +
  // method-name capture), matching the JUnit 4 chain where setup() runs after the rules.
  @BeforeEach
  public void jupiterProvisionDatabaseAndSetup(final TestInfo testInfo) {
    databaseRule.beforeFromJupiter(testInfo.getTestClass().orElse(null), testInfo.getTestMethod().orElse(null));
    setup();
  }

  // JUnit 5 (Jupiter) teardown counterpart: run cleanUp() then the rule's after() so the reused fixture's
  // dirty/reset bookkeeping stays correct (otherwise per-test data leaks across the reused container).
  @AfterEach
  public void jupiterCleanupDatabase() {
    cleanUp();
    databaseRule.afterFromJupiter();
  }

  @Override
  protected Tenant testAsNewTenant(ConsumerWithException<Tenant> test) {
    return super.testAsNewTenant(t -> {
      // create the database for the tenant
      databaseRule.provisionDatabaseForTenant(t);

      test.accept(t);
    });
  }

  protected Tenant provisionTestTenant() {
    return super.testAsNewTenant(t -> {
      databaseRule.provisionDatabaseForTenant(t);
    });
  }

  protected void loadSqlDump(Path sqlFile) {
    databaseRule.loadSqlDump(sqlFile);
  }

  protected String dumpSchema(String schema) {
    return databaseRule.dumpSchema(schema);
  }
}
