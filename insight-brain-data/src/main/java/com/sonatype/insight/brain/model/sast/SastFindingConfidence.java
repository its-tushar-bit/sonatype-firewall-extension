/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.sast;

// NOTE: Changing the order of these enum values will require a
// database migration since the database stores the ordinal value
// and not the text
public enum SastFindingConfidence
{
  LOW,
  MEDIUM,
  HIGH;
}
