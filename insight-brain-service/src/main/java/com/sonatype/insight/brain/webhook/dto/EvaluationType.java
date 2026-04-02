/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.webhook.dto;

/**
 * Evaluation type for Application Evaluation webhooks.
 * Distinguishes between Lifecycle application evaluations and Firewall container evaluations.
 *
 */
public enum EvaluationType
{
  /**
   * Application evaluation in Lifecycle context
   */
  APPLICATION,

  /**
   * Container evaluation in Firewall context (Repository)
   */
  CONTAINER
}
