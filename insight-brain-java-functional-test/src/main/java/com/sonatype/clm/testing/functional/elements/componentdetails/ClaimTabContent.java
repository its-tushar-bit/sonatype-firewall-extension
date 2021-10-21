/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

public class ClaimTabContent
    extends BasicElement<ClaimTabContent>
{
  public static final String CLAIM_TAB_SELECTOR = "#component-details-claim-unknown-component";

  public ClaimTabContent() {
    super(CLAIM_TAB_SELECTOR);
  }
}
