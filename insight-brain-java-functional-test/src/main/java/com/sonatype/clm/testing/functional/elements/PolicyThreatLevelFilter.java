/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

public class PolicyThreatLevelFilter
    extends NxTreeViewMultiSelect
{
  public PolicyThreatLevelFilter(final String selector) {
    super(selector);
  }

  public NxThreatLevelSlider slider() {
    return new NxThreatLevelSlider(childSelector(".nx-policy-threat-slider"));
  }
}
