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

  private static final String API_VERSION_PATH_V2 = BASE_PATH + "/v2/";

  public static final String APP_RESOURCE_PATH = API_VERSION_PATH_V2 + "applications";

  public static final String ORG_RESOURCE_PATH = API_VERSION_PATH_V2 + "organizations";

  public static final String POLICY_RESOURCE_PATH = API_VERSION_PATH_V2 + "policies";

  public static final String POLICY_VIOLATION_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "policyViolations";

  public static final String SEARCH_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "search/component";

  public static final String REPORTS_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "reports";

  public static final String REPORT_DATA_RESOURCE_PATH_V2 = API_VERSION_PATH_V2
      + "applications/{applicationPublicId}/reports/{scanId}";

  public static final String APPLICATION_EVALUATION_PATH_V2 = API_VERSION_PATH_V2 + "evaluation/applications";

  public static final String PROMOTE_SCAN_STATUS_PATH_V2 =
      APPLICATION_EVALUATION_PATH_V2 + "/{applicationId}/status/{statusId}";

  public static final String COMPONENT_DETAILS_PATH_V2 = API_VERSION_PATH_V2 + "components/details";

  public static final String COMPONENT_VERSIONS_PATH_V2 = API_VERSION_PATH_V2 + "components/versions";

  public static final String APP_COMPONENT_LABELS_PATH_V2 = API_VERSION_PATH_V2
      + "components/{componentHash}/labels/{labelName}/applications/{applicationId}";

  public static final String DATA_RETENTION_POLICY_RESOURCE_PATH = API_VERSION_PATH_V2 + "dataRetentionPolicies";

  public static final String COMPONENT_REMEDIATION_PATH_V2 =
      API_VERSION_PATH_V2 + "components/remediation/{ownerType: application|organization}/{ownerId}";

  public static final String PROXY_CONFIG_PATH_V2 = API_VERSION_PATH_V2 + "config/proxy";

  public static final String SOURCE_CONTROL_PATH_V2 = API_VERSION_PATH_V2 + "sourceControl";

  public static final String USER_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "users";

  public static final String ROLE_MEMBERSHIP_PATH_V2 = API_VERSION_PATH_V2 + "roleMemberships";

  public static final String ROLE_RESOURCE_PATH_V2 = API_VERSION_PATH_V2 + "roles";
}
