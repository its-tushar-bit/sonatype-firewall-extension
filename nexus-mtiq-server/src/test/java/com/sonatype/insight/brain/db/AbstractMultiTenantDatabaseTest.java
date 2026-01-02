/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Set;
import javax.inject.Provider;

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
