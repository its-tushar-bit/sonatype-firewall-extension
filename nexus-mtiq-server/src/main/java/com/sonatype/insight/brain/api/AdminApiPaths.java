/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

public class AdminApiPaths
{
  public static final String ADMIN_PATH = "admin/";

  public static final String ADMIN_CONFIG_FEATURES_PATH = ADMIN_PATH + "config/features";

  public static final String ADMIN_TENANT_PROVISIONING_PATH = ADMIN_PATH + "tenants/{tenantSlug}";

  public static final String ADMIN_TENANT_LICENSE_PATH = ADMIN_PATH + "tenants/{tenantSlug}/license";
}
