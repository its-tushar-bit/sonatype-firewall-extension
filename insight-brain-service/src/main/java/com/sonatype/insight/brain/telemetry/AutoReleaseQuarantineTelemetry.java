/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.telemetry;

import java.util.ArrayList;
import java.util.List;

public class AutoReleaseQuarantineTelemetry
{
  public final List<String> enabledConditionTypes = new ArrayList<>();

  public final List<String> disabledConditionTypes = new ArrayList<>();
}
