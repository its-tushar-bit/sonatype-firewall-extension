/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.integration;

/**
 * Constants for Resource Endpoints of Application Evaluation for Integrations.
 *
 * @since 1.97
 */
public class ApplicationEvaluationResourceConstants
{
  public static final String RESOURCE_PATH = "rest/integration/applications/{applicationPublicId}/evaluations";

  public static final String EVALUATE_PATH = "{integrationType: ci|cli|rm}/stages/{stageId}";

  public static final String STATUS_PATH = "status/{statusId}";

  private ApplicationEvaluationResourceConstants() {
  }
}
