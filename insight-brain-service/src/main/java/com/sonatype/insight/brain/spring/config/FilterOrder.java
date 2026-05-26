/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

/**
 * Centralized filter ordering constants for single-tenant and multi-tenant contexts.
 * <p>
 * Filter order determines the sequence in which filters are applied to requests.
 * Lower numbers execute first. This class defines shared constants used across
 * multiple filter configuration classes to ensure consistent ordering.
 * <p>
 * Note: The MTIQ admin context ({@link com.sonatype.insight.brain.service.MtiqAdminFilterConfiguration})
 * uses a separate numbering scheme (values 100+) for its admin-specific filters, but references
 * shared constants for common filters where applicable.
 */
public final class FilterOrder
{
  private FilterOrder() {
    // Utility class
  }

  // Core filters (0-9) - exception handling and request preprocessing
  public static final int THROWABLE_HANDLER = 0;

  public static final int GZIP_REQUEST_DECOMPRESSION = 1;

  public static final int LEGACY_WEB_CORS = 4;

  public static final int HTTP_HEADER_VALIDATION = 5;

  // Request context filters (10-29) - headers, counters, tenant context
  public static final int FORWARDED_HEADER = 10;

  public static final int ACTIVE_REQUEST_COUNTER = 10;

  public static final int TENANT_URL = 11;

  public static final int CONSUMPTION_CONTEXT = 12;

  public static final int SERVER_HEADER = 20;

  public static final int PLATFORM_CONTEXT = 25;

  public static final int STATIC_ASSETS_CHARSET = 25;

  // URL/context filters (30-39)
  public static final int BASE_URL = 30;

  // Audit and security header filters (40-59)
  public static final int AUDIT = 40;

  public static final int CONTENT_TYPE_OPTIONS = 46;

  public static final int HSTS = 47;

  public static final int FRAME_OPTIONS = 48;

  public static final int MCP_LICENSE = 54;

  // Cache and logging filters (60-79)
  public static final int INDEX_CACHE_CONTROL = 60;

  public static final int AUTHENTICATION_LOGGING = 70;

  // CSP and security policy filters (80-99)
  public static final int CSP = 80;

  public static final int CSP_FRAME = 90;

  public static final int LEGACY_WEB_HEADERS = 92;

  public static final int FIREWALL_REDIRECT = 95;

  // MTIQ admin context uses separate range (100+)
  // See MtiqAdminFilterConfiguration for admin-specific constants
}
