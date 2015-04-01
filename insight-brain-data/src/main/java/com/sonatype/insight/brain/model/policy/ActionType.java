/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.model.policy;

import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.insight.brain.model.ValidationResult;

public interface ActionType
{
  String getId();

  String getName();

  boolean isRequiresTarget();

  String getSummary();

  ValidationResult validateAction(Action action);
}
