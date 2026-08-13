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

  public static String createTenantNameFromTest(TestName testName) {
    return createTenantNameFromTest(testName.getMethodName());
  }

  public static String createTenantNameFromTest(String methodName) {
    String test = StringUtils.left(methodName.toLowerCase().replaceAll("_|\\[|\\]", "-"), 45);
    String randomness = StringUtils.left(UUID.randomUUID().toString(), 10);

    return test + "-" + randomness;
  }

  public static Tenant setupNewTestTenant(TestName testName) {
    Tenant tenant = new Tenant(createTenantNameFromTest(testName));
    setTenantWithoutValidation(tenant);
    return tenant;
  }

  /**
   * Run the given test closure as a <B>NEW</B> {@link Tenant} with the tenant name generated using
   * {@link #createTenantNameFromTest(TestName)}. Restores any current tenant after completion.
   */
  public static Tenant testAsNewTenant(TestName testName, ConsumerWithException<Tenant> test) {
    String tenantName = createTenantNameFromTest(testName);
    return testAsNewTenant(tenantName, test);
  }

  /**
   * Run the given test closure as a <B>NEW</B> {@link Tenant}. Restores any current tenant after completion.
   */
  public static Tenant testAsNewTenant(String tenantName, ConsumerWithException<Tenant> test) {
    Tenant tenant = new Tenant(tenantName);

    testAsTenant(tenant, test);

    return tenant;
  }

  /**
   * Run the given test closure as the provided {@link Tenant}. Restores any current tenant after completion.
   */
  public static void testAsTenant(Tenant tenant, ConsumerWithException<Tenant> test) {
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

  /**
   * Run the given test closure as a <B>NEW</B> {@link Tenant}. Restores any current tenant after completion and
   * additionally invalidate the tenant after completion.
   * <BR>
   * In most situations use one of the other `testAs*` methods that do not invalidate the tenant. In general tenant
   * invalidation would always be done as part of the regular tenancy code (see the multi-tenancy.md devdoc) and using
   * this might obfuscate a real tenancy issue. Only use this method when the test code itself needs to perform tenant
   * actions.
   */
  public static void testAsTenantAndInvalidate(String tenantName, ConsumerWithException<Tenant> test) {
    Tenant tenant = new Tenant(tenantName);
    testAsTenant(tenant, test);
    tenant.invalidate();
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

  @FunctionalInterface
  public interface ConsumerWithException<T>
  {
    void accept(T t) throws Exception;
  }
}
