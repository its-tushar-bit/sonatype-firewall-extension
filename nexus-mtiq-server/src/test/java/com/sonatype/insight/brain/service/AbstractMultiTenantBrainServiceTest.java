/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantTestHelper.ConsumerWithException;

import org.junit.ClassRule;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ENABLE_SSO_ONLY;

public class AbstractMultiTenantBrainServiceTest
    extends AbstractBrainServiceTest
{
  private final SystemConfigurationPropertyDAO configurationPropertyDAO = new SystemConfigurationPropertyDAO();

  /**
   * MultiTenantBrainServiceTestHelper this sets up AbstractBrainServiceTest
   * ready for use with MTIQ registering before, after and afterClass handlers.
   */
  @ClassRule
  public static final MultiTenantBrainServiceTestHelper multiTenantBrainServiceTestHelper =
      new MultiTenantBrainServiceTestHelper();

  /**
   * afterDatabaseReset performs further TemporaryEntity cleanup action after TemporaryEntity has reset the database
   */
  @Override
  protected void afterDatabaseReset() {
    // Reset Global tenant temp entity system props
    configurationPropertyDAO.set(ENABLE_SSO_ONLY, Boolean.toString(true));
  }

  /**
   * getTestTenant returns the pre provisioned tenant ready for use in test
   */
  protected static Tenant getTestTenant() {
    return MultiTenantBrainServiceTestHelper.getTestTenant();
  }

  /**
   * testAsTestTenant run the test as the current the pre provisioned tenant
   */
  protected static void testAsTestTenant(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAs(getTestTenant(), test);
  }

  protected static void testAsGlobal(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAs(Tenant.GLOBAL_TENANT, test);
  }

  protected String generateTestTenantName() {
    return TenantTestHelper.createTenantName(testName);
  }

  protected void setTenantSlug(String tenantSlug) {
    MultiTenantBrainServiceTestHelper.setTenantBySlug(tenantSlug);
  }

  protected HttpResponse provisionTenant(String tenantName) throws Exception {
    setTenantSlug(tenantName);
    return MultiTenantBrainServiceTestHelper.provisionTenant(this, tenantName);
  }
}
