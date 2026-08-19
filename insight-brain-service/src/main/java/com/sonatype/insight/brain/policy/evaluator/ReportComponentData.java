/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.report.LifecycleReport;

public class ReportComponentData
{
  public final LifecycleReport lifecycleReport;

  public final List<Component> components;

  public ReportComponentData(final LifecycleReport lifecycleReport, final List<Component> components) {
    this.lifecycleReport = lifecycleReport;
    this.components = components;
  }
}
