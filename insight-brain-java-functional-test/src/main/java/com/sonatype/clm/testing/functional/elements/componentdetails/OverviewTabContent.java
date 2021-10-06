/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

public class OverviewTabContent
    extends BasicElement<OverviewTabContent>
{
  public static final String OVERVIEW_TAB_SELECTOR = "#component-details-overview-tab-content";

  public OverviewTabContent() {
    super(OVERVIEW_TAB_SELECTOR);
  }

  public ComponentInformationTile componentInformationTile() {
    return ComponentInformationTile.getOverviewTileForParent(OVERVIEW_TAB_SELECTOR);
  }

  public RiskRemediationTile riskRemediationTile() {
    return RiskRemediationTile.getOverviewTileForParent(OVERVIEW_TAB_SELECTOR);
  }
}
