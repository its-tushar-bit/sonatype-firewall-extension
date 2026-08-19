/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

public class Tooltip
    extends BasicElement<Tooltip>
{
  private static final String DEFAULT_SELECTOR = ".tooltip, .nx-tooltip";

  public Tooltip(String... selectors) {
    super(selectors);
  }

  public static Tooltip get() {
    return new Tooltip(DEFAULT_SELECTOR);
  }
}
