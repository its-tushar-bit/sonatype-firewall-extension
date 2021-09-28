/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.functional.elements.componentdetails;

import com.sonatype.clm.testing.functional.BasicElement;

public class SecurityTabContent
    extends BasicElement<SecurityTabContent>
{
  public static final String SECURITY_TAB_SELECTOR = "#component-details-security-tab-content";

  public SecurityTabContent() {
    super(SECURITY_TAB_SELECTOR);
  }

  public PolicyViolationsTable policyViolationsTable() {
    return PolicyViolationsTable.getPolicyViolationsTableForParent(SECURITY_TAB_SELECTOR);
  }

  public VulnerabilitiesTable vulnerabilitiesTable() {
    return VulnerabilitiesTable.getVulnerabilitiesTableForParent(SECURITY_TAB_SELECTOR);
  }
}
