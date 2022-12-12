/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;

public class TenantUtil
{
  private static final Logger log = LoggerFactory.getLogger(TenantUtil.class);

  public static boolean isMultiTenant() {
    return !isSingleTenant();
  }

  public static boolean isSingleTenant() {
    return SINGLE_TENANT.equals(TenantThreadLocal.getTenantWithoutValidation());
  }

  public static boolean isGlobalTenant() {
    return GLOBAL_TENANT.equals(validateTenant(TenantThreadLocal.getTenant()));
  }

  public static boolean isGlobalTenant(String tenantSlug) {
    return GLOBAL_TENANT.tenantSlug.equals(tenantSlug);
  }

  static Tenant validateTenant(Tenant tenant) {
    return validateTenantForType(null, tenant);
  }

  /*
      MTIQ - None of the errors in this validation method should ever be hit once multi-tenancy is complete.
      During MTIQ development this can be used as a catch-all for any tenancy issues which still need to be addressed.
      Most commonly for system initiated events you need to call TenantUtils.initTenancy
   */
  public static Tenant validateTenantForType(Class clazz, Tenant tenant) {
    if (!isMultiTenant()) {
      return SINGLE_TENANT;
    }

    Tenant defaultTenant = GLOBAL_TENANT;

    // If we are here we are multi-tenant and a tenant setting is REQUIRED
    if (tenant == null) {

      logTenancyIssue("A tenant was expected but no tenant is set.");

      return defaultTenant;
    }

    // Skip type validation if no type is specified
    if (clazz == null) {
      return tenant;
    }

    if (GlobalTenantJob.class.isAssignableFrom(clazz) && !GLOBAL_TENANT.equals(tenant)) {
      logTenancyIssue("GlobalTenantJob was invoked which expects a global tenant to be set but instead a specific " +
          "tenant was set");
    }
    else if (!GlobalTenantJob.class.isAssignableFrom(clazz) && TenantJob.class.isAssignableFrom(clazz) &&
        GLOBAL_TENANT.equals(tenant)) {
      logTenancyIssue("TenantJob was invoked which expects a specific tenant to be set but instead global " +
          "tenant was set");
    }
    else if (!GlobalTenantJob.class.isAssignableFrom(clazz) && !TenantJob.class.isAssignableFrom(clazz)) {
      logTenancyIssue("Class specified for tenancy validation but no validation exists: " + clazz);
    }

    return tenant;
  }

  private static void logTenancyIssue(String message) {
    log.warn("----------------------------------------------------------------------------------------------------");
    log.warn("POSSIBLE TENANCY ISSUE!!! {}", message);
    log.warn("For now going to return the GLOBAL tenant default but this needs to be fixed");
    log.warn("Stack trace: " + ExceptionUtils.getStackTrace(new Exception()));
    log.warn("----------------------------------------------------------------------------------------------------");
  }

  /**
   * Extracts the tenant slug from the vanity url. The first part of the URL (before the first .) is the tenant slug.
   *
   * @param serverName - server name not including http://
   * @return the tenant slug
   */
  public static String getTenantName(final String serverName) {
    validateTenantName(serverName);

    return serverName.substring(0, serverName.indexOf("."));
  }

  private static void validateTenantName(String serverName) {
    if ("localhost".equals(serverName)) {
      throw new RuntimeException("You should not be accessing multi-tenant IQ via localhost. Use a fake vanity URL");
    }

    if (!isSupportedUrl(serverName)) {
      throw new RuntimeException("Unsupported URL. Supported URLs must contain a tenant identifying slug");
    }
  }

  private static boolean isSupportedUrl(String serverName) {
    if (serverName.contains(".")) {
      return true;
    }

    return false;
  }
}
