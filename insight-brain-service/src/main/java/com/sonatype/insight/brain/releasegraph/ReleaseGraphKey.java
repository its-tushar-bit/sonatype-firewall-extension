/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.releasegraph;

import java.util.Objects;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.ComponentPopularity;

public class ReleaseGraphKey
{
  private ComponentIdentifier componentIdentifier;

  private ReportItemKey reportKey;

  public ReleaseGraphKey(ComponentIdentifier componentIdentifier, ReportItemKey reportKey) {
    this.componentIdentifier = componentIdentifier;
    this.reportKey = reportKey;
  }

  @Override
  public int hashCode() {
    return Objects.hash(reportKey, componentIdentifier);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ReleaseGraphKey)) {
      return false;
    }
    ReleaseGraphKey that = (ReleaseGraphKey) obj;
    return Objects.equals(reportKey, that.reportKey) && Objects.equals(componentIdentifier, that.componentIdentifier);
  }

  public ReportItemKey getReportItemKey() {
    return reportKey;
  }

  boolean isMatch(ComponentPopularity component) {
    return Objects.equals(componentIdentifier, component.getComponentIdentifier());
  }

  ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }
}
