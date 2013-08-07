/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.label;

import java.util.Set;

import com.sonatype.insight.brain.model.label.Color;

public class ComponentLabelState
{
  private Color color;

  private Set<String> labels;

  public Color getColor() {
    return color;
  }

  public Set<String> getLabels() {
    return labels;
  }

  public void setColor(Color color) {
    this.color = color;
  }

  public void setLabels(Set<String> labels) {
    this.labels = labels;
  }

}
