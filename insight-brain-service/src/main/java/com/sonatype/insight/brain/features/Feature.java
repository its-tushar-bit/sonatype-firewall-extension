/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.features;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Denotes a feature of the CLM server.
 * 
 * @since 1.9
 */
public enum Feature
{
  POLICY, LABELS, RELEASE_GRAPH, POLICY_VIOLATIONS, NOTIFICATIONS, REEVALUATE_POLICY, POLICY_MONITORING;

  @Override
  @JsonValue
  public String toString() {
    return name().toLowerCase(Locale.ENGLISH).replace('_', '-');
  }
}