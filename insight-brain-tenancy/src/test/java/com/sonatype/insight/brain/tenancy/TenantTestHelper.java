/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.junit.rules.TestName;

import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;
import static org.assertj.core.api.Assertions.assertThat;

public class TenantTestHelper
{
  public static void initMultiTenantMode() {
    TenantThreadLocal.resetTenantForTesting();
  }

  /**
   * Allows direct invocation of package-private {@link TenantThreadLocal#setTenantWithoutValidation(Tenant)} for usage
   * by tests that know that they can directly set the tenant in the ThreadLocal without validation.
   */
  static void setTenantWithoutValidation(final Tenant tenant) {
    TenantThreadLocal.setTenantWithoutValidation(tenant);
  }

  public static void assertTenantSet(Tenant tenant) {
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(tenant);
  }

  public static Tenant setupNewTestTenant(TestName testName) {
    Tenant tenant = createTenant(testName);
    setTenantWithoutValidation(tenant);

    return tenant;
  }

  public static Tenant testAsNewTenant(TestName testName, ConsumerWithException<Tenant> test) {
    String tenantName = createTenantNameFromTest(testName);
    return testAsNewTenant(tenantName, test);
  }

  public static Tenant testAsNewTenant(String tenantName, ConsumerWithException<Tenant> test) {
    Tenant tenant = createTenant(tenantName);

    testAs(tenant, test);

    return tenant;
  }

  public static void testAs(Tenant tenant, ConsumerWithException<Tenant> test) {
    Tenant currentTenant = TenantThreadLocal.getTenantWithoutValidation();
    try {
      setTenantWithoutValidation(tenant);

      test.accept(tenant);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      setTenantWithoutValidation(currentTenant);
    }
  }

  static Tenant createTenant(final String tenantName) {
    return new Tenant(tenantName);
  }

  static Tenant createTenant(TestName testName) {
    return new Tenant(createTenantNameFromTest(testName));
  }

  public static String createTenantNameFromTest(TestName testName) {
    String test = StringUtils.left(testName.getMethodName().toLowerCase().replace('_', '-'), 45);
    String randomness = StringUtils.left(UUID.randomUUID().toString(), 10);

    return test + "-" + randomness;
  }

  public static void setSingleTenant() {
    TenantThreadLocal.setTenantWithoutValidation(SINGLE_TENANT);
  }

  public static void setGlobalTenant() {
    TenantThreadLocal.setGlobalTenant();
  }

  public static void resetAfterTest() {
    TenantTestHelper.setSingleTenant();
  }

  public static void testAs(String tenantName, ConsumerWithException<Tenant> test) {
    Tenant tenant = new Tenant(tenantName);
    testAs(tenant, test);
    tenant.invalidate();
  }

  @FunctionalInterface
  public interface ConsumerWithException<T>
  {
    void accept(T t) throws Exception;
  }
}
