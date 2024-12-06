/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.report.ApplicationReport;

public class ReportComponentData
{
  public final ApplicationReport applicationReport;

  public final List<Component> components;

  public ReportComponentData(final ApplicationReport applicationReport, final List<Component> components) {
    this.applicationReport = applicationReport;
    this.components = components;
  }
}
