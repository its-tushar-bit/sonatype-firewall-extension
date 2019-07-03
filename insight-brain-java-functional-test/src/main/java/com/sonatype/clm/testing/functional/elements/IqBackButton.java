/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

package com.sonatype.clm.testing.functional.elements;

import com.sonatype.clm.testing.functional.BasicElement;

public class IqBackButton
    extends BasicElement<IqBackButton>
{
  /**
   * @param context the context selector, within which the back-button is located
   */
  public IqBackButton(String context) {
    super(context, ".iq-back-button");
  }

  @Override
  public IqBackButton click() {
    child("a").click();
    return me();
  }
}
