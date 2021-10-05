/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

public class LegalTabContent
    extends BasicElement<LegalTabContent>
{
  public static final String LEGAL_TAB_SELECTOR = "#component-details-legal-tab-content";

  public LegalTabContent() {
    super(LEGAL_TAB_SELECTOR);
  }

  public PolicyViolationsTable policyViolationsTable() {
    return PolicyViolationsTable.getPolicyViolationsTableForParent(LEGAL_TAB_SELECTOR);
  }

  public LicenseDetectionsTile licenseDetectionsTile() {
    return LicenseDetectionsTile.getLicenseDetectionsTileForParent(LEGAL_TAB_SELECTOR);
  }
}
