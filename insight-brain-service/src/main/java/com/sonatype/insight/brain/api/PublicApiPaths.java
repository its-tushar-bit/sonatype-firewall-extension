/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

/**
 * @since 1.11.0
 */
public class PublicApiPaths
{
  /**
   * @deprecated V1 API since 1.12.0
   */
  private static final String API_VERSION_PATH = "api/v1/";

  private static final String API_VERSION_PATH_V2 = "api/v2/";

  private static final String API_VERSION_PATH_V1_V2 = "api/{apiVersion: v1|v2}/";

  public static final String APP_SERVICE_PATH = API_VERSION_PATH_V1_V2 + "applications";

  public static final String ORG_SERVICE_PATH = API_VERSION_PATH_V1_V2 + "organizations";

  public static final String POLICY_SERVICE_PATH = API_VERSION_PATH_V1_V2 + "policies";

  public static final String POLICY_VIOLATION_SERVICE_PATH = API_VERSION_PATH + "policyViolations";

  public static final String POLICY_VIOLATION_SERVICE_PATH_V2 = API_VERSION_PATH_V2 + "policyViolations";

  public static final String SEARCH_SERVICE_PATH = API_VERSION_PATH + "search/component";

  public static final String SEARCH_SERVICE_PATH_V2 = API_VERSION_PATH_V2 + "search/component";

}
