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

  static void setTenant(final Tenant tenant) {
    TenantThreadLocal.setTenantWithoutValidation(tenant);
  }

  public static void assertTenantSet(Tenant tenant) {
    assertThat(TenantThreadLocal.getTenantWithoutValidation()).isEqualTo(tenant);
  }

  public static Tenant setupNewTestTenant(TestName testName) {
    Tenant tenant = createTenant(testName);
    setTenant(tenant);

    return tenant;
  }

  public static Tenant testAsNewTenant(TestName testName, ConsumerWithException<Tenant> test) {
    Tenant tenant = createTenant(testName);

    TenantTestHelper.testAs(tenant, test);

    return tenant;
  }

  public static void testAs(Tenant tenant, ConsumerWithException<Tenant> test) {
    Tenant currentTenant = TenantThreadLocal.getTenantWithoutValidation();
    try {
      setTenant(tenant);

      test.accept(tenant);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
    finally {
      setTenant(currentTenant);
    }
  }

  static Tenant createTenant(TestName testName) {
    return new Tenant(createTenantName(testName));
  }

  public static String createTenantName(TestName testName) {
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

  @FunctionalInterface
  public interface ConsumerWithException<T>
  {
    void accept(T t) throws Exception;
  }
}
