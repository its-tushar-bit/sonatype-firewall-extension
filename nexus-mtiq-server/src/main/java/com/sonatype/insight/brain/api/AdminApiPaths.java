/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

public class AdminApiPaths
{
  public static final String ADMIN_PATH = "admin/";

  public static final String ADMIN_TENANT_CONFIG_FEATURES_PATH = ADMIN_PATH + "tenants/{tenantSlug}/config/features";

  public static final String ADMIN_TENANT_PROVISIONING_PATH = ADMIN_PATH + "tenants/{tenantSlug}";

  public static final String ADMIN_TENANT_LICENSE_PATH = ADMIN_PATH + "tenants/{tenantSlug}/license";

  public static final String ADMIN_SUPPORT_INFO_PATH = ADMIN_PATH + "tenants/{tenantSlug}/supportInfo";

  public static final String ADMIN_TENANT_SECURITY_CONFIG_PATH = ADMIN_PATH + "tenants/{tenantSlug}/security";

  public static final String ADMIN_TENANT_SCHEMA_PATH = ADMIN_PATH + "tenants/{tenantSlug}/schema";

  public static final String ADMIN_CONFIG_PATH = ADMIN_PATH + "tenants/{tenantSlug}/config";

  public static final String ADMIN_TENANT_METADATA_PATH = ADMIN_PATH + "tenants/{tenantSlug}/metadata";

  public static final String ADMIN_TENANT_CACHE_PATH = ADMIN_PATH + "tenants/{tenantSlug}/cache";

  public static final String ADMIN_TENANT_SSO_CONFIGURATION_PATH = ADMIN_PATH + "tenants/{tenantSlug}/sso";

  public static final String ADMIN_ANNOUNCEMENT_BANNER_PATH =
      ADMIN_PATH + "tenants/{tenantSlug}/announcement-banner";
}
