/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

/**
 * Constants for Resource Endpoints of Application Summary for Integrations.
 *
 * @since 1.98
 */
public class ApplicationSummaryResourceConstants
{
  public static final String RESOURCE_PATH = "rest/integration/applications";

  public static final String VERIFY_OR_CREATE_APPLICATION_PATH = "verifyOrCreate/{applicationPublicId}";

  public static final String APPLICATION_PUBLIC_ID_PARAM = "applicationPublicId";

  public static final String GOAL_PARAM = "goal";

  public static final String ORG_ID_PARAM = "organizationId";

  private ApplicationSummaryResourceConstants() {
  }
}
