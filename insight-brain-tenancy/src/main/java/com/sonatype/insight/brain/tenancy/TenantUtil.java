/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tenancy;

import jakarta.inject.Named;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;

import com.sonatype.insight.error.exception.BadRequestException;

import com.google.common.net.InetAddresses;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static com.sonatype.insight.brain.tenancy.Tenant.SINGLE_TENANT;

@Named
public class TenantUtil
{
  static final String TENANT_DOES_NOT_EXIST = "Tenant does not exist";

  private static final Logger log = LoggerFactory.getLogger(TenantUtil.class);

  static final String IS_MTIQ_BATCH = "IS_MTIQ_BATCH";

  //Visible for testing
  Boolean mtiqBatchMode;

  public boolean isAllTenantsJob(Class clazz) {
    return AllTenantsJob.class.isAssignableFrom(clazz);
  }

  public boolean isMtiqBatchJob(Class clazz) {
    return MtiqBatchJob.class.isAssignableFrom(clazz);
  }

  Tenant validateTenant(Tenant tenant) {
    return validateTenantForType(null, tenant);
  }

  /*
      MTIQ - None of the errors in this validation method should ever be hit once multi-tenancy is complete.
      During MTIQ development this can be used as a catch-all for any tenancy issues which still need to be addressed.
      Most commonly for system initiated events you need to call TenantUtils.initTenancy
   */
  public Tenant validateTenantForType(Class clazz, Tenant tenant) {
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

    // Skip validation if clazz can be used in either a per-tenant or global context
    if (GlobalTenantJob.class.isAssignableFrom(clazz)
        && TenantManaged.class.isAssignableFrom(clazz)
        && !AllTenantsJob.class.isAssignableFrom(clazz)) {
      return tenant;
    }

    if (AllTenantsJob.class.isAssignableFrom(clazz) && !GLOBAL_TENANT.equals(tenant)) {
      throw new InvalidTenantForJobTypeException("AllTenantJob(s) cannot be created against a non-global tenant. " +
          "Type=" + clazz.getSimpleName() + ", Tenant=" + tenant);
    }
    else if (GlobalTenantJob.class.isAssignableFrom(clazz) && !GLOBAL_TENANT.equals(tenant)) {
      logTenancyIssue("GlobalTenantJob was invoked which expects a global tenant to be set but instead a specific " +
          "tenant was set: " + clazz);
    }
    else if (!GlobalTenantJob.class.isAssignableFrom(clazz) && TenantManaged.class.isAssignableFrom(clazz)
        && !isAllTenantsJob(clazz) && GLOBAL_TENANT.equals(tenant)) {
      logTenancyIssue("TenantJob was invoked which expects a specific tenant to be set but instead global " +
          "tenant was set: " + clazz);
    }
    else if (!GlobalTenantJob.class.isAssignableFrom(clazz) && !TenantManaged.class.isAssignableFrom(clazz)) {
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

  private static void validateTenantName(String serverName) {
    if ("localhost".equals(serverName)) {
      throw new BadRequestException("You should not be accessing multi-tenant IQ via localhost. Use a fake vanity URL");
    }

    if (serverName.startsWith(GLOBAL_TENANT.tenantSlug + ".")) {
      throw new BadRequestException(TENANT_DOES_NOT_EXIST);
    }

    if (serverName.startsWith(SINGLE_TENANT.tenantSlug + ".")) {
      throw new BadRequestException(TENANT_DOES_NOT_EXIST);
    }

    if (!isSupportedUrl(serverName)) {
      throw new BadRequestException("Unsupported URL. Supported URLs must contain a tenant identifying slug");
    }
  }

  private static boolean isSupportedUrl(String serverName) {
    return serverName.contains(".");
  }

  protected static boolean isAdminApiRequest(ServletRequest request) {
    if (request instanceof HttpServletRequest) {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      String path = httpRequest.getPathInfo();
      String pathWithContext = httpRequest.getRequestURI();

      return path != null && pathWithContext != null && pathWithContext.startsWith("/api") && path.startsWith("/admin");
    }

    return false;
  }

  public boolean isGlobalTenant(String tenantSlug) {
    return GLOBAL_TENANT.tenantSlug.equals(tenantSlug);
  }

  public boolean isGlobalTenant() {
    return GLOBAL_TENANT.equals(validateTenant(TenantThreadLocal.getTenant()));
  }

  /**
   * Extracts the tenant slug from the vanity url. The first part of the URL (before the first .) is the tenant slug.
   *
   * @param serverName - server name not including http://
   * @return the tenant slug
   */
  public String getTenantName(final String serverName) {
    validateTenantName(serverName);

    return serverName.substring(0, serverName.indexOf("."));
  }

  public boolean isSingleTenant() {
    return SINGLE_TENANT.equals(TenantThreadLocal.getTenantWithoutValidation());
  }

  public boolean isMultiTenant() {
    return !isSingleTenant();
  }

  public void setGlobalTenant() {
    TenantThreadLocal.setGlobalTenant();
  }

  public String getTenantSlugForSynchronization() {
    return TenantThreadLocal.getTenantWithoutValidation().tenantSlug.intern();
  }

  /**
   * When the node is running as a Mtiq Batch it is responsible for running quartz jobs that implement AllTenantsJob
   *
   * @return - is this instance a mtiq batch
   */
  public boolean isMtiqBatchMode() {
    if (mtiqBatchMode == null) {
      mtiqBatchMode = Boolean.parseBoolean(System.getenv(IS_MTIQ_BATCH));
    }
    return mtiqBatchMode;
  }

  /**
   * Admin API requests should use the global tenant.  Additionally, assume that calls to a IP-address hostname are to
   * admin APIs (probably /healthcheck)
   */
  public boolean requestShouldUseGlobalTenant(ServletRequest request) {
    return isAdminApiRequest(request) || InetAddresses.isInetAddress(request.getServerName());
  }

  public static String getTenantNameFromSchema(String schema) {
    return schema.replaceFirst("t_", "").replace('_', '-');
  }

  public boolean isCustomerTenantInBatchMode() {
    return isMtiqBatchMode() && !isGlobalTenant();
  }

  /**
   * Validate that there is no customer tenant set. i.e. tenant is `global` for MTIQ or `single` for on-prem
   */
  public void validateNoCustomerTenantSet() {
    if (isGlobalTenant() || isSingleTenant()) {
      return;
    }

    throw new RuntimeException(String.format("Found tenant '%s' but was expecting no tenant to be set",
        TenantThreadLocal.getTenantWithoutValidation().tenantSlug));
  }

  static class InvalidTenantForJobTypeException
      extends RuntimeException
  {
    public InvalidTenantForJobTypeException(String message) {
      super(message);
    }
  }
}
