/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

public class NxBackButton
    extends BasicElement<NxBackButton>
{
  /**
   * @param context the context selector, within which the back-button is located
   */
  public NxBackButton(String context) {
    super(context, ".nx-back-button");
  }
}
