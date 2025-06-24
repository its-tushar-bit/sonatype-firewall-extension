/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sourcecontrol;

public enum PullRequestSource
{
  /**
   * Indicates a PR created outside of IQ
   */
  EXTERNAL,
  /**
   * Indicates a PR created automatically by IQ
   */
  AUTOMATIC,
  /**
   * Indicates a PR for an InnerSource component created automatically by IQ
   */
  AUTOMATIC_INNER_SOURCE,
  /**
   * Indicates a PR created manually by a user through IQ
   */
  MANUAL,
  /**
   * Indicates a PR for an InnerSource component created manually by a user through IQ
   */
  MANUAL_INNER_SOURCE
}
