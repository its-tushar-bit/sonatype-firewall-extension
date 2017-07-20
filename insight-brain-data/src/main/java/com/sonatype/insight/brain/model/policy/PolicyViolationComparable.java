/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;

/**
 * @since 1.33
 */
public interface PolicyViolationComparable
{
  String getPolicyId();

  String getPolicyName();

  int getThreatLevel();

  String getHash();

  ComponentIdentifier getComponentIdentifier();
}
