/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.policy.evaluator;

import java.util.List;

import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.report.Report;

public class ReportComponentData
{
  public final Report reportFile;

  public final List<Component> components;

  public ReportComponentData(final Report reportFile, final List<Component> components) {
    this.reportFile = reportFile;
    this.components = components;
  }
}
