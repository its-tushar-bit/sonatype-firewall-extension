/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scm.event;

/**
 * Event types for source control operations logging
 */
public enum SourceControlEventType
{
  PR_COMMENT_CREATED,
  PR_COMMENT_UPDATED,
  PR_CREATED,
  API_ERROR
}
