/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api;

/**
 * @since 1.11.0
 */
public class PublicApiPaths
{
  public static final String BASE_PATH = "api";

  /**
   * @deprecated V1 API since 1.12.0
   */
  @Deprecated
  private static final String API_VERSION_PATH = BASE_PATH + "/v1/";

  private static final String API_VERSION_PATH_V2 = BASE_PATH + "/v2/";

  private static final String API_VERSION_PATH_V1_V2 = BASE_PATH + "/{apiVersion: v1|v2}/";

  public static final String APP_SERVICE_PATH = API_VERSION_PATH_V1_V2 + "applications";

  public static final String ORG_SERVICE_PATH = API_VERSION_PATH_V1_V2 + "organizations";

  public static final String POLICY_SERVICE_PATH = API_VERSION_PATH_V1_V2 + "policies";

  public static final String POLICY_VIOLATION_SERVICE_PATH = API_VERSION_PATH + "policyViolations";

  public static final String POLICY_VIOLATION_SERVICE_PATH_V2 = API_VERSION_PATH_V2 + "policyViolations";

  public static final String SEARCH_SERVICE_PATH = API_VERSION_PATH + "search/component";

  public static final String SEARCH_SERVICE_PATH_V2 = API_VERSION_PATH_V2 + "search/component";

  public static final String REPORTS_SERVICE_PATH_V2 = API_VERSION_PATH_V2 + "reports";

  public static final String REPORT_DATA_SERVICE_PATH = API_VERSION_PATH +
      "applications/{applicationPublicId}/reports/{scanId}";

  public static final String REPORT_DATA_SERVICE_PATH_V2 = API_VERSION_PATH_V2 +
      "applications/{applicationPublicId}/reports/{scanId}";

  public static final String APPLICATION_EVALUATION_PATH_V2 = API_VERSION_PATH_V2 + "evaluation/applications";

  public static final String COMPONENT_DETAILS_PATH_V2 = API_VERSION_PATH_V2 + "components/details";
}
